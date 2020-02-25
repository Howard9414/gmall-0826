package app

import java.text.SimpleDateFormat
import java.util
import java.util.Date

import bean.{CouponAlertInfo, EventLog}
import com.alibaba.fastjson.JSON
import constants.GmallConstants
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import utils.{MyEsUtil, MyKafkaUtil}

import scala.util.control.Breaks._

/**
 * @author Howard
 * @create 2020-02-23-11:12 上午
 */
object AlterAPP {
  def main(args: Array[String]): Unit = {
    val sc: SparkConf = new SparkConf().setAppName("AlterAPP").setMaster("local[*]")
    val ssc = new StreamingContext(sc, Seconds(5))
    val kafkaDStream: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtil.getKafkaStream(ssc, Set(GmallConstants.GMALL_EVENT_TOPIC))

    val sdf = new SimpleDateFormat("yyyy-MM-dd HH")

    //kafka数据封装成样例类
    val eventLogDStream: DStream[EventLog] = kafkaDStream.map(record => {
      val value: String = record.value()
      val log: EventLog = JSON.parseObject(value, classOf[EventLog])

      val ts: Long = log.ts
      val date: String = sdf.format(new Date(ts))
      log.logDate = date.split(" ")(0)
      log.logHour = date.split(" ")(1)

      log
    })

    //开窗
    val windowEventLogDStream: DStream[EventLog] = eventLogDStream.window(Seconds(30))

    //按照mid进行groupbykey
    val midToLogDStream: DStream[(String, Iterable[EventLog])] = windowEventLogDStream.map(log => (log.mid, log)).groupByKey()

    //进行逻辑判断，是否有三个以上uid && 是否点击浏览商品
    val boolToAlterInfoDStream: DStream[(Boolean, CouponAlertInfo)] = midToLogDStream.map { case (mid, logIter) => {

      //存放uid的集合
      val uids = new util.HashSet[String]()
      //存放商品id的集合
      val itemIds = new util.HashSet[String]()
      //存放eventid的集合
      val events = new util.ArrayList[String]()
      //是否有点击记录的标记位
      var noClick: Boolean = true

      breakable { //遍历
        logIter.foreach(log => {

          events.add(log.evid)

          if ("lickItem".equals(log.evid)) {
            noClick = false
            break()
          } else if ("coupon".equals(log.evid)) {
            uids.add(log.uid)
            itemIds.add(log.itemid)
          }

        })
      }

      (uids.size() >= 3 && noClick, CouponAlertInfo(mid, uids, itemIds, events, System.currentTimeMillis()))

    }
    }

    //过滤出有效数据
    val alertInfoDStream: DStream[CouponAlertInfo] = boolToAlterInfoDStream.filter(_._1).map(_._2)

    //将预警信息结构变为(id+time,CouponAlertInfo)
    val midToAlertInfoDStream: DStream[(String, CouponAlertInfo)] = alertInfoDStream.map(log => {
      val ts: Long = log.ts
      val minute: Long = ts / 1000 / 60
      (log.mid + minute.toString, log)
    })

    //写入ES
    midToAlertInfoDStream.foreachRDD(rdd => {
      rdd.foreachPartition(midToAlertInfoIter => {
        MyEsUtil.insertBulk(GmallConstants.GMALL_COUPON_ALERT_TOPIC, midToAlertInfoIter.toList)
      })
    })

    ssc.start()
    ssc.awaitTermination()

  }
}
