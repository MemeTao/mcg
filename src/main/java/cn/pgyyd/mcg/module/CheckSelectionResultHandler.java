package cn.pgyyd.mcg.module;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

public class CheckSelectionResultHandler implements Handler<RoutingContext> {
    //FIXME: 这种方式的RedisClient是所有线程共享一个连接，如果遇到性能瓶颈，可以改成一个线程一个RedisClient
    private static RedisClient redisClient;

    public static void init(Vertx vertx, JsonObject redisConfig) {
        redisClient = RedisClient.create(vertx,
                new RedisOptions()
                    .setAddress(redisConfig.getString("host"))
                    .setAuth(redisConfig.getString("auth")
                    )
        );
    }
    @Override
    public void handle(RoutingContext event) {
        String jobID = event.request().getParam("jobid");
        redisClient.get(jobID, res->{
            if (res.succeeded()) {
                String[] redisResult = res.result().split("_");
                if (redisResult.length != 0 && redisResult.length % 2 == 0) {
                    JsonObject response = new JsonObject().put("status_code", 0);
                    JsonArray results = new JsonArray();
                    for (int i = 0; i < redisResult.length; i+=2) {
                        results.add(new JsonObject()
                                .put("course", redisResult[i])
                                .put("result", Boolean.valueOf(redisResult[i+1]))
                        );
                    }
                    response.put("results", results);
                    event.response()
                            .putHeader("content-type", "application/json")
                            .end(response.toString());
                } else {
                    event.response()
                            .putHeader("content-type", "application/json")
                            .end(new JsonObject()
                                    .put("status_code", -1)
                                    .put("message", "result format wrong")
                                    .toString()
                            );
                }
            } else {
                event.response()
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                                .put("status_code", -1)
                                .put("message", "query result fail")
                                .toString()
                        );
            }
        });
    }
}
