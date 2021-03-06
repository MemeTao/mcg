package cn.pgyyd.mcg;

import cn.pgyyd.mcg.verticle.*;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
//import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
    public static void main(String[] args) {
        log.info("System starting...");
        Vertx vertx = Vertx.vertx();
//      用于画指标图标，暂时注释掉
//      Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
//             new DropwizardMetricsOptions().setJmxEnabled(true)));
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
                //vertx.deployVerticle(SelectCourseVerticle.class.getName(), selectFuture);
                //CompositeFuture.all(redisFuture, selectFuture).setHandler(ar->{
                vertx.deployVerticle(new SelectCourseVerticle(), new DeploymentOptions().setConfig(cfg.result()), ar-> {
                    if (ar.succeeded()) {
                        JsonObject config = cfg.result();
                        DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(config);
                        int cores = Runtime.getRuntime().availableProcessors();
                        deploymentOptions.setInstances(config.getInteger("threads", cores));
                        log.info(String.format("Start MainVerticle with %d cores", cores));
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
