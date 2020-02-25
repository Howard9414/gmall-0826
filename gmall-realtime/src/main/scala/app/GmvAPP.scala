package app

import bean.OrderInfo
import com.alibaba.fastjson.JSON
import constants.GmallConstants
import org.apache.hadoop.conf.Configuration
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import utils.MyKafkaUtil
import org.apache.phoenix.spark._

/**
 * @author Howard
 * @create 2020-02-22-10:29 上午
 */
object GmvAPP {
  def main(args: Array[String]): Unit = {
    //准备工作
    val sc: SparkConf = new SparkConf().setMaster("local[*]").setAppName("GmvAPP")
    val ssc = new StreamingContext(sc, Seconds(5))
    val kafkaDStream: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtil.getKafkaStream(ssc, Set(GmallConstants.GMALL_ORDER_INFO_TOPIC))

    //将每一行数据装入样例类
    val orderInfoDStream: DStream[OrderInfo] = kafkaDStream.map(record => {
      val value: String = record.value()
      val orderInfo: OrderInfo = JSON.parseObject(value, classOf[OrderInfo])
      val createTimes: Array[String] = orderInfo.create_time.split(" ")
      orderInfo.create_date = createTimes(0)
      orderInfo.create_hour = createTimes(1).split(":")(0)
      orderInfo.consignee_tel = orderInfo.consignee_tel.splitAt(4)._1 + "*******"
      orderInfo
    })

    //将订单数据写入HBase
    orderInfoDStream.foreachRDD(rdd=>{
      rdd.saveToPhoenix("GMALL190826_ORDER_INFO", Seq("ID", "PROVINCE_ID", "CONSIGNEE", "ORDER_COMMENT", "CONSIGNEE_TEL", "ORDER_STATUS", "PAYMENT_WAY", "USER_ID", "IMG_URL", "TOTAL_AMOUNT", "EXPIRE_TIME", "DELIVERY_ADDRESS", "CREATE_TIME", "OPERATE_TIME", "TRACKING_NO", "PARENT_ORDER_ID", "OUT_TRADE_NO", "TRADE_BODY", "CREATE_DATE", "CREATE_HOUR"), new Configuration(), Some("hadoop102,hadoop103,hadoop104:2181"))
    })

    //开启任务
    ssc.start()
    ssc.awaitTermination()
  }
}
