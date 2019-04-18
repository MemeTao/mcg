package cn.pgyyd.mcg.verticle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.sql.SQLConnection;

public class MySqlVerticle extends AbstractVerticle {
    private static int DEFAULT_POOL_SIZE = 10;
    
    final public static String EXEC = "mysql-exec";
    
    final public static String QUERY = "mysql-query";
    
    public static String TRANSACTION = "mysql-transaction";
    
    private int connection_pool_size = DEFAULT_POOL_SIZE;
    
    private List<SQLConnection> connections_idle;
    
    private List<SQLConnection> connections_busy;
    
    private AsyncSQLClient client;
    
    /*简单的包裹一下task
     * */
    class TaskOp{
        public TaskOp(final String t,final Message<Object> m,final String operation) {
            task = t;
            mess = m;
            op = operation;
        }
        final String task;
        final String op;
        final Message<Object> mess;
    }
    class TaskTransaction{
        public TaskTransaction(final List<String> ts,final Message<Object> m) {
            tasks = ts;
            mess = m;
        }
        final List<String> tasks;
        final Message<Object> mess;
    }
    /**假设1纳秒自增一次，64位的整数也够执行584年
     * 好吧，297年
     * */
    private long accounter_tasks = 0;  
    private long accounter_transaction = 0;
    
    private TreeMap<Long,TaskOp> tasks;
    private TreeMap<Long,TaskTransaction> tasks_transaction;
    
    @Override
    public void start() throws Exception {
        String host = Vertx.currentContext().config().getString("host_mysql");
        Integer port = Vertx.currentContext().config().getInteger("port_mysql");
        Integer connection_num = Vertx.currentContext().config().getInteger("connections_mysql");
        
        if(host == null) {
            host = "127.0.0.1";
        }
        
        if(port == null) {
            port = 8080;
        }
        
        if( connection_num != null && connection_num.intValue() > 0) {
            connection_pool_size = connection_num.intValue();
        }

        JsonObject mySQLClientConfig = new JsonObject().put("host", host).put("port",port).
                                                    put("user","mcg").put("password","admin_mcg").
                                                    put("max_pool_size",connection_pool_size);
        
        /*异步执行的情况下，一个mysql业务线程是不是就够了呢*/
        client = MySQLClient.createNonShared(vertx, mySQLClientConfig);
        connections_idle = new LinkedList<SQLConnection>();
        connections_busy = new LinkedList<SQLConnection>();
        
        for(int i = 0 ;i < connection_num; i++) {
            client.getConnection(res -> {
                if (res.succeeded()) {
                    SQLConnection conn = res.result();
                    connections_idle.add(conn);
                }
                else {
                    connection_pool_size--;
                    //logger("unexpected error happened");
                }
              });
        }

        tasks = new TreeMap<Long,TaskOp>();
        tasks_transaction = new TreeMap<Long,TaskTransaction>();
        
        vertx.eventBus().consumer(EXEC,message->{
            String op = message.body().toString();
            tasks.put( accounter_tasks ++, new TaskOp(op,message,EXEC));
            schedule();
        });

        vertx.eventBus().consumer(QUERY,message->{
            String op = message.body().toString();
            tasks.put(accounter_tasks ++, new TaskOp(op,message,QUERY));
            schedule();
        });
        
        vertx.eventBus().consumer(TRANSACTION,message->{
            @SuppressWarnings("unchecked")
            List<String> ops = (List<String>) message.body();
            tasks_transaction.put(accounter_transaction,new TaskTransaction(ops,message));
            schedule();
        });
    }
    /**
     * @param <T>
     * @param conn 分配到的数据库连接
     * @param time 创建时间
     * @param op   待执行操作
     * @param message 
     */
    private <T> void execute(SQLConnection conn,Long time,String op,Message<T> message) {
        conn.execute(op, res->{
            if(res.succeeded()) {
                /*获取结果并返回*/
                message.reply(res.result());
            }
            /**释放该链接，重新调度任务*/
            reSchedule(conn);
        });
    }
    private <T> void query(SQLConnection conn,Long time,String op,Message<T> message) {
        conn.query(op, res->{
            if(res.succeeded()) {
                /*获取结果并返回*/
                message.reply(res.result());
            }
            /**释放该链接，重新调度任务*/
            reSchedule(conn);
        });
    }
    /**
     * @param <T>
     * @param conn  分配到的数据库连接
     * @param time  创建时间
     * @param ops   待执行事务
     * @param message
     */
    /*FIXME:当前的实现只支持update操作*/
    private <T> void sql_transaction(SQLConnection conn,Long time,List<String> ops,Message<T> message) {
        List<Future<Void>> futures = new ArrayList<Future<Void>>();
        Future<Void> f_b =  Future.future();
        /**准备事务*/
        conn.setAutoCommit(false, f_b.completer());
        futures.add(f_b);
        for(String op : ops) {
            Future<Void> future = Future.future();
            futures.add(future);
            /*FIXME: 这里添加上op字段，也可以实现多种类型*/
            conn.update(op,res->{
                if(res.succeeded()) {
                    future.complete();
                }
                else {
                    future.fail(res.cause().toString());
                }
            });
        }
        Future<Void> f_e =  Future.future();
        futures.add(f_b);
        /**提交事务*/
        conn.commit(f_e.completer());
        /**立即恢复auto-commit属性*/
        conn.setAutoCommit(true,null);
        
        CompositeFuture.all(new ArrayList<>(futures)).setHandler(res->{
            if(res.succeeded()) {
                message.reply(res.result());
            }
            else {
                conn.rollback(null);
                message.fail(-1, res.cause().toString());
            }
            /**释放该链接，重新调度任务*/
            reSchedule(conn);
        });
    }
    
    /**需要这两个操作的原因：在之前的实现中，"事务"和"普通操作"都有被对方饿死的可能
     * 比如说，来了一个事务，但是此时所有连接都被"普通sql操作"沾满，后续又不断的插入新的"普通sql操作"，
     * 那么事务就永远得不到执行。
     * 
     * 调度的策略:
     *    先到先执行
     * @param conn
     */
    private void reSchedule(SQLConnection conn) {
        connections_idle.add(conn);
        connections_busy.remove(conn);
        schedule();
    }
    private void schedule() {
        
        SQLConnection conn = null;
        
        if(connections_idle.size() > 0 && (tasks_transaction.size() > 0 || tasks.size() >0 )) {
            //FIXME: 获取空闲连接(取最近刚活动过的)
            conn = connections_idle.get(connections_idle.size()-1);
            connections_busy.add(conn);
        }
        else {
            //logger("no mysql client idle now");
            return;
        }
        
        long latest_access_tasks = Long.MAX_VALUE;
        long latest_access_transaction = Long.MAX_VALUE;
        
        if( tasks.size() > 0) {
            latest_access_tasks = tasks.firstKey();
        }
        if( tasks_transaction.size() > 0) {
            latest_access_transaction = tasks_transaction.firstKey();
        }
        if(latest_access_tasks < latest_access_transaction) {
            if(tasks.size() > 0) {
                /*取出一个，接着调用execute_sql*/
                Entry<Long,MySqlVerticle.TaskOp> next = null;
                for(Entry<Long,MySqlVerticle.TaskOp> it : tasks.entrySet()) {
                    if(it != null) {
                        next = it;
                        break;
                    }
                }
                tasks.remove(next.getKey()); //为了防止某操作一直未执行成功，又被其它连接执行，造成重复执行的bug
                final String op = next.getValue().op;
                switch(op) {
                case EXEC:
                    execute(conn,next.getKey(),next.getValue().task,next.getValue().mess);
                    break;
                case QUERY:
                    query(conn,next.getKey(),next.getValue().task,next.getValue().mess);
                    break;
                default:
                    break;
                }
            }
        }
        else {
            if(tasks_transaction.size() > 0) {
                Entry<Long,MySqlVerticle.TaskTransaction> next = null;
                for(Entry<Long,MySqlVerticle.TaskTransaction> it : tasks_transaction.entrySet()) {
                    if(it != null) {
                        next = it;
                        break;
                    }
                }
                tasks_transaction.remove(next.getKey());
                sql_transaction(conn,next.getKey(),next.getValue().tasks,next.getValue().mess);
            }
        }
    }
}
