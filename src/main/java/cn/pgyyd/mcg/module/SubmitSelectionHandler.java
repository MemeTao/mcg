package cn.pgyyd.mcg.module;

import cn.pgyyd.mcg.constant.McgConst;
import cn.pgyyd.mcg.ds.SelectCourseMessage;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.List;

import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Collectors;

@Slf4j
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
            log.error(String.format("parameters format wrong, uid:%s, courseids:%s", uid, courseids));
            event.fail(400);
            return;
        } catch (Exception e) {
            //unknown exception
            return;
        }

        SelectCourseMessage msg = new SelectCourseMessage();
        msg.request = msg.new SelectCourseRequest(userId, courseIdList);
        log.info("send request to SelectCourseVerticleKt");
        DeliveryOptions deliveryOptions = new DeliveryOptions().setCodecName(new UserMessageCodec.SelectCourseMessageCodec().name());
        event.vertx().eventBus().send(McgConst.EVENT_BUS_SELECT_COURSE, msg, deliveryOptions, res -> {
            SelectCourseMessage replyMsg = (SelectCourseMessage) res.result().body();
            switch (replyMsg.result.status) {
                //处理完成
                case 0:
                    log.debug("receive immediate reply from SelectCourseVerticleKt");
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
                    log.debug("receive pending reply from SelectCourseVerticleKt");
                    event.response()
                            .putHeader("content-type", "application/json")
                            .end(new JsonObject()
                                    .put("status_code", 1)
                                    .put("job_id", replyMsg.result.jobID)
                                    .toString());
                    break;
                default:
                    log.debug("receive wrong reply type");
            }
        });
    }
}
