package cn.pgyyd.mcg.db

import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.ext.asyncsql.AsyncSQLClient
import io.vertx.ext.asyncsql.MySQLClient
import io.vertx.ext.sql.ResultSet
import io.vertx.ext.sql.SQLConnection
import io.vertx.ext.sql.UpdateResult
import io.vertx.kotlin.ext.sql.closeAwait
import io.vertx.kotlin.ext.sql.getConnectionAwait
import io.vertx.kotlin.ext.sql.queryAwait
import io.vertx.kotlin.ext.sql.updateAwait
import org.apache.commons.lang3.StringUtils


/**
 * 封装MySql连接，目的是模拟C++的RAII
 */
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


/**
 * 分配哪个学生到哪个数据库
 */
class MysqlProxy(vertx: Vertx, dbs: JsonArray) {

    private lateinit var clients : List<Map.Entry<Regex, AsyncSQLClient>>

    private var commonSQLClient: AsyncSQLClient? = null

    init {
        val clis = mutableListOf<Map.Entry<Regex, AsyncSQLClient>>()
        for (i in 0..dbs.size()) {
            val dbConfig = dbs.getJsonObject(i)
            val mysqlClient = MySQLClient.createShared(
                    vertx,
                    dbConfig,
                    dbConfig.getString("host") + dbConfig.getString("database")
            )

            if (StringUtils.isEmpty(dbConfig.getString("pattern"))) {
                if (commonSQLClient != null) {
                    throw Exception("double init commonSQLClient")
                }
                commonSQLClient = mysqlClient
            }
            val regex = Regex(dbConfig.getString("pattern"))
            clis.add(Entry<Regex, AsyncSQLClient>(regex, mysqlClient))
        }
    }

    /**
     * 分配到公共数据库
     */
    suspend fun dispatch() : MysqlConnection {
        return MysqlConnection(commonSQLClient!!.getConnectionAwait())
    }

    /**
     * 分配到每个学院的数据库
     */
    suspend fun dispatch(userId: String) : MysqlConnection {
        for (client in clients) {
            if (client.key.matches(userId)) {
                return MysqlConnection(client.value.getConnectionAwait())
            }
        }
        throw Exception("no match mysql")
    }

    data class Entry<K, V>(private val ky: K, private val vl: V) : Map.Entry<K, V> {
        override val key: K
            get() = ky
        override val value: V
            get() = vl
    }
}
