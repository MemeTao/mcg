package cn.pgyyd.mcg.verticle;

import cn.pgyyd.mcg.constant.McgConst;
import cn.pgyyd.mcg.ds.SelectCourseRequest;
import cn.pgyyd.mcg.ds.SelectCourseResult;
import io.vertx.core.AbstractVerticle;

public class SelectCourseVerticle  extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        vertx.eventBus().consumer(McgConst.SELECTING_COURSE, msg->{
            SelectCourseRequest request = (SelectCourseRequest)msg.body();
            //xxxxx
            msg.reply(new SelectCourseResult());
        });
        vertx.eventBus().consumer(McgConst.SELECT_COURSE_QUEUE, msg->{
            SelectCourseRequest request = (SelectCourseRequest)msg.body();
        });
    }
}
