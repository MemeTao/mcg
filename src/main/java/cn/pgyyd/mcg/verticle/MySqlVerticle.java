package cn.pgyyd.mcg.verticle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import cn.pgyyd.mcg.module.DBSelector;
import cn.pgyyd.mcg.module.MysqlMessage.CompositeMessage;
import cn.pgyyd.mcg.module.MysqlMessage.ExecuteMessage;
import cn.pgyyd.mcg.module.MysqlMessage.QueryMessage;
import cn.pgyyd.mcg.module.MysqlMessage.UpdateMessage;
import cn.pgyyd.mcg.module.UserMessageCodec;

import java.util.TreeMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.sql.SQLConnection;
import lombok.extern.slf4j.Slf4j;
/**
 * 这个类提供mysql的连接管理以及数据库请求的控制
 * FIXME:暂时取消事务操作的支持
 * @author memetao
 */
@Slf4j
public class MySqlVerticle extends AbstractVerticle {
    /*简单的包裹一下task
     * */
    class TaskOp{
        public TaskOp(final String t,Message<Object> m,final String operation,final String h) {
            task = t;
            mess = m;
            op = operation;
            hash = h;
        }
        final String task;
        final String op;
        final String hash;
        Message<Object> mess;
    }
    class TaskTransaction{
        public TaskTransaction(final List<String> ts,Message<Object> m) {
            tasks = ts;
            mess = m;
        }
        final List<String> tasks;
        final Message<Object> mess;
        String hash;
    }
    
    final public static String EXEC = "mysql-exec";
    
    final public static String QUERY = "mysql-query";
    
    final public static String UPDATE = "musql-update";

    
    public static String TRANSACTION = "mysql-transaction";
    
    private TreeMap<String/*hash*/,AsyncSQLClient> clients;
    
    private TreeMap<String/*hash*/,LinkedList<SQLConnection>> idle_connections;
    
    private TreeMap<String/*hash*/,LinkedList<SQLConnection>> busy_connections;
   
    /**假设1纳秒自增一次，64位的整数也够执行584年
     * 好吧，297年
     * */
    private long accounter_tasks = 0;  
    private long accounter_transaction = 0;
    
    private TreeMap<Long,TaskOp> tasks;
    private TreeMap<Long,TaskTransaction> tasks_transaction;
    @Override
    public void start() throws Exception {
        log.info("mysql verticle start...");
        TreeMap<String,JsonObject> hash_and_config = DBSelector.hashkey_and_db_configs(); //同步执行?
        if(hash_and_config != null) {
            clients = new TreeMap<String/*hash*/,AsyncSQLClient>();
            idle_connections = new TreeMap<String/*hash*/,LinkedList<SQLConnection>>();
            busy_connections = new TreeMap<String/*hash*/,LinkedList<SQLConnection>>();
            for(Entry<String,JsonObject> item : hash_and_config.entrySet()) {
                String hash = item.getKey();
                log.info("initialize mysql connections use mysql configurate(:" +hash +")" + item.getValue().toString());
                int connection_num = item.getValue().getInteger("maxPoolSize");
                AsyncSQLClient client = MySQLClient.createNonShared(vertx, item.getValue());
                idle_connections.put(hash, new LinkedList<SQLConnection>());
                busy_connections.put(hash, new LinkedList<SQLConnection>());
                clients.put(hash, client);
                log.debug("clients:" + clients.size());
                for(int i = 0 ;i < connection_num / 2; i++) {
                    log.debug("try get connections");
                    client.getConnection(res -> {
                        if (res.succeeded()) {
                            SQLConnection conn = res.result();
                            idle_connections.get(hash).add(conn);
                            log.debug("hash:" + hash + ",connected:" + idle_connections.get(hash).size());
                        }
                        else {
                            log.warn("get mysql connection fail,hash:" + hash);
                        }
                      });
                }
                //TODO:如果所有的连接都建立不起来，需要结束程序
            }
        }
        tasks = new TreeMap<Long,TaskOp>();
        tasks_transaction = new TreeMap<Long,TaskTransaction>();
        
        vertx.eventBus().registerCodec(new UserMessageCodec.MysqlQuery());
        vertx.eventBus().registerCodec(new UserMessageCodec.MysqlUpdate());
        vertx.eventBus().registerCodec(new UserMessageCodec.MysqlExecute());
        vertx.eventBus().registerCodec(new UserMessageCodec.MysqlComposite());

        vertx.eventBus().consumer(EXEC,message->{
            ExecuteMessage mess = (ExecuteMessage) message.body();
            if(mess.indentification != -1) {
                log.debug("mysql verticle recived ExecuteMessage,identification:" + mess.indentification);
            }
            tasks.put( accounter_tasks ++, new TaskOp(mess.operation(),message,EXEC,mess.hash()));
            schedule();
        });
        
        vertx.eventBus().consumer(UPDATE,message->{
            UpdateMessage mess = (UpdateMessage) message.body();
            if(mess.indentification != -1) {
                log.debug("mysql verticle recived UpdateMessage,identification:" + mess.indentification);
            }
            tasks.put( accounter_tasks ++, new TaskOp(mess.operation(),message,UPDATE,mess.hash()));
            schedule();
        });
        
        vertx.eventBus().consumer(QUERY,message->{
            QueryMessage mess = (QueryMessage) message.body();
            if(mess.indentification != -1) {
                log.debug("mysql verticle recived QueryMessage,identification:" + mess.indentification);
            }
            tasks.put( accounter_tasks ++, new TaskOp(mess.operation(),message,QUERY,mess.hash()));
            schedule();
        });
        
        vertx.eventBus().consumer(TRANSACTION,message->{
            CompositeMessage mess = (CompositeMessage) message.body();
            tasks_transaction.put(accounter_transaction,new TaskTransaction(mess.operations(),message));
            schedule();
        });
    }
    
    private void reSchedule(String hash,SQLConnection conn) {

        AsyncSQLClient client = clients.get(hash);
        if(client == null) {
            log.error("find client failed,hash:" + hash);
            return ;
        }
        busy_connections.get(hash).remove(conn);
        idle_connections.get(hash).addFirst(conn);
        schedule();
    }
    
    private void schedule() {
        /*
         * 1.有某库的空闲连接，有某库的sql请求
         * 2.有某库的空闲连接，没有某库的sql请求
         * 3.没有某库的空闲连接....
         * 
         * 所以是以是否有sql请求为导向
         */
        ArrayList<Long> keys_delete = new ArrayList<Long>();
        for(Entry<Long,TaskOp> task : tasks.entrySet()) {
            String hash = task.getValue().hash;
            if(hash == null || !idle_connections.containsKey(hash)) {
                log.error("no exist connections corresponding to hash:," + hash);
                keys_delete.add(task.getKey());
                continue;
            }
            LinkedList<SQLConnection> conns = idle_connections.get(hash);
            if(conns.size() == 0) {  //对 == null 不处理
                log.info("wait for idle connections,hash:" + hash);
                continue;
            }
            SQLConnection conn = conns.get(0);
            idle_connections.get(hash).remove(conn);
            busy_connections.get(hash).add(conn);
            keys_delete.add(task.getKey());
            final String op = task.getValue().op;
            switch(op) {
                case EXEC:
                    execute(hash,conn,task.getValue().task,task.getValue().mess);
                    break;
                case QUERY:
                    query(hash,conn,task.getValue().task,task.getValue().mess);
                    break;
                case UPDATE:
                    update(hash,conn,task.getValue().task,task.getValue().mess);
                    break;
                default:
                    break;
            }
        }
        for(int i = 0 ; i< keys_delete.size(); i++) {
            tasks.remove(keys_delete.get(i));
        }
        keys_delete.clear();
         //事务操作
//            if(tasks_transaction.size() > 0) {
//                Entry<Long,MySqlVerticle.TaskTransaction> next = null;
//                for(Entry<Long,MySqlVerticle.TaskTransaction> it : tasks_transaction.entrySet()) {
//                    if(it != null) {
//                        next = it;
//                        break;
//                    }
//                }
//                tasks_transaction.remove(next.getKey());
//                sql_transaction(conn,next.getValue().tasks,next.getValue().mess);
//            }
        
    }
    /**
     * @param <T>
     * @param conn 分配到的数据库连接
     * @param op   待执行操作
     * @param message 
     */
    private <T> void execute(String hash,SQLConnection conn,String op,Message<T> message) {
        ExecuteMessage mess = (ExecuteMessage)(message.body());
        if(mess.indentification != -1) {
            log.debug("mysql execute:" + op + ",identification:" + mess.indentification);
        }
        conn.execute(op, res->{
            if(mess.indentification != -1) {
                log.debug("mysql execute done(" + res.succeeded()  + "),identification:" + mess.indentification);
            }
            ExecuteMessage result = new ExecuteMessage(res);
            message.reply(result,new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlExecute().name()));
            reSchedule(hash,conn);
        });
    }
    
    private <T> void query(String hash,SQLConnection conn,String op,Message<T> message) {
        QueryMessage mess = (QueryMessage)(message.body());
        if(mess.indentification != -1) {
            log.debug("mysql query:" + op + ",identification:" + mess.indentification);
        }
        conn.query(op, res->{
            if(mess.indentification != -1) {
                log.debug("mysql query donedone(" + res.succeeded() +"),identification:" + mess.indentification);
            }
            QueryMessage result = new QueryMessage(res);
            message.reply(result,new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlQuery().name()));
            reSchedule(hash,conn);
        });
    }
    
    private <T> void update(String hash,SQLConnection conn,String op,Message<T> message) {
        UpdateMessage mess = (UpdateMessage)(message.body());
        if(mess.indentification != -1) {
            log.debug("mysql update:" + op + ",identification:" + mess.indentification);
        }
        conn.update(op, res->{
            if(mess.indentification != -1) {
                log.debug("mysql update done (" + res.succeeded() + "),identification:" + mess.indentification);
            }
            UpdateMessage result = new UpdateMessage(res);
            message.reply(result,new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlUpdate().name()));
            reSchedule(hash,conn);
        });
    }
    
    /**
     * @param <T>
     * @param conn  分配到的数据库连接
     * @param ops   待执行事务
     * @param message
     */
    /*FIXME:当前的实现只支持update操作*/
//    private <T> void sql_transaction(SQLConnection conn,List<String> ops,Message<T> message) {
//        log.info("mysql transaction:\n" + ops);
//        List<Future<Void>> futures = new ArrayList<Future<Void>>();
//        Future<Void> f_b =  Future.future();
//        /**准备事务*/
//        conn.setAutoCommit(false, f_b.completer());
//        futures.add(f_b);
//        for(String op : ops) {
//            Future<Void> future = Future.future();
//            futures.add(future);
//            /*FIXME: 这里添加上op字段，也可以实现多种类型*/
//            conn.update(op,res->{
//                if(res.succeeded()) {
//                    future.complete();
//                }
//                else {
//                    future.fail(res.cause().toString());
//                }
//            });
//        }
//        Future<Void> f_e =  Future.future();
//        futures.add(f_b);
//        /**提交事务*/
//        conn.commit(f_e.completer());
//        /**立即恢复auto-commit属性*/
//        conn.setAutoCommit(true,null);
//        
//        CompositeFuture.all(new ArrayList<>(futures)).setHandler(res->{
//            CompositeMessage result = new CompositeMessage(res);
//            message.reply(result,new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlComposite().name()));
//            reSchedule(conn);
//        });
//    }
}
