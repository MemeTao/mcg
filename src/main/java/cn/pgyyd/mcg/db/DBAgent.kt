package cn.pgyyd.mcg.db

import cn.pgyyd.mcg.ds.CourseSchedule
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

/**
 * 封装数据库业务
 */
class DBAgent(vertx: Vertx, val coroutineScope: CoroutineScope, dbConfig: JsonArray) {

    private val mysqlProxy = MysqlProxy(vertx, dbConfig)


    /** 查询学生课表，包括必须课、已选选修课
     * @param userId 学生学号
     * @return 学生课表（未排序）
     */
    suspend fun queryStudentSchedule(userId: String): List<CourseSchedule> {
        //1 获取必修课表
        //2 获取选修课表
        //3 组合返回
        val req = coroutineScope.async { queryReqStudentSchedule(userId) }
        val opt = coroutineScope.async { queryOptStudentSchedule(userId) }
        val reqCourseSchedule = req.await()
        val optCourseSchedule = opt.await()

        return reqCourseSchedule + optCourseSchedule
    }


    /** 查询学生必修课课表
     * @param userId 学生学号
     * @return 学生必修课表（未排序）
     */
    private suspend fun queryReqStudentSchedule(userId: String) : List<CourseSchedule> {
        val querySql = """SELECT b.cid, b.week, b.day, b.lessons
 FROM req_student_course a, req_course_schedule b
 WHERE a.cid = b.cid AND a.uid = '$userId';"""

        val mysqlConn = mysqlProxy.dispatch()
        val resultSet = mysqlConn.query(querySql).results
        val courseSchedule = ArrayList<CourseSchedule>()
        for (row in resultSet) {
            val course = row.getString(0)
            val week = row.getInteger(1)
            val day = row.getInteger(2)
            val lessons = row.getString(3).split(",")
            for (lesson in lessons) {
                //TODO: 错误处理
                courseSchedule.add(CourseSchedule(course, week, day, lesson.toInt()))
            }

        }
        return courseSchedule
    }


    /** 查询学生选修课课表
     * @param userId 学生学号
     * @return 学生选修课表（未排序）
     */
    private suspend fun queryOptStudentSchedule(userId: String) : List<CourseSchedule> {
        val querySql = """SELECT b.cid, b.week, b.day, b.lessons
 FROM opt_student_course a, opt_course_schedule b
 WHERE a.cid = b.cid AND a.uid = '$userId';"""

        val mysqlConn = mysqlProxy.dispatch(userId)
        val resultSet = mysqlConn.query(querySql).results
        val courseSchedule = ArrayList<CourseSchedule>()
        for (row in resultSet) {
            val course = row.getString(0)
            val week = row.getInteger(1)
            val day = row.getInteger(2)
            val lessons = row.getString(3).split(",")
            for (lesson in lessons) {
                //TODO: 错误处理
                courseSchedule.add(CourseSchedule(course, week, day, lesson.toInt()))
            }

        }
        return courseSchedule
    }


    /** 查询待选课的课程时间表
     * @param courseIds 待选的课程id
     * @return 待选课的课程时间表（未排序）
     */
    suspend fun queryOptCoursesSchedule(courseIds: List<String>): Map<String, List<CourseSchedule>> {
        val builder = StringBuilder("""SELECT cid, week, day, lessons
 FROM opt_course_schedule
 WHERE cid IN ('""")
        for (i in 0 until courseIds.size) {
            if (i == 0) {
                builder.append(courseIds[i])
            } else {
                builder.append("','")
                builder.append(courseIds[i])
            }
        }
        builder.append("');")

        val querySql = builder.toString()
        val mysqlConn = mysqlProxy.dispatch()
        val resultSet = mysqlConn.query(querySql).results
        val coursesSchedule = HashMap<String, MutableList<CourseSchedule>>()
        for (row in resultSet) {
            val course = row.getString(0)
            val week = row.getInteger(1)
            val day = row.getInteger(2)
            val lessons = row.getString(3).split(",")
            for (lesson in lessons) {
                //TODO: 错误处理
                coursesSchedule.getOrPut(row.getString(0), {ArrayList()})
                        .add(CourseSchedule(course, week, day, lesson.toInt()))
            }

        }
        return coursesSchedule
    }

    /** 更新待选课的剩余人数(-1)
     * @param courseId 课程id
     * @return 是否更新成功
     */
    suspend fun minusCourseRemain(userId: String, courseId: String) : Boolean {
        val updateSql = """UPDATE opt_course_remain SET remain = remain - 1 WHERE cid = '$courseId';"""
        val mysqlConn = mysqlProxy.dispatch(userId)
        val updateResult = mysqlConn.update(updateSql)
        return updateResult.updated == 1
    }

    /** 更新待选课的剩余人数(+1)
     * @param courseId 课程id
     * @return 是否更新成功
     */
    suspend fun plusCourseRemain(userId: String, courseId: String) : Boolean {
        val updateSql = """UPDATE opt_course_remain SET remain = remain + 1 WHERE cid = '$courseId';"""
        val mysqlConn = mysqlProxy.dispatch(userId)
        val updateResult = mysqlConn.update(updateSql)
        return updateResult.updated == 1
    }


    /** 插入一条建立{学生-课程}关系的记录
     * @param userId 学生学号
     * @param courseId 课程id
     * @return 建立关系是否成功
     */
    suspend fun insertStudentCourseRelation(userId: String, courseId: String) : Boolean {
        val insertSql = """INSERT INTO opt_student_course (uid, cid) VALUES ('$userId', '$courseId');"""
        val mysqlConn = mysqlProxy.dispatch(userId)
        val updateResult = mysqlConn.update(insertSql)
        return updateResult.updated == 1
    }
}
