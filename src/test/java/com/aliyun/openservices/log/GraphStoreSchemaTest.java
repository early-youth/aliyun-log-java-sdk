package com.aliyun.openservices.log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.log.common.*;
import com.aliyun.openservices.log.internal.GraphStoreSchemaCodec;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class GraphStoreSchemaTest {
    @Test
    public void updateJsonUsesTypedReplacementAndRejectsUnknownFields() {
        JSONObject input = JSONObject.parseObject("{\"name\":\"one\",\"description\":\"雪\",\"ttlSeconds\":null,\"fields\":[]}");
        GraphStoreCollectionSchemaUpdate update = GraphStoreSchemaCodec.readCollectionSchemaUpdate(input);
        assertEquals("雪", update.getDescription());
        assertNull(update.getTtlSeconds());
        assertFalse(GraphStoreSchemaCodec.encode(update).containsKey("ttlSeconds"));
        input.put("ttlSeconds", 0L);
        assertEquals(Long.valueOf(0), GraphStoreSchemaCodec.readCollectionSchemaUpdate(input).getTtlSeconds());
        for (String ttl : new String[]{"-1", "9223372036854775808", "1.5"}) {
            input.put("ttlSeconds", JSONObject.parse(ttl));
            invalid(() -> GraphStoreSchemaCodec.readCollectionSchemaUpdate(input));
        }
        input.remove("ttlSeconds");
        input.put("graphLayout", "vertex");
        invalid(() -> GraphStoreSchemaCodec.readCollectionSchemaUpdate(input));
        input.remove("graphLayout");
        input.remove("name");
        invalid(() -> GraphStoreSchemaCodec.readCollectionSchemaUpdate(input));
    }

    @Test
    public void fixedLayoutsAndOptionalKeysRoundTrip() {
        GraphStoreBaseSchema base = GraphStoreManagementTest.base();
        base.getVertexLayout().setCk(Collections.<String>emptyList());
        base.getVertexLayout().setKeyIndexConfigs(Collections.<String, GraphStoreIndexConfig>emptyMap());
        base.getEdgeLayout().setRouteKey(Arrays.asList("sl"));
        JSONObject encoded = GraphStoreSchemaCodec.encode(base);
        assertFalse(encoded.containsKey("vertexLayout"));
        JSONArray layouts = encoded.getJSONArray("graphLayouts");
        assertEquals("vertex", layouts.getJSONObject(0).getString("name"));
        assertEquals("edge", layouts.getJSONObject(1).getString("name"));
        assertEquals(0, layouts.getJSONObject(0).getJSONArray("ck").size());
        assertFalse(layouts.getJSONObject(0).containsKey("pk"));
        assertEquals(encoded, GraphStoreSchemaCodec.encode(GraphStoreSchemaCodec.readBaseSchema(encoded)));
        for (String bad : new String[]{"9223372036854775808", "1.5", "null", "-1", "\"2\""}) {
            JSONObject value = JSONObject.parseObject(encoded.toJSONString());
            value.put("ttlSeconds", JSONObject.parse(bad));
            invalid(() -> GraphStoreSchemaCodec.readBaseSchema(value));
        }
        for (String field : new String[]{"timeStateEnable", "indexEnable", "ttlSeconds", "graphLayouts"}) {
            JSONObject value = JSONObject.parseObject(encoded.toJSONString());
            value.remove(field);
            invalid(() -> GraphStoreSchemaCodec.readBaseSchema(value));
        }
        JSONObject unknown = JSONObject.parseObject(encoded.toJSONString());
        unknown.getJSONArray("graphLayouts").getJSONObject(0).put("source", null);
        invalid(() -> GraphStoreSchemaCodec.readBaseSchema(unknown));
        unknown.getJSONArray("graphLayouts").getJSONObject(0).remove("source");
        unknown.put("newOption", true);
        invalid(() -> GraphStoreSchemaCodec.readBaseSchema(unknown));
    }

    @Test
    public void collectionTypesAndReplacementSemantics() {
        GraphStoreCollectionSchema collection = GraphStoreManagementTest.collection("unicode\u0000\u96ea");
        GraphStoreFullTextIndex fullText = new GraphStoreFullTextIndex(" ,", false, false);
        GraphStoreIndexConfig full = new GraphStoreIndexConfig(true);
        full.setFullTextIndex(fullText);
        GraphStoreIndexConfig range = new GraphStoreIndexConfig(true);
        range.setRangeIndex(new GraphStoreRangeIndex());
        collection.setFields(Arrays.asList(new GraphStoreFieldSchema("exact", GraphStoreFieldType.INT, new GraphStoreIndexConfig(true)),
                new GraphStoreFieldSchema("full", GraphStoreFieldType.STRING, full),
                new GraphStoreFieldSchema("range", GraphStoreFieldType.DOUBLE, range),
                new GraphStoreFieldSchema("bool", GraphStoreFieldType.BOOL, new GraphStoreIndexConfig(false))));
        collection.setCollectionKeyValues(Arrays.asList(new GraphStoreCollectionKeyValue("vl", "")));
        collection.setTtlSeconds(Long.MAX_VALUE);
        JSONObject encoded = GraphStoreSchemaCodec.encode(collection);
        assertEquals(encoded, GraphStoreSchemaCodec.encode(GraphStoreSchemaCodec.readCollectionSchema(encoded)));
        for (String type : new String[]{"UNKNOWN", "int", "NEW_TYPE"}) {
            JSONObject bad = JSONObject.parseObject(encoded.toJSONString());
            bad.getJSONArray("fields").getJSONObject(0).put("type", type);
            invalid(() -> GraphStoreSchemaCodec.readCollectionSchema(bad));
        }
        JSONObject bad = JSONObject.parseObject(encoded.toJSONString());
        bad.getJSONArray("fields").getJSONObject(2).getJSONObject("indexConfig").getJSONObject("rangeIndex").put("newOption", true);
        invalid(() -> GraphStoreSchemaCodec.readCollectionSchema(bad));
        for (Long ttl : new Long[]{null, 0L, 123L}) {
            GraphStoreCollectionSchemaUpdate update = GraphStoreManagementTest.update("collection");
            update.setTtlSeconds(ttl);
            JSONObject wire = GraphStoreSchemaCodec.encode(update);
            assertEquals(ttl != null, wire.containsKey("ttlSeconds"));
            assertFalse(wire.containsKey("graphLayout"));
            assertFalse(wire.containsKey("collectionKeyValues"));
            assertEquals(0, wire.getJSONArray("fields").size());
        }
        collection.setTtlSeconds(-1L);
        invalid(() -> GraphStoreSchemaCodec.encode(collection));
        invalid(() -> GraphStoreSchemaCodec.encode(new GraphStoreBaseSchema()));
    }

    private static void invalid(Runnable action) {
        try { action.run(); fail("expected a schema compatibility/parameter error"); }
        catch (IllegalArgumentException expected) { }
    }
}
