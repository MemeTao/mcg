package cn.pgyyd.mcg.verticle;

import cn.pgyyd.mcg.constant.McgConst;
import cn.pgyyd.mcg.ds.SelectCourseRequest;
import cn.pgyyd.mcg.ds.SelectCourseResult;
import cn.pgyyd.mcg.module.MysqlProxy;
import cn.pgyyd.mcg.singleton.JobIDGenerator;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.ext.sql.ResultSet;

import java.util.ArrayDeque;
import java.util.Deque;

public class SelectCourseVerticle  extends AbstractVerticle {
    private class Task {
        Task(Message msg, Long jobid) {
            this.msg = msg;
            this.jobid = jobid;
        }
        Task(Message msg) {
            this.msg = msg;
        }
        Message msg;
        Long jobid;
    }
    private Deque<Task> jobQueue = new ArrayDeque<>();
    private Deque<Integer> emptySeat = new ArrayDeque<>();
    private MysqlProxy mysqlProxy;


    @Override
    public void start() throws Exception {
        int maxDoinJobs = config().getInteger("max_doing_jobs");
        for (int i = 0; i < maxDoinJobs; i++) {
            emptySeat.add(1);
        }
        mysqlProxy = new MysqlProxy(vertx);
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
                //如果已经没有空余位置，排队，并立即返回排队id
                long jobID = JobIDGenerator.getInstance().generate();
                waitForAvailableSeat(new Task(msg, jobID));
                msg.reply(new SelectCourseResult(1, jobID));
            } else {
                doSelectCourse(new Task(msg), e->{
                    if (e.succeeded()) {
                        e.result().reply(new SelectCourseResult(0, -1));
                    } else {
                        //不应该走到这来
                        //e.result().reply(new SelectCourseResult(-1, -1));
                    }
                });
            }
        });
    }

    private void waitForAvailableSeat(Task task) {
        jobQueue.add(task);
    }

    private void doSelectCourse(Task task, Handler<AsyncResult<Message>> handler) {
        //TODO: 选课
        //1. 根据courseids取出课程的时间信息，根据学生id取出学生的课表信息(先不管redis缓存的事，直接从mysql取）
        mysqlProxy.query("select xxxx", someThing->{
            ResultSet resultSet = (ResultSet)someThing.result();
            resultSet.getResults();

        });
        //2. 遍历courseids，把跟学生课表时间能匹配上的courseid，取出组成新的courseids
        //3. 遍历courseids去更新remain表，更新成功的，组成一个success list，失败的组成一个fail list
        //4. 向handler写入结果: handler.handle(Future.succeededFuture(msg));
        //5. 处理剩余数据库操作
        //6. 退出

        Task queuedTask = jobQueue.poll();
        if (task != null) {
            doSelectCourse(queuedTask, e -> {
                //TODO: 结果写入redis
            });
        } else if (emptySeat.size() < config().getInteger("max_doing_jobs")) {
            emptySeat.add(1);
        }
    }
}
