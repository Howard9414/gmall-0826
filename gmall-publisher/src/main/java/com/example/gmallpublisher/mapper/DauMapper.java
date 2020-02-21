package com.example.gmallpublisher.mapper;

import java.util.List;
import java.util.Map;

/**
 * @author Howard
 * @create 2020-02-20-8:26 下午
 */
public interface DauMapper {

    //获取总数
    public int getTotal(String date);

    //获取分时统计
    public List<Map> selectDauTotalHourMap(String date);

}
