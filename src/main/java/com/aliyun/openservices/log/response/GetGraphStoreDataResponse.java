package com.aliyun.openservices.log.response;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.alibaba.fastjson.parser.Feature;
import com.aliyun.openservices.log.common.Consts;
import com.aliyun.openservices.log.internal.ErrorCodes;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.http.comm.ResponseMessage;
import net.jpountz.lz4.LZ4Factory;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Public row response: each cell is a string or null, without numeric coercion. */
public class GetGraphStoreDataResponse extends Response {
    private static final long serialVersionUID = 1L;
    // JSONValidator checks structure but accepts non-JSON numbers, escapes and control characters.
    // Possessive string repetition keeps long/escape-heavy cells off the regex recursion stack.
    private static final Pattern JSON_TOKEN = Pattern.compile(
            "\"(?:[^\"\\\\\\x00-\\x1f]++|\\\\(?:[\"\\\\/bfnrt]|u[0-9a-fA-F]{4}))*+\""
            + "|-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?(?=[ \\t\\r\\n,}\\]]|$)"
            + "|true|false|null|[\\[\\]{},: \\t\\r\\n]");
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
            if (value.size() != 2) throw new IllegalArgumentException("invalid GraphStore data fields");
            fields = strings(field(value, "fields", JSONArray.class), false);
            JSONArray rows = field(value, "values", JSONArray.class);
            values = new ArrayList<List<String>>(rows.size());
            for (Object row : rows) {
                if (!(row instanceof JSONArray) || ((JSONArray) row).size() != fields.size()) {
                    throw new IllegalArgumentException("invalid GraphStore row width");
                }
                values.add(strings((JSONArray) row, true));
            }
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
            if (value.size() != 3) throw new IllegalArgumentException("invalid GraphStore meta fields");
            elapsedMs = integer(value, "elapsedMs");
            truncated = field(value, "truncated", Boolean.class);
            warnings = strings(field(value, "warnings", JSONArray.class), false);
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
                if (!"lz4".equals(compression)) {
                    throw new IllegalArgumentException("unsupported GraphStore response compression");
                }
                long rawSize = Long.parseLong(response.getHeaders().get(Consts.CONST_X_SLS_BODYRAWSIZE));
                if (rawSize < 0 || rawSize > 256 * 1024 * 1024) {
                    throw new IllegalArgumentException("invalid GraphStore response raw size");
                }
                byte[] decoded = new byte[(int) rawSize];
                int consumed = LZ4Factory.fastestInstance().fastDecompressor()
                        .decompress(body, 0, decoded, 0, decoded.length);
                if (consumed != body.length) {
                    throw new IllegalArgumentException("invalid GraphStore LZ4 response length");
                }
                body = decoded;
            }
            String json = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(body)).toString();
            // FastJSON's default parser accepts extensions; the public wire contract is strict JSON.
            Matcher tokens = JSON_TOKEN.matcher(json);
            for (int offset = 0; offset < json.length(); offset = tokens.end()) {
                if (!tokens.region(offset, json.length()).lookingAt()) {
                    throw new IllegalArgumentException("invalid GraphStore JSON token");
                }
            }
            if (!JSONValidator.from(json).setSupportMultiValue(false).validate()) {
                throw new IllegalArgumentException("invalid GraphStore JSON response");
            }
            Object parsed = JSON.parse(json, Feature.DisableSpecialKeyDetect.mask);
            if (!(parsed instanceof JSONObject)) {
                throw new IllegalArgumentException("invalid GraphStore response object");
            }
            JSONObject value = (JSONObject) parsed;
            if (value.size() != 2) throw new IllegalArgumentException("invalid GraphStore response fields");
            Data data = new Data(field(value, "data", JSONObject.class));
            Meta meta = new Meta(field(value, "meta", JSONObject.class));
            return new GetGraphStoreDataResponse(response.getHeaders(), data, meta);
        } catch (RuntimeException | CharacterCodingException | StackOverflowError cause) {
            LogException error = new LogException(ErrorCodes.BAD_RESPONSE,
                    "invalid GraphStore query response", cause, response.getRequestId());
            error.setHttpCode(response.getStatusCode());
            throw error;
        }
    }

    private static <T> T field(JSONObject value, String name, Class<T> type) {
        Object field = value.get(name);
        if (!type.isInstance(field)) {
            throw new IllegalArgumentException("invalid GraphStore field: " + name);
        }
        return type.cast(field);
    }

    private static long integer(JSONObject value, String name) {
        Object number = value.get(name);
        if (!(number instanceof Integer || number instanceof Long
                || number instanceof BigInteger && ((BigInteger) number).bitLength() <= 63)) {
            throw new IllegalArgumentException("invalid GraphStore int64: " + name);
        }
        return ((Number) number).longValue();
    }

    private static List<String> strings(JSONArray array, boolean nullable) {
        List<String> result = new ArrayList<String>(array.size());
        for (Object item : array) {
            if (!(item instanceof String) && !(nullable && item == null)) {
                throw new IllegalArgumentException("invalid GraphStore string cell");
            }
            result.add((String) item);
        }
        return result;
    }
}
