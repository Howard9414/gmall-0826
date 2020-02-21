package utils

import java.util.Properties

import constants.GmallConstants
import kafka.serializer.StringDecoder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}

/**
 * @author Howard
 * @create 2020-02-19-6:40 下午
 */
object MyKafkaUtil {
  def getKafkaStream(ssc: StreamingContext, topics: Set[String]): InputDStream[ConsumerRecord[String, String]] = {

    val properties: Properties = PropertiesUtil.load("config.properties")

    val kafkaPara = Map(
      "bootstrap.servers" -> properties.getProperty("kafka.broker.list"),
      "group.id" -> "bigdata0826",

      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    //基于Direct方式消费Kafka数据
    val kafkaDStream: InputDStream[ConsumerRecord[String, String]] = KafkaUtils.createDirectStream[String, String](
      ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](topics, kafkaPara)
    )

    //返回
    kafkaDStream
  }

}
