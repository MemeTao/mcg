package cn.pgyyd.mcg.verticle

import cn.pgyyd.mcg.constant.McgConst
import cn.pgyyd.mcg.ds.SelectCourseMessage
import cn.pgyyd.mcg.ds.SelectCourseRequest
import cn.pgyyd.mcg.ds.SelectCourseResult
import cn.pgyyd.mcg.module.UserMessageCodec
import cn.pgyyd.mcg.singleton.JobIDGenerator
import io.vertx.core.eventbus.Message
import io.vertx.ext.asyncsql.AsyncSQLClient
import io.vertx.ext.asyncsql.MySQLClient
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.receiveChannelHandler
import io.vertx.kotlin.ext.sql.getConnectionAwait
import io.vertx.kotlin.ext.sql.queryAwait
import io.vertx.kotlin.ext.sql.updateAwait
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

data class CourseSchedule(val course: Long, val week: Int, val day: Int, val section: Int) : Comparable<CourseSchedule> {
    override fun compareTo(other: CourseSchedule): Int {
        return when {
            this.week != other.week -> this.week  - other.week
            this.day != other.week -> this.day - other.day
            this.section != other.section -> this.section - other.section
            else -> 0
        }
    }
}

class SelectCourseVericleKt : CoroutineVerticle() {

    private lateinit var mySqlClient : AsyncSQLClient

    private var emptySeat = ArrayDeque<Int?>()

    private var jobQueue = ArrayDeque<Message<SelectCourseMessage>>()

    override suspend fun start() {
        val maxDoingJobs = config.getInteger("max_doing_jobs")
        for (i in 1..maxDoingJobs) {
            emptySeat.add(1)
        }
        mySqlClient = MySQLClient.createShared(vertx, config.getJsonObject("mysql"), "kotlin.sql.pool")

        vertx.eventBus().registerCodec( UserMessageCodec.SelectCourseMessageCodec())
        val adapter = vertx.receiveChannelHandler<Message<SelectCourseMessage>>()
        vertx.eventBus().consumer<SelectCourseMessage>(McgConst.EVENT_BUS_SELECT_COURSE, adapter)
        launch {
            while (true) {
                val msg = adapter.receive()
                val seat: Int? = emptySeat.poll()
                if (seat == null) {
                    val jobID = JobIDGenerator.getInstance().generate()
                    waitForAvailableSeat(msg)
                    msg.body().result = msg.body().SelectCourseResult(1, jobID)
                    msg.reply(msg)
                } else {
                    msg.body().result.Status = 0
                    doSelectCourse(msg)
                }
            }
        }

    }


    private fun waitForAvailableSeat(msg: Message<SelectCourseMessage>) {
        jobQueue.add(msg)
    }

    private suspend fun doSelectCourse(msg: Message<SelectCourseMessage>) {
        //1. 根据courseids取出课程的时间信息，根据学生id取出学生的课表信息(先不管redis缓存的事，直接从mysql取）
        //2. 遍历courseids，把跟学生课表时间能匹配上的courseid，取出组成新的courseids
        //3. 遍历courseids去更新remain表，更新成功的，组成一个success list
        //4. 向handler写入结果: handler.handle(Future.succeededFuture(msg));
        //5. 处理剩余数据库操作
        //6. 退出
        val mysqlConn = mySqlClient.getConnectionAwait()
        val studentCourseSqlResult = mysqlConn.queryAwait(makeStudentScheduleSQL(msg.body().request.userID)).results  //学生自己的课表
        val sortedStudentCourseSchedule: List<CourseSchedule>
        //获取学生课表失败
        if (studentCourseSqlResult.size == 0) {
            //如果是非排队请求，立马返回，告知失败
            if (msg.body().result.Status == 0) {
                msg.body().result.Results = ArrayList<SelectCourseMessage.Result>()
                msg.reply(msg.body())
            } else {
                //结果插入redis
            }
            tryPollJobQueue()
            return
        } else {
            //从studentCourseSqlResult提取课表信息，主要是上课时间
            var studentCourseSchedule = ArrayList<CourseSchedule>()
            for (row in studentCourseSqlResult) {
                val course = row.getLong(0)
                val week = row.getInteger(1)
                val day = row.getInteger(2)
                val section = row.getInteger(3)
                studentCourseSchedule.add(CourseSchedule(course, week, day, section))
            }
            sortedStudentCourseSchedule = studentCourseSchedule.sorted()
        }

        val courseTimeSqlResult = mysqlConn.queryAwait(makeCourseTimeSQL(msg.body().request.courseIDs)).results          //所选课程的信息，主要是上课时间
        val toSelectCoursesSchedule = HashMap<Long, MutableList<CourseSchedule>>()
        for (row in courseTimeSqlResult) {
            //等价于 toSelectCoursesSchedule[course_id].push_back(course)
            toSelectCoursesSchedule.getOrDefault(row.getLong(0), ArrayList())
                    .add(CourseSchedule(
                            row.getLong(0),     //courseId
                            row.getInteger(1),  //week
                            row.getInteger(2),  //day_of_week
                            row.getInteger(3))) //section_of_day
        }
        for (course in toSelectCoursesSchedule) {
            if (timeMatch(sortedStudentCourseSchedule, course.value)) {
                //FIXME: 如果这么干，学生自己选的课时间有可能冲突，一个方法是从Login处限制一个用户只能有一个session
                val updateResult = mysqlConn.updateAwait(makeUpdateCourseRemainSQL(course.key))
                if (updateResult.updated == 0) {
                    msg.body().result.Results.add(msg.body().Result(false, course.key))
                    continue
                }
                //FIXME: 有没有可能一次不成功，要update多次
                mysqlConn.updateAwait(makeInsertStudentCourseRelationSQL(msg.body().request.userID, course.key))
                msg.body().result.Results.add(msg.body().Result(true, course.key))
            } else {
                msg.body().result.Results.add(msg.body().Result(false, course.key))
            }
        }
        //结果插入redis
        msg.reply(msg.body())
        tryPollJobQueue()
    }

    //学生和课程时间匹配
    private fun timeMatch(studentSchedule: List<CourseSchedule>, courseSchedule: List<CourseSchedule>): Boolean {
        val sortedCourseSchedule = courseSchedule.sorted()
        //TODO: 匹配时间
        //学生课表和要选的课课表，数据结构一致，并且按相同方式排序好了，O(N)就能匹配出结果
        var i = 0
        var j = 0
        while (i < studentSchedule.size && j < sortedCourseSchedule.size) {
            val compare = studentSchedule[i].compareTo(sortedCourseSchedule[j])
            when {
                compare < 0 -> i++
                compare > 0 -> j++
                else -> return false
            }
        }
        return true
    }

    private fun tryPollJobQueue() {
        val task = jobQueue.poll()
        if (task != null) {
            launch {
                doSelectCourse(task)
            }
        } else if (emptySeat.size < config.getInteger("max_doing_jobs")) {
            emptySeat.add(1)
        }
    }

}

// some helper function
private fun makeStudentScheduleSQL(uid: Int) : String {
    return """SELECT b.course_id, b.week, b.day_of_week, b.section_of_day
 FROM tb_student_course a, tb_course_schedule b
 WHERE a.course = b.course_id AND a.student = $uid;"""
}

private fun makeCourseTimeSQL(uids: List<Int>) : String {
    val builder = StringBuilder("""SELECT course_id, week, day_of_week, section_of_day
 FROM tb_course_schedule
 WHERE course_id IN (""")
    for (i in 0..uids.size) {
        if (i == 0) {
            builder.append(uids[i])
        } else {
            builder.append(",")
            builder.append(uids[i])
        }
    }
    builder.append(';')
    return builder.toString()
}

private fun makeUpdateCourseRemainSQL(courseId: Long) : String {
    return """UPDATE tb_course
 SET students = students + 1
 WHERE id = $courseId;"""
}

private fun makeInsertStudentCourseRelationSQL(uid: Int, courseId: Long) : String {
    return """INSERT INTO tb_course_schedule
 (student, course)
 VALUES
 ($uid, $courseId);"""
}
