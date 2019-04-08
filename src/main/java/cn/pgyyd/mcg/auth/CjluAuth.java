package cn.pgyyd.mcg.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;

public class CjluAuth implements AuthProvider {
    @Override
    public void authenticate(JsonObject jsonObject, Handler<AsyncResult<User>> handler) {
        String uid = jsonObject.getString("uid");
        String token = jsonObject.getString("token");

        //TODO: 拿uid和token去学校统一认证中心鉴权

        //此处假设认证成功，于是得到一个User对象
        User user = new CjluUser(jsonObject);
        handler.handle(Future.succeededFuture(user));
        //handler.handle(Future.failedFuture("auth failed"));
    }
}
