package cn.pgyyd.mcg.verticle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.sql.SQLConnection;

public class MySqlVerticle extends AbstractVerticle {
    
    private static int DEFAULT_POOL_SIZE = 10;
    private static String SUBSCRIBEADDR = "mysql-op";
    
    private int connection_pool_size = DEFAULT_POOL_SIZE;
    
    private List<SQLConnection> connections ;
    
    private AsyncSQLClient client;
    
    private int available_connections;
    
    private Map<String,Message<Object>> tasks;
    @Override
    public void start() throws Exception {
        String host = Vertx.currentContext().config().getString("host_mysql");
        Integer port = Vertx.currentContext().config().getInteger("port_mysql");
        Integer connection_num = Vertx.currentContext().config().getInteger("connections_mysql");
        if( connection_num != null && connection_num.intValue() > 0) {
            connection_pool_size = connection_num.intValue();
        }

        JsonObject mySQLClientConfig = new JsonObject().put("host", host).put("port",port).
                                                    put("user","mcg").put("password","admin_mcg").
                                                    put("max_pool_size",connection_pool_size);
        
        /*异步执行的情况下，一个mysql业务线程是不是就够了呢*/
        client = MySQLClient.createNonShared(vertx, mySQLClientConfig);
        connections = new ArrayList<SQLConnection>();
        for(int i = 0 ;i < connection_num; i++) {
            client.getConnection(res -> {
                if (res.succeeded()) {
                    SQLConnection conn = res.result();
                    connections.add(conn);
                }
                else {
                    connection_pool_size--;
                }
              });
        }
        available_connections = connection_pool_size;
        tasks = new HashMap<String,Message<Object>>();
        vertx.eventBus().consumer(SUBSCRIBEADDR,message->{
            String op = message.body().toString();
            tasks.put(op, message);
            if(available_connections > 0) {
                SQLConnection conn = connections.get(available_connections-1);
                available_connections--;
                execute_sql(conn,op,message);
            }
            else {
                /*数据库繁忙*/
                //logger("mysql client busy now");
            }
        });
    }
    
    private <T> void execute_sql(SQLConnection conn,String op,Message<T> message) {
            conn.execute(op, res->{
                if(res.succeeded()) {
                    /*获取结果并返回*/
                    message.reply(res.result());
                    tasks.remove(op);
                }
                if(tasks.size() > 0) {
                    /*取出一个，接着调用execute_sql*/
                    Entry<String,Message<Object>> next = null;
                    for(Entry<String,Message<Object>> it : tasks.entrySet()) {
                        if(it != null) {
                            next = it;
                            break;
                        }
                    }
                    execute_sql(conn,next.getKey(),next.getValue());
                }
                //FIXME:++ 和 -- 写在一块才对
                available_connections ++;   
            });
    }
    
    
    
}
