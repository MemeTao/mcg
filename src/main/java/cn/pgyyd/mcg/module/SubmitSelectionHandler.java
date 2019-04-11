package cn.pgyyd.mcg.module;

import cn.pgyyd.mcg.constant.McgConst;
import cn.pgyyd.mcg.ds.SelectCourseRequest;
import cn.pgyyd.mcg.ds.SelectCourseResult;
import cn.pgyyd.mcg.singleton.JobCounter;
import cn.pgyyd.mcg.singleton.JobIDGenerator;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SubmitSelectionHandler implements Handler<RoutingContext> {
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

        int doingJobs = JobCounter.getInstance().get();
        if (doingJobs < 50) {
            //TODO: 完善逻辑
            event.vertx().eventBus().send(McgConst.SELECTING_COURSE, new SelectCourseRequest(userId, -1, courseIdList), res->{
                SelectCourseResult result = (SelectCourseResult)res.result().body();
                event.response().putHeader("content-type", "application/json").end(new JsonObject().toString());
            });
        } else {
            int jobId = JobIDGenerator.getInstance().generate();
            event.vertx().eventBus().send(McgConst.SELECT_COURSE_QUEUE, new SelectCourseRequest(userId, jobId, courseIdList));
            event.response().putHeader("content-type", "application/json").end(new JsonObject().put("jobid", jobId).toString());
        }
    }
}
