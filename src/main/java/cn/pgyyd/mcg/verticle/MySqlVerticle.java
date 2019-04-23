package cn.pgyyd.mcg.verticle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import cn.pgyyd.mcg.module.MysqlMessage.CompositeMessage;
import cn.pgyyd.mcg.module.MysqlMessage.ExecuteMessage;
import cn.pgyyd.mcg.module.MysqlMessage.QueryMessage;
import cn.pgyyd.mcg.module.MysqlMessage.UpdateMessage;
import cn.pgyyd.mcg.module.UserMessageCodec;

import java.util.TreeMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.sql.SQLConnection;
/**
 * 这个类提供mysql的连接管理以及数据库请求的控制
 * @author memetao
 */
public class MySqlVerticle extends AbstractVerticle {
    private static int DEFAULT_POOL_SIZE = 10;
    
    final public static String EXEC = "mysql-exec";
    
    final public static String QUERY = "mysql-query";
    
    final public static String UPDATE = "musql-update";

    
    public static String TRANSACTION = "mysql-transaction";
    
    private int connection_pool_size = DEFAULT_POOL_SIZE;
    
    private List<SQLConnection> connections_idle;
    
    private List<SQLConnection> connections_busy;
    
    private AsyncSQLClient client;
    
    /*简单的包裹一下task
     * */
    class TaskOp{
        public TaskOp(final String t,Message<Object> m,final String operation) {
            task = t;
            mess = m;
            op = operation;
        }
        final String task;
        final String op;
        Message<Object> mess;
    }
    class TaskTransaction{
        public TaskTransaction(final List<String> ts,Message<Object> m) {
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
        //String host = Vertx.currentContext().config().getString("host_mysql");
        String host = null;
        //Integer port = Vertx.currentContext().config().getInteger("port_mysql");
        Integer port = null;
        //Integer connection_num = Vertx.currentContext().config().getInteger("connections_mysql");
        int connection_num = 1;
        if(host == null) {
            host = "127.0.0.1";
        }
        
        if(port == null) {
            port = 3306;
        }
        
        JsonObject mySQLClientConfig = new JsonObject().put("host", host).put("port",port).
                                                    put("username","memetao").put("password","123456").
                                                    put("maxPoolSize",connection_pool_size).
                                                    put("database","mcg").
                                                    put("queryTimeout",1000);   /*查询操作，一秒超时*/ 
        /*异步执行的情况下，一个mysql业务线程是不是就够了呢*/
        client = MySQLClient.createShared(vertx, mySQLClientConfig);

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
                    System.out.println("[error] get mysql connection fail!");
                }
              });
        }
        
        tasks = new TreeMap<Long,TaskOp>();
        tasks_transaction = new TreeMap<Long,TaskTransaction>();
        
        vertx.eventBus().registerCodec(new UserMessageCodec.MysqlQuery());
        vertx.eventBus().registerCodec(new UserMessageCodec.MysqlUpdate());
        vertx.eventBus().registerCodec(new UserMessageCodec.MysqlExecute());
        vertx.eventBus().registerCodec(new UserMessageCodec.MysqlComposite());

        vertx.eventBus().consumer(EXEC,message->{
            ExecuteMessage mess = (ExecuteMessage) message.body();
            tasks.put( accounter_tasks ++, new TaskOp(mess.operation(),message,EXEC));
            schedule();
        });
        
        vertx.eventBus().consumer(UPDATE,message->{
            UpdateMessage mess = (UpdateMessage) message.body();
            tasks.put( accounter_tasks ++, new TaskOp(mess.operation(),message,UPDATE));
            schedule();
        });
        
        vertx.eventBus().consumer(QUERY,message->{
            QueryMessage mess = (QueryMessage) message.body();
            tasks.put( accounter_tasks ++, new TaskOp(mess.operation(),message,QUERY));
            schedule();
        });
        
        vertx.eventBus().consumer(TRANSACTION,message->{
            CompositeMessage mess = (CompositeMessage) message.body();
            tasks_transaction.put(accounter_transaction,new TaskTransaction(mess.operations(),message));
            schedule();
        });
    }
    /**
     * @param <T>
     * @param conn 分配到的数据库连接
     * @param op   待执行操作
     * @param message 
     */
    private <T> void execute(SQLConnection conn,String op,Message<T> message) {
        conn.execute(op, res->{
            System.out.println("[info] mysql execute:" + op);
             /*获取结果并返回*/
             ExecuteMessage result = new ExecuteMessage(res);
             message.reply(result,new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlExecute().name()));
            reSchedule(conn);
        });
    }
    
    private <T> void query(SQLConnection conn,String op,Message<T> message) {
        System.out.println("[info] mysql query:" + op);
        conn.query(op, res->{
            QueryMessage result = new QueryMessage(res);
            message.reply(result,new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlQuery().name()));
            reSchedule(conn);
        });
    }
    
    private <T> void update(SQLConnection conn,String op,Message<T> message) {
        System.out.println("[info] mysql update:" + op);
        conn.update(op, res->{
            UpdateMessage result = new UpdateMessage(res);
            message.reply(result,new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlUpdate().name()));
            reSchedule(conn);
        });
    }
    /**
     * @param <T>
     * @param conn  分配到的数据库连接
     * @param ops   待执行事务
     * @param message
     */
    /*FIXME:当前的实现只支持update操作*/
    private <T> void sql_transaction(SQLConnection conn,List<String> ops,Message<T> message) {
        System.out.println("[info] mysql transaction:\n" + ops);
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
            CompositeMessage result = new CompositeMessage(res);
            message.reply(result,new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlComposite().name()));
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
        
        if(connections_idle.size() > 0 && (tasks_transaction.size() > 0 || tasks.size() > 0 )) {
            //FIXME: 获取空闲连接(取最近刚活动过的)
            conn = connections_idle.get(connections_idle.size()-1);
            connections_busy.add(conn);
            connections_idle.remove(conn);
        }
        else {
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
                        execute(conn,next.getValue().task,next.getValue().mess);
                        break;
                    case QUERY:
                        query(conn,next.getValue().task,next.getValue().mess);
                        break;
                    case UPDATE:
                        update(conn,next.getValue().task,next.getValue().mess);
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
                sql_transaction(conn,next.getValue().tasks,next.getValue().mess);
            }
        }
    }
}
