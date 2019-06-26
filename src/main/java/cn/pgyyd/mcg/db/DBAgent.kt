package cn.pgyyd.mcg.db

import cn.pgyyd.mcg.ds.CourseSchedule
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray


/**
 * 封装数据库业务
 */
class DBAgent(vertx: Vertx, dbConfig: JsonArray) {

    private val mysqlProxy = MysqlProxy(vertx, dbConfig)


    /** 查询学生课表
     * @param userId 学生学号
     * @return 学生课表（未排序）
     */
    suspend fun queryStudentSchedule(userId: String): List<CourseSchedule> {
        val querySql = """SELECT b.course_id, b.week, b.day_of_week, b.section_of_day
 FROM tb_student_course a, tb_course_schedule b
 WHERE a.course_id = b.course_id AND a.student_id = $userId;"""

        val mysqlConn = mysqlProxy.dispatch(userId)
        val resultSet = mysqlConn.query(querySql).results
        val courseSchedule = ArrayList<CourseSchedule>()
        for (row in resultSet) {
            val course = row.getLong(0)
            val week = row.getInteger(1)
            val day = row.getInteger(2)
            val section = row.getInteger(3)
            courseSchedule.add(CourseSchedule(course, week, day, section))
        }
        return courseSchedule
    }


    /** 查询待选课的课程时间表
     * @param courseIds 待选的课程id
     * @return 待选课的课程时间表（未排序）
     */
    suspend fun queryCoursesSchedule(courseIds: List<Long>): Map<Long, List<CourseSchedule>> {
        val builder = StringBuilder("""SELECT course_id, week, day_of_week, section_of_day
 FROM tb_course_schedule
 WHERE course_id IN (""")
        for (i in 0 until courseIds.size) {
            if (i == 0) {
                builder.append(courseIds[i])
            } else {
                builder.append(",")
                builder.append(courseIds[i])
            }
        }
        builder.append(");")

        val querySql = builder.toString()
        val mysqlConn = mysqlProxy.dispatch()
        val resultSet = mysqlConn.query(querySql).results
        val coursesSchedule = HashMap<Long, MutableList<CourseSchedule>>()
        for (row in resultSet) {
            coursesSchedule.getOrPut(row.getLong(0), {ArrayList()})
                    .add(CourseSchedule(
                            row.getLong(0),     //courseId
                            row.getInteger(1),  //week
                            row.getInteger(2),  //day_of_week
                            row.getInteger(3))) //section_of_day
        }
        return coursesSchedule
    }

    /** 更新待选课的剩余人数
     * @param courseId 课程id
     * @return 是否更新成功
     */
    suspend fun updateCourseReamin(courseId: Long) : Boolean {
        val mysqlConn = mysqlProxy.dispatch()
        val updateResult = mysqlConn.update("some sql statement")
        return updateResult.updated == 1
    }


    /** 插入一条建立{学生-课程}关系的记录
     * @param userId 学生学号
     * @param courseId 课程id
     * @return 建立关系是否成功
     */
    suspend fun insertStudentCourseRelation(userId: String, courseId: Long) : Boolean {
        val mysqlConn = mysqlProxy.dispatch(userId)
        val updateResult = mysqlConn.update("some sql statement")
        return updateResult.updated == 1
    }
}
