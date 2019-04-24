package cn.pgyyd.mcg.module;

import cn.pgyyd.mcg.constant.McgConst;
import cn.pgyyd.mcg.ds.SelectCourseRequest;
import cn.pgyyd.mcg.module.BussinessMessage.SelectCourseMessage;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;

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
        ArrayList<Integer> courseIdList;
        int userId;
        try {
            userId = Integer.parseInt(uid);
            courseIdList = (ArrayList<Integer>) Arrays.stream(courseids.split(","))
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
        //构建选课消息
        SelectCourseMessage mess = new SelectCourseMessage(new SelectCourseRequest(userId,courseIdList));
        //事件总线参数（FIXME: static）
        DeliveryOptions options = new DeliveryOptions().setCodecName(new UserMessageCodec.CourseSelect().name());
        
        event.vertx().eventBus().send(McgConst.EVENT_BUS_SELECT_COURSE, mess,options,res -> {
            SelectCourseMessage response = (SelectCourseMessage) res.result().body();
            event.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("status_code", 1)
                            .put("job_id", response.response())
                            .toString());

        });
    }
}
