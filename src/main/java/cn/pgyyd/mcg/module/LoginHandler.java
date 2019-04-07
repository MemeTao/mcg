package cn.pgyyd.mcg.module;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class LoginHandler implements Handler<RoutingContext> {
    @Override
    public void handle(RoutingContext event) {
        String uid = event.request().getParam("uid");
        String token = event.request().getParam("token");
        event.response()
                .putHeader("content-type", "text/plain")
                .end("login with uid:" + uid + ", token:" + token);
    }
}
