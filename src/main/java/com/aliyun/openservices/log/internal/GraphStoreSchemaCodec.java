package com.aliyun.openservices.log.internal;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.log.common.GraphStoreBaseSchema;
import java.math.BigInteger;

/** BaseSchema wire layout adaptation and lossless management metadata reads. */
public final class GraphStoreSchemaCodec {
    private GraphStoreSchemaCodec() { }

    public static <T> T field(JSONObject object, String name, Class<T> type) {
        Object value = object.get(name);
        if (!type.isInstance(value)) throw new IllegalArgumentException("invalid GraphStore field " + name);
        return type.cast(value);
    }

    // FastJSON numeric getters truncate overflowing management IDs and counts.
    public static long integer(JSONObject object, String name) {
        Object value = object.get(name);
        if (value instanceof Integer || value instanceof Long
                || value instanceof BigInteger && ((BigInteger) value).bitLength() <= 63) {
            return ((Number) value).longValue();
        }
        throw new IllegalArgumentException("invalid GraphStore int64 " + name);
    }

    public static int int32(JSONObject object, String name) {
        long value = integer(object, name);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)
            throw new IllegalArgumentException("GraphStore int32 overflow " + name);
        return (int) value;
    }

    public static JSONObject encode(GraphStoreBaseSchema value) {
        if (value == null) return null;
        JSONObject result = (JSONObject) JSON.toJSON(value);
        JSONArray layouts = new JSONArray(2);
        layouts.add(result.remove("vertexLayout"));
        layouts.add(result.remove("edgeLayout"));
        result.put("graphLayouts", layouts);
        return result;
    }

    public static GraphStoreBaseSchema readBaseSchema(JSONObject value) {
        if (value == null) return null;
        JSONObject bean = (JSONObject) value.clone();
        JSONArray layouts = bean.getJSONArray("graphLayouts");
        bean.remove("graphLayouts");
        bean.remove("vertexLayout");
        bean.remove("edgeLayout");
        if (layouts != null) for (int i = 0; i < layouts.size(); i++) {
            JSONObject layout = layouts.getJSONObject(i);
            if (layout == null) continue;
            String name = layout.getString("name");
            if ("vertex".equals(name) || "edge".equals(name)) bean.put(name + "Layout", layout);
        }
        return bean.toJavaObject(GraphStoreBaseSchema.class);
    }
}
