package cn.pgyyd.mcg.verticle

import cn.pgyyd.mcg.constant.McgConst
import cn.pgyyd.mcg.db.DBAgent
import cn.pgyyd.mcg.ds.CourseSchedule
import cn.pgyyd.mcg.ds.SelectCourseMessage
import cn.pgyyd.mcg.ds.UserMessageCodec
import cn.pgyyd.mcg.singleton.JobIDGenerator
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.receiveChannelHandler
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.kotlin.redis.setAwait
import io.vertx.redis.RedisClient
import io.vertx.redis.RedisOptions
import org.slf4j.LoggerFactory
import kotlin.random.Random



class SelectCourseVerticleKt : CoroutineVerticle() {

    val log = LoggerFactory.getLogger(SelectCourseVerticleKt::class.java)

    private lateinit var redisClient : RedisClient
    private lateinit var dbAgent: DBAgent

    private var emptySeat = ArrayDeque<Int?>()

    private var jobQueue = ArrayDeque<Message<SelectCourseMessage>>()

    private val deliveryOptions = DeliveryOptions().setCodecName(UserMessageCodec.SelectCourseMessageCodec().name())

    override suspend fun start() {

        //初始化任务并发限制
        val maxDoingJobs = config.getInteger("max_doing_jobs", 10)
        log.info("Start SelectCourseVerticleKt with max_doing_jobs:$maxDoingJobs")
        for (i in 1 until maxDoingJobs) {
            emptySeat.add(1)
        }

        //初始化redis
        val redisConfig = config.getJsonObject("redis")
        redisClient = RedisClient.create(vertx, RedisOptions()
                .setHost(redisConfig.getString("host"))
                .setAuth(redisConfig.getString("auth"))
                )
        log.info("redisClient started")

        //初始化mysql
        dbAgent = DBAgent(vertx, config.getJsonArray("dbs"))
        log.info("mysqlClients started")

        //初始化JobID生成器
        var nodeId = config.getInteger("node_id")
        if (nodeId == null) {
            nodeId = Random(System.currentTimeMillis()).nextInt();
            log.warn("node_id not set, use random integer:$nodeId")
        }
        JobIDGenerator.init(nodeId)

        vertx.eventBus().registerCodec(UserMessageCodec.SelectCourseMessageCodec())
        val adapter = vertx.receiveChannelHandler<Message<SelectCourseMessage>>()
        vertx.eventBus().consumer(McgConst.EVENT_BUS_SELECT_COURSE, adapter)
        //启动一个协程，此协程将运行在当前线程下
        launch {
            while (true) {
                val msg = adapter.receive()
                val seat: Int? = emptySeat.poll()
                val jobID = JobIDGenerator.getInstance().generate()
                if (seat == null) {
                    log.info(String.format("push_job %d to queue", jobID))
                    waitForAvailableSeat(msg)
                    msg.body().result = msg.body().SelectCourseResult(1, jobID)
                    msg.reply(msg.body(), deliveryOptions)
                } else {
                    msg.body().result = msg.body().SelectCourseResult(0, jobID)
                    //启动一个协程，此协程将运行在当前线程下
                    launch { doSelectCourse(msg) }
                }
            }
        }

    }


    private fun waitForAvailableSeat(msg: Message<SelectCourseMessage>) {
        jobQueue.add(msg)
        //log.info("add request to queue, queue size:${jobQueue.size}")
    }

    private suspend fun doSelectCourse(msg: Message<SelectCourseMessage>) {
        val studentCourseSchedule = dbAgent.queryStudentSchedule(msg.body().request.userID)
        val sortedStudentCourseSchedule: List<CourseSchedule>
        //获取学生课表失败
        //这里有个逻辑，就是不允许学生的“必须课表”是空，这被当作是读mysql错误
        if (studentCourseSchedule.isEmpty()) {
            //如果是非排队请求，立马返回，告知失败
            if (msg.body().result.status == 0) {
                msg.body().result.results = ArrayList<SelectCourseMessage.Result>()
                msg.reply(msg.body(), deliveryOptions)
            } else {
                //结果插入redis
                val builder = StringBuilder()
                for (idx in 0 until msg.body().request.courseIDs.size) {
                    if (idx != 0)
                        builder.append("_")
                    builder.append(msg.body().request.courseIDs[idx])
                    builder.append("_")
                    builder.append(false)
                }
                redisClient.setAwait(msg.body().result.jobID.toString(), builder.toString())
            }
            tryPollJobQueue()
            return
        } else {
            sortedStudentCourseSchedule = studentCourseSchedule.sorted()
        }

        msg.body().result.results = ArrayList<SelectCourseMessage.Result>()
        val toSelectCoursesSchedule = dbAgent.queryCoursesSchedule(msg.body().request.courseIDs)
        for (course in toSelectCoursesSchedule) {
            if (timeMatch(sortedStudentCourseSchedule, course.value)) {
                //FIXME: 如果这么干，学生自己选的课时间有可能冲突，一个方法是从Login处限制一个用户只能有一个session
                val updated = dbAgent.updateCourseReamin(course.key)
                if (updated) {
                    msg.body().result.results.add(msg.body().Result(false, course.key))
                    continue
                }
                //FIXME: 有没有可能一次不成功，要update多次
                dbAgent.insertStudentCourseRelation(msg.body().request.userID, course.key)
                msg.body().result.results.add(msg.body().Result(true, course.key))
            } else {
                msg.body().result.results.add(msg.body().Result(false, course.key))
            }
        }
        //结果插入redis
        val builder = StringBuilder()
        val tempResult = msg.body().result.results
        for (idx in 0 until tempResult.size) {
            if (idx != 0)
                builder.append("_")
            builder.append(tempResult[idx].courseID)
            builder.append("_")
            builder.append(tempResult[idx].success)
        }
        redisClient.setAwait(msg.body().result.jobID.toString(), builder.toString())
        msg.reply(msg.body(), deliveryOptions)
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
        //log.info("tryPollJobQueue jobQueue.size:${jobQueue.size}")
        val task = jobQueue.poll()
        if (task != null) {
            launch {
                doSelectCourse(task)
            }
        } else if (emptySeat.size < config.getInteger("max_doing_jobs")) {
            //log.info("tryPollJobQueue add to emptySeat")
            emptySeat.add(1)
        }
    }

}

// some helper function
private fun makeStudentScheduleSQL(uid: Int) : String {
    return """SELECT b.course_id, b.week, b.day_of_week, b.section_of_day
 FROM tb_student_course a, tb_course_schedule b
 WHERE a.course_id = b.course_id AND a.student_id = $uid;"""
}

private fun makeCourseTimeSQL(uids: List<Int>) : String {
    val builder = StringBuilder("""SELECT course_id, week, day_of_week, section_of_day
 FROM tb_course_schedule
 WHERE course_id IN (""")
    for (i in 0 until uids.size) {
        if (i == 0) {
            builder.append(uids[i])
        } else {
            builder.append(",")
            builder.append(uids[i])
        }
    }
    builder.append(");")
    return builder.toString()
}

private fun makeUpdateCourseRemainSQL(courseId: Long) : String {
    return """UPDATE tb_course
 SET students = students + 1
 WHERE course_id = $courseId;"""
}

private fun makeInsertStudentCourseRelationSQL(uid: Int, courseId: Long) : String {
    return """INSERT INTO tb_student_course
 (student_id, course_id)
 VALUES
 ($uid, $courseId);"""
}
