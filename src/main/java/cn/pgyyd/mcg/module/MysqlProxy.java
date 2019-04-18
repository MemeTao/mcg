package cn.pgyyd.mcg.module;

import java.util.List;

import cn.pgyyd.mcg.verticle.MySqlVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

public class MysqlProxy {
    
    private static String EXEC = MySqlVerticle.EXEC;
    
    private static String QUERY = MySqlVerticle.QUERY;
    
    private static String UPDATE = MySqlVerticle.UPDATE;
    
    private static String TRANSACTION = MySqlVerticle.TRANSACTION;
    
    private Vertx vertx;
    
    public MysqlProxy(Vertx v){
        vertx = v;
    }
    
    public <T> void execute(String op,Handler<AsyncResult<Message<T>>> replyHandler) {
        vertx.eventBus().send(EXEC,op,replyHandler);
    }
    
    public <T> void query(String op,Handler<AsyncResult<Message<T>>> replyHandler) {
        vertx.eventBus().send(QUERY,op,replyHandler);
    }
    
    //update
    public <T> void update(String op,Handler<AsyncResult<Message<T>>> replyHandler) {
        vertx.eventBus().send(UPDATE,op,replyHandler);
    }
    /*不直接提供事务接口，而以具体的业务接口的形式给出
     * 底层负责“失败回滚”操作
     * */
    private <T> void transaction(List<String> ops,Handler<AsyncResult<Message<T>>> replyHandler) {
        vertx.eventBus().send(TRANSACTION,ops,replyHandler);
    }
}
