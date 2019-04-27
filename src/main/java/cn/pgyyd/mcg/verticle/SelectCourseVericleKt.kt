package cn.pgyyd.mcg.verticle

import cn.pgyyd.mcg.constant.McgConst
import cn.pgyyd.mcg.ds.SelectCourseResult
import cn.pgyyd.mcg.ds.StudentSchedule
import cn.pgyyd.mcg.singleton.JobIDGenerator
import io.vertx.core.eventbus.Message
import io.vertx.ext.asyncsql.AsyncSQLClient
import io.vertx.ext.asyncsql.MySQLClient
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.receiveChannelHandler
import io.vertx.kotlin.ext.sql.getConnectionAwait
import io.vertx.kotlin.ext.sql.queryAwait
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class Task(var msg: Message<Any>, var jobid: kotlin.Long) {
}

data class CourseSchedule(val course: Int, val week: Int, val day: Int, val section: Int) : Comparable<CourseSchedule> {
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

    private var emptySeat = ArrayDeque<kotlin.Int?>()

    private var jobQueue = ArrayDeque<Task>()

    override suspend fun start() {
        val maxDoingJobs = config.getInteger("max_doing_jobs")
        for (i in 1..maxDoingJobs) {
            emptySeat.add(1)
        }
        mySqlClient = MySQLClient.createShared(vertx, config.getJsonObject("mysql"), "kotlin.sql.pool")

        val adapter = vertx.receiveChannelHandler<Message<Any>>()
        vertx.eventBus().consumer<Any>(McgConst.EVENT_BUS_SELECT_COURSE, adapter)
        launch {
            while (true) {
                val msg = adapter.receive()
                val seat: Int? = emptySeat.poll()
                if (seat == null) {
                    val jobID = JobIDGenerator.getInstance().generate()
                    waitForAvailableSeat(Task(msg, jobID))
                    msg.reply(SelectCourseResult(1, jobID))
                } else {
                    doSelectCourse(Task(msg, -1))
                }
            }
        }

    }


    private fun waitForAvailableSeat(task: Task) {
        jobQueue.add(task)
    }

    private suspend fun doSelectCourse(task: Task) {
        //1. 根据courseids取出课程的时间信息，根据学生id取出学生的课表信息(先不管redis缓存的事，直接从mysql取）
        //2. 遍历courseids，把跟学生课表时间能匹配上的courseid，取出组成新的courseids
        //3. 遍历courseids去更新remain表，更新成功的，组成一个success list
        //4. 向handler写入结果: handler.handle(Future.succeededFuture(msg));
        //5. 处理剩余数据库操作
        //6. 退出
        var finalResult = SelectCourseResult(0, task.jobid)
        val mysqlConn = mySqlClient.getConnectionAwait()
        val studentCourseSqlResult = mysqlConn.queryAwait(makeStudentScheduleSQL(1/*学生uid*/)).results  //学生自己的课表
        val sortedStudentCourseSchedule: List<CourseSchedule>
        if (studentCourseSqlResult.size == 0) {
            //获取学生课表失败
            if (task.jobid == -1L) {
                //立马返回，告知失败
                finalResult.Results = ArrayList<SelectCourseResult.Result>()
                task.msg.reply(finalResult)
            } else {
                //结果插入redis
            }
            tryPollJobQueue()
            return
        } else {
            //从studentCourseSqlResult提取课表信息，主要是上课时间
            var studentCourseSchedule = ArrayList<CourseSchedule>()
            for (row in studentCourseSqlResult) {
                val course = row.getInteger(0)
                val week = row.getInteger(1)
                val day = row.getInteger(2)
                val section = row.getInteger(3)
                studentCourseSchedule.add(CourseSchedule(course, week, day, section))
            }
            sortedStudentCourseSchedule = studentCourseSchedule.sorted()
        }

        val courseTimeSqlResult = mysqlConn.queryAwait(makeCourseTimeSQL(ArrayList<Int>()/**/)).results          //所选课程的信息，主要是上课时间
        val toSelectCoursesSchedule = HashMap<Int, MutableList<CourseSchedule>>()
        for (row in courseTimeSqlResult) {
            //等价于 toSelectCoursesSchedule[course_id].push_back(course)
            toSelectCoursesSchedule.getOrDefault(row.getInteger(0), ArrayList<CourseSchedule>())
                    .add(CourseSchedule(
                            row.getInteger(0),
                            row.getInteger(1),
                            row.getInteger(2),
                            row.getInteger(3)))

            //TODO: 获取每一个课程的上课时间
        }
        for (course in toSelectCoursesSchedule) {
            if (timeMatch(sortedStudentCourseSchedule, course.value)) {
                finalResult.Results.add(finalResult.Result(true, course.key))
            } else {
                finalResult.Results.add(finalResult.Result(false, course.key))
            }
        }
        //结果插入redis
        tryPollJobQueue()
    }

    //学生和课程时间匹配
    private fun timeMatch(studentSchedule: List<CourseSchedule>, courseSchedule: List<CourseSchedule>): Boolean {
        val sortedCourseSchedule = courseSchedule.sorted()
        //TODO: 匹配时间
        //学生课表和要选的课课表，数据结构一致，并且按相同方式排序好了，O(N)就能匹配出结果
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
        | FROM tb_student_course a, tb_course_schedule b
        | WHERE a.course = b.course_id AND a.student = $uid""".trimMargin()
}

private fun makeCourseTimeSQL(uids: List<Int>) : String {
    val builder = StringBuilder("""SELECT course_id, week, day_of_week, section_of_day
        | FROM tb_course_schedule
        | WHERE course_id IN (
    """)
    for (i in 0..uids.size) {
        if (i == 0) {
            builder.append(uids[i])
        } else {
            builder.append(",")
            builder.append(uids[i])
        }
    }
    return builder.toString()
}