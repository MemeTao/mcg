package cn.pgyyd.mcg.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class CheckRemainHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext event) {
        String courseids = event.request().getParam("courseids");

        if (StringUtils.isEmpty(courseids)) {
            event.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("status_code", 1)
                            .put("msg", "courseids不能为空")
                            .toString());
            return;
        }
        //解析url参数courseids
        List<Integer> courseIdList;
        try {
            courseIdList = Arrays.stream(courseids.split(","))
                    .filter(s -> !s.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .boxed()
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            log.error(String.format("parameters format wrong courseids:%s", courseids));
            event.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("status_code", 1)
                            .put("msg", "courseids格式错误")
                            .toString());
            return;
        } catch (Exception e) {
            log.error("unknown error", e);
            event.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("status_code", 1)
                            .put("msg", "未知错误")
                            .toString());
            return;
        }
        //TODO: 通过mysql proxy查询剩余人数
        event.response()
                .putHeader("content-type", "application/json")
                .end(new JsonObject()
                        .put("status_code", 0)
                        .put("msg", "success")
                        .toString());
    }
}
