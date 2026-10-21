package com.aliyun.openservices.log.response;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.aliyun.openservices.log.common.Consts;
import com.aliyun.openservices.log.internal.ErrorCodes;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.http.comm.ResponseMessage;
import com.aliyun.openservices.log.util.LZ4Encoder;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Public row response preserving JSON cell types, including nested arrays and objects. */
public class GetGraphStoreDataResponse extends Response {
    private static final long serialVersionUID = 1L;
    private final Data data;
    private final String status;
    private final Stats stats;
    private final List<String> warnings;

    private GetGraphStoreDataResponse(Map<String, String> headers, Data data, String status, Stats stats, List<String> warnings) {
        super(headers);
        this.data = data;
        this.status = status;
        this.stats = stats;
        this.warnings = warnings;
    }

    public Data getData() { return data; }
    public String getStatus() { return status; }
    public Stats getStats() { return stats; }
    public List<String> getWarnings() { return warnings; }

    public static class Data implements Serializable {
        private static final long serialVersionUID = 1L;
        private final List<String> fields;
        private final List<List<Object>> values;

        private Data(JSONObject value) {
            fields = value.getObject("fields", new TypeReference<List<String>>() {});
            values = value.getObject("values", new TypeReference<List<List<Object>>>() {});
        }

        public List<String> getFields() { return fields; }
        public List<List<Object>> getValues() { return values; }
    }

    public static class Stats implements Serializable {
        private static final long serialVersionUID = 1L;
        private final BigInteger rowCount;
        private final long elapsedMs;
        private final BigInteger peakMemoryBytes;

        private Stats(JSONObject value) {
            rowCount = value.getBigInteger("rowCount");
            elapsedMs = value.getLongValue("elapsedMs");
            peakMemoryBytes = value.getBigInteger("peakMemoryBytes");
        }

        public BigInteger getRowCount() { return rowCount; }
        public long getElapsedMs() { return elapsedMs; }
        public BigInteger getPeakMemoryBytes() { return peakMemoryBytes; }
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
            JSONObject value = JSON.parseObject(new String(body, StandardCharsets.UTF_8), Feature.DisableSpecialKeyDetect);
            if (value == null) return new GetGraphStoreDataResponse(response.getHeaders(), null, null, null, null);
            JSONObject data = value.getJSONObject("data");
            JSONObject stats = value.getJSONObject("stats");
            return new GetGraphStoreDataResponse(response.getHeaders(),
                    data == null ? null : new Data(data), value.getString("status"), stats == null ? null : new Stats(stats),
                    value.getObject("warnings", new TypeReference<List<String>>() {}));
        } catch (LogException | RuntimeException | StackOverflowError cause) {
            LogException error = new LogException(ErrorCodes.BAD_RESPONSE,
                    "invalid GraphStore query response", cause, response.getRequestId());
            error.setHttpCode(response.getStatusCode());
            throw error;
        }
    }

}
