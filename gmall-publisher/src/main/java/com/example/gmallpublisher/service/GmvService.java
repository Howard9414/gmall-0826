package com.example.gmallpublisher.service;

import java.util.Map;

/**
 * @author Howard
 * @create 2020-02-22-11:53 上午
 */
public interface GmvService {
    //获取GMV总额
    public Double getTotal(String date);

    //获取GMV分时统计结果
    public Map getHoursGmv(String date);
}
