package cn.pgyyd.mcg.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
/**
 * A verticle setting and reading a value in Redis.
 */
public class RedisClientVerticle extends AbstractVerticle {
    private RedisClient client;
    public  class RedisKeeper{
        public final static String SET = "RedisKeeper-Set";
        
        public final static String GET = "RedisKeeper-Get";
        
        public final static String GET_SET = "RedisKeeper-GetSet";
        
        public final static String INCR = "RedisKeeper-INCR";
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
    }

}