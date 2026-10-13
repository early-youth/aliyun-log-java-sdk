package com.aliyun.openservices.log.internal;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.log.common.*;
import java.math.BigInteger;
import java.util.*;

/** Internal wire mapping for the concrete GraphStore models. */
public final class GraphStoreSchemaCodec {
    private GraphStoreSchemaCodec() { }

    public static <T> T field(JSONObject object, String name, Class<T> type) {
        Object value = object.get(name);
        if (!type.isInstance(value)) throw new IllegalArgumentException("invalid GraphStore field " + name);
        return type.cast(value);
    }

    // FastJSON numeric getters truncate overflowing IDs and TTL values.
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

    private static <T> T required(T value, String name) {
        if (value == null) throw new IllegalArgumentException("missing GraphStore schema " + name);
        return value;
    }

    private static String text(String value, String name, boolean emptyAllowed) {
        required(value, name);
        if (!emptyAllowed && value.isEmpty()) throw new IllegalArgumentException("empty GraphStore schema " + name);
        return value;
    }

    private static Long ttl(Long value) {
        if (value != null && value < 0) throw new IllegalArgumentException("negative GraphStore TTL");
        return value;
    }

    private static void known(JSONObject value, String... names) {
        Set<String> allowed = new HashSet<String>(Arrays.asList(names));
        for (String name : value.keySet()) {
            if (!allowed.contains(name)) throw new IllegalArgumentException("unsupported GraphStore schema field " + name);
        }
    }

    private static JSONArray strings(List<String> values, String name) {
        JSONArray result = new JSONArray(values.size());
        for (String value : values) result.add(text(value, name, false));
        return result;
    }

    private static List<String> strings(JSONArray values, String name) {
        List<String> result = new ArrayList<String>(values.size());
        for (Object value : values) {
            if (!(value instanceof String)) throw new IllegalArgumentException("invalid schema string " + name);
            result.add(text((String) value, name, false));
        }
        return result;
    }

    private static GraphStoreLayoutType layoutType(String value) {
        for (GraphStoreLayoutType type : GraphStoreLayoutType.values()) if (type.getValue().equals(value)) return type;
        throw new IllegalArgumentException("unsupported GraphStore layout " + value);
    }

    private static JSONObject encode(GraphStoreFullTextIndex value) {
        required(value, "GraphStoreFullTextIndex");
        JSONObject result = new JSONObject();
        result.put("token", text(value.getToken(), "token", false));
        if (value.getOptionToken() != null) result.put("optionToken", text(value.getOptionToken(), "optionToken", false));
        result.put("caseSensitive", required(value.getCaseSensitive(), "caseSensitive"));
        result.put("chn", required(value.getChn(), "chn"));
        return result;
    }

    private static JSONObject encode(GraphStoreIndexConfig value) {
        required(value, "GraphStoreIndexConfig");
        JSONObject result = new JSONObject();
        result.put("enable", required(value.getEnable(), "enable"));
        if (value.getFullTextIndex() != null) result.put("fullTextIndex", encode(value.getFullTextIndex()));
        if (value.getRangeIndex() != null) result.put("rangeIndex", new JSONObject());
        return result;
    }

    private static JSONObject encode(GraphStoreFieldSchema value) {
        required(value, "GraphStoreFieldSchema");
        JSONObject result = new JSONObject();
        result.put("name", text(value.getName(), "name", false));
        result.put("type", required(value.getType(), "type").getValue());
        result.put("indexConfig", encode(value.getIndexConfig()));
        return result;
    }

    private static JSONObject encode(GraphStoreCollectionKeyValue value) {
        required(value, "GraphStoreCollectionKeyValue");
        JSONObject result = new JSONObject();
        result.put("name", text(value.getName(), "name", false));
        result.put("value", text(value.getValue(), "value", true));
        return result;
    }

    private static JSONObject encode(GraphStoreEndpoint value) {
        required(value, "GraphStoreEndpoint");
        JSONObject result = new JSONObject();
        result.put("label", text(value.getLabel(), "label", false));
        result.put("id", text(value.getId(), "id", false));
        return result;
    }

    private static JSONObject encode(GraphStoreVertexLayout value) {
        required(value, "GraphStoreVertexLayout");
        JSONObject result = new JSONObject();
        result.put("name", value.getName());
        result.put("label", text(value.getLabel(), "label", false));
        result.put("id", text(value.getId(), "id", false));
        if (value.getCk() != null) result.put("ck", strings(value.getCk(), "ck"));
        if (value.getPk() != null) result.put("pk", strings(value.getPk(), "pk"));
        if (value.getRouteKey() != null) result.put("routeKey", strings(value.getRouteKey(), "routeKey"));
        if (value.getKeyIndexConfigs() != null) {
            JSONObject indexes = new JSONObject();
            for (Map.Entry<String, GraphStoreIndexConfig> entry : value.getKeyIndexConfigs().entrySet())
                indexes.put(text(entry.getKey(), "keyIndexConfigs", false), encode(entry.getValue()));
            result.put("keyIndexConfigs", indexes);
        }
        return result;
    }

    private static JSONObject encode(GraphStoreEdgeLayout value) {
        required(value, "GraphStoreEdgeLayout");
        JSONObject result = new JSONObject();
        result.put("name", value.getName());
        result.put("source", encode(value.getSource()));
        result.put("label", text(value.getLabel(), "label", false));
        result.put("target", encode(value.getTarget()));
        if (value.getCk() != null) result.put("ck", strings(value.getCk(), "ck"));
        if (value.getPk() != null) result.put("pk", strings(value.getPk(), "pk"));
        if (value.getRouteKey() != null) result.put("routeKey", strings(value.getRouteKey(), "routeKey"));
        if (value.getKeyIndexConfigs() != null) {
            JSONObject indexes = new JSONObject();
            for (Map.Entry<String, GraphStoreIndexConfig> entry : value.getKeyIndexConfigs().entrySet())
                indexes.put(text(entry.getKey(), "keyIndexConfigs", false), encode(entry.getValue()));
            result.put("keyIndexConfigs", indexes);
        }
        return result;
    }

    public static JSONObject encode(GraphStoreBaseSchema value) {
        required(value, "GraphStoreBaseSchema");
        JSONObject result = new JSONObject();
        result.put("timeStateEnable", required(value.getTimeStateEnable(), "timeStateEnable"));
        result.put("ttlSeconds", ttl(required(value.getTtlSeconds(), "ttlSeconds")));
        result.put("commonFieldNames", strings(value.getCommonFieldNames() == null ? Collections.<String>emptyList() : value.getCommonFieldNames(), "commonFieldNames"));
        result.put("indexEnable", required(value.getIndexEnable(), "indexEnable"));
        if (value.getFullTextIndex() != null) result.put("fullTextIndex", encode(value.getFullTextIndex()));
        JSONArray layouts = new JSONArray(2);
        layouts.add(encode(value.getVertexLayout()));
        layouts.add(encode(value.getEdgeLayout()));
        result.put("graphLayouts", layouts);
        return result;
    }

    public static JSONObject encode(GraphStoreCollectionSchema value) {
        required(value, "GraphStoreCollectionSchema");
        JSONObject result = new JSONObject();
        result.put("name", text(value.getName(), "name", false));
        result.put("description", text(value.getDescription(), "description", false));
        result.put("graphLayout", required(value.getGraphLayout(), "graphLayout").getValue());
        JSONArray collectionKeyValues = new JSONArray();
        for (GraphStoreCollectionKeyValue item : required(value.getCollectionKeyValues(), "collectionKeyValues")) collectionKeyValues.add(encode(item));
        result.put("collectionKeyValues", collectionKeyValues);
        if (value.getTtlSeconds() != null) result.put("ttlSeconds", ttl(value.getTtlSeconds()));
        JSONArray fields = new JSONArray();
        for (GraphStoreFieldSchema item : required(value.getFields(), "fields")) fields.add(encode(item));
        result.put("fields", fields);
        return result;
    }

    public static JSONObject encode(GraphStoreCollectionSchemaUpdate value) {
        required(value, "GraphStoreCollectionSchemaUpdate");
        JSONObject result = new JSONObject();
        result.put("name", text(value.getName(), "name", false));
        result.put("description", text(value.getDescription(), "description", false));
        if (value.getTtlSeconds() != null) result.put("ttlSeconds", ttl(value.getTtlSeconds()));
        JSONArray fields = new JSONArray();
        for (GraphStoreFieldSchema item : required(value.getFields(), "fields")) fields.add(encode(item));
        result.put("fields", fields);
        return result;
    }

    private static GraphStoreFullTextIndex readFullTextIndex(JSONObject value) {
        required(value, "GraphStoreFullTextIndex");
        known(value, "token", "optionToken", "caseSensitive", "chn");
        GraphStoreFullTextIndex result = new GraphStoreFullTextIndex();
        result.setToken(text(field(value, "token", String.class), "token", false));
        if (value.get("optionToken") != null) result.setOptionToken(text(field(value, "optionToken", String.class), "optionToken", false));
        result.setCaseSensitive(field(value, "caseSensitive", Boolean.class));
        result.setChn(field(value, "chn", Boolean.class));
        return result;
    }

    private static GraphStoreRangeIndex readRangeIndex(JSONObject value) {
        required(value, "GraphStoreRangeIndex");
        known(value);
        GraphStoreRangeIndex result = new GraphStoreRangeIndex();
        return result;
    }

    private static GraphStoreIndexConfig readIndexConfig(JSONObject value) {
        required(value, "GraphStoreIndexConfig");
        known(value, "enable", "fullTextIndex", "rangeIndex");
        GraphStoreIndexConfig result = new GraphStoreIndexConfig();
        result.setEnable(field(value, "enable", Boolean.class));
        if (value.get("fullTextIndex") != null) result.setFullTextIndex(readFullTextIndex(field(value, "fullTextIndex", JSONObject.class)));
        if (value.get("rangeIndex") != null) result.setRangeIndex(readRangeIndex(field(value, "rangeIndex", JSONObject.class)));
        return result;
    }

    private static GraphStoreFieldSchema readFieldSchema(JSONObject value) {
        required(value, "GraphStoreFieldSchema");
        known(value, "name", "type", "indexConfig");
        GraphStoreFieldSchema result = new GraphStoreFieldSchema();
        result.setName(text(field(value, "name", String.class), "name", false));
        result.setType(GraphStoreFieldType.valueOf(field(value, "type", String.class)));
        result.setIndexConfig(readIndexConfig(field(value, "indexConfig", JSONObject.class)));
        return result;
    }

    private static GraphStoreCollectionKeyValue readCollectionKeyValue(JSONObject value) {
        required(value, "GraphStoreCollectionKeyValue");
        known(value, "name", "value");
        GraphStoreCollectionKeyValue result = new GraphStoreCollectionKeyValue();
        result.setName(text(field(value, "name", String.class), "name", false));
        result.setValue(text(field(value, "value", String.class), "value", true));
        return result;
    }

    private static GraphStoreEndpoint readEndpoint(JSONObject value) {
        required(value, "GraphStoreEndpoint");
        known(value, "label", "id");
        GraphStoreEndpoint result = new GraphStoreEndpoint();
        result.setLabel(text(field(value, "label", String.class), "label", false));
        result.setId(text(field(value, "id", String.class), "id", false));
        return result;
    }

    private static GraphStoreVertexLayout readVertexLayout(JSONObject value) {
        required(value, "GraphStoreVertexLayout");
        known(value, "name", "label", "id", "ck", "pk", "routeKey", "keyIndexConfigs");
        GraphStoreVertexLayout result = new GraphStoreVertexLayout();
        if (!"vertex".equals(field(value, "name", String.class))) throw new IllegalArgumentException("invalid layout name");
        result.setLabel(text(field(value, "label", String.class), "label", false));
        result.setId(text(field(value, "id", String.class), "id", false));
        if (value.get("ck") != null) result.setCk(strings(field(value, "ck", JSONArray.class), "ck"));
        if (value.get("pk") != null) result.setPk(strings(field(value, "pk", JSONArray.class), "pk"));
        if (value.get("routeKey") != null) result.setRouteKey(strings(field(value, "routeKey", JSONArray.class), "routeKey"));
        if (value.get("keyIndexConfigs") != null) {
            JSONObject indexes = field(value, "keyIndexConfigs", JSONObject.class);
            Map<String, GraphStoreIndexConfig> configs = new LinkedHashMap<String, GraphStoreIndexConfig>();
            for (String key : indexes.keySet()) configs.put(text(key, "keyIndexConfigs", false), readIndexConfig(field(indexes, key, JSONObject.class)));
            result.setKeyIndexConfigs(configs);
        }
        return result;
    }

    private static GraphStoreEdgeLayout readEdgeLayout(JSONObject value) {
        required(value, "GraphStoreEdgeLayout");
        known(value, "name", "source", "label", "target", "ck", "pk", "routeKey", "keyIndexConfigs");
        GraphStoreEdgeLayout result = new GraphStoreEdgeLayout();
        if (!"edge".equals(field(value, "name", String.class))) throw new IllegalArgumentException("invalid layout name");
        result.setSource(readEndpoint(field(value, "source", JSONObject.class)));
        result.setLabel(text(field(value, "label", String.class), "label", false));
        result.setTarget(readEndpoint(field(value, "target", JSONObject.class)));
        if (value.get("ck") != null) result.setCk(strings(field(value, "ck", JSONArray.class), "ck"));
        if (value.get("pk") != null) result.setPk(strings(field(value, "pk", JSONArray.class), "pk"));
        if (value.get("routeKey") != null) result.setRouteKey(strings(field(value, "routeKey", JSONArray.class), "routeKey"));
        if (value.get("keyIndexConfigs") != null) {
            JSONObject indexes = field(value, "keyIndexConfigs", JSONObject.class);
            Map<String, GraphStoreIndexConfig> configs = new LinkedHashMap<String, GraphStoreIndexConfig>();
            for (String key : indexes.keySet()) configs.put(text(key, "keyIndexConfigs", false), readIndexConfig(field(indexes, key, JSONObject.class)));
            result.setKeyIndexConfigs(configs);
        }
        return result;
    }

    public static GraphStoreBaseSchema readBaseSchema(JSONObject value) {
        required(value, "GraphStoreBaseSchema");
        known(value, "timeStateEnable", "ttlSeconds", "commonFieldNames", "indexEnable", "fullTextIndex", "graphLayouts");
        GraphStoreBaseSchema result = new GraphStoreBaseSchema();
        result.setTimeStateEnable(field(value, "timeStateEnable", Boolean.class));
        result.setTtlSeconds(ttl(integer(value, "ttlSeconds")));
        if (value.get("commonFieldNames") != null) result.setCommonFieldNames(strings(field(value, "commonFieldNames", JSONArray.class), "commonFieldNames"));
        result.setIndexEnable(field(value, "indexEnable", Boolean.class));
        if (value.get("fullTextIndex") != null) result.setFullTextIndex(readFullTextIndex(field(value, "fullTextIndex", JSONObject.class)));
        JSONArray layouts = field(value, "graphLayouts", JSONArray.class);
        if (layouts.size() != 2) throw new IllegalArgumentException("both fixed layouts are required");
        for (Object item : layouts) {
            if (!(item instanceof JSONObject)) throw new IllegalArgumentException("invalid layout object");
            JSONObject layout = (JSONObject) item;
            String name = field(layout, "name", String.class);
            if ("vertex".equals(name) && result.getVertexLayout() == null) result.setVertexLayout(readVertexLayout(layout));
            else if ("edge".equals(name) && result.getEdgeLayout() == null) result.setEdgeLayout(readEdgeLayout(layout));
            else throw new IllegalArgumentException("unsupported or duplicate layout " + name);
        }
        required(result.getVertexLayout(), "vertexLayout");
        required(result.getEdgeLayout(), "edgeLayout");
        return result;
    }

    public static GraphStoreCollectionSchema readCollectionSchema(JSONObject value) {
        required(value, "GraphStoreCollectionSchema");
        known(value, "name", "description", "graphLayout", "collectionKeyValues", "ttlSeconds", "fields");
        GraphStoreCollectionSchema result = new GraphStoreCollectionSchema();
        result.setName(text(field(value, "name", String.class), "name", false));
        result.setDescription(text(field(value, "description", String.class), "description", false));
        result.setGraphLayout(layoutType(field(value, "graphLayout", String.class)));
        List<GraphStoreCollectionKeyValue> collectionKeyValues = new ArrayList<GraphStoreCollectionKeyValue>();
        for (Object item : field(value, "collectionKeyValues", JSONArray.class)) {
            if (!(item instanceof JSONObject)) throw new IllegalArgumentException("invalid collectionKeyValues item");
            collectionKeyValues.add(readCollectionKeyValue((JSONObject) item));
        }
        result.setCollectionKeyValues(collectionKeyValues);
        if (value.get("ttlSeconds") != null) result.setTtlSeconds(ttl(integer(value, "ttlSeconds")));
        List<GraphStoreFieldSchema> fields = new ArrayList<GraphStoreFieldSchema>();
        for (Object item : field(value, "fields", JSONArray.class)) {
            if (!(item instanceof JSONObject)) throw new IllegalArgumentException("invalid fields item");
            fields.add(readFieldSchema((JSONObject) item));
        }
        result.setFields(fields);
        return result;
    }

}
