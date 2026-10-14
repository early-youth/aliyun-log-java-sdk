package com.aliyun.openservices.log.response;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.aliyun.openservices.log.common.Consts;
import com.aliyun.openservices.log.internal.ErrorCodes;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.http.comm.ResponseMessage;
import com.aliyun.openservices.log.util.LZ4Encoder;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Public row response; cells use the same FastJSON string conversion as LogStore queries. */
public class GetGraphStoreDataResponse extends Response {
    private static final long serialVersionUID = 1L;
    private final Data data;
    private final Meta meta;

    private GetGraphStoreDataResponse(Map<String, String> headers, Data data, Meta meta) {
        super(headers);
        this.data = data;
        this.meta = meta;
    }

    public Data getData() { return data; }
    public Meta getMeta() { return meta; }

    public static class Data implements Serializable {
        private static final long serialVersionUID = 1L;
        private final List<String> fields;
        private final List<List<String>> values;

        private Data(JSONObject value) {
            fields = field(value, "fields", JSONArray.class).toJavaList(String.class);
            JSONArray rows = field(value, "values", JSONArray.class);
            values = new ArrayList<List<String>>(rows.size());
            for (int i = 0; i < rows.size(); i++) values.add(rows.getJSONArray(i).toJavaList(String.class));
        }

        public List<String> getFields() { return fields; }
        public List<List<String>> getValues() { return values; }
    }

    public static class Meta implements Serializable {
        private static final long serialVersionUID = 1L;
        private final long elapsedMs;
        private final boolean truncated;
        private final List<String> warnings;

        private Meta(JSONObject value) {
            elapsedMs = required(value.getLong("elapsedMs"), "elapsedMs");
            truncated = required(value.getBoolean("truncated"), "truncated");
            warnings = field(value, "warnings", JSONArray.class).toJavaList(String.class);
        }

        public long getElapsedMs() { return elapsedMs; }
        public boolean isTruncated() { return truncated; }
        public List<String> getWarnings() { return warnings; }
    }

    public static GetGraphStoreDataResponse deserializeFrom(ResponseMessage response) throws LogException {
        try {
            byte[] body = response.GetRawBody();
            String compression = response.getHeaders().get(Consts.CONST_X_SLS_COMPRESSTYPE);
            if (compression != null && !compression.isEmpty()) {
                if (!"lz4".equals(compression)) throw new IllegalArgumentException("unsupported GraphStore response compression");
                int rawSize = Integer.parseInt(response.getHeaders().get(Consts.CONST_X_SLS_BODYRAWSIZE));
                if (rawSize < 0) throw new IllegalArgumentException("negative GraphStore response raw size");
                body = LZ4Encoder.decompressFromLhLz4Chunk(body, rawSize);
            }
            Object parsed = JSON.parse(new String(body, StandardCharsets.UTF_8), Feature.DisableSpecialKeyDetect);
            if (!(parsed instanceof JSONObject)) throw new IllegalArgumentException("invalid GraphStore response object");
            JSONObject value = (JSONObject) parsed;
            return new GetGraphStoreDataResponse(response.getHeaders(),
                    new Data(field(value, "data", JSONObject.class)), new Meta(field(value, "meta", JSONObject.class)));
        } catch (LogException | RuntimeException | StackOverflowError cause) {
            LogException error = new LogException(ErrorCodes.BAD_RESPONSE,
                    "invalid GraphStore query response", cause, response.getRequestId());
            error.setHttpCode(response.getStatusCode());
            throw error;
        }
    }

    private static <T> T required(T value, String name) {
        if (value == null) throw new IllegalArgumentException("missing GraphStore field " + name);
        return value;
    }

    private static <T> T field(JSONObject value, String name, Class<T> type) {
        Object field = value.get(name);
        if (!type.isInstance(field)) throw new IllegalArgumentException("invalid GraphStore field " + name);
        return type.cast(field);
    }
}
