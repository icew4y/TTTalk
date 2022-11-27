package com.test.tttalk;

import java.util.HashMap;
import java.util.Map;

public class TagIds {
    public static HashMap<String, Integer> tagIds = new HashMap<String, Integer>(){{
        put("全房间", 0);
        put("英雄联盟手游", 98);
        put("香肠派对", 190);
        put("大富翁", 24);
        put("QQ飞车手游", 83);
        put("王者荣耀", 1);
        put("和平精英", 2);
        put("未来之役", 178);
        put("扩列聊天", 17);
        put("碰碰作战", 60);
        put("飞行棋", 62);
    }};

    public static int getId(String tagName) {
        if (tagIds.containsKey(tagName)) {
            return tagIds.get(tagName);
        } else {
            return tagIds.get("全房间");
        }
    }
}
