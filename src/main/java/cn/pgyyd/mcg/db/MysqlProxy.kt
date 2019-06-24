package cn.pgyyd.mcg.db

import io.vertx.ext.asyncsql.AsyncSQLClient
import io.vertx.ext.sql.ResultSet
import io.vertx.ext.sql.SQLConnection
import io.vertx.ext.sql.UpdateResult
import io.vertx.kotlin.ext.sql.closeAwait
import io.vertx.kotlin.ext.sql.getConnectionAwait
import io.vertx.kotlin.ext.sql.queryAwait
import io.vertx.kotlin.ext.sql.updateAwait

class MysqlConnection(private val mysqlConn: SQLConnection) {

    suspend fun query(sql: String) : ResultSet {
        val resultSet = mysqlConn.queryAwait(sql)
        mysqlConn.closeAwait()
        return resultSet
    }

    suspend fun update(sql: String) : UpdateResult {
        val updateResult = mysqlConn.updateAwait(sql)
        mysqlConn.close()
        return updateResult
    }

}

class MysqlProxy {

    private val clients : Map<String, AsyncSQLClient> = HashMap()

    suspend fun dispatch() : MysqlConnection {

        //FIXME: 貌似没有tryGetConnection()这种函数
        //for (client in clients) {
            //找到第一个可用的sql连接
        //}
        return MysqlConnection(clients["abc"].getConnectionAwait())
    }

    suspend fun dispatch(userId: String) : MysqlConnection {
        return MysqlConnection(clients[userId].getConnectionAwait())
    }
}
