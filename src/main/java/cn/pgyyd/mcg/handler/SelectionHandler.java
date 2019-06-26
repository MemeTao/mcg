package cn.pgyyd.mcg.handler;

import cn.pgyyd.mcg.constant.McgConst;
import cn.pgyyd.mcg.ds.SelectCourseMessage;
import cn.pgyyd.mcg.ds.UserMessageCodec;
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
public class SelectionHandler implements Handler<RoutingContext> {
    @Override
    public void handle(RoutingContext event) {
        String userId = event.request().getParam("uid");
        String courseids = event.request().getParam("courseids");
        
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(courseids)) {
            event.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("status_code", 1)
                            .put("msg", "uid和courseids不能为空")
                            .toString());
            return;
        }
        //解析url参数courseids
        List<Long> courseIdList;
        try {
            courseIdList = Arrays.stream(courseids.split(","))
                    .filter(s -> !s.isEmpty())
                    .mapToLong(Long::parseLong)
                    .boxed()
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            log.error(String.format("parameters format wrong, courseids:%s", courseids));
            event.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("status_code", 1)
                            .put("msg", "courseids格式错误")
                            .toString());
            return;
        } catch (Exception e) {
            log.error("unknown error", e);
            event.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("status_code", 1)
                            .put("msg", "未知错误")
                            .toString());
            return;
        }

        SelectCourseMessage msg = new SelectCourseMessage();
        msg.request = msg.new SelectCourseRequest(userId, courseIdList);
        log.info("send request to SelectCourseVerticleKt");
        DeliveryOptions deliveryOptions = new DeliveryOptions().setCodecName(new UserMessageCodec.SelectCourseMessageCodec().name());
        event.vertx().eventBus().send(McgConst.EVENT_BUS_SELECT_COURSE, msg, deliveryOptions, res -> {
            if (res.failed()) {
                log.error("send via eventbus fail", res.cause());
                event.response()
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                                .put("status_code", 1)
                                .put("msg", "内部错误")
                                .toString());
                return;
            }
            SelectCourseMessage replyMsg = (SelectCourseMessage) res.result().body();
            switch (replyMsg.result.status) {
                //处理完成
                case 0:
                    log.info("receive immediate reply from SelectCourseVerticleKt");
                    JsonArray selectResults = new JsonArray();
                    for (SelectCourseMessage.Result r : replyMsg.result.results) {
                        selectResults.add(new JsonObject().put("course", r.courseID).put("success", r.success));
                    }
                    event.response()
                            .putHeader("content-type", "application/json")
                            .end(new JsonObject()
                                    .put("status_code", 0)
                                    .put("results", selectResults)
                                    .toString());
                    break;
                //排队选课
                case 1:
                    log.info("receive pending reply from SelectCourseVerticleKt");
                    event.response()
                            .putHeader("content-type", "application/json")
                            .end(new JsonObject()
                                    .put("status_code", 1)
                                    .put("jobid", replyMsg.result.jobID)
                                    .toString());
                    break;
                default:
                    log.error("receive wrong reply type");
                    event.response()
                            .putHeader("content-type", "application/json")
                            .end(new JsonObject()
                                    .put("status_code", 1)
                                    .put("msg", "内部错误")
                                    .toString());
                    return;
            }
        });
    }
}
