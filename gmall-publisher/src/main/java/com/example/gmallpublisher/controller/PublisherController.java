package com.example.gmallpublisher.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Howard
 * @create 2020-02-20-7:38 下午
 */
@RestController
public class PublisherController {

    @GetMapping("realtime-total")
    public String getRealTImeTotal(@RequestParam("date") String date){

       return  "";
    }
}
