package cn.pgyyd.mcg.verticle;

import cn.pgyyd.mcg.constant.McgConst;
import cn.pgyyd.mcg.module.CheckSelectionResultHandler;
import cn.pgyyd.mcg.module.CourseInfoHandler;
import cn.pgyyd.mcg.module.LoginHandler;
import cn.pgyyd.mcg.module.SubmitSelectionHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class MainVerticle extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        HttpServer httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);

        addMainPageHandler(router);
        addStaticResourceHandler(router);
        addCourseInfoHandler(router);
        addSubmitSelectionHandler(router);
        addCheckSelectionResultHandler(router);
        addLoginHandler(router);

        httpServer.requestHandler(router);

        //TODO: 端口改成从配置文件读取
        httpServer.listen(8080);
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
        router.route(HttpMethod.POST, McgConst.SELECT_QUERY_PATH).handler(new SubmitSelectionHandler());
    }

    //轮询选课结果请求处理器
    private void addCheckSelectionResultHandler(Router router) {
        router.route(HttpMethod.POST, McgConst.CHECK_QUERY_PATH).handler(new CheckSelectionResultHandler());
    }

    private void addLoginHandler(Router router) {
        router.route(HttpMethod.POST, McgConst.LOGIN_QUERY_PATH).handler(new LoginHandler());
    }
}
