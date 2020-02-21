package com.example.gmalllogger.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import constants.GmallConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Howard
 * @create 2020-02-17-9:10 下午
 */
@RestController
@Slf4j
public class LoggerController {

//    @GetMapping("test1")
//    public String test1() {
//        return "success";
//    }

    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;

    @PostMapping("log")
    public String logger(@RequestParam("logString") String logString){

        //添加时间戳
        JSONObject jsonObject = JSON.parseObject(logString);
        jsonObject.put("ts", System.currentTimeMillis());

        //用log4j打印日志
        String tsJson = jsonObject.toString();
        log.info(tsJson);

        //使用Kafka生产者将数据发送到Kafka
        if (tsJson.contains("startup")) {
            kafkaTemplate.send(GmallConstants.GMALL_STARTUP_TOPIC, tsJson);
        } else {
            kafkaTemplate.send(GmallConstants.GMALL_EVENT_TOPIC,tsJson);
        }

        return "success";
    }
}
