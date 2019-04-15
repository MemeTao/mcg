package cn.pgyyd.mcg.module;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class SubmitSelectionHandler implements Handler<RoutingContext> {
    @Override
    public void handle(RoutingContext event) {
        String uid = event.request().getParam("uid");
        String courseids = event.request().getParam("courseids");
        /*不管是否空闲，统一入队列
         * 1.获取一个新的task_id(由Redis生成全局唯一的id不太可靠)
         * */
        String task_id = "wait for db # abcd";
        System.out.println("task id :" + task_id);
        new RedisProxy(event.vertx()).get("task id", res ->{
            System.out.println("get task id : " + res.result().body());
        });
        
        event.response()
                .putHeader("content-type", "text/plain")
                .end("submit with uid:" + uid + ", courseids:" + courseids + ", user == null:" + String.valueOf(event.user() == null)
                + ",task_id:" + task_id);
    }
}
