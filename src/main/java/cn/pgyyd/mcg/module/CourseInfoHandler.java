package cn.pgyyd.mcg.module;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

public class CourseInfoHandler implements Handler<RoutingContext> {
    private static RedisClient redisClient;

    public static void init(Vertx vertx, JsonObject redisConfig) {
        redisClient = RedisClient.create(vertx,
                new RedisOptions()
                        .setAddress(redisConfig.getString("host"))
                        .setAuth(redisConfig.getString("auth")
                        )
        );
    }

    //TODO:  根据url参数uid和type选择相应课程信息
    @Override
    public void handle(RoutingContext event) {
        String uid = event.request().getParam("uid");
        String type = event.request().getParam("type");
        /**
         * redis静态信息，直接查找返回
         */
        event.response()
                .putHeader("content-type", "text/plain")
                .end("get course info with uid:" + uid + ", type:" + type);
    }
}
