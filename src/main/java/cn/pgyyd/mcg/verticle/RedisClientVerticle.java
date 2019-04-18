package cn.pgyyd.mcg.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import io.vertx.redis.RedisTransaction;
/**
 * A verticle setting and reading a value in Redis.
 */
public class RedisClientVerticle extends AbstractVerticle {
    private RedisClient client;
    
    private RedisTransaction transactionClient;

    public  class RedisKeeper{
        public final static String SET = "redis_keeper_set";
        
        public final static String GET = "redis_keeper_get";
        
        public final static String INCR = "redis_keeper_incr";
        
        public final static String TRANSACTION_READY = "redis_keeper_transaction_ready";
        
        public final static String TRANSACTION_EXEC= "redis_keeper_transaction_exec";
        
        public final static String TRANSACTION_GET = "redis_transaction_get";
        
        public final static String TRANSACTION_SET = "redis_transaction_set";
        
        public final static String TRANSACTION_INCR = "redis_transaction_incr";
    }
    @Override
    public void start() throws Exception {
        /*获取配置文件中的redis配置信息*/
        String host = Vertx.currentContext().config().getString("host_redis");
        Integer port = Vertx.currentContext().config().getInteger("port_redis");
        if (host == null ) {
            host = "127.0.0.1";
        }
        if(port == null) {
            port = 8080;
        }
        /*FIXME:添加client超时机制*/
        client = RedisClient.create(vertx,
                new RedisOptions().setHost(host).setPort(port));
        transactionClient = client.transaction();
        
        vertx.eventBus().consumer(RedisKeeper.GET,message->{
            //String key = message.body().toString();
        });
        
        vertx.eventBus().consumer(RedisKeeper.SET,message->{
            //String key = message.body().toString();
        });
        
        vertx.eventBus().consumer(RedisKeeper.INCR,message->{
            String key = message.body().toString();
            client.incr(key, res ->{
                if(res.succeeded()) {
                    message.reply(res.result());
                }
                else {
                    message.fail(-1, res.cause().toString());
                }
            });
        });
        
        vertx.eventBus().consumer(RedisKeeper.TRANSACTION_READY,message->{
            transactionClient.multi(res->{
                if(res.succeeded()) {
                    message.reply(res.result());
                }
                else {
                    message.fail(-1,res.cause().toString());
                }
            });
        });
        
        vertx.eventBus().consumer(RedisKeeper.TRANSACTION_EXEC,message->{
            transactionClient.exec(res->{
                if(res.succeeded()) {
                    message.reply(res.result());
                }
                else {
                    message.fail(-1,res.cause().toString());
                }
            });
        });
        
        vertx.eventBus().consumer(RedisKeeper.TRANSACTION_GET,message->{
            String op = message.body().toString();
            
        });
        
        vertx.eventBus().consumer(RedisKeeper.TRANSACTION_SET,message->{
            String op = message.body().toString();
            
        });
        
        vertx.eventBus().consumer(RedisKeeper.TRANSACTION_INCR,message->{
            String key = message.body().toString();
            transactionClient.incr(key, res->{
                if(res.succeeded()) {
                    message.reply(res.result());
                }
                else{
                    message.fail(-1, res.cause().toString());
                }
            });
        });
    }

}