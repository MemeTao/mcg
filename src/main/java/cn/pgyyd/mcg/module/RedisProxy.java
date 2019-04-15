package cn.pgyyd.mcg.module;

import cn.pgyyd.mcg.verticle.RedisClientVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

public class RedisProxy {
    private final static String GET = RedisClientVerticle.RedisKeeper.GET;
    private final static String INCR = RedisClientVerticle.RedisKeeper.INCR;
    private final static String KEY_TASK_ID = "uuid/db_operation";
    private Vertx vertx;
    
    public RedisProxy(Vertx v){
        vertx = v;
    }
    
    public void allocateTaskId(Handler<AsyncResult<Message<Long>>> replyHandler)
    {
        incr(KEY_TASK_ID,replyHandler);
    }

    private <T> void get(String key,Handler<AsyncResult<Message<T>>> replyHandler) {
        vertx.eventBus().send(GET,key,replyHandler);
    }
    
    private <T> void incr(String key,Handler<AsyncResult<Message<T>>> replyHandler) {
        vertx.eventBus().send(INCR,key,replyHandler);
    }
}
