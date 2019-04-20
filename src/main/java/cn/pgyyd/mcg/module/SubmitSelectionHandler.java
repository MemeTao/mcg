package cn.pgyyd.mcg.module;

import cn.pgyyd.mcg.constant.McgConst;
import cn.pgyyd.mcg.ds.SelectCourseRequest;
import cn.pgyyd.mcg.ds.SelectCourseResult;
import cn.pgyyd.mcg.singleton.JobCounter;
import cn.pgyyd.mcg.singleton.JobIDGenerator;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.List;

import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SubmitSelectionHandler implements Handler<RoutingContext> {

    private int MAX_DOING_JOBS;

    public SubmitSelectionHandler(int jobs) {
        MAX_DOING_JOBS = jobs;
    }

    @Override
    public void handle(RoutingContext event) {
        String uid = event.request().getParam("uid");
        String courseids = event.request().getParam("courseids");
        if (StringUtils.isEmpty(uid) || StringUtils.isEmpty(courseids)) {
            event.fail(400);
            return;
        }
        List<Long> courseIdList;
        int userId;
        try {
            userId = Integer.parseInt(uid);
            courseIdList = Arrays.stream(courseids.split(","))
                    .filter(s -> !s.isEmpty())
                    .mapToLong(Long::parseLong)
                    .boxed()
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            //TODO: log something
            event.fail(400);
            return;
        } catch (Exception e) {
            //unknown exception
            return;
        }

        event.vertx().eventBus().send(McgConst.EVENT_BUS_SELECT_COURSE, new SelectCourseRequest(userId, courseIdList), res -> {
            SelectCourseResult result = (SelectCourseResult) res.result().body();
            switch (result.Status) {
                //处理完成
                case 0:
                    JsonArray selectResults = new JsonArray();
                    for (SelectCourseResult.Result r : result.Results) {
                        selectResults.add(new JsonObject().put("course", r.CourseID).put("status", r.Success));
                    }
                    event.response()
                            .putHeader("content-type", "application/json")
                            .end(new JsonObject()
                                    .put("status_code", 0)
                                    .put("result", selectResults)
                                    .toString());
                    break;
                //排队选课
                case 1:
                    event.response()
                            .putHeader("content-type", "application/json")
                            .end(new JsonObject()
                                    .put("status_code", 1)
                                    .put("job_id", result.JobID)
                                    .toString());
                    break;
            }
        });
    }

    private class Info{
        public String student_id;
        public List<String> courseids;
    }

    public void handle2(RoutingContext event) {
        String uid = event.request().getParam("uid");
        String[] courseids = event.request().getParam("courseids").split(",");//以逗号分割
        Info info = new Info();
        info.student_id = uid;
        info.courseids = Arrays.asList(courseids);
        Handler<AsyncResult<Message<Long>>> handler = res->{
            if(res.succeeded()) {
                this.allocatedTaskId(res,event,info);
            }
            else {
                this.allocateTaskIdFail(event,res.cause().toString());
            }
        };
        
        /**示例: 通过RedisProxy获取全局唯一的task id*/
        new RedisProxy(event.vertx()).allocateTaskId(handler);
    }

    private void allocatedTaskId(AsyncResult<Message<Long>> res,RoutingContext event,Info info){
        /* 在这之前，redis中应该存在了以下信息：
         * task_id   status
         *   1        ing
         *   2        done
         *   3        failed
         *   ...      ...
         *   x        ing        第一列表示任务id，第二列表示该任务的执行状态
         */
        Long task_id = new Long(res.result().body());
        /**
         * 立即返回taskid
         */
        event.response()
            .putHeader("content-type", "text/plain")
            .end("task_id = " + task_id);
        /**
         * 1. 查询courseids中的课程是否“合法”（这个应该是去操作静态的内容,理论上会比较快才对）
         *      1.1 课程是否存在
         *      1.2 课程时间与已选课程是否冲突
         * 2. 查询当前课程剩余人数(直接查redis，与mysql不一致并没有太大关系)
         *    2.1 redis获取剩余人数
         * 3. 更新课程剩余人数(操作mysql)，修改该学生的课表信息，并且将课程剩余人数写入redis
         *    3.1 如果此时更新失败（人数已满），返回失败，告诉前端将页面展示为“人数满”
         *    3.2 更新redis中的剩余人数（如果3.1不失败，可以选择性是否更新redis中剩余人数）
         *    3.2 上面这步执行成功，那么就可以直接修改该学生的课表信息了
         */
    }
    
    private void allocateTaskIdFail(RoutingContext event,String failed_reason) {
        event.response()
            .putHeader("content-type", "text/plain")
            .end("task_id = null,error:" + failed_reason);
    }
}
