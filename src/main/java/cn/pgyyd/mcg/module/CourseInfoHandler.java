package cn.pgyyd.mcg.module;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class CourseInfoHandler implements Handler<RoutingContext> {
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
