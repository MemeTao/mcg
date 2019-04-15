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
        /*创建Redis客户端,默认配置:utf-8\tcp_keepalive\tcp_no_delay*/
//        client = RedisClient.create(vertx,
//                new RedisOptions().setHost(host).setPort(port));
        /*client.select(dbindex, handler)
        * client.auth();
        * */
        vertx.eventBus().consumer(RedisKeeper.GET,message->{
            String key = new String(message.body().toString());
            System.out.println("recive key:" + key);
        });
        
        vertx.eventBus().consumer(RedisKeeper.SET,message->{
            ;
        });
        vertx.eventBus().consumer(RedisKeeper.INCR,message->{
            ;
        });
        System.out.println("RedisClientVerticle launched");
    }

}