package cn.pgyyd.mcg.verticle;

import cn.pgyyd.mcg.constant.McgConst;
import cn.pgyyd.mcg.ds.SelectCourseRequest;
import cn.pgyyd.mcg.ds.SelectCourseResult;
import cn.pgyyd.mcg.singleton.JobIDGenerator;
import io.vertx.core.AbstractVerticle;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class SelectCourseVerticle  extends AbstractVerticle {
    private Queue<SelectCourseRequest> jobQueue = new ArrayDeque<>();
    private AtomicInteger doingJobs = new AtomicInteger(0);

    private int MAX_DOING_JOBS = 5;

    @Override
    public void start() throws Exception {
        vertx.eventBus().consumer(McgConst.EVENT_BUS_SELECT_COURSE, msg->{
            //此处的jobQueue只在1个线程操作
            if (jobQueue.size() <= MAX_DOING_JOBS) {
                //立即选课，并返回选课结果
                msg.reply(new SelectCourseResult(0, -1));
            } else {
                SelectCourseRequest request = (SelectCourseRequest)msg.body();
                request.JobID = JobIDGenerator.getInstance().generate();
                jobQueue.add(request);
                msg.reply(new SelectCourseResult(2, request.JobID));
            }
        });
    }
}
