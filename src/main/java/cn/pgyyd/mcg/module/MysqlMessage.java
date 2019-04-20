package cn.pgyyd.mcg.module;

import java.util.List;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;

public class MysqlMessage {
    /**
     * 不使用模板，明确区分每一个操作类型
     */
    static public class QueryMessage{
        
        private String op;
        private AsyncResult<ResultSet> result;
        
        public QueryMessage(final AsyncResult<ResultSet> r){
            result = r;
        }
        public QueryMessage(final String operation){
            op = operation;
        }
        public String operation() {
            return op;
        }
        public AsyncResult<ResultSet> result() {
            return result;
        }
    }
    
    static public class ExecuteMessage{
        
        private String op;
        private AsyncResult<Void> result;
        
        public ExecuteMessage(final AsyncResult<Void> r){
            result = r;
        }
        public ExecuteMessage(final String operation){
            op = operation;
        }
        public String operation() {
            return op;
        }
        public AsyncResult<Void> result() {
            return result;
        }
    }
    
    static public class UpdateMessage{
        
        private String op;
        private AsyncResult<UpdateResult> result;
        
        public UpdateMessage(final AsyncResult<UpdateResult> r){
            result = r;
        }
        public UpdateMessage(final String operation){
            op = operation;
        }
        public String operation() {
            return op;
        }
        public AsyncResult<UpdateResult> result() {
            return result;
        }
    }
    /**暂且这样吧
     * */
    static public class CompositeMessage{
        private List<String> ops;
        private AsyncResult<CompositeFuture> result;
        
        public CompositeMessage(final AsyncResult<CompositeFuture> r){
            result = r;
        }
        public CompositeMessage(final List<String> operations){
            ops = operations;
        }
        public List<String> operations() {
            return ops;
        }
        public AsyncResult<CompositeFuture> result() {
            return result;
        }
    }
}
