package cn.pgyyd.mcg.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;

public class CjluUser implements User {

    private JsonObject userData;

    CjluUser(JsonObject jsonObject) {
        userData = jsonObject;
    }

    @Override
    public User isAuthorized(String s, Handler<AsyncResult<Boolean>> handler) {
        //登录成功后，用这个接口认证用户是否拥有权限A、权限B、权限C...
        //此处不作判断，给出true的结果
        handler.handle(Future.succeededFuture(true));
        return this;
    }

    @Override
    public User clearCache() {
        return null;
    }

    @Override
    public JsonObject principal() {
        return userData;
    }

    @Override
    public void setAuthProvider(AuthProvider authProvider) {

    }
}
