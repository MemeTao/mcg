package cn.pgyyd.mcg.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import cn.pgyyd.mcg.ds.CourseSchedule;
import cn.pgyyd.mcg.ds.StudentSchedule;
import cn.pgyyd.mcg.verticle.MySqlVerticle;
import cn.pgyyd.mcg.verticle.SelectCourseVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import lombok.extern.slf4j.Slf4j;

/**
 * 在该类中添加的任何业务接口务必保证有详细的说明，接口的参数以及返回值等类型尽力做到"不改动"
 * 当前存在的业务接口有：
 * 1. 获取所有课程id列表
 * 2. 根据课程id，获取该课程的时间信息
 * 3. 根据学生学号，获取该学生的全部课程(除本次选修课以外)
 * @author memetao
 *
 */
@Slf4j
public class MysqlProxy {
    /**
     * sql查询结果封装
     * */
    private static String EXEC = MySqlVerticle.EXEC;
    
    private static String QUERY = MySqlVerticle.QUERY;
    
    private static String UPDATE = MySqlVerticle.UPDATE;
    
    private static String TRANSACTION = MySqlVerticle.TRANSACTION;
    
    private Vertx vertx;
    
    private class Failed<T> implements  AsyncResult<T>{
        @Override
        public T result() {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public Throwable cause() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean succeeded() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean failed() {
            // TODO Auto-generated method stub
            return !succeeded();
        }
        ;
    }
    private class Success<T> implements  AsyncResult<T>{
        
        private T t1;
        
        public  Success(T t){
            this.t1 = t;
        }
        @Override
        public T result() {
            // TODO Auto-generated method stub
            return t1;
        }
        @Override
        public Throwable cause() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean succeeded() {
            // TODO Auto-generated method stub
            return t1 != null;
        }

        @Override
        public boolean failed() {
            // TODO Auto-generated method stub
            return !succeeded();
        }
        ;
    }
    
    public MysqlProxy(Vertx v){
        vertx = v;
    }
    /**
     * 示例：获取课程表中的所有课程id
     * @param reply
     */
    //FIXME: 使用AsyncResult封装HashSet<Integer>
    public void getCourseIdList(Handler<HashSet<Integer>> reply) {
        String sql = "select courseId from mcg_course";
        query(sql,res->{
            HashSet<Integer> ids = new HashSet<Integer>();
            if( res.succeeded() ) {
                String column_name = res.result().getColumnNames().get(0);
                for(JsonObject obj : res.result().getRows()) {
                    ids.add(obj.getInteger(column_name));
                }
            }
            reply.handle(ids);
        });
    }
    /**
     * 根据课程id列表获取课程时间信息
     * @param course_ids
     * @param reply  key:课程id  value:该课程的时间
     */
    public void getCourseSchedule(ArrayList<Integer> course_ids,long identification,Handler<AsyncResult<HashMap<Integer,CourseSchedule>>> reply) {
        if(identification != -1) {
            log.debug("try getCourseSchedule,identification:" + identification);
        }
        if(course_ids.size() == 0 ) {
            reply.handle(new Failed<HashMap<Integer,CourseSchedule>>());
        }
        HashMap<Integer,CourseSchedule> infos = new HashMap<Integer,CourseSchedule>();
        String ids = new String("(");
        for(int course_id : course_ids) {
            ids += course_id;
            ids += ",";
        }
        ids = ids.substring(0, ids.length()-1);
        ids += ")";
        
        String sql = "select * from mcg_course_timerange where courseId in " + ids;
        query(sql,identification,res->{
            if( res != null && res.succeeded() )
            {
                //long cost = System.currentTimeMillis() - t1;
                //log.info("mysql proxy getCourseSchedule cost:" + cost + "ms");
                ResultSet result = res.result();
                //会存在多行结果
                for(JsonObject obj : result.getRows()) {
                    final int course_id = obj.getInteger(result.getColumnNames().get(1));
                    final int day = obj.getInteger(result.getColumnNames().get(2));
                    final int start = obj.getInteger(result.getColumnNames().get(3));
                    final int end = obj.getInteger(result.getColumnNames().get(4));
                    CourseSchedule course = new CourseSchedule(course_id);
                    if(!infos.containsKey(course_id)) {
                        infos.put(course_id, course);
                    }
                    course = infos.get(course_id);
                    course.add_info(day, start,end);
                }
                reply.handle(new Success<HashMap<Integer,CourseSchedule>>(infos));
            }
            else 
            {
                reply.handle(new Failed<HashMap<Integer,CourseSchedule>>());
            }
        });
    }
    /**
     * 不带标识符
     * @param course_ids
     * @param reply
     */
    public void getCourseSchedule(ArrayList<Integer> course_ids,Handler<AsyncResult<HashMap<Integer,CourseSchedule>>> reply) {
        getCourseSchedule(course_ids,-1,res->{
            reply.handle(res);
        });
    }
    /**
     * 获取某门课程的上课时间
     * @param course_id
     * @param reply
     */
    public void getCourseSchedule(Integer course_id,long identification,Handler<AsyncResult<CourseSchedule>> reply) {
        ArrayList<Integer> course_ids = new ArrayList<Integer>();
        course_ids.add(course_id);
        getCourseSchedule(course_ids,identification,res->{
            if(res.succeeded()) {
                CourseSchedule schedule = null;
                for(Entry <Integer,CourseSchedule> it :  res.result().entrySet()) {
                    schedule = it.getValue();
                    break;
                }
                reply.handle(new Success<CourseSchedule>(schedule));
            }else {
                reply.handle(new Failed<CourseSchedule>());
            }
        });
    }
    
    public void getCourseSchedule(Integer course_id,Handler<AsyncResult<CourseSchedule>> reply) {
        getCourseSchedule(course_id,-1,res->{
            reply.handle(res);
        });
    }
    /**
     * 获取学生的课表
     * @param student_id
     * @param reply
     */
    public void getStudentCourses(int student_id,long identification,Handler<AsyncResult<StudentSchedule>> reply) {
        if(identification != -1) {
            log.debug("try getStudentCourses,identification:" + identification);
        }
        //long t1 = System.currentTimeMillis();
        StudentSchedule infos = new StudentSchedule(student_id);
        //获取该学生有哪些课程
        String sql = "select courseId from mcg_student_course where studentId = " + student_id;
        query(sql,identification,res->{
            if( res != null && res.succeeded() ) {
                ResultSet result = res.result();
                //会存在多行结果
                ArrayList<Integer> course_ids = new ArrayList<Integer>();
                for(JsonObject obj : result.getRows()) {
                    final int course_id = obj.getInteger(result.getColumnNames().get(0));
                    course_ids.add(course_id);
                }
                getCourseSchedule(course_ids,identification,course_res->{
                    if(identification != -1) {
                        log.debug("get student course done,identification:" + identification);
                    }
                    if(course_res.succeeded()) {
                        HashMap<Integer,CourseSchedule> courses_schedule = course_res.result();
                        for(Entry<Integer,CourseSchedule> item : courses_schedule.entrySet()) {
                            infos.add_course(item.getValue());
                        }
                        reply.handle(new Success<StudentSchedule>(infos));
                    }else {
                        reply.handle(new Failed<StudentSchedule>());
                    }
                });
           }
        });
    }
    
    public void getStudentCourses(int student_id, Handler<AsyncResult<StudentSchedule>> reply) {
        getStudentCourses(student_id,-1,res->{
            reply.handle(res);
        });
    }
    /**
     * 更新目标课程的剩余人数(选课的最终结果)
     * @param course_id
     * @param num : 你懂得
     * @param reply  如果写入，则返回true，否则返回false
     */
    public void updateRemain(int course_id, int num, long identification,Handler<AsyncResult<Boolean>> reply) {
        if(identification != -1) {
            log.debug("try updateRemain,identification:" + identification);
        }
        //long t1 = System.currentTimeMillis();
        String sql = new String();
        if(num < 0) {
            num = -num;
            sql = "update mcg_course_remain set remain = remain - " + num
                                + " where courseId = " + course_id +
                                  " and remain > 0";
        }else {
            sql = "update mcg_course_remain set remain = remain + " + num
                    + " where courseId = " + course_id ;
        }
        update(sql,identification,res->{
            if(identification != -1) {
                log.debug("updateRemain done,identification:" + identification);
            }
            if(res.succeeded()) {
                //long cost = System.currentTimeMillis() - t1;
                //log.info("mysql proxy updateRemain:" + cost + "ms");
                reply.handle(new Success<Boolean>(true));
            }else {
                reply.handle(new Failed<Boolean>());
            }
        });
    }
    public void updateRemain(int course_id, int num,Handler<AsyncResult<Boolean>> reply) {
        updateRemain(course_id, num,-1, res->{
            reply.handle(res);
        }) ;
    }
    /**
     * 向某学生的选课课课表中，增加信息
     * @param student_id
     * @param course_id 
     * @param reply
     */
    public void addElectiveCourse(int student_id,int course_id,long identification,Handler<AsyncResult<Boolean>> reply) {
        if(identification != -1) {
            log.debug("try addElectiveCourse,identification:" + identification);
        }
        String sql = "insert into mcg_student_course "
                + "(studentId,"
                + "courseId)"
                +  " values "
                +                "(" 
                +   student_id +  "," 
                +   course_id +  ")";
        
        update(sql,identification,res->{
            if(identification != -1) {
                log.debug("add elective course done,identification:" + identification);
            }
            if(res.succeeded()) {
                reply.handle(new Success<Boolean>(true));
            }else {
                reply.handle(new Failed<Boolean>());
            }
        });
    }
    public void addElectiveCourse(int student_id,int course_id,Handler<AsyncResult<Boolean>> reply) {
        addElectiveCourse( student_id,course_id,res->{
            reply.handle(res);
        });
    }
    /********************************************业务代码*******************************************************/
    /*********************************************分界线********************************************************/
    /********************************************基础代码*******************************************************/
    
    public void execute(String op,Handler<AsyncResult<Void>> reply) {
        Handler<AsyncResult<Message<MysqlMessage.ExecuteMessage>>> handler = res ->{
            AsyncResult<Void> result = res.result().body().result();
            reply.handle(result);
        };
        DeliveryOptions options = new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlExecute().name());
        vertx.eventBus().send(EXEC,new MysqlMessage.ExecuteMessage(op),options,handler);
    }
    
    public void execute(String op,long identification,Handler<AsyncResult<Void>> reply) {
        Handler<AsyncResult<Message<MysqlMessage.ExecuteMessage>>> handler = res ->{
            AsyncResult<Void> result = res.result().body().result();
            reply.handle(result);
        };
        DeliveryOptions options = new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlExecute().name());
        vertx.eventBus().send(EXEC,new MysqlMessage.ExecuteMessage(op,identification),options,handler);
    }
    
    public void query(String op,Handler<AsyncResult<ResultSet>> reply) {
        Handler<AsyncResult<Message<MysqlMessage.QueryMessage>>> handler = res ->{
            if(res.succeeded()) {
                AsyncResult<ResultSet> result = res.result().body().result();
                reply.handle(result);
            }else {
                reply.handle(new Failed<ResultSet>());
            }
        };
        DeliveryOptions options = new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlQuery().name());
        vertx.eventBus().send(QUERY,new MysqlMessage.QueryMessage(op),options,handler);
    }
    
    public void query(String op,long identification,Handler<AsyncResult<ResultSet>> reply) {
        Handler<AsyncResult<Message<MysqlMessage.QueryMessage>>> handler = res ->{
            if(res.succeeded()) {
                AsyncResult<ResultSet> result = res.result().body().result();
                reply.handle(result);
            }else {
                reply.handle(new Failed<ResultSet>());
            }
        };
        DeliveryOptions options = new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlQuery().name());
        vertx.eventBus().send(QUERY,new MysqlMessage.QueryMessage(op,identification),options,handler);
    }
    //update
    public void update(String op,Handler<AsyncResult<UpdateResult>> reply) {
        Handler<AsyncResult<Message<MysqlMessage.UpdateMessage>>> handler = res ->{
            AsyncResult<UpdateResult> result = res.result().body().result();
            reply.handle(result);
        };
        DeliveryOptions options = new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlUpdate().name());
        vertx.eventBus().send(UPDATE,new MysqlMessage.UpdateMessage(op),options,handler);
    }

    public void update(String op,long identification,Handler<AsyncResult<UpdateResult>> reply) {
        Handler<AsyncResult<Message<MysqlMessage.UpdateMessage>>> handler = res ->{
            AsyncResult<UpdateResult> result = res.result().body().result();
            reply.handle(result);
        };
        DeliveryOptions options = new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlUpdate().name());
        vertx.eventBus().send(UPDATE,new MysqlMessage.UpdateMessage(op,identification),options,handler);
    }
    /**不直接提供事务接口，而以具体的业务接口的形式给出
     * 底层负责“失败回滚”操作
     * */
    private void transaction(List<String> ops,Handler<AsyncResult<CompositeFuture>> reply) {
        Handler<AsyncResult<Message<MysqlMessage.CompositeMessage>>> handler = res ->{
            AsyncResult<CompositeFuture> result = res.result().body().result();
            reply.handle(result);
        };
        DeliveryOptions options = new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlComposite().name());
        vertx.eventBus().send(TRANSACTION,new MysqlMessage.CompositeMessage(ops),options,handler);
    }
}
