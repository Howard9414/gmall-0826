package com.example.gmallpublisher.service.impl;

import com.example.gmallpublisher.mapper.GmvMapper;
import com.example.gmallpublisher.service.GmvService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Howard
 * @create 2020-02-22-11:54 上午
 */
@Service
public class GmvServiceImpl implements GmvService {

    @Autowired
    GmvMapper gmvMapper;


    @Override
    public Double getTotal(String date) {
        return gmvMapper.selectOrderAmountTotal(date);
    }

    @Override
    public Map getHoursGmv(String date) {
        List<Map> list = gmvMapper.selectOrderAmountHourMap(date);

        HashMap<Object, Object> map = new HashMap<>();

        for (Map m : list) {
            map.put((String) m.get("CREATE_HOUR"), (Double) m.get("SUM_AMOUNT"));
        }

        return map;
    }
}
