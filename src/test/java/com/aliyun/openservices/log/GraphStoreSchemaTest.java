package com.aliyun.openservices.log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.aliyun.openservices.log.common.*;
import com.aliyun.openservices.log.internal.GraphStoreSchemaCodec;
import org.junit.Test;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class GraphStoreSchemaTest {
    @Test
    public void updateUsesNativeBindingAndReplacementFields() {
        JSONObject input = wire(GraphStoreManagementTest.update("one"));
        input.put("ttlSeconds", null);
        input.put("newOption", true);
        GraphStoreCollectionSchemaUpdate update = input.toJavaObject(GraphStoreCollectionSchemaUpdate.class);
        assertNull(update.getTtlSeconds());
        assertFalse(wire(update).containsKey("ttlSeconds"));
        for (Long ttl : new Long[]{0L, -1L, Long.MAX_VALUE}) {
            input.put("ttlSeconds", ttl);
            assertEquals(ttl, input.toJavaObject(GraphStoreCollectionSchemaUpdate.class).getTtlSeconds());
        }
        input.put("graphLayout", "vertex");
        input.put("collectionKeyValues", new JSONArray());
        JSONObject json = wire(input.toJavaObject(GraphStoreCollectionSchemaUpdate.class));
        assertFalse(json.containsKey("graphLayout"));
        assertFalse(json.containsKey("collectionKeyValues"));
        assertTrue(json.getJSONArray("fields").isEmpty());
    }

    @Test
    public void baseLayoutMappingPreservesOptionalValuesAndInput() {
        GraphStoreBaseSchema base = GraphStoreManagementTest.base();
        base.getVertexLayout().setCk(Collections.<String>emptyList());
        base.getVertexLayout().setKeyIndexConfigs(Collections.singletonMap("vl", new GraphStoreIndexConfig(true)));
        JSONObject encoded = wire(GraphStoreSchemaCodec.encode(base));
        assertFalse(encoded.containsKey("vertexLayout"));
        JSONArray layouts = encoded.getJSONArray("graphLayouts");
        assertEquals("vertex", layouts.getJSONObject(0).getString("name"));
        assertEquals("edge", layouts.getJSONObject(1).getString("name"));
        assertTrue(layouts.getJSONObject(0).getJSONArray("ck").isEmpty());
        assertFalse(layouts.getJSONObject(0).containsKey("pk"));
        encoded.put("ttlSeconds", new BigInteger(String.valueOf(Long.MAX_VALUE)));
        encoded.put("timeStateEnable", "false");
        encoded.put("newOption", true);
        String before = encoded.toJSONString();
        GraphStoreBaseSchema read = GraphStoreSchemaCodec.readBaseSchema(encoded);
        assertEquals(before, encoded.toJSONString());
        assertEquals(Long.valueOf(Long.MAX_VALUE), read.getTtlSeconds());
        assertFalse(read.getTimeStateEnable());
        assertTrue(read.getVertexLayout().getKeyIndexConfigs().get("vl").getEnable());
        assertNull(read.getCommonFieldNames());
        assertEquals("edge", read.getEdgeLayout().getName());
        assertFalse(wire(GraphStoreSchemaCodec.encode(read)).containsKey("newOption"));
    }

    @Test
    public void nativeEnumsAndNestedValuesRoundTrip() {
        GraphStoreCollectionSchema collection = GraphStoreManagementTest.collection("unicode\u0000\u96ea");
        GraphStoreIndexConfig full = new GraphStoreIndexConfig(true);
        full.setFullTextIndex(new GraphStoreFullTextIndex(" ,", false, false));
        GraphStoreIndexConfig range = new GraphStoreIndexConfig(true);
        range.setRangeIndex(new GraphStoreRangeIndex());
        collection.setFields(Arrays.asList(
                new GraphStoreFieldSchema("exact", GraphStoreFieldType.INT, new GraphStoreIndexConfig(true)),
                new GraphStoreFieldSchema("full", GraphStoreFieldType.STRING, full),
                new GraphStoreFieldSchema("range", GraphStoreFieldType.DOUBLE, range),
                new GraphStoreFieldSchema("bool", GraphStoreFieldType.BOOL, new GraphStoreIndexConfig(false))));
        collection.setCollectionKeyValues(Arrays.asList(new GraphStoreCollectionKeyValue("vl", "")));
        collection.setTtlSeconds(Long.MAX_VALUE);
        JSONObject encoded = wire(collection);
        assertEquals("vertex", encoded.getString("graphLayout"));
        encoded.put("newOption", true);
        JSONObject roundTrip = wire(encoded.toJavaObject(GraphStoreCollectionSchema.class));
        encoded.remove("newOption");
        assertEquals(encoded, roundTrip);
        collection.setGraphLayout(GraphStoreLayoutType.EDGE);
        assertEquals("edge", wire(collection).getString("graphLayout"));
        encoded.getJSONArray("fields").getJSONObject(0).put("type", "UNKNOWN");
        assertNull(encoded.toJavaObject(GraphStoreCollectionSchema.class).getFields().get(0).getType());
    }

    @Test
    public void missingSchemaValuesRemainNull() {
        assertNull(GraphStoreSchemaCodec.encode(null));
        assertNull(GraphStoreSchemaCodec.readBaseSchema(null));
        GraphStoreBaseSchema base = GraphStoreSchemaCodec.readBaseSchema(new JSONObject());
        assertNull(base.getTtlSeconds());
        assertNull(base.getTimeStateEnable());
        assertNull(base.getIndexEnable());
        assertNull(base.getCommonFieldNames());
        assertNull(base.getVertexLayout());
        assertNull(base.getEdgeLayout());
        GraphStoreCollectionSchema collection = new JSONObject().toJavaObject(GraphStoreCollectionSchema.class);
        assertNull(collection.getName());
        assertNull(collection.getDescription());
        assertNull(collection.getGraphLayout());
        assertNull(collection.getCollectionKeyValues());
        assertNull(collection.getFields());
        GraphStoreCollectionSchemaUpdate update = new JSONObject().toJavaObject(GraphStoreCollectionSchemaUpdate.class);
        assertNull(update.getName());
        assertNull(update.getDescription());
        assertNull(update.getFields());
        JSONObject partial = new JSONObject();
        partial.put("graphLayouts", new JSONArray(Arrays.<Object>asList(
                JSONObject.parseObject("{\"name\":\"vertex\"}"), null,
                JSONObject.parseObject("{\"name\":\"unknown\"}"))));
        base = GraphStoreSchemaCodec.readBaseSchema(partial);
        assertNull(base.getVertexLayout().getId());
        assertNull(base.getVertexLayout().getLabel());
        assertNull(base.getEdgeLayout());
    }

    private static JSONObject wire(Object value) {
        return JSONObject.parseObject(JSONObject.toJSONString(value, SerializerFeature.DisableCircularReferenceDetect));
    }
}
