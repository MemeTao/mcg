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

        event.vertx().eventBus().send(McgConst.EVENT_BUS_SELECT_COURSE, new SelectCourseRequest(userId, courseIdList), res-> {
            SelectCourseResult result = (SelectCourseResult) res.result().body();
            switch (result.Status) {
                //直接选课成功
                case 0:
                    event.response()
                            .putHeader("content-type", "application/json")
                            .end(new JsonObject()
                                    .put("status_code", 0)
                                    .toString());
                    break;
                //选课失败
                case 1:
                    event.response()
                            .putHeader("content-type", "application/json")
                            .end(new JsonObject()
                                    .put("status_code", 1)
                                    .toString());
                    break;
                //选课排队中
                case 2:
                    event.response()
                            .putHeader("content-type", "application/json")
                            .end(new JsonObject()
                                    .put("status_code", 2)
                                    .put("jobid", result.PendingID)
                                    .toString());
            }
        });
    }
}
