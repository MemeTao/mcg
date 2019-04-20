package cn.pgyyd.mcg.module;

import io.vertx.core.AsyncResult;

public class AllcateTaskIdResult implements AsyncResult<Long> {
    private  Long taskid;
    public AllcateTaskIdResult(Long id) {
        taskid = id;
    }
    @Override
    public Long result() {
        // TODO Auto-generated method stub
        return taskid;
    }

    @Override
    public Throwable cause() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean succeeded() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean failed() {
        // TODO Auto-generated method stub
        return !succeeded();
    }

}
