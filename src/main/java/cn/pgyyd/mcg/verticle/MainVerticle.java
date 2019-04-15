package cn.pgyyd.mcg.verticle;

import cn.pgyyd.mcg.auth.CjluAuth;
import cn.pgyyd.mcg.constant.McgConst;
import cn.pgyyd.mcg.module.CheckSelectionResultHandler;
import cn.pgyyd.mcg.module.CourseInfoHandler;
import cn.pgyyd.mcg.module.LoginHandler;
import cn.pgyyd.mcg.module.SubmitSelectionHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.apache.commons.lang3.StringUtils;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        HttpServer httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);

        addLoginHandler(router);
        addMainPageHandler(router);
        addStaticResourceHandler(router);
        addCourseInfoHandler(router);
        addSubmitSelectionHandler(router);
        addCheckSelectionResultHandler(router);

        httpServer.requestHandler(router);

        httpServer.listen(config().getInteger("port", 8080));
    }


    private void addMainPageHandler(Router router) {
        StaticHandler mainPageHandler = StaticHandler.create(McgConst.HTML_ASSERT_LOCAL_PATH);
        mainPageHandler.setIndexPage(McgConst.INDEX_HTML);
        router.route(HttpMethod.GET, "/").handler(mainPageHandler);
        router.route(HttpMethod.GET, "/index.html").handler(mainPageHandler);
    }

    //静态资源处理器
    private void addStaticResourceHandler(Router router) {
        StaticHandler staticHandler = StaticHandler.create(McgConst.STATIC_ASSERT_LOCAL_PATH);
        router.route(HttpMethod.GET, McgConst.STATIC_ASSERT_QUERY_PATH).handler(staticHandler);
    }

    //课程信息请求处理器
    private void addCourseInfoHandler(Router router) {
        router.route(HttpMethod.POST, McgConst.COURSE_INFO_QUERY_PATH).handler(new CourseInfoHandler());
    }

    //选课请求处理器
    private void addSubmitSelectionHandler(Router router) {
        router.route(HttpMethod.POST, McgConst.SELECT_QUERY_PATH).handler(new SubmitSelectionHandler(config().getInteger("max_jobs")));
    }

    //轮询选课结果请求处理器
    private void addCheckSelectionResultHandler(Router router) {
        router.route(HttpMethod.POST, McgConst.CHECK_QUERY_PATH).handler(new CheckSelectionResultHandler());
    }

    //登录与认证处理器
    private void addLoginHandler(Router router) {
        AuthProvider authProvider = new CjluAuth();
        //登录功能依次依赖于 用户session -> http session -> cookie
        router.route().handler(CookieHandler.create());
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
        router.route().handler(UserSessionHandler.create(authProvider));
        router.route().handler(event -> {
            if (StringUtils.equals(McgConst.LOGIN_QUERY_PATH, event.request().path())) {
                event.next();
                return;
            }
            if (event.user() == null) {
                event.fail(401);    //未登录
            }
            event.next();
        });
        router.route(HttpMethod.POST, McgConst.LOGIN_QUERY_PATH).handler(new LoginHandler(authProvider, McgConst.INDEX_HTML));
    }
}
