package cn.pgyyd.mcg.verticle;

import cn.pgyyd.mcg.constant.McgConst;
import cn.pgyyd.mcg.ds.SelectCourseRequest;
import cn.pgyyd.mcg.ds.SelectCourseResult;
import cn.pgyyd.mcg.singleton.JobIDGenerator;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;

import java.util.ArrayDeque;
import java.util.Deque;

public class SelectCourseVerticle  extends AbstractVerticle {
    private Deque<Message> jobQueue = new ArrayDeque<>();
    private Deque<Integer> emptySeat = new ArrayDeque<>();


    @Override
    public void start() throws Exception {
        int maxDoinJobs = config().getInteger("max_doing_jobs");
        for (int i = 0; i < maxDoinJobs; i++) {
            emptySeat.add(1);
        }
        /*
        vertx.eventBus().consumer(McgConst.EVENT_BUS_SELECT_COURSE, msg->{
            if (doingJobs <= MAX_DOING_JOBS) {
                //立即选课，并返回选课结果
                JsonObject mySQLClientConfig = new JsonObject().put("host", "mymysqldb.mycompany");
                SQLClient sqlClient = MySQLClient.createShared(vertx, mySQLClientConfig, "select.write");
                sqlClient.getConnection(res->{
                    if (res.succeeded()) {
                        SQLConnection conn = res.result();
                    } else {
                        //
                    }
                });
                msg.reply(new SelectCourseResult(0, -1));
            } else {
                SelectCourseRequest request = (SelectCourseRequest)msg.body();
                request.JobID = JobIDGenerator.getInstance().generate();
                jobQueue.add(request);
                msg.reply(new SelectCourseResult(2, request.JobID));
            }
        });
        */
        vertx.eventBus().consumer(McgConst.EVENT_BUS_SELECT_COURSE, msg->{
            Integer seat = emptySeat.poll();
            if (seat == null) {
                //如果已经没有空余位置
                waitForAvailableSeat(msg);
                long jobID = JobIDGenerator.getInstance().generate();
                msg.reply(new SelectCourseResult(2, jobID));
            } else {
                doSelectCourse(msg, e->{
                    if (e.succeeded()) {
                        e.result().reply(new SelectCourseResult(0, -1));
                    } else {
                        e.result().reply(new SelectCourseResult(1, -1));
                    }
                });
            }
        });
    }

    private void waitForAvailableSeat(Message msg) {
        jobQueue.add(msg);
    }

    private void doSelectCourse(Message msg, Handler<AsyncResult<Message>> handler) {
        //TODO: 选课
        //1. 根据courseids取出课程的时间信息，根据学生id取出学生的课表信息(先不管redis缓存的事，直接从mysql取）
        //2. 遍历courseids，把跟学生课表时间能匹配上的courseid，取出组成新的courseids
        //3. 遍历courseids去更新remain表，更新成功的，组成一个success list，失败的组成一个fail list
        //4. 向handler写入结果: handler.handle(Future.succeededFuture(msg));
        //5. 处理剩余数据库操作
        //6. 退出

        Message waitMsg = jobQueue.poll();
        if (waitMsg != null) {
            doSelectCourse(waitMsg, e -> {
                //TODO: 结果写入redis
            });
        } else if (emptySeat.size() < config().getInteger("max_doing_jobs")) {
            emptySeat.add(1);
        }
    }
}
