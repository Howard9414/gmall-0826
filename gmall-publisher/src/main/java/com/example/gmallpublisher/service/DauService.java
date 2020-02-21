package com.example.gmallpublisher.service;

import java.util.Map;

/**
 * @author Howard
 * @create 2020-02-20-8:27 下午
 */
public interface DauService {
    //获取总数
    public int getTotal(String date);

    //获取分时统计的数据
    public Map getRealTimeHours(String date);

}
