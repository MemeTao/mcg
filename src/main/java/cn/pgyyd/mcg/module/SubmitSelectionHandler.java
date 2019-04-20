package cn.pgyyd.mcg.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.ext.web.RoutingContext;

public class SubmitSelectionHandler implements Handler<RoutingContext> {
    
    private class Info{
        public String student_id;
        public List<String> courseids;
    }
    
    @Override
    public void handle(RoutingContext event) {
        String uid = event.request().getParam("uid");
        String[] courseids = event.request().getParam("courseids").split(",");//以逗号分割
        Info info = new Info();
        info.student_id = uid;
        info.courseids = Arrays.asList(courseids);
        Handler<AsyncResult<Long>> handler = res->{
                this.allocatedTaskId(res,event,info);
        };
        
        /**示例: 通过RedisProxy获取全局唯一的task id
         * */
        //new RedisProxy(event.vertx()).allocateTaskId(handler);
    }
    /**
     * 
     * @param res
     * @param event
     * @param courseids
     */
    private void allocatedTaskId(AsyncResult<Long> res,RoutingContext event,Info info){
        /* 在这之前，redis中应该存在了以下信息：
         * task_id   status
         *   1        ing
         *   2        done
         *   3        failed
         *   ...      ...
         *   x        ing        第一列表示任务id，第二列表示该任务的执行状态
         */
        if(res.failed()) {
            allocateTaskIdFail(event,info);
            return;
        }
        long task_id = res.result();
        event.response()   /*统一让前端轮询*/
            .putHeader("content-type", "text/plain")
            .end("task_id = " + task_id);
        /**
         * 1. 查询courseids中的课程是否“合法”（这个应该是去操作静态的内容,理论上会比较快才对）
         *      1.1 课程是否存在  (redis)
         *      1.2 课程时间与已选课程是否冲突 (该学生的原始课程+之前选择的课程)
         * 2. 查询当前课程剩余人数(直接查redis，与mysql不一致并没有太大关系)
         *    2.1 redis获取剩余人数
         * 3. 更新课程剩余人数(操作mysql)，修改该学生的课表信息，并且将课程剩余人数写入redis
         *    3.1 如果此时更新失败（人数已满），返回失败，告诉前端将页面展示为“人数满”
         *    3.2 更新redis中的剩余人数（如果3.1不失败，可以选择性更新redis中剩余人数）
         *    3.2 上面这步执行成功，那么就可以直接修改该学生的课表信息了，
         *        这步不至于失败,即使失败也可以通过人工（通过日志信息或者其它途径）修复改问题，
         *        这样就可以把3.2和"更新剩余人数"给分开操作
         */
        List<Future<Void>> futures = new ArrayList<Future<Void>>();
        new MysqlProxy(event.vertx()).getCourseIdList(reply ->{
            Future<Void> f = Future.future();
            futures.add(f);
            for(String id : info.courseids) {
                if(!reply.contains(id)) {  //不剔除了（肯定是前端代码出错了)
                    f.fail("not exsit course id:" + id);
                }
            }
            f.complete();
         });
        CompositeFuture.all(new ArrayList<>(futures)).setHandler( res->{
            
        });
    }
    
    private void allocateTaskIdFail(RoutingContext event,Info info) {
        event.response()
            .putHeader("content-type", "text/plain")
            .end("task_id = null,error:" + info);
    }
}
