package cn.pgyyd.mcg.module;

import cn.pgyyd.mcg.interfaces.Test;
import cn.pgyyd.mcg.verticle.RedisClientVerticle;
import cn.pgyyd.mcg.verticle.RedisClientVerticle.RedisKeeper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

public class RedisProxy {
    private final static String GET = RedisClientVerticle.RedisKeeper.GET;
    private final static String INCR = RedisClientVerticle.RedisKeeper.INCR;
    
    private final static String TRANSACTION_READY = RedisClientVerticle.RedisKeeper.TRANSACTION_READY;
    private final static String TRANSACTION_EXEC = RedisClientVerticle.RedisKeeper.TRANSACTION_EXEC;
    private final static String TRANSACTION_GET = RedisClientVerticle.RedisKeeper.TRANSACTION_GET;
    private final static String TRANSACTION_SET = RedisClientVerticle.RedisKeeper.TRANSACTION_SET;
    private final static String TRANSACTION_INCR = RedisClientVerticle.RedisKeeper.TRANSACTION_INCR;
    
    private final static String KEY_TASK_ID = "uuid/task_id";
    private final static String KEY_TASK_STATUS = "task/status";
    
    private Vertx vertx;
    
    public RedisProxy(Vertx v){
        vertx = v;
    }
    
    public void allocateTaskId(Handler<AsyncResult<Message<Long>>> replyHandler)
    {
        //redis事务
        //1.分配一个task
        //2.在任务状态表中记录此任务
        transaction_ready(res->{
            if(res.succeeded()) {
                /*这里的任何一个回调应该都是在事务执行成功后才执行的*/
                incr_transaction(KEY_TASK_ID,replyHandler);
                set_transaction(KEY_TASK_STATUS,null);
                transaction_exec(null);
            }
            else {
                /*不断进行*/
                allocateTaskId(replyHandler);
            }
        });
    }
    
    private <T> void get(String key,Handler<AsyncResult<Message<T>>> replyHandler) {
        vertx.eventBus().send(GET,key,replyHandler);
    }
    
    private <T> void get_transaction(String key,Handler<AsyncResult<Message<T>>> replyHandler) {
        vertx.eventBus().send(TRANSACTION_GET,key,replyHandler);
    }
    
    private <T> void incr(String key,Handler<AsyncResult<Message<T>>> replyHandler) {
        vertx.eventBus().send(INCR,key,replyHandler);
    }
    private <T> void transaction_ready(Handler<AsyncResult<Message<T>>> replyHandler) {
        vertx.eventBus().send(TRANSACTION_READY,null,replyHandler);
    }
    private <T> void transaction_exec(Handler<AsyncResult<Message<T>>> replyHandler) {
        vertx.eventBus().send(TRANSACTION_EXEC,null,replyHandler);
    }
    private <T> void set_transaction(String key,Handler<AsyncResult<Message<T>>> replyHandler) {
        vertx.eventBus().send(TRANSACTION_SET,key,replyHandler);
    }
    
    private <T> void incr_transaction(String key,Handler<AsyncResult<Message<T>>> replyHandler) {
        vertx.eventBus().send(TRANSACTION_INCR,key,replyHandler);
    }
    
}
