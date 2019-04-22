package cn.pgyyd.mcg.module;

import java.util.HashSet;
import java.util.List;

import cn.pgyyd.mcg.verticle.MySqlVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;

public class MysqlProxy {
    /**
     * sql查询结果封装
     * */
    private static String EXEC = MySqlVerticle.EXEC;
    
    private static String QUERY = MySqlVerticle.QUERY;
    
    private static String UPDATE = MySqlVerticle.UPDATE;
    
    private static String TRANSACTION = MySqlVerticle.TRANSACTION;
    
    private Vertx vertx;
    
    public MysqlProxy(Vertx v){
        vertx = v;
    }
    /**
     * 示例：获取课程表中的所有课程id
     * @param reply
     */
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
    /**为了完全和"直接使用mysql"的返回值一致，这里做了一次变换
     */
    public void execute(String op,Handler<AsyncResult<Void>> reply) {
        Handler<AsyncResult<Message<MysqlMessage.ExecuteMessage>>> handler = res ->{
            AsyncResult<Void> result = res.result().body().result();
            reply.handle(result);
        };
        DeliveryOptions options = new DeliveryOptions().setCodecName(new UserMessageCodec.Mysql().name());
        vertx.eventBus().send(EXEC,new MysqlMessage.ExecuteMessage(op),options,handler);
    }
    
    public void query(String op,Handler<AsyncResult<ResultSet>> reply) {
        Handler<AsyncResult<Message<MysqlMessage.QueryMessage>>> handler = res ->{
            AsyncResult<ResultSet> result = res.result().body().result();
            reply.handle(result);
        };
        DeliveryOptions options = new DeliveryOptions().setCodecName(new UserMessageCodec.Mysql().name());
        vertx.eventBus().send(QUERY,new MysqlMessage.QueryMessage(op),options,handler);
    }
    
    //update
    public void update(String op,Handler<AsyncResult<UpdateResult>> reply) {
        Handler<AsyncResult<Message<MysqlMessage.UpdateMessage>>> handler = res ->{
            AsyncResult<UpdateResult> result = res.result().body().result();
            reply.handle(result);
        };
        DeliveryOptions options = new DeliveryOptions().setCodecName(new UserMessageCodec.Mysql().name());
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
        DeliveryOptions options = new DeliveryOptions().setCodecName(new UserMessageCodec.Mysql().name());
        vertx.eventBus().send(TRANSACTION,new MysqlMessage.CompositeMessage(ops),options,handler);
    }
}
