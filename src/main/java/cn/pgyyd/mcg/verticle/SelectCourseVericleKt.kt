package cn.pgyyd.mcg.verticle

import cn.pgyyd.mcg.constant.McgConst
import cn.pgyyd.mcg.ds.SelectCourseResult
import cn.pgyyd.mcg.singleton.JobIDGenerator
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.ext.asyncsql.AsyncSQLClient
import io.vertx.ext.asyncsql.MySQLClient
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.receiveChannelHandler
import io.vertx.kotlin.ext.sql.getConnectionAwait
import io.vertx.kotlin.ext.sql.queryAwait
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class Task(var msg: Message<Any>, var jobid: kotlin.Long) {
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
        mySqlClient = MySQLClient.createShared(vertx, config.getJsonObject("mysql"), "kotlin.sql.poll")

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
        val mysqlConn = mySqlClient.getConnectionAwait()
        val studentCourseTable = mysqlConn.queryAwait("select xx from xx")
    }
}