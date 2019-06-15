package cn.pgyyd.mcg.verticle;

import cn.pgyyd.mcg.ds.CourseSchedule;
import cn.pgyyd.mcg.ds.SelectCourseRequest;
import cn.pgyyd.mcg.ds.SelectCourseResponse;
import cn.pgyyd.mcg.module.MysqlProxy;
import cn.pgyyd.mcg.module.UserMessageCodec;
import cn.pgyyd.mcg.module.BussinessMessage.SelectCourseMessage;
import cn.pgyyd.mcg.singleton.JobIDGenerator;
import cn.pgyyd.mcg.ds.StudentSchedule;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SelectCourseVerticle<getCourseSchedule>  extends AbstractVerticle {
    static public String SELECT = "course-select";
    
    //private AtomicInteger seq_generator = new AtomicInteger(0);
    
    private static long num_success = 0;
    
    private static AtomicInteger temp_id = new AtomicInteger(0);
    
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
    
    private class SelectCourseOperator{
        private Task task;
        private Handler<ArrayList<String>> handler;
        //提交的待选课课程id  key: 课程id，value: 该课程的课程时间
        HashMap<String,CourseSchedule> courses_schdule = null;
        //该学生课程表(除本次选修课外)
        StudentSchedule student_schdule = null;
        
        ArrayList<String> successed_courses = new ArrayList<String>();
        
        public SelectCourseOperator(Task t, Handler<ArrayList<String>> h) {
            task = t;
            handler = h;
        }
        
        public void go() {
            log.info("SelectCourse Task running,identification:" + task.sequence);
            String student_id = task.request.student_id;
            ArrayList<String> courses_wanted = task.request.courses;
            
            Future<HashMap<String,CourseSchedule>> f_get_courses_schedule = Future.future();
            
            Future<Boolean> f_end = Future.future();
            
            //选课结束时会回调到这里
            f_end.setHandler(res->{
                //TODO:通知外面已成功的课程id
               if(res.failed()) {
                   log.error("select course failed,student_id:" + student_id + ",courses_wanted:" + courses_wanted);
               }
               log.info("select course operator done!identification:" + task.sequence + 
                       ",success:" + num_success + 
                       ",current time:" + System.currentTimeMillis());
               handler.handle(successed_courses);
            });
            //1.根据提交的课程id列表去获取这些课程的时间信息,成功后调用f_get_courses_schedule.completer()
            //FIXME: 这次查询可以和学生课表放在一块查
            mysqlProxy.getCourseSchedule(courses_wanted,task.sequence,f_get_courses_schedule.completer());
            
            f_get_courses_schedule.compose(v->{ //操作1完成后(成功)，会来到这里
                courses_schdule = v;
                if(courses_schdule.size() != courses_wanted.size()) {
                    log.error("getCourseSchedule wrong!");
                    //TODO: 不要让下面的操作继续执行
                }
                Future<StudentSchedule> f2 = Future.future();
                //2.根据学生id获取该学生的课表时间信息
                mysqlProxy.getStudentCourses(student_id,task.sequence,f2.completer());
                return f2;
            }).compose(v->{
                student_schdule = v;
                ArrayList<String> valid_course_ids = new ArrayList<String>();
                //3.遍历提交的选修课，检查时间是否冲突
                //FIXME: 测试时不检查冲突
                for(Entry<String, CourseSchedule> entry : courses_schdule.entrySet()) {
//                    CourseSchedule course_schedule = entry.getValue(); 
//                    if(!student_schdule.confict(course_schedule)) {
//                        valid_course_ids.add(entry.getKey());
//                    }
                    valid_course_ids.add(entry.getKey());
                }
                ArrayList<Future<Void>> fs = new ArrayList<Future<Void>>();
                //遍历不冲突的课程，进行选课
                for(String course_id : valid_course_ids) {
                    Future<Void> f = Future.future();
                    fs.add(f);
                    //4. 对该门课的剩余人数进行更新
                    //FIXME：这里也许需要用事务保证
                    //+1
                    mysqlProxy.updateRemain(course_id,1,task.sequence,res->{
                        if(res.succeeded()) {
                            num_success ++;
                            //String global = "" + 10000 + temp_id.addAndGet(1);
                            mysqlProxy.addElectiveCourse(student_id,course_id,task.sequence,res_add->{
                                if(res_add.succeeded()) {
                                    successed_courses.add(course_id);  //直到这里才算成功
                                }else {
//                                    mysqlProxy.updateRemain(course_id, 1,res_dec->{
//                                        //不可能失败，除非数据库死了
//                                    });
                                    log.error("addElectiveCourse failed!");
                                }
                                f.complete(); 
                            });
                        }else {
                            f.fail("updateRemain failed!"); 
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
        
        vertx.eventBus().registerCodec(new UserMessageCodec.CourseSelect());
        
        vertx.eventBus().consumer(SELECT, message->{
            SelectCourseMessage mess = (SelectCourseMessage) message.body();
            final long job_id = JobIDGenerator.getInstance().generate();
            //final long seq = seq_generator ++;
            Task task =  new Task(mess.request(), mess.id(), job_id);  //sequence 直接取自request_id
            jobs.put(task.sequence, task);
            set_task_status(task,Task.ING,res->{
                if(res) {
                    /*也可以立即返回，这时候如果客户端立即去查询redis可能还没有记录*/
                    SelectCourseMessage response = new SelectCourseMessage(new SelectCourseResponse(task.job_id));
                    DeliveryOptions options = new DeliveryOptions().setCodecName(new UserMessageCodec.CourseSelect().name());
                    message.reply(response,options);
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
            return;
        }
        Long key = jobs.firstKey();
        Task t = jobs.get(key);
        log.info("current task queue size:" + jobs.size() + ",schedule...identification:" + t.sequence);
        jobs.remove(key);       //立即删除
        doSelectCourse(t,res->{
            /*FIXME:选课结果可能是部分成功，在redis中需要另外一个地方来记录已成功课程s*/
            set_task_status(t,Task.DONE ,r->{
                if(res.size() == 0 ) {
                    //做不了任何事情，打日志就可以了
                    //客户端的行为：轮询失败，下次页面更新的时候才能看到他自己的选课记录
                }
            });
            schedule();
        });
    }
    private void doSelectCourse(Task task, Handler<ArrayList<String>> handler) {
        /**
         * java中的lambda表达式无法修改外部变量，所以用类的形式封装
         */
        new SelectCourseOperator(task,handler).go();
    }
    private void set_task_status(Task task,String status,Handler<Boolean> handler) {
        /**
         * redis.set(...,..., res-> { handler.handle(true)};
         */
        handler.handle(true);
    }
}
