package cn.pgyyd.mcg.module;

import cn.pgyyd.mcg.constant.McgConst;
import cn.pgyyd.mcg.ds.SelectCourseMessage;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.List;

import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

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
        List<Integer> courseIdList;
        int userId;
        try {
            userId = Integer.parseInt(uid);
            courseIdList = Arrays.stream(courseids.split(","))
                    .filter(s -> !s.isEmpty())
                    .mapToInt(Integer::parseInt)
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

        SelectCourseMessage msg = new SelectCourseMessage();
        msg.request = msg.new SelectCourseRequest(userId, courseIdList);
        event.vertx().eventBus().send(McgConst.EVENT_BUS_SELECT_COURSE, msg, res -> {
            SelectCourseMessage replyMsg = (SelectCourseMessage) res.result().body();
            switch (replyMsg.result.Status) {
                //处理完成
                case 0:
                    JsonArray selectResults = new JsonArray();
                    for (SelectCourseMessage.Result r : replyMsg.result.Results) {
                        selectResults.add(new JsonObject().put("course", r.courseID).put("status", r.success));
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
                                    .put("job_id", replyMsg.request.jobID)
                                    .toString());
                    break;
            }
        });
    }
}
