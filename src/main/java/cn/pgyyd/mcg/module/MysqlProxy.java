package cn.pgyyd.mcg.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import cn.pgyyd.mcg.verticle.MySqlVerticle;
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
import cn.pgyyd.mcg.ds.CourseSchedule;
import cn.pgyyd.mcg.ds.StudentSchedule;

/**
 * 在该类中添加的任何业务接口务必保证有详细的说明，接口的参数以及返回值等类型尽力做到"不改动"
 * 当前存在的业务接口有：
 * 1. 获取所有课程id列表
 * 2. 根据课程id，获取该课程的时间信息
 * 3. 根据学生学号，获取该学生的全部课程(除本次选修课以外)
 * @author memetao
 *
 */
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
    public void getCourseSchedule(ArrayList<Integer> course_ids,Handler<AsyncResult<HashMap<Integer,CourseSchedule>>> reply) {
        
        HashMap<Integer,CourseSchedule> infos = new HashMap<Integer,CourseSchedule>();
        List<Future<Void>> futures = new ArrayList<Future<Void>>();
        for(int course_id : course_ids) {
            String sql = "select * from mcg_course_timerange where courseId = " + course_id;
            Future<Void> f =  Future.future();
            futures.add(f);
            query(sql,res->{
                if( res != null && res.succeeded() ) {
                    ResultSet result = res.result();
                    CourseSchedule course = new CourseSchedule(course_id);
                    infos.put(course_id, course);
                    //会存在多行结果
                    for(JsonObject obj : result.getRows()) {
                        final int id = obj.getInteger(result.getColumnNames().get(1));
                        final int day = obj.getInteger(result.getColumnNames().get(2));
                        final int start = obj.getInteger(result.getColumnNames().get(3));
                        final int end = obj.getInteger(result.getColumnNames().get(4));
                        if(id != course_id) {
                            //log.fatal("result queried form mysql is not equal with function parameter");
                        }else {
                            course.add_info(day, start,end);
                        }
                    }
                }
                f.complete();
            });
        }
        CompositeFuture.all(new ArrayList<>(futures)).setHandler(res->{
            if(res.failed()) {
                reply.handle(new Failed<HashMap<Integer,CourseSchedule>>());
            }else {
                /*TODO:需要将infos封装一个asyncresult*/
                reply.handle(new Success<HashMap<Integer,CourseSchedule>>(infos));
            }
        });
    }
    /**
     * 获取某门课程的上课时间
     * @param course_id
     * @param reply
     */
    public void getCourseSchedule(Integer course_id,Handler<AsyncResult<HashMap<Integer,CourseSchedule>>> reply) {
        ArrayList<Integer> course_ids = new ArrayList<Integer>();
        course_ids.add(course_id);
        getCourseSchedule(course_ids,reply);
    }
    /**
     * 获取学生的课表
     * @param student_id
     * @param reply
     */
    public void getStudentCourses(int student_id,Handler<AsyncResult<StudentSchedule>> reply) {
        /*TODO:同上*/
        reply.handle(null);
    }
    /**
     * 更新目标课程的剩余人数(选课的最终结果)
     * @param course_id
     * @param reply  如果写入，则返回true，否则返回false
     */
    public void updateRemain(int course_id,Handler<AsyncResult<Boolean>> reply) {
        /*TODO:同上*/
        reply.handle(null);
    }
    /**
     * 向某学生的选课课课表中，增加信息
     * @param student_id
     * @param course_id 
     * @param reply
     */
    public void addElectiveCourse(int student_id,int course_id,Handler<AsyncResult<Boolean>> reply) {
        /*TODO:同上*/
        reply.handle(null);
    }
    
    /**为了完全和"直接使用mysql"的返回值一致，这里做了一次变换
     */
    public void execute(String op,Handler<AsyncResult<Void>> reply) {
        Handler<AsyncResult<Message<MysqlMessage.ExecuteMessage>>> handler = res ->{
            AsyncResult<Void> result = res.result().body().result();
            reply.handle(result);
        };
        DeliveryOptions options = new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlExecute().name());
        vertx.eventBus().send(EXEC,new MysqlMessage.ExecuteMessage(op),options,handler);
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
    
    //update
    public void update(String op,Handler<AsyncResult<UpdateResult>> reply) {
        Handler<AsyncResult<Message<MysqlMessage.UpdateMessage>>> handler = res ->{
            AsyncResult<UpdateResult> result = res.result().body().result();
            reply.handle(result);
        };
        DeliveryOptions options = new DeliveryOptions().setCodecName(new UserMessageCodec.MysqlUpdate().name());
        vertx.eventBus().send(UPDATE,new MysqlMessage.UpdateMessage(op),options,handler);
    }
    /*不直接提供事务接口，而以具体的业务接口的形式给出
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
