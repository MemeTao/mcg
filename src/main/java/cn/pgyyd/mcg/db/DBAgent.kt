package cn.pgyyd.mcg.db

import cn.pgyyd.mcg.ds.CourseSchedule

class DBAgent {

    private val mysqlProxy = MysqlProxy()

    suspend fun queryStudentSchedule(userId: String): List<CourseSchedule>? {
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

    suspend fun updateCourseReamin(courseId: Long) : Boolean {
        val mysqlConn = mysqlProxy.dispatch()
        val updateResult = mysqlConn.update("some sql statement")
        return updateResult.updated == 1
    }

    suspend fun insertStudentCourseRelation(userId: String, courseId: Long) : Boolean {
        val mysqlConn = mysqlProxy.dispatch(userId)
        val updateResult = mysqlConn.update("some sql statement")
        return updateResult.updated == 1
    }
}
