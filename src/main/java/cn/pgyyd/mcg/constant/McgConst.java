package cn.pgyyd.mcg.constant;

public class McgConst {
    public static String INDEX_HTML = "index.html";

    public static String HTML_ASSERT_LOCAL_PATH = "asserts/html";

    public static String STATIC_ASSERT_LOCAL_PATH = "asserts";

    public static String STATIC_ASSERT_QUERY_PATH = "/asserts/*";

    //检查某次选课的结果
    //e.g. www.xxxx.com/check?jobid=45263245
    public static String CHECK_QUERY_PATH = "/check";

    //提交选课
    //e.g. www.xxxx.com/select?uid=1400304000&courseids=1111,2222,3333
    public static String SELECT_QUERY_PATH = "/select";

    //获取课程列表
    //e.g. www.xxxx.com/courseinfo?uid=1400304000&type=pe
    public static String COURSE_INFO_QUERY_PATH = "/courseinfo";

    //登录选课系统，token是用户先从别处得到的，选课系统通过uid和token去检验是否合法
    //e.g. www.xxxx.com/login?uid=1400304000&token=f9b447f02d59
    public static String LOGIN_QUERY_PATH = "/login";
}