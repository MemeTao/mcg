package cn.pgyyd.mcg.module;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class SubmitSelectionHandler implements Handler<RoutingContext> {
    @Override
    public void handle(RoutingContext event) {
        String uid = event.request().getParam("uid");
        String courseids = event.request().getParam("courseids");
        event.response()
                .putHeader("content-type", "text/plain")
                .end("submit with uid:" + uid + ", courseids:" + courseids + ", user == null:" + String.valueOf(event.user() == null));
    }
}
