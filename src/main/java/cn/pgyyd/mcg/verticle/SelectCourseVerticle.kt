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
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.kotlin.redis.setAwait
import io.vertx.redis.RedisClient
import io.vertx.redis.RedisOptions
import org.slf4j.LoggerFactory
import kotlin.collections.HashMap
import kotlin.random.Random



class SelectCourseVerticle : CoroutineVerticle() {

    val log = LoggerFactory.getLogger(SelectCourseVerticle::class.java)

    private lateinit var redisClient : RedisClient
    private lateinit var dbAgent: DBAgent

    private var emptySeat = ArrayDeque<Int?>()

    private var jobQueue = ArrayDeque<Message<SelectCourseMessage>>()

    private val deliveryOptions = DeliveryOptions().setCodecName(UserMessageCodec.SelectCourseMessageCodec().name())

    private val lockedUser = HashMap<String, Long>()

    override suspend fun start() {
        //初始化任务并发限制
        val maxDoingJobs = config.getInteger("max_doing_jobs", 10)
        log.info("Start SelectCourseVerticle with max_doing_jobs:$maxDoingJobs")
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

        //初始化mysql, 有点丑
        dbAgent = DBAgent(vertx, this, config.getJsonArray("dbs"))
        log.info("mysqlClients started")

        //初始化JobID生成器
        var nodeId = config.getInteger("node_id")
        if (nodeId == null) {
            nodeId = Random(System.currentTimeMillis()).nextInt();
            log.warn("node_id not set, use random integer:$nodeId")
        }
        JobIDGenerator.init(nodeId)

        //注册EventBus消息Codec
        vertx.eventBus().registerCodec(UserMessageCodec.SelectCourseMessageCodec())
        //用一个协程Adapter包装异步的EventBus
        val adapter = vertx.receiveChannelHandler<Message<SelectCourseMessage>>()
        vertx.eventBus().consumer(McgConst.EVENT_BUS_SELECT_COURSE, adapter)
        //启动一个协程，此协程将运行在当前线程下
        launch {
            while (true) {
                val msg = adapter.receive()
                val newJobId = JobIDGenerator.getInstance().generate()
                //不允许用户同时发送多个选课请求
                val userId = msg.body().request.userId
                val lockedJobId = tryLockUser(userId, newJobId)
                if (lockedJobId != null) {
                    msg.body().result = msg.body().SelectCourseResult(2, lockedJobId)
                    msg.reply(msg.body(), deliveryOptions)
                    continue
                }

                val seat: Int? = emptySeat.poll()
                if (seat == null) {             //并发已满，将此消息放入queue中，并立马返回一个jobid
                    waitForAvailableSeat(msg)
                    msg.body().result = msg.body().SelectCourseResult(1, newJobId)
                    msg.reply(msg.body(), deliveryOptions)
                } else {                        //并发未满，立即处理选课
                    msg.body().result = msg.body().SelectCourseResult(0, newJobId)
                    launch { doSelectCourse(msg) }      //启动一个协程，此协程将运行在当前线程下
                }
            }
        }

    }


    private fun waitForAvailableSeat(msg: Message<SelectCourseMessage>) {
        jobQueue.add(msg)
    }

    private suspend fun doSelectCourse(msg: Message<SelectCourseMessage>) {
        val userId = msg.body().request.userId
        val lockGuard = UserLockGuard(userId, this)
        //获取这个学生的必修课课表(此论选课前已有的课)
        val studentCourseSchedule = dbAgent.queryStudentSchedule(userId)
        val sortedStudentCourseSchedule = studentCourseSchedule.sorted()
        if (studentCourseSchedule.isEmpty()) {          //获取学生课表失败（一门课都没获取到当作是失败）
            if (msg.body().result.status == 0) {        //如果是非排队请求，立马返回，告知失败
                msg.body().result.results = ArrayList<SelectCourseMessage.Result>()
                msg.reply(msg.body(), deliveryOptions)
            } else {                                    //如果是排队请求，将失败结果插入redis
                val builder = StringBuilder()
                for (idx in 0 until msg.body().request.courseIds.size) {
                    if (idx != 0)
                        builder.append("_")
                    builder.append(msg.body().request.courseIds[idx])
                    builder.append("_")
                    builder.append(false)
                }
                redisClient.setAwait(msg.body().result.jobId.toString(), builder.toString())
            }
            tryPollJobQueue()
            return
        }

        msg.body().result.results = ArrayList<SelectCourseMessage.Result>()
        //获取待选课的课程时间表
        val toSelectCoursesSchedule = dbAgent.queryOptCoursesSchedule(msg.body().request.courseIds)
        for (courseSchedule in toSelectCoursesSchedule) {
            //判断该门课的课表是否与学生已有的课冲突
            val match = timeMatch(sortedStudentCourseSchedule, courseSchedule.value)
            if (!match) {
                msg.body().result.results.add(msg.body().Result(false, courseSchedule.key))
                continue
            }
            //尝试为该学生在这门课占一个位置
            val updated = dbAgent.minusCourseRemain(userId, courseSchedule.key)
            if (!updated) {
                msg.body().result.results.add(msg.body().Result(false, courseSchedule.key))
                continue
            }
            //建立{学生-课程}关系，代表学生已选这门课
            val selected = dbAgent.insertStudentCourseRelation(userId, courseSchedule.key)
            if (!selected) {
                msg.body().result.results.add(msg.body().Result(false, courseSchedule.key))
                val fallbackSuccess = dbAgent.plusCourseRemain(userId, courseSchedule.key)
                if (!fallbackSuccess) {
                    log.error("course remain auto fallback fail, need fallback by hand")
                }
                continue
            }
            msg.body().result.results.add(msg.body().Result(true, courseSchedule.key))
        }
        //结果插入redis
        val builder = StringBuilder()
        val tempResult = msg.body().result.results
        for (idx in 0 until tempResult.size) {
            if (idx != 0)
                builder.append("_")
            builder.append(tempResult[idx].courseId)
            builder.append("_")
            builder.append(tempResult[idx].success)
        }
        redisClient.setAwait(msg.body().result.jobId.toString(), builder.toString())

        //如果是非排队请求，返回结果
        if (msg.body().result.status == 0) {
            msg.reply(msg.body(), deliveryOptions)
        }
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
            launch { doSelectCourse(task) }
        } else if (emptySeat.size < config.getInteger("max_doing_jobs")) {
            emptySeat.add(1)
        }
    }

    private fun tryLockUser(userId: String, newJobId: Long) : Long? {
        return lockedUser.putIfAbsent(userId, newJobId)
    }


    private fun releaseUser(userId: String) {
        lockedUser.remove(userId)
    }

    class UserLockGuard(val userId: String, val parent: SelectCourseVerticle) {
        fun done() {
            parent.releaseUser(userId)
        }
    }

}

