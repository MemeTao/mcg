package cn.pgyyd.mcg.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class CjluAuth implements AuthProvider {
    private Vertx vertx;

    public CjluAuth(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void authenticate(JsonObject jsonObject, Handler<AsyncResult<User>> handler) {
        //TODO: 将来要在User对象中存储更多学生相关的静态信息
        String uid = jsonObject.getString("uid");
        String token = jsonObject.getString("token");

        WebClient client = WebClient.create(vertx);
        client.post(8080, "host", "uri path")
                .send(ar -> {
                    if (!ar.succeeded()) {
                        handler.handle(Future.failedFuture("错误提示"));
                        return;
                    }
                    if (ar.result().statusCode() != 200) {
                        handler.handle(Future.failedFuture("错误提示"));
                        return;
                    }
                    JsonObject resp = ar.result().bodyAsJsonObject();
                    if (!resp.getString("code").equals("0")) {
                        handler.handle(Future.failedFuture("错误提示"));
                        return;
                    }
                    //认证成功，得到一个User对象
                    User user = new CjluUser(jsonObject);
                    handler.handle(Future.succeededFuture(user));
                });
    }
}
