package cn.pgyyd.mcg.handler;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import org.apache.commons.lang3.StringUtils;

public class CheckSelectionResultHandler implements Handler<RoutingContext> {
    private RedisClient redisClient;

    public CheckSelectionResultHandler(Vertx vertx, JsonObject redisConfig) {
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
        if (StringUtils.isEmpty(jobID)) {
            event.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("status_code", 1)
                            .put("msg", "jobid不能为空")
                            .toString());
            return;
        }

        redisClient.get(jobID, res->{
            if (res.succeeded()) {
                String[] redisResult = res.result().split("_");
                if (redisResult.length != 0 && redisResult.length % 2 == 0) {
                    JsonObject response = new JsonObject().put("status_code", 0).put("msg", "success");
                    JsonArray results = new JsonArray();
                    for (int i = 0; i < redisResult.length; i+=2) {
                        results.add(new JsonObject()
                                .put("course", Integer.valueOf(redisResult[i]))
                                .put("success", Boolean.valueOf(redisResult[i+1]))
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
                                    .put("msg", "result format wrong")
                                    .toString()
                            );
                }
            } else {
                event.response()
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                                .put("status_code", -1)
                                .put("msg", "query result fail")
                                .toString()
                        );
            }
        });
    }
}
