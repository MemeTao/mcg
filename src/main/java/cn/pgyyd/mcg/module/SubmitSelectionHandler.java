package cn.pgyyd.mcg.module;

import cn.pgyyd.mcg.ds.SelectCourseRequest;
import cn.pgyyd.mcg.module.BussinessMessage.SelectCourseMessage;
import cn.pgyyd.mcg.verticle.SelectCourseVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.PatternSyntaxException;

import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class SubmitSelectionHandler implements Handler<RoutingContext> {
    static private AtomicInteger request_id = new AtomicInteger(0); 
    @Override
    public void handle(RoutingContext event) {
        String uid = event.request().getParam("uid");
        String courseids = event.request().getParam("courseids");
        if (StringUtils.isEmpty(uid) || StringUtils.isEmpty(courseids)) {
            event.fail(400);
            return;
        }
        ArrayList<String> courseIdList = new ArrayList<String>();
        String userId;
        try {
            userId = uid;
            //FIXME:我不知道如何直接转为ArrayList
            List<String> l =  Arrays.asList(courseids.split(","));
            for(String course : l) {
                courseIdList.add(course);
            }
        } catch (PatternSyntaxException e) {
            //TODO: log something
            event.fail(400);
            return;
        } catch (Exception e) {
            log.error("unknown exception");
            //unknown exception
            return;
        }
        long id = request_id.addAndGet(1);
        log.info("recived select course request,uid:" + userId + ",courseids:" + courseIdList + ",identification:"+ id);
        //构建选课消息
        SelectCourseMessage mess = new SelectCourseMessage(new SelectCourseRequest(userId,courseIdList),id);
        //事件总线参数（FIXME: static）
        DeliveryOptions options = new DeliveryOptions().setCodecName(new UserMessageCodec.CourseSelect().name());
        event.vertx().eventBus().send(SelectCourseVerticle.SELECT, mess,options,res -> {
            if(res.succeeded()) {
                SelectCourseMessage response = (SelectCourseMessage) res.result().body();
                event.response()
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                                .put("job_id", response.response().job_id())
                                .toString());
            }else {
                event.fail(400);
            }
        });
    }
}
