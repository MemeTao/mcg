package cn.pgyyd.mcg.interfaces;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;

public interface Test {
    void handle(Handler<AsyncResult<Message<Long>>> handler);
}
