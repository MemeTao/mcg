package cn.pgyyd.mcg.handler;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;


//登录逻辑为 用户先从第三方统一认证中心获得token，再把uid和token拿来请求该服务的/login，
//服务拿这些信息去第三方统一认证中心认证，认证成功后在session写入相关信息，并告知前端登录成功
@Slf4j
public class LoginHandler implements Handler<RoutingContext> {

    private AuthProvider authProvider;
//    private String redirectURL;

    public LoginHandler(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    public void handle(RoutingContext event) {
        HttpServerRequest request = event.request();
        //必须是post请求
        if (request.method() != HttpMethod.POST) {
            event.fail(405);
            return;
        }
        String uid = event.request().getParam("uid");
        String token = event.request().getParam("token");
        if (StringUtils.isEmpty(uid) || StringUtils.isEmpty(token)) {
            event.fail(400);
            return;
        }
        JsonObject authInfo = new JsonObject().put("uid", uid).put("token", token);
        authProvider.authenticate(authInfo, e -> {
            if (e.succeeded()) {
                //认证成功，创建用户
                User user = e.result();
                event.setUser(user);
                log.debug(String.format("user:%s login success", uid));
                event.response()
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                                .put("status_code", 0)
                                .put("msg", "success")
                                .toString());
            } else {
                //认证失败
                log.debug(String.format("%s login failed", uid));
                event.response()
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                                .put("status_code", 1)
                                .put("msg", "登录失败")
                                .toString());
            }
        });
    }

//    private void doRedirect(HttpServerResponse response, String directURL) {
//        response.putHeader("location", directURL).setStatusCode(302).end();
//    }
}
