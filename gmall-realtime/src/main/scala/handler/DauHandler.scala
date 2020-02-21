package handler

import bean.StartUpLog
import org.apache.spark.streaming.dstream.DStream
import redis.clients.jedis.Jedis
import utils.RedisUtil

/**
 * @author Howard
 * @create 2020-02-19-8:30 下午
 */
object DauHandler {
  /**
   * 批次内去重过滤
   *
   * @param filterByRedisDStream
   */
  def filterDataBybatch(filterByRedisDStream: DStream[StartUpLog]) = {

    //log=>((date,mid),log)
    val dateMidToLogDStream: DStream[((String, String), StartUpLog)] = filterByRedisDStream.map(log => ((log.logDate, log.mid), log))

    //groupby
    val dateMidToLogIterDStream: DStream[((String, String), Iterable[StartUpLog])] = dateMidToLogDStream.groupByKey()

    //sortwith(ts).take(1)
    val value1: DStream[StartUpLog] = dateMidToLogIterDStream.flatMap { case ((_, _), value) => {
      val logs: List[StartUpLog] = value.toList.sortWith(_.ts < _.ts).take(1)
      logs
    }
    }

    value1

  }


  /**
   * 跨批次进行去重过滤
   *
   * @param startupLogDStream
   * @return
   */
  def filterDataByRedis(startupLogDStream: DStream[StartUpLog]): DStream[StartUpLog] = {
    startupLogDStream.transform(rdd => {
      rdd.mapPartitions(iter => {
        //connect
        val jedisClient: Jedis = RedisUtil.getJedisClient

        //filter
        val logs: Iterator[StartUpLog] = iter.filter(log => {
          val redisKey = s"dau_${log.logDate}"
          !jedisClient.sismember(redisKey, log.mid)
        })

        //close
        jedisClient.close()

        //return
        logs

      })
    })
  }

  /**
   * 将mid存入redis，为过滤作准备
   *
   * @param startupLogDStream
   */
  def saveMidToRedis(startupLogDStream: DStream[StartUpLog]) = {
    startupLogDStream.foreachRDD(rdd => {
      rdd.foreachPartition(iter => {
        //创建连接
        val jedisClient: Jedis = RedisUtil.getJedisClient

        //把mid放入redis中，用set存储
        iter.foreach(log => {
          val redisKey = s"dau_${log.logDate}"
          jedisClient.sadd(redisKey, log.mid)
        })

        //关闭连接
        jedisClient

      })
    })
  }

}
