package org.wiki.service;

import com.yomahub.roguemap.RogueMap;
import lombok.extern.slf4j.Slf4j;
import com.yomahub.roguemap.serialization.StringCodec;

@Slf4j
public class RogueMapService {





    public static void main(String[] args) {
        try (RogueMap<String, String> map = RogueMap.<String, String>mmap()
                .persistent("data/mydata.db")
                .keyCodec(StringCodec.INSTANCE)
                .valueCodec(StringCodec.INSTANCE)
                .build()) {

            // 存储数据
            map.put("user:1001", "150L");
            map.put("user:1002", "250L");
            map.put("user:1003", "350L");

            // 查询数据
            System.out.println("user:1001 = " + map.get("user:1001")); // 150
            System.out.println("user:1002 = " + map.get("user:1002")); // 250

            // 删除数据
            map.remove("user:1003");

            // 遍历数据
            map.forEach((key, value) -> {
                System.out.println(key + " -> " + value);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
