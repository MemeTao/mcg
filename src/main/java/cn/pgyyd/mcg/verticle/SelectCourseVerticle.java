package cn.pgyyd.mcg.verticle;

import cn.pgyyd.mcg.ds.CourseSchedule;
import cn.pgyyd.mcg.ds.SelectCourseRequest;
import cn.pgyyd.mcg.module.MysqlProxy;
import cn.pgyyd.mcg.singleton.JobIDGenerator;
import cn.pgyyd.mcg.ds.StudentSchedule;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

public class SelectCourseVerticle<getCourseSchedule>  extends AbstractVerticle {
    static public String SELECT = "course-select";
    private long seq_generator = 0;
    
    private class Task {

        final private static String ING = "ing";
        
        final private static String DONE = "done";
        /**
         * @param r 选课请求
         * @param seq 序号(本线程内部使用)
         * @param id  task id(全局)
         */
        Task(final SelectCourseRequest r,final long seq ,final long id) {
            request = r;
            sequence = seq;
            job_id = id;
        }
        public final SelectCourseRequest request;
        public final long sequence;
        public final long job_id;
    }
    
    private TreeMap<Long,Task> jobs = new TreeMap<Long,Task>();
    
    private MysqlProxy mysqlProxy;
    
    private class SelectCourse{
        private Task task;
        private Handler<Boolean> handler;
        HashMap<Integer/*课程id*/,CourseSchedule/*该课程的时间信息*/> courses_schdule = null;
        StudentSchedule student_schdule = null;  //该学生课程表(除本次选修课外)
        public SelectCourse(Task t, Handler<Boolean> h) {
            task = t;
            handler = h;
            go();
        }
        private void go() {
            int student_id = task.request.UserID;
            List<Integer> courses_wanted = task.request.CourseIDs;
            Future<HashMap<Integer,CourseSchedule>> f_get_courses_schedule = Future.future();
            f_get_courses_schedule.setHandler(res->{
                if(res.succeeded()) {
                    courses_schdule = res.result();
                }
            });
            
            Future<Boolean> f_end = Future.future();
            f_end.setHandler(res->{
                if(res.failed()) {
                    //选课失败
                }else {
                    //选课成功
                    handler.handle(true);
                }
            });
            
            //1.根据提交的课程id列表去获取这些课程的时间信息,成功后调用f_get_courses_schedule.completer()
            mysqlProxy.getCourseSchedule(courses_wanted,f_get_courses_schedule.completer());
            
            f_get_courses_schedule.compose(v->{     //操作1完成后，会来到这里 
                Future<StudentSchedule> f2 = Future.future();
                f2.setHandler(res->{
                    if(res.succeeded()) {
                        student_schdule = res.result();
                    }
                });
                //2.根据学生id获取该学生的课表时间信息
                mysqlProxy.getStudentCourses(student_id,f2.completer());
                return f2;
            }).compose(v->{
                ArrayList<Integer> valid_course_ids = new ArrayList<Integer>();
                //3.遍历提交的选修课，检查时间是否冲突
                for(Entry<Integer, CourseSchedule> entry:courses_schdule.entrySet()) {
                    CourseSchedule course_schedule = entry.getValue(); //这门课的时间安排
                    TreeMap<Integer,ArrayList<Integer>> schedule = course_schedule.lesson(); //每门课可能有好几个时间段
                    boolean conflict = false;
                    for(Entry<Integer,ArrayList<Integer>> item : schedule.entrySet()) {  //遍历每一天
                        int day = item.getKey();  
                        ArrayList<Integer> lessons = item.getValue(); 
                        
                        if(student_schdule.exsit(day, lessons)) { //如果该学生的课表与这个时间段冲突
                            conflict = true;
                            break;
                        }
                    }
                    //检查结束
                    if(!conflict) {  //如果不冲突
                        valid_course_ids.add(entry.getKey());
                    }
                }
                //遍历不冲突的课程，进行选课
                ArrayList<Future<Void>> fs = new ArrayList<Future<Void>>();
                for(int course_id : valid_course_ids) {
                    Future<Void> f = Future.future();
                    fs.add(f);
                    //4. 往该学生的"选修课课表"中添加选修课记录
                    mysqlProxy.addElectiveCourse(student_id,course_id,res->{
                        if(res.succeeded()) {
                            f.complete();
                        }else {
                            f.fail("addElectiveCourse fail,student id:" + student_id + " course_id:" + course_id );
                        }
                    });
                }
                CompositeFuture.all(new ArrayList<>(fs)).setHandler(res->{
                    if(res.succeeded()) {
                        f_end.complete(); 
                    }else {
                        f_end.fail("fuck!");
                    }
                });
            },f_end);
        }
    }

    @Override
    public void start() throws Exception {

        mysqlProxy = new MysqlProxy(vertx);
        
        vertx.eventBus().consumer(SELECT, msg->{
            /**
             * 假设msg内部存放了一个request
             * 这里将“请求”单独拿出来
             */
            SelectCourseRequest request = (SelectCourseRequest) msg.body();
            final long job_id = JobIDGenerator.getInstance().generate();
            final long seq = seq_generator ++;
            Task task =  new Task(request, seq, job_id);
            jobs.put(task.sequence, task);
            set_task_status(task,Task.ING,res->{
                if(res) {
                    msg.reply(task.job_id);   /*也可以立即返回，但是这时候客户端如果立即去查询，redis可能还没有记录*/
                }else {
                    //不作任何回应，事件总线超时自动失败(或者返回一个非法job_id标识失败)
                    if(jobs.containsKey(task.sequence)) {
                        jobs.remove(task.sequence);
                    }
                }
            });
            schedule();
        });
    }
    private void schedule() {
        if(jobs.size() <= 0) {
            /*log something*/
            return;
        }
        Long key = jobs.firstKey();
        Task t = jobs.get(key);
        doSelectCourse(t,res->{
            jobs.remove(key);
            set_task_status(t,Task.DONE ,r->{
                if(r) {/*log is enough*/
                    ;
                }
            });
            schedule();
        });
    }
    private void doSelectCourse(Task task, Handler<Boolean> handler) {
        //1. 根据courseids取出课程的时间信息，根据学生id取出学生的课表信息(先不管redis缓存的事，直接从mysql取）
        //2. 遍历courseids，把跟学生课表时间能匹配上的courseid，取出组成新的courseids
        //3. 遍历courseids去更新remain表，更新成功的，组成一个success list
        //4. 向handler写入结果: handler.handle(Future.succeededFuture(msg));
        //5. 处理剩余数据库操作
        //6. 退出
        ;
        /**
         * java中的lambda表达式无法修改外部变量，所以用类的形式封装
         */
        new SelectCourse(task,handler);
    }
    private void set_task_status(Task task,String status,Handler<Boolean> handler) {
        /**
         * redis.set(...,..., res-> { handler.handle(true)};
         */
        handler.handle(true);
        
    }
}
