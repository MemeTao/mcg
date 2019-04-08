package cn.pgyyd.mcg.module;

import cn.pgyyd.mcg.constant.McgConst;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;


//登录逻辑为 用户先从第三方统一认证中心获得token，再把uid和token拿来请求该服务的/login，
//服务拿这些信息去第三方统一认证中心认证，认证成功后在session写入相关信息，并跳转index.html
public class LoginHandler implements Handler<RoutingContext> {

    private AuthProvider authProvider;
    private String redirectURL;

    public LoginHandler(AuthProvider authProvider, String redirectURL) {
        if (StringUtils.isEmpty(redirectURL))
            redirectURL = McgConst.INDEX_HTML;
        if (!StringUtils.startsWith(redirectURL, "/"))
            redirectURL = "/" + redirectURL;

        this.redirectURL = redirectURL;
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
                //跳转到主页
                doRedirect(event.response(), redirectURL);
            } else {
                //认证失败
                event.fail(403);
            }
        });
    }

    private void doRedirect(HttpServerResponse response, String directURL) {
        response.putHeader("location", directURL).setStatusCode(302).end();
    }
}
