package cn.pgyyd.mcg;

import cn.pgyyd.mcg.verticle.MainVerticle;
import cn.pgyyd.mcg.verticle.SelectCourseVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        ConfigStoreOptions storeOptions = new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path", "config.json"));
        ConfigRetrieverOptions retrieverOptions = new ConfigRetrieverOptions().addStore(storeOptions);
        ConfigRetriever retriever =  ConfigRetriever.create(vertx, retrieverOptions);
        retriever.getConfig(cfg -> {
            if (cfg.failed()) {
                //TODO: log something
            } else {
                vertx.deployVerticle(SelectCourseVerticle.class.getName(), res->{
                    if (!res.succeeded()) {
                        //TODO: log something
                    } else {
                        JsonObject config = cfg.result();
                        DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(config);
                        int cores = Runtime.getRuntime().availableProcessors();
                        deploymentOptions.setInstances(config.getInteger("threads", cores));

                        vertx.deployVerticle(MainVerticle.class.getName(), deploymentOptions);
                    }
                });
            }
        });
    }
}
