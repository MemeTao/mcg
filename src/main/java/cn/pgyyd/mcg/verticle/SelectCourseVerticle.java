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

import java.util.ArrayList;
import java.util.HashMap;
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
    
    private class SelectCourseOperator{
        private Task task;
        private Handler<ArrayList<Integer>> handler;
        //提交的带选课课程id  key: 课程id，value: 该课程的课程时间
        HashMap<Integer,CourseSchedule> courses_schdule = null;
        //该学生课程表(除本次选修课外)
        StudentSchedule student_schdule = null;  

        ArrayList<Integer> successed_courses = new ArrayList<Integer>();
        
        public SelectCourseOperator(Task t, Handler<ArrayList<Integer>> h) {
            task = t;
            handler = h;
        }
        
        public void go() {
            System.out.println("[info] select course...");
            int student_id = task.request.student_id;
            ArrayList<Integer> courses_wanted = task.request.courses;
            
            Future<HashMap<Integer,CourseSchedule>> f_get_courses_schedule = Future.future();
            
            Future<Boolean> f_end = Future.future();
            
            f_get_courses_schedule.setHandler(res->{
                if(res.succeeded()) {
                    courses_schdule = res.result();   
                }
            });
            //选课结束时会回调到这里
            f_end.setHandler(res->{
                //TODO:通知外面已成功的课程id
               if(res.failed()) {
                   System.out.println("[warning] select course failed");
               }
                handler.handle(successed_courses);
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
                for(Entry<Integer, CourseSchedule> entry : courses_schdule.entrySet()) {
                    //这门课的时间安排
                    CourseSchedule course_schedule = entry.getValue(); 
                    //这门课在一周中可能有好几个时间段,key:周x  value: 第几节
                    TreeMap<Integer,ArrayList<Integer>>  schedule_day = course_schedule.lesson(); 
                    boolean conflict = false;
                    //遍历每一天
                    for(Entry<Integer,ArrayList<Integer>> item : schedule_day.entrySet()) {
                        int day = item.getKey();  
                        ArrayList<Integer> lessons = item.getValue();
                        //如果该学生的课表与这个时间段冲突
                        if(student_schdule.exsit(day, lessons)) {
                            conflict = true;
                            break;
                        }
                    }
                    //如果这门课的时间安排和该学生的所有课程都不冲突
                    if(!conflict) {
                        valid_course_ids.add(entry.getKey());
                    }
                }
                
                ArrayList<Future<Void>> fs = new ArrayList<Future<Void>>();
                //遍历不冲突的课程，进行选课
                for(int course_id : valid_course_ids) {
                    Future<Void> f = Future.future();
                    fs.add(f);
                    //4. 对该门课的剩余人数进行更新
                    mysqlProxy.updateRemain(course_id, res->{
                        if(res.succeeded()) {
                            //往该学生课表中增加课程记录(添加结束再返回成功，防止该学生瞬间提交两次相同的课程)
                            mysqlProxy.addElectiveCourse(student_id,course_id,res_add->{
                                if(res_add.succeeded()) {
                                    successed_courses.add(course_id);  //直到这里才算成功
                                }
                            });
                        }
                        f.complete(); //不管"更新剩余人数"是否成功，总是要通知最后阶段
                    });
                }
                CompositeFuture.all(new ArrayList<>(fs)).setHandler(res->{
                    f_end.complete(); 
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
            System.out.println("[info] select course verticle recived mess,student id " 
                                + mess.request().student_id + ",courses list:" + mess.request().courses);
            final long job_id = JobIDGenerator.getInstance().generate();
            final long seq = seq_generator ++;
            Task task =  new Task(mess.request(), seq, job_id);
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
        doSelectCourse(t,res->{
            /*FIXME:选课结果可能是部分成功，在redis中需要另外一个地方来记录已成功课程s*/
            set_task_status(t,Task.DONE ,r->{
                if(res.size() == 0 ) {
                    //做不了任何事情，打日志就可以了
                    //客户端的行为：轮询失败，下次页面更新的时候才能看到他自己的选课记录
                    System.out.println("[waring] misson failed!");
                }
            });
            if(jobs.containsKey(key)) {
                jobs.remove(key);
            }
            schedule();
        });
    }
    private void doSelectCourse(Task task, Handler<ArrayList<Integer>> handler) {
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
