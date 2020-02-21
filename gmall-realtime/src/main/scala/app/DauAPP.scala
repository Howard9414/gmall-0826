package app

import java.text.SimpleDateFormat
import java.util.Date

import bean.StartUpLog
import com.alibaba.fastjson.JSON
import constants.GmallConstants
import handler.DauHandler
import org.apache.hadoop.conf.Configuration
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import utils.MyKafkaUtil
import org.apache.phoenix.spark._

/**
 * @author Howard
 * @create 2020-02-19-6:54 下午
 */
object DauAPP {
  def main(args: Array[String]): Unit = {

    val sc: SparkConf = new SparkConf().setAppName("TestKafka").setMaster("local[*]")

    val ssc = new StreamingContext(sc, Seconds(5))

    val sdf = new SimpleDateFormat("yyyy-MM-dd HH")

    val kafkaDStream: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtil.getKafkaStream(ssc, Set(GmallConstants.GMALL_STARTUP_TOPIC))

    val startupLogDStream: DStream[StartUpLog] = kafkaDStream.map { record => {
      val value: String = record.value()
      val log: StartUpLog = JSON.parseObject(value, classOf[StartUpLog])

      //加入时间属性
      val ts: Long = log.ts
      val dateHour: String = sdf.format(new Date(ts))
      val dateAndHour: Array[String] = dateHour.split(" ")
      log.logDate = dateAndHour(0)
      log.logHour = dateAndHour(1)

      log
    }
    }

    //批次间过滤数据
    val filterByRedisDStream: DStream[StartUpLog] = DauHandler.filterDataByRedis(startupLogDStream)

    //批次内过滤数据
    val filterByBatchDStream: DStream[StartUpLog] = DauHandler.filterDataBybatch(filterByRedisDStream)

    //将数据保存到redis，供下一次过滤使用
    DauHandler.saveMidToRedis(filterByBatchDStream)

    filterByBatchDStream.count().print()

    //有效数据写入hbase
    filterByBatchDStream.foreachRDD(rdd=>{
      rdd.saveToPhoenix("GMALL190826_DAU", Seq("MID", "UID", "APPID", "AREA", "OS", "CH", "TYPE", "VS", "LOGDATE", "LOGHOUR", "TS"), new Configuration, Some("hadoop102,hadoop103,hadoop104:2181"))
    })

    ssc.start()
    ssc.awaitTermination()
  }
}
