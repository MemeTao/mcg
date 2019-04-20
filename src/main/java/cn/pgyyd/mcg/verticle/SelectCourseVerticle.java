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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

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

    private class StudentCourseTable {
        //FIXME: 这个类只是个大概，还有第一周到第十七周这个维度，暂时认为每周都一样的课; table的数据结构后面也要改
        public int uid;
        public List<List<Integer>> table;   //暂定：外层数组为，周一至周五，内层为某天的 第一节到第九节，最后的Integer是courseid，如果是null说明这节没课
    }
    private class Course {
        public int teacherid;
        public int courseid;
        public List<List<Boolean>> has_course;  //同上，周一至周五，第一节到第九节
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

    private boolean timeMatch(StudentCourseTable x, Course y) {
        return true;
    }

    private void doSelectCourse(Task task, Handler<AsyncResult<Message>> handler) {
        //1. 根据courseids取出课程的时间信息，根据学生id取出学生的课表信息(先不管redis缓存的事，直接从mysql取）
        //2. 遍历courseids，把跟学生课表时间能匹配上的courseid，取出组成新的courseids
        //3. 遍历courseids去更新remain表，更新成功的，组成一个success list
        //4. 向handler写入结果: handler.handle(Future.succeededFuture(msg));
        //5. 处理剩余数据库操作
        //6. 退出

        //1
        mysqlProxy.query("select xxxx from ", queryRes->{
            List<JsonObject> queryResult = queryRes.result().getRows();
            for (JsonObject row : queryResult) {
                //xxx
            }
            //假设已经知道该学生已有课表为studentCourseTable，各个课上课时间为courses
            StudentCourseTable studentCourseTable = new StudentCourseTable();
            List<Course> courses = new ArrayList<>();

            List<Course> timeMatchCourse = new ArrayList<>();
            for (Course course : courses) {
                if (timeMatch(studentCourseTable, course)) {
                    timeMatchCourse.add(course);
                }
            }

            if (timeMatchCourse.isEmpty()) {
                //TODO: 指明所有课都不匹配
                handler.handle(Future.succeededFuture(task.msg));
            }

            //TODO: 把for循环的东西写成正确的、同步的
            List<Integer> successCourse = new ArrayList<>();
            for (Course course : timeMatchCourse) {
                mysqlProxy.update("if remain > 0 remain++", updateRes->{
                    if (updateRes.succeeded()) {
                        successCourse.add(course.courseid);
                    }
                });
            }

            //处理其它关系表

            //返回handler里返回successCourse
            handler.handle(Future.succeededFuture(task.msg));
        });


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
