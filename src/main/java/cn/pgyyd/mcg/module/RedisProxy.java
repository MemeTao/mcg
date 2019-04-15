package cn.pgyyd.mcg.module;

import cn.pgyyd.mcg.verticle.RedisClientVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

public class RedisProxy {
    private final static String GET = RedisClientVerticle.RedisKeeper.GET;
    
    private Vertx vertx;
    public RedisProxy(Vertx v){
        vertx = v;
        //vertx.eventBus().send(GET, message);
    }
    
    public <T> void get(String key,Handler<AsyncResult<Message<T>>> replyHandler) {
        System.out.println("try get key:" + key);
        vertx.eventBus().send(GET,key,replyHandler);
    }
}
