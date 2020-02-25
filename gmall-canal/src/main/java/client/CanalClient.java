package client;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.InvalidProtocolBufferException;
import constants.GmallConstants;
import utils.KafkaSender;

import java.net.InetSocketAddress;
import java.util.Random;

/**
 * @author Howard
 * @create 2020-02-22-9:47 上午
 */
//读取canal数据，解析后发送到Kafka
public class CanalClient {
    public static void main(String[] args) {
        //获取canal连接器
        CanalConnector canalConnector = CanalConnectors.newSingleConnector(new InetSocketAddress("hadoop102", 11111), "example", "", "");

        //抓去数据并解析
        while (true) {
            //连接canal
            canalConnector.connect();
            //指定订阅的数据库
            canalConnector.subscribe();
            //抓去数据
            Message message = canalConnector.get(100);

            //判断当前抓去是否有数据
            if (message.getEntries().size() <= 0) {
                System.out.println("没有数据，休息一下。。。");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                for (CanalEntry.Entry entry : message.getEntries()) {
                    //判断当前操作的类型，只留下对于数据操作的内容
                    if (CanalEntry.EntryType.ROWDATA.equals(entry.getEntryType())) {
                        //反序列数据
                        CanalEntry.RowChange rowChange = null;
                        try {
                            rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                        } catch (InvalidProtocolBufferException e) {
                            e.printStackTrace();
                        }
                        //获取表名
                        String tableName = entry.getHeader().getTableName();
                        //取出事件类型
                        CanalEntry.EventType eventType = rowChange.getEventType();
                        //处理数据，发送至Kafka
                        handler(tableName, eventType, rowChange);
                    }
                }
            }
        }
    }

    //处理数据，发送至Kafka
    private static void handler(String tableName, CanalEntry.EventType eventType, CanalEntry.RowChange rowChange) {

        //订单表并且是下单数据
        if ("order_info".equals(tableName) && CanalEntry.EventType.INSERT.equals(eventType)) {
            //将数据写入订单信息主题
            sendToKafka(rowChange, GmallConstants.GMALL_ORDER_INFO_TOPIC);
        } else if ("order_detail".equals(tableName) && CanalEntry.EventType.INSERT.equals(eventType
        )) {
            //将数据写入订单详情主题
            sendToKafka(rowChange, GmallConstants.GMALL_ORDER_DETAIL_TOPIC);
        } else if ("user_info".equals(tableName) && (CanalEntry.EventType.INSERT.equals(eventType) || CanalEntry.EventType.UPDATE.equals(eventType))) {
            //将数据写入订单用户主题
            sendToKafka(rowChange, GmallConstants.GMALL_USER_INFO_TOPIC);
        }
    }

    //将数据发送之Kafka
    private static void sendToKafka(CanalEntry.RowChange rowChange, String topic) {
        //遍历RowDatasList
        for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
            //创建JSON对象，用于存放一行数据
            JSONObject jsonObject = new JSONObject();
            //获取变化后的数据并遍历
            for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
                jsonObject.put(column.getName(), column.getValue());
            }
            //发送至Kafka
            System.out.println(jsonObject.toString());
            try {
                Thread.sleep(new Random().nextInt(5) * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            KafkaSender.send(topic, jsonObject.toString());
        }
    }

}
