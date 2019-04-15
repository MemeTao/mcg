package cn.pgyyd.mcg.module;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.ext.web.RoutingContext;

public class SubmitSelectionHandler implements Handler<RoutingContext> {
    @Override
    public void handle(RoutingContext event) {
        //String uid = event.request().getParam("uid");
        //String courseids = event.request().getParam("courseids");
//      new RedisProxy(event.vertx()).allocateTaskId( res ->{
//      Long task_id = null;
//      if(res.succeeded()) {
//          task_id = new Long(res.result().body());
//          /*插入某个队列，执行数据库操作*/
//      }
//      else {
//          ;
//      }
//      event.response()
//          .putHeader("content-type", "text/plain")
//          .end("submit with uid:" + uid + ", courseids:" + courseids + ", user == null:" + String.valueOf(event.user() == null)
//          + "task_id = " + task_id);
//  });
        /* 我喜欢这种风格*/
        Handler<AsyncResult<Message<Long>>> handler = res->{
            if(res.succeeded()) {
                this.allocatedTaskId(res,event);
            }
            else {
                allocateTaskIdFail(event,res.cause().toString());
            }
        };
        /*示例: 通过RedisProxy获取全局唯一的task id*/
        new RedisProxy(event.vertx()).allocateTaskId(handler);
    }
    
    private void allocatedTaskId(AsyncResult<Message<Long>> res,RoutingContext event)
    {
        Long task_id = new Long(res.result().body());
        event.response()
            .putHeader("content-type", "text/plain")
            .end("task_id = " + task_id);
    }
    
    private void allocateTaskIdFail(RoutingContext event,String failed_reason) {
        event.response()
            .putHeader("content-type", "text/plain")
            .end("task_id = null,error:" + failed_reason);
    }
}
