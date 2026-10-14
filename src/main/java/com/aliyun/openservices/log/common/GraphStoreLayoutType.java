package com.aliyun.openservices.log.common;

import com.alibaba.fastjson.serializer.JSONSerializable;
import com.alibaba.fastjson.serializer.JSONSerializer;
import java.lang.reflect.Type;

/** Logical layout values serialized using the SDK's native FastJSON enum pattern. */
public enum GraphStoreLayoutType implements JSONSerializable {
    VERTEX("vertex"), EDGE("edge");

    private final String value;
    GraphStoreLayoutType(String value) { this.value = value; }
    public String getValue() { return value; }

    @Override
    public void write(JSONSerializer serializer, Object fieldName, Type fieldType, int features) {
        serializer.write(value);
    }
}
