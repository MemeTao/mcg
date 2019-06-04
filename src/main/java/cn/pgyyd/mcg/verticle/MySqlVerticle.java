package cn.pgyyd.mcg.verticle;

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
    
    private static int DEFAULT_POOL_SIZE = 16;
    
    final public static String EXEC = "mysql-exec";
    
    final public static String QUERY = "mysql-query";
    
    final public static String UPDATE = "musql-update";

    
    public static String TRANSACTION = "mysql-transaction";
    
    private TreeMap<String/*hash*/,AsyncSQLClient> clients;
    
    //private TreeMap<AsyncSQLClient/*client*/,SQLConnection> connections;
    
    private TreeMap<AsyncSQLClient/*client*/,LinkedList<SQLConnection>> idle_connections;
    
    private TreeMap<AsyncSQLClient/*client*/,LinkedList<SQLConnection>> busy_connections;  //仅仅是为了记录、debug
    
    private TreeMap<SQLConnection,String> hashs;
    /**假设1纳秒自增一次，64位的整数也够执行584年
     * 好吧，297年
     * */
    private long accounter_tasks = 0;  
    private long accounter_transaction = 0;
    
    private TreeMap<Long,TaskOp> tasks;
    private TreeMap<Long,TaskTransaction> tasks_transaction;
    @Override
    public void start() throws Exception {
        TreeMap<String,JsonObject> hash_and_config = DBSelector.hashkey_and_db_configs(); //同步执行?
        if(hash_and_config != null) {
            clients = new TreeMap<String/*hash*/,AsyncSQLClient>();
            //connections = new TreeMap<AsyncSQLClient/*client*/,SQLConnection>();
            idle_connections = new TreeMap<AsyncSQLClient/*client*/,LinkedList<SQLConnection>>();
            busy_connections = new TreeMap<AsyncSQLClient/*client*/,LinkedList<SQLConnection>>();
            for(Entry<String,JsonObject> item : hash_and_config.entrySet()) {
                String hash = item.getKey();
                JsonObject value = item.getValue();
                String host = value.getString("host");
                Integer port = value.getInteger("port");
                int connection_num = DEFAULT_POOL_SIZE;
                JsonObject mySQLClientConfig = new JsonObject().put("host", host).put("port",port).
                                                            put("username","memetao").put("password","123456").
                                                            put("maxPoolSize",connection_num).
                                                            put("database","mcg").
                                                            put("queryTimeout",1000);   /*查询操作，一秒超时*/
                /*异步执行的情况下，一个mysql业务线程是不是就够了呢*/
                AsyncSQLClient client = MySQLClient.createShared(vertx, mySQLClientConfig);
                idle_connections.put(client, new LinkedList<SQLConnection>());
                busy_connections.put(client, new LinkedList<SQLConnection>());
                clients.put(hash, client);
                for(int i = 0 ;i < connection_num; i++) {
                    client.getConnection(res -> {
                        if (res.succeeded()) {
                            SQLConnection conn = res.result();
                            idle_connections.get(client).add(conn);
                            hashs.put(conn,hash);
                        }
                        else {
                            log.warn("get mysql connection fail,hash:" + hash);
                        }
                      });
                }
                if(idle_connections.get(client).size() == 0) {
                    log.error("get mysql connection fialed,config:" + mySQLClientConfig.toString());
                    System.exit(-1);
                }
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
    
    private void reSchedule(SQLConnection conn) {
        String hash = hashs.get(conn);
        if(hash == null) {
            log.error("find hash for connection failed");
            return ;
        }
        AsyncSQLClient client = clients.get(hash);
        if(client == null) {
            log.error("find client failed,hash:" + hash);
            return ;
        }
        idle_connections.get(client).add(conn);
        busy_connections.get(client).remove(conn);
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
        for(Entry<Long,TaskOp> task : tasks.entrySet()) {
            String hash = task.getValue().hash;
            AsyncSQLClient client = clients.get(hash);
            if(client == null) {
                log.error("no such mysql client exist,hash:" + hash);
                continue;
            }
            LinkedList<SQLConnection> conns = idle_connections.get(client);
            if(conns.size() == 0) {
                log.info("wait for idle connections,hash:" + hash);
                continue;
            }
            SQLConnection conn = conns.get(0);
            idle_connections.get(client).remove(conn);
            busy_connections.get(client).add(conn);
            tasks.remove(task.getKey()); //为了防止某操作一直未执行成功，又被其它连接执行，需要立马删除
            final String op = task.getValue().op;
            switch(op) {
                case EXEC:
                    execute(conn,task.getValue().task,task.getValue().mess);
                    break;
                case QUERY:
                    query(conn,task.getValue().task,task.getValue().mess);
                    break;
                case UPDATE:
                    update(conn,task.getValue().task,task.getValue().mess);
                    break;
                default:
                    break;
            }
        }
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
    private <T> void execute(SQLConnection conn,String op,Message<T> message) {
        ExecuteMessage mess = (ExecuteMessage)(message.body());
        if(mess.indentification != -1) {
            log.debug("mysql execute:" + op + ",identification:" + mess.indentification);
        }
        conn.execute(op, res->{
            if(mess.indentification != -1) {
                log.debug("mysql execute done" + ",identification:" + mess.indentification);
            }
            ExecuteMessage result = new ExecuteMessage(res);
            message.reply(result,new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlExecute().name()));
            reSchedule(conn);
        });
    }
    
    private <T> void query(SQLConnection conn,String op,Message<T> message) {
        QueryMessage mess = (QueryMessage)(message.body());
        if(mess.indentification != -1) {
            log.debug("mysql query:" + op + ",identification:" + mess.indentification);
        }
        conn.query(op, res->{
            if(mess.indentification != -1) {
                log.debug("mysql query done" + ",identification:" + mess.indentification);
            }
            QueryMessage result = new QueryMessage(res);
            message.reply(result,new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlQuery().name()));
            reSchedule(conn);
        });
    }
    
    private <T> void update(SQLConnection conn,String op,Message<T> message) {
        UpdateMessage mess = (UpdateMessage)(message.body());
        if(mess.indentification != -1) {
            log.debug("mysql update:" + op + ",identification:" + mess.indentification);
        }
        conn.update(op, res->{
            if(mess.indentification != -1) {
                log.debug("mysql update done" + ",identification:" + mess.indentification);
            }
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
