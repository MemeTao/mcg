package cn.pgyyd.mcg;

import cn.pgyyd.mcg.verticle.*;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
    public static void main(String[] args) {
        log.info("System starting...");
        Vertx vertx = Vertx.vertx();
        ConfigStoreOptions storeOptions = new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path", "config.json"));
        ConfigRetrieverOptions retrieverOptions = new ConfigRetrieverOptions().addStore(storeOptions);
        ConfigRetriever retriever =  ConfigRetriever.create(vertx, retrieverOptions);
        retriever.getConfig(cfg -> {
            if (cfg.failed()) {
                log.error("Load config file failed");
            } else {
                //先启动周边Verticle，再启动接收Http请求的Verticle
                Future<String> mysqlFuture = Future.future();
                Future<String> redisFuture = Future.future();
                Future<String> selectFuture = Future.future();
                //vertx.deployVerticle(MySqlVerticle.class.getName(), mysqlFuture);
                //vertx.deployVerticle(RedisClientVerticle.class.getName(), redisFuture);
                //vertx.deployVerticle(SelectCourseVerticleKt.class.getName(), selectFuture);
                //CompositeFuture.all(redisFuture, selectFuture).setHandler(ar->{
                vertx.deployVerticle(new SelectCourseVerticleKt(), new DeploymentOptions().setConfig(cfg.result()), ar-> {
                    if (ar.succeeded()) {
                        JsonObject config = cfg.result();
                        DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(config);
                        int cores = Runtime.getRuntime().availableProcessors();
                        deploymentOptions.setInstances(config.getInteger("threads", cores));

                        vertx.deployVerticle(MainVerticle.class.getName(), deploymentOptions, r->{
                            if (r.succeeded()) {
                                log.info("Start success");
                            } else {
                                log.error("Start MainVerticle failed");
                            }
                        });
                    } else {
                        log.error("Start pre Verticles failed");
                    }
                });
            }
        });
    }
}
