package cn.pgyyd.mcg.verticle;

import cn.pgyyd.mcg.module.MysqlProxy;
import io.vertx.core.AbstractVerticle;
import io.vertx.redis.RedisClient;

/**
 * A verticle setting and reading a value in Redis.
 */
public class RedisClientVerticle extends AbstractVerticle {
    
    static public RedisClient client;
    
    //static public RedisTransaction transaction_client;
    
    @Override
    public void start() throws Exception {
        /*获取配置文件中的redis配置信息*/
//        String host = Vertx.currentContext().config().getString("host_redis");
//        Integer port = Vertx.currentContext().config().getInteger("port_redis");
//        if (host == null ) {
//            host = "127.0.0.1";
//        }
//        if(port == null) {
//            port = 8080;
//        }
//        /**
//         * 将mysql中的数据初始化到redis
//         * 先不考虑缓存大小
//         * 1. 课程id列表 
//         * 2. 
//         */
////        client = RedisClient.create(vertx,
////                new RedisOptions().setHost(host).setPort(port));
//        //transaction_client = client.transaction();
        System.out.println("[info] waiting for mysql initilization to complete...");
        Thread.sleep(1000);
        new MysqlProxy(vertx).getCourseIdList(res->{
            System.out.println("course id list:" + res);
        });
    }
    
    static public RedisClient client() {
        return client;
    }
    
}