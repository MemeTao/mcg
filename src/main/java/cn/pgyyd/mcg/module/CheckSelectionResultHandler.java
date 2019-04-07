package cn.pgyyd.mcg.module;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class CheckSelectionResultHandler implements Handler<RoutingContext> {
    @Override
    public void handle(RoutingContext event) {
        String jobID = event.request().getParam("jobid");
        event.response()
                .putHeader("content-type", "text/plain")
                .end("check with jobID:" + jobID);
    }
}
