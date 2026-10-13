package com.aliyun.openservices.log;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.log.common.Consts;
import com.aliyun.openservices.log.common.Consts.CompressType;
import com.aliyun.openservices.log.common.Logs;
import com.aliyun.openservices.log.common.auth.DefaultCredentials;
import com.aliyun.openservices.log.common.auth.StaticCredentialsProvider;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.http.client.ClientConfiguration;
import com.aliyun.openservices.log.http.client.ClientErrorCode;
import com.aliyun.openservices.log.http.client.ClientException;
import com.aliyun.openservices.log.http.client.HttpMethod;
import com.aliyun.openservices.log.http.comm.DefaultServiceClient;
import com.aliyun.openservices.log.http.comm.ResponseMessage;
import com.aliyun.openservices.log.http.signer.SignVersion;
import com.aliyun.openservices.log.http.utils.IOUtils;
import com.aliyun.openservices.log.internal.ErrorCodes;
import com.aliyun.openservices.log.request.*;
import com.aliyun.openservices.log.response.GetGraphStoreDataResponse;
import com.aliyun.openservices.log.util.GzipUtils;
import com.aliyun.openservices.log.util.LZ4Encoder;
import com.aliyun.openservices.log.util.ZSTDEncoder;
import com.google.protobuf.ByteString;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;

public class GraphStoreDataTest {
    private static final String ROWS = "{\"data\":{\"fields\":[\"a\",\"b\",\"c\"],"
            + "\"values\":[[null,\"\",\"雪\\u0000\"],[\"9223372036854775808\",\"[1,2]\",\"true\"]]},"
            + "\"meta\":{\"elapsedMs\":-9223372036854775808,\"truncated\":true,\"warnings\":[\"雪\"]}}";
    private static final GetGraphStoreDataRequest QUERY = new GetGraphStoreDataRequest("project", "store", "MATCH (n) RETURN n", 10, 20);

    private static Logs.LogGroup group(byte[] value) {
        return Logs.LogGroup.newBuilder().addLogs(Logs.Log.newBuilder().setTime(1)
                .addContents(Logs.Log.Content.newBuilder().setKey("binary").setValueBytes(ByteString.copyFrom(value))))
                .build();
    }

    @Test
    public void nativeWirePreservesBytesCompressionAndSignatures() throws Exception {
        byte[] binary = new byte[65536];
        for (int i = 0; i < binary.length; i++) { binary[i] = (byte) i; }
        for (SignVersion version : new SignVersion[]{SignVersion.V1, SignVersion.V4}) {
            for (String token : new String[]{null, "session-token"}) {
                ClientConfiguration config = new ClientConfiguration();
                config.setRegion("cn-hangzhou");
                config.setSignatureVersion(version);
                try (Wire wire = new Wire(config, token)) {
                    LogService client = wire.client;
                    for (Logs.LogGroup logs : new Logs.LogGroup[]{Logs.LogGroup.getDefaultInstance(), group(new byte[]{0, -1}), group(binary)}) {
                        PostGraphStoreDataRequest request = new PostGraphStoreDataRequest("project", "store", logs);
                        assertEquals(CompressType.LZ4, request.getCompressType());
                        for (CompressType compression : new CompressType[]{CompressType.NONE, CompressType.LZ4, CompressType.ZSTD, CompressType.GZIP}) {
                            request.setCompressType(compression);
                            assertEquals("request-id", client.postGraphStoreData(request).GetRequestId());
                            wire.check("write", Consts.CONST_PROTO_BUF, version, token);
                            assertEquals(String.valueOf(logs.toByteArray().length), wire.headers.get(Consts.CONST_X_SLS_BODYRAWSIZE));
                            assertEquals(compression == CompressType.NONE ? null : compression.toString(), wire.headers.get(Consts.CONST_X_SLS_COMPRESSTYPE));
                            byte[] decoded = wire.body;
                            if (compression == CompressType.LZ4) decoded = LZ4Encoder.decompressFromLhLz4Chunk(decoded, logs.toByteArray().length);
                            if (compression == CompressType.ZSTD) decoded = ZSTDEncoder.decompress(decoded, logs.toByteArray().length);
                            if (compression == CompressType.GZIP) decoded = GzipUtils.uncompress(decoded);
                            assertArrayEquals(logs.toByteArray(), decoded);
                        }
                    }
                    String query = "MATCH (n) WHERE n.x=\"雪\\\n\u0000\" RETURN n";
                    GetGraphStoreDataRequest request = new GetGraphStoreDataRequest("project", "store", query, 0, 0xffffffffL);
                    request.setSession("opaque\u0000雪");
                    GetGraphStoreDataResponse result = client.getGraphStoreData(request);
                    wire.check("query", Consts.CONST_SLS_JSON, version, token);
                    assertEquals(String.valueOf(wire.body.length), wire.headers.get(Consts.CONST_X_SLS_BODYRAWSIZE));
                    assertEquals("lz4", wire.headers.get(Consts.CONST_ACCEPT_ENCODING));
                    JSONObject body = JSONObject.parseObject(new String(wire.body, StandardCharsets.UTF_8));
                    assertEquals(4, body.size());
                    assertEquals(query, body.get("query"));
                    assertEquals(0L, body.getLongValue("start"));
                    assertEquals(0xffffffffL, body.getLongValue("end"));
                    assertEquals("opaque\u0000雪", body.get("session"));
                    assertRows(result);
                }
            }
        }
    }

    @Test
    public void closedInt64TimeRangeAndDefaultsAreSentExactly() throws Exception {
        try (Wire wire = new Wire(new ClientConfiguration(), null)) {
            for (GetGraphStoreDataRequest request : new GetGraphStoreDataRequest[]{
                    new GetGraphStoreDataRequest("project", "store", "q"),
                    new GetGraphStoreDataRequest("project", "store", "q", 0, 0),
                    new GetGraphStoreDataRequest("project", "store", "q", 4294967296L, Long.MAX_VALUE)}) {
                wire.client.getGraphStoreData(request);
                JSONObject body = JSONObject.parseObject(new String(wire.body, StandardCharsets.UTF_8));
                assertEquals(3, body.size());
                assertEquals(request.getStart(), body.getLongValue("start"));
                assertEquals(request.getEnd(), body.getLongValue("end"));
                assertFalse(body.containsKey("from"));
                assertFalse(body.containsKey("to"));
                assertFalse(body.containsKey("session"));
            }
        }
    }

    @Test
    public void validationAndNativeRawSizeLimitsPrecedeSending() throws Exception {
        try (Wire wire = new Wire(new ClientConfiguration(), null)) {
            Client client = wire.client;
            expect(ErrorCodes.INVALID_PARAMETER, () -> client.postGraphStoreData(null));
            expect(ErrorCodes.INVALID_PARAMETER, () -> client.getGraphStoreData(null));
            expect(ErrorCodes.INVALID_PARAMETER, () -> client.postGraphStoreData(new PostGraphStoreDataRequest("project", "store", null)));
            for (String name : new String[]{null, "", "../store", "store/next", "store?x", "雪"}) {
                expect(ErrorCodes.INVALID_PARAMETER, () -> client.postGraphStoreData(new PostGraphStoreDataRequest("project", name, group(new byte[0]))));
                expect(ErrorCodes.INVALID_PARAMETER, () -> client.getGraphStoreData(new GetGraphStoreDataRequest("project", name, "q", 10, 20)));
            }
            for (String empty : new String[]{null, ""}) {
                expect(ErrorCodes.INVALID_PARAMETER, () -> client.postGraphStoreData(new PostGraphStoreDataRequest(empty, "store", group(new byte[0]))));
                expect(ErrorCodes.INVALID_PARAMETER, () -> client.getGraphStoreData(new GetGraphStoreDataRequest(empty, "store", "q", 10, 20)));
                expect(ErrorCodes.INVALID_PARAMETER, () -> client.getGraphStoreData(new GetGraphStoreDataRequest("project", "store", empty, 10, 20)));
            }
            Logs.LogGroup partial = Logs.LogGroup.newBuilder().addLogs(Logs.Log.newBuilder().buildPartial()).buildPartial();
            for (long[] range : new long[][]{{2, 1}, {-1, 1}, {0, -1}}) {
                expect(ErrorCodes.INVALID_PARAMETER, () -> client.getGraphStoreData(
                        new GetGraphStoreDataRequest("project", "store", "q", range[0], range[1])));
            }
            expect(ErrorCodes.INVALID_PARAMETER, () -> client.postGraphStoreData(new PostGraphStoreDataRequest("project", "store", partial)));
            PostGraphStoreDataRequest request = new PostGraphStoreDataRequest("project", "store", group(new byte[256]));
            request.setCompressType(null);
            expect(ErrorCodes.INVALID_PARAMETER, () -> client.postGraphStoreData(request));
            assertEquals(0, wire.calls);
            request.setCompressType(CompressType.LZ4);
            int size = request.getLogGroup().toByteArray().length;
            int putLimit = Consts.CONST_MAX_PUT_SIZE, postLimit = Consts.CONST_MAX_POST_BODY_SIZE;
            try {
                Consts.CONST_MAX_PUT_SIZE = size - 1;
                Consts.CONST_MAX_POST_BODY_SIZE = size - 1;
                expect("InvalidLogSize", () -> client.postGraphStoreData(request));
                Consts.CONST_MAX_PUT_SIZE = size;
                expect("PostBodyTooLarge", () -> client.postGraphStoreData(request));
                assertEquals(0, wire.calls);
                Consts.CONST_MAX_POST_BODY_SIZE = size;
                client.postGraphStoreData(request);
                assertEquals(1, wire.calls);
            } finally {
                Consts.CONST_MAX_PUT_SIZE = putLimit;
                Consts.CONST_MAX_POST_BODY_SIZE = postLimit;
            }
        }
    }

    @Test
    public void malformedRowsFailWithoutPartialResultsOrRetries() throws Exception {
        try (Wire wire = new Wire(new ClientConfiguration(), null)) {
            for (String bad : new String[]{"", "null", "[]", "{}", ROWS + "{}", ROWS + "\u0000tail",
                    ROWS.replace("\\u0000", "\\u00xz"),
                    "{\"header\":[],\"data\":[],\"meta\":{}}"}) {
                wire.bad(bad.getBytes(StandardCharsets.UTF_8));
            }
            for (String field : new String[]{"data", "meta", "fields", "values", "elapsedMs", "truncated", "warnings"}) {
                for (boolean missing : new boolean[]{true, false}) {
                    JSONObject value = JSONObject.parseObject(ROWS);
                    JSONObject target = value.containsKey(field) ? value
                            : value.getJSONObject("data").containsKey(field) ? value.getJSONObject("data") : value.getJSONObject("meta");
                    if (missing) target.remove(field); else target.put(field, null);
                    wire.bad(JSONObject.toJSONBytes(value, com.alibaba.fastjson.serializer.SerializerFeature.WriteMapNullValue));
                }
            }
            wire.bad(new byte[]{(byte) 0xff});
            char[] brackets = new char[20000];
            Arrays.fill(brackets, '[');
            String nested = new String(brackets) + "0";
            Arrays.fill(brackets, ']');
            wire.bad((ROWS.substring(0, ROWS.length() - 1) + ",\"extra\":" + nested + new String(brackets) + "}")
                    .getBytes(StandardCharsets.UTF_8));
            wire.responseBody = "{\"data\":{\"fields\":[],\"values\":[]},\"meta\":{\"elapsedMs\":0,\"truncated\":false,\"warnings\":[]}}".getBytes(StandardCharsets.UTF_8);
            assertTrue(wire.client.getGraphStoreData(QUERY).getData().getValues().isEmpty());
            char[] manyEscapes = new char[100000];
            Arrays.fill(manyEscapes, '\n');
            String cell = new String(manyEscapes);
            wire.responseBody = ROWS.replace("\"雪\\u0000\"", JSONObject.toJSONString(cell)).getBytes(StandardCharsets.UTF_8);
            assertEquals(cell, wire.client.getGraphStoreData(QUERY).getData().getValues().get(0).get(2));
        }
    }

    @Test
    public void queryUsesLogStoreConversionsAndIgnoresAdditionalMembers() throws Exception {
        try (Wire wire = new Wire(new ClientConfiguration(), null)) {
            for (String value : new String[]{
                    ROWS.replace("[null,\"\",\"雪\\u0000\"]", "[]"),
                    ROWS.replace("null,\"\"", "false,\"\""),
                    ROWS.replace("null,\"\"", "3,\"\""),
                    ROWS.replace("null,\"\"", "{},\"\""),
                    ROWS.replace("[\"a\",", "[null,"),
                    ROWS.replace("[\"雪\"]", "[1]"),
                    ROWS.replace("-9223372036854775808", "\"2\""),
                    ROWS.replace("-9223372036854775808", "2.0"),
                    ROWS.replace("\"elapsedMs\":", "\"diagnostic\":{},\"elapsedMs\":"),
                    ROWS.substring(0, ROWS.length() - 1) + ",\"extra\":1}"}) {
                wire.responseBody = value.getBytes(StandardCharsets.UTF_8);
                wire.client.getGraphStoreData(QUERY);
            }
            wire.responseBody = ROWS.replace("null,\"\"", "42,\"\"").getBytes(StandardCharsets.UTF_8);
            assertEquals("42", wire.client.getGraphStoreData(QUERY).getData().getValues().get(0).get(0));
        }
    }

    @Test
    public void lz4ResponseUsesSharedDecoderAndPreservesErrors() throws Exception {
        try (Wire wire = new Wire(new ClientConfiguration(), null)) {
            byte[] raw = ROWS.getBytes(StandardCharsets.UTF_8);
            byte[] compressed = LZ4Encoder.compressToLhLz4Chunk(raw);
            wire.responseBody = compressed;
            wire.responseHeaders.put(Consts.CONST_X_SLS_COMPRESSTYPE, "lz4");
            wire.responseHeaders.put(Consts.CONST_X_SLS_BODYRAWSIZE, String.valueOf(raw.length));
            assertRows(wire.client.getGraphStoreData(QUERY));
            for (String size : new String[]{null, "", "-1", "0", "abc", "4294967296", "9223372036854775808", String.valueOf(raw.length - 1), String.valueOf(raw.length + 1)}) {
                wire.responseHeaders.put(Consts.CONST_X_SLS_BODYRAWSIZE, size);
                wire.bad(compressed);
            }
            wire.responseHeaders.put(Consts.CONST_X_SLS_BODYRAWSIZE, String.valueOf(raw.length));
            wire.bad(Arrays.copyOf(compressed, compressed.length - 1));
            wire.responseBody = Arrays.copyOf(compressed, compressed.length + 1);
            assertRows(wire.client.getGraphStoreData(QUERY)); // Shared LogStore decoder accepts trailing compressed bytes.
            for (String compression : new String[]{"none", "zstd", "deflate", "unknown"}) {
                wire.responseHeaders.put(Consts.CONST_X_SLS_COMPRESSTYPE, compression);
                wire.bad(raw);
            }
        }
    }

    @Test
    public void errorsAndRetriesFollowNativeLogStorePaths() throws Exception {
        for (boolean disabled : new boolean[]{false, true}) {
            ClientConfiguration config = new ClientConfiguration();
            config.setMaxErrorRetry(1);
            config.setRetryDisabled(disabled);
            try (Wire wire = new Wire(config, null)) {
                PostGraphStoreDataRequest write = new PostGraphStoreDataRequest("project", "store", group(new byte[]{0, -1}));
                for (Call call : new Call[]{() -> wire.client.postGraphStoreData(write), () -> wire.client.getGraphStoreData(QUERY)}) {
                    for (int status : new int[]{400, 401, 403, 404, 409, 413, 500, 502, 503, 504}) {
                        wire.status = status;
                        int before = wire.calls;
                        LogException error = expect("ServiceError", call);
                        assertEquals(status, error.GetHttpCode());
                        assertEquals("request-id", error.GetRequestId());
                        assertEquals(before + 1, wire.calls);
                    }
                }
                wire.status = 200;
                wire.networkFailures = 1;
                int before = wire.calls;
                if (disabled) expect("RequestError", () -> wire.client.postGraphStoreData(write));
                else wire.client.postGraphStoreData(write);
                assertEquals(before + (disabled ? 1 : 2), wire.calls);
                wire.networkFailures = 100;
                before = wire.calls;
                expect("RequestError", () -> wire.client.postGraphStoreData(write));
                assertEquals(before + (disabled ? 1 : 4), wire.calls);
                before = wire.calls;
                expect("RequestError", () -> wire.client.getGraphStoreData(QUERY));
                assertEquals(before + (disabled ? 1 : 2), wire.calls);
            }
        }
    }

    private static void assertRows(GetGraphStoreDataResponse result) {
        assertEquals("request-id", result.GetRequestId());
        assertEquals(Arrays.asList("a", "b", "c"), result.getData().getFields());
        assertEquals(Arrays.asList(null, "", "雪\u0000"), result.getData().getValues().get(0));
        assertEquals(Arrays.asList("9223372036854775808", "[1,2]", "true"), result.getData().getValues().get(1));
        assertEquals(Long.MIN_VALUE, result.getMeta().getElapsedMs());
        assertTrue(result.getMeta().isTruncated());
        assertEquals(Arrays.asList("雪"), result.getMeta().getWarnings());
    }

    private interface Call { void run() throws LogException; }

    private static LogException expect(String code, Call call) throws Exception {
        try { call.run(); fail("expected " + code); }
        catch (LogException error) { assertEquals(code, error.GetErrorCode()); return error; }
        throw new AssertionError("unreachable");
    }

    private static class Wire extends DefaultServiceClient implements AutoCloseable {
        final Client client;
        int calls, networkFailures, status = 200;
        String uri;
        Map<String, String> headers;
        final Map<String, String> responseHeaders = new HashMap<String, String>();
        byte[] body, responseBody = ROWS.getBytes(StandardCharsets.UTF_8);

        Wire(ClientConfiguration config, String token) {
            super(config);
            client = new Client("http://example.com", new StaticCredentialsProvider(
                    new DefaultCredentials("key", "secret", token)), this, "127.0.0.1");
        }

        @Override
        public ResponseMessage sendRequestCore(Request request, String charset) throws IOException {
            calls++;
            assertEquals(HttpMethod.POST, request.getMethod());
            uri = request.getUri();
            headers = new HashMap<String, String>(request.getHeaders());
            body = IOUtils.readStreamAsByteArray(request.getContent());
            if (networkFailures-- > 0) throw new ClientException("timeout", ClientErrorCode.SOCKET_TIMEOUT, "");
            ResponseMessage response = new ResponseMessage();
            response.setStatusCode(status);
            response.getHeaders().putAll(responseHeaders);
            response.addHeader(Consts.CONST_X_SLS_REQUESTID, "request-id");
            byte[] bytes = status == 200 ? responseBody : "{\"errorCode\":\"ServiceError\",\"errorMessage\":\"failed\"}".getBytes(StandardCharsets.UTF_8);
            response.setContent(new ByteArrayInputStream(bytes));
            if (status != 200) response.setErrorResponseAsString(new String(bytes, StandardCharsets.UTF_8));
            return response;
        }

        void check(String operation, String contentType, SignVersion version, String token) throws Exception {
            assertEquals("http://project.example.com/graphstore/store/" + operation, uri);
            assertEquals(contentType, headers.get(Consts.CONST_CONTENT_TYPE));
            assertEquals(token, headers.get(Consts.CONST_X_ACS_SECURITY_TOKEN));
            assertTrue(headers.get(Consts.CONST_AUTHORIZATION).startsWith(version == SignVersion.V1
                    ? "LOG key:" : "SLS4-HMAC-SHA256 Credential=key/"));
            String digest = Hex.encodeHexString(MessageDigest.getInstance(version == SignVersion.V1 ? "MD5" : "SHA-256").digest(body));
            assertEquals(version == SignVersion.V1 ? (body.length == 0 ? null : digest.toUpperCase(Locale.ROOT)) : digest,
                    headers.get(version == SignVersion.V1 ? Consts.CONST_CONTENT_MD5 : Consts.X_LOG_CONTENT_SHA256));
        }

        void bad(byte[] bytes) throws Exception {
            responseBody = bytes;
            int before = calls;
            LogException error;
            try { error = expect(ErrorCodes.BAD_RESPONSE, () -> client.getGraphStoreData(QUERY)); }
            catch (AssertionError failure) { throw new AssertionError(new String(bytes, StandardCharsets.UTF_8), failure); }
            assertEquals(200, error.GetHttpCode());
            assertEquals("request-id", error.GetRequestId());
            assertEquals(before + 1, calls);
        }

        @Override
        public void close() { client.shutdown(); }
    }
}
