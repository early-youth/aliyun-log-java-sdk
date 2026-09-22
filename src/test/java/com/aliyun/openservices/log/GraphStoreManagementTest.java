package com.aliyun.openservices.log;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.log.common.Consts;
import com.aliyun.openservices.log.common.auth.DefaultCredentials;
import com.aliyun.openservices.log.common.auth.StaticCredentialsProvider;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.http.client.ClientConfiguration;
import com.aliyun.openservices.log.http.client.HttpMethod;
import com.aliyun.openservices.log.http.comm.DefaultServiceClient;
import com.aliyun.openservices.log.http.comm.ResponseMessage;
import com.aliyun.openservices.log.http.signer.SignVersion;
import com.aliyun.openservices.log.internal.ErrorCodes;
import com.aliyun.openservices.log.request.*;
import com.aliyun.openservices.log.response.*;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class GraphStoreManagementTest {
    private static final String STORE = "{\"projectId\":2147483647,\"storeName\":\"store\","
            + "\"state\":\"READY\",\"currentShardCount\":2}";
    private static final String SCHEMA = "{\"collectionSchemaId\":9223372036854775807,\"unknown\":\"雪\"}";
    private static final String STORE_WITH_SHARDS = STORE.substring(0, STORE.length() - 1)
            + ",\"shards\":[{\"shardId\":2},{\"shardId\":7}]}";

    @Test
    public void mutationsUseNativeRequestAndResponse() throws Exception {
        CapturingClient client = new CapturingClient();
        JSONObject schema = JSONObject.parseObject("{\"unknown\":\"雪\",\"id\":9223372036854775807}");
        Response created = client.createGraphStore(new CreateGraphStoreRequest("project", "store", 2, schema));
        assertEquals("request-id", created.GetRequestId());
        client.check(HttpMethod.POST, "/graphstore");
        JSONObject body = client.jsonBody();
        assertEquals(3, body.size());
        assertEquals("store", body.get("storeName"));
        assertEquals(2, body.get("shardCount"));
        assertTrue(body.get("baseSchema") instanceof JSONObject);
        assertEquals(Long.MAX_VALUE, body.getJSONObject("baseSchema").getLongValue("id"));
        assertEquals("雪", body.getJSONObject("baseSchema").get("unknown"));

        SplitGraphStoreShardRequest split = new SplitGraphStoreShardRequest("project", "store", 4);
        client.splitGraphStoreShard(split);
        client.check(HttpMethod.POST, "/graphstore/store/shards/4/split");
        assertEquals(0, client.body.length);
        split.setSplitHash(BigInteger.ZERO);
        client.splitGraphStoreShard(split);
        assertEquals("{\"splitHash\":0}", new String(client.body, StandardCharsets.UTF_8));
        split.setSplitHash(new BigInteger("18446744073709551615"));
        client.splitGraphStoreShard(split);
        assertEquals("{\"splitHash\":18446744073709551615}", new String(client.body, StandardCharsets.UTF_8));

        client.createGraphStoreCollectionSchema(new CreateGraphStoreCollectionSchemaRequest("project", "store", schema));
        client.check(HttpMethod.POST, "/graphstore/store/collectionschemas");
        assertEquals(1, client.jsonBody().size());
        assertTrue(client.jsonBody().get("value") instanceof JSONObject);
        client.updateGraphStoreCollectionSchema(new UpdateGraphStoreCollectionSchemaRequest("project", "store", Long.MAX_VALUE, schema));
        client.check(HttpMethod.PUT, "/graphstore/store/collectionschemas/9223372036854775807");
        assertEquals(Long.MAX_VALUE, client.jsonBody().getJSONObject("value").getLongValue("id"));
        client.deleteGraphStoreCollectionSchema(new DeleteGraphStoreCollectionSchemaRequest("project", "store", Long.MAX_VALUE));
        client.check(HttpMethod.DELETE, "/graphstore/store/collectionschemas/9223372036854775807");
        assertEquals(0, client.body.length);
        client.deleteGraphStore(new DeleteGraphStoreRequest("project", "store"));
        client.check(HttpMethod.DELETE, "/graphstore/store");
        assertEquals(8, client.calls);
    }

    @Test
    public void readsKeepIntegerPrecisionAndOpaqueSchemas() throws Exception {
        CapturingClient client = new CapturingClient();
        client.responseBody = STORE_WITH_SHARDS;
        GetGraphStoreResponse store = client.getGraphStore(new GetGraphStoreRequest("project", "store"));
        client.check(HttpMethod.GET, "/graphstore/store");
        assertEquals(Integer.MAX_VALUE, store.getGraphStore().getProjectId());
        assertEquals(2, store.getGraphStore().getCurrentShardCount());
        assertEquals("READY", store.getGraphStore().getState());
        assertEquals(2, store.getShards().size());
        assertEquals(2, store.getShards().get(0).getShardId());
        assertEquals(7, store.getShards().get(1).getShardId());
        assertEquals("request-id", store.GetRequestId());

        client.responseBody = "{\"stores\":[" + STORE + "],\"count\":1,\"total\":9223372036854775807}";
        ListGraphStoresRequest list = new ListGraphStoresRequest("project");
        list.setOffset(0);
        list.setSize(1);
        ListGraphStoresResponse stores = client.listGraphStores(list);
        client.check(HttpMethod.GET, "/graphstore");
        assertEquals(1, stores.getStores().size());
        assertEquals(1L, stores.getCount());
        assertEquals(Long.MAX_VALUE, stores.getTotal());
        assertEquals("0", client.parameters.get("offset"));
        assertEquals("1", client.parameters.get("size"));
        list.setOffset(null);
        list.setSize(null);
        client.listGraphStores(list);
        assertTrue(client.parameters.isEmpty());

        client.responseBody = "{\"projectId\":1,\"storeName\":\"store\",\"storeId\":9223372036854775807,"
                + "\"baseSchema\":{\"opaque\":9223372036854775807}}";
        GetGraphStoreBaseSchemaResponse base = client.getGraphStoreBaseSchema(new GetGraphStoreBaseSchemaRequest("project", "store"));
        client.check(HttpMethod.GET, "/graphstore/store/baseschema");
        assertEquals(Long.MAX_VALUE, base.getStoreId());
        assertEquals(Long.MAX_VALUE, base.getBaseSchema().getLongValue("opaque"));

        client.responseBody = "{\"projectId\":1,\"storeName\":\"store\",\"collectionSchema\":" + SCHEMA + "}";
        GetGraphStoreCollectionSchemaResponse collection = client.getGraphStoreCollectionSchema(
                new GetGraphStoreCollectionSchemaRequest("project", "store", Long.MAX_VALUE));
        client.check(HttpMethod.GET, "/graphstore/store/collectionschemas/9223372036854775807");
        assertEquals(Long.MAX_VALUE, collection.getCollectionSchema().getLongValue("collectionSchemaId"));
        assertEquals("雪", collection.getCollectionSchema().get("unknown"));

        client.responseBody = "{\"projectId\":1,\"storeName\":\"store\",\"collectionSchemas\":["
                + SCHEMA + "],\"nextPageToken\":\"a+b/=&\"}";
        ListGraphStoreCollectionSchemasRequest page = new ListGraphStoreCollectionSchemasRequest("project", "store");
        page.setPageSize(1);
        page.setPageToken("a+b/=&");
        ListGraphStoreCollectionSchemasResponse result = client.listGraphStoreCollectionSchemas(page);
        client.check(HttpMethod.GET, "/graphstore/store/collectionschemas");
        assertEquals("a+b/=&", result.getNextPageToken());
        assertEquals("a+b/=&", client.parameters.get("pageToken"));
        assertEquals("1", client.parameters.get("pageSize"));
        assertEquals(1, result.getCollectionSchemas().size());
        assertEquals(Long.MAX_VALUE, result.getCollectionSchemas().get(0).getLongValue("collectionSchemaId"));
        page.setPageSize(null);
        page.setPageToken(null);
        client.responseBody = "{\"projectId\":1,\"storeName\":\"store\",\"collectionSchemas\":[]}";
        result = client.listGraphStoreCollectionSchemas(page);
        assertNull(result.getNextPageToken());
        assertTrue(client.parameters.isEmpty());
    }

    @Test
    public void schemaSerializationKeepsNullsAndSharedValues() throws Exception {
        CapturingClient client = new CapturingClient();
        JSONObject schema = JSONObject.parseObject("{\"commonFieldNames\":null,\"unknown\":null,\"fields\":[{\"name\":\"one\"},{\"name\":\"two\"}]}");
        JSONObject index = JSONObject.parseObject("{\"enable\":false,\"unknown\":null}");
        schema.getJSONArray("fields").getJSONObject(0).put("indexConfig", index);
        schema.getJSONArray("fields").getJSONObject(1).put("indexConfig", index);
        client.createGraphStore(new CreateGraphStoreRequest("project", "store", 1, schema));
        assertEquals(schema, client.jsonBody().getJSONObject("baseSchema"));
        assertTrue(new String(client.body, StandardCharsets.UTF_8).contains("\"commonFieldNames\":null"));
        client.createGraphStoreCollectionSchema(new CreateGraphStoreCollectionSchemaRequest("project", "store", schema));
        assertEquals(schema, client.jsonBody().getJSONObject("value"));
        client.updateGraphStoreCollectionSchema(new UpdateGraphStoreCollectionSchemaRequest("project", "store", 1, schema));
        String wire = new String(client.body, StandardCharsets.UTF_8);
        assertFalse(wire.contains("$ref"));
        assertEquals(schema, client.jsonBody().getJSONObject("value"));
        JSONObject cycle = new JSONObject();
        cycle.put("cycle", cycle);
        int before = client.calls;
        expectInvalid(() -> client.createGraphStore(new CreateGraphStoreRequest("project", "store", 1, cycle)));
        assertEquals(before, client.calls);
    }

    @Test
    public void nativeTransportSignsV1AndV4WithAkAndSts() throws Exception {
        for (final SignVersion version : new SignVersion[]{SignVersion.V1, SignVersion.V4}) {
            for (final String token : new String[]{null, "session-token"}) {
                ClientConfiguration configuration = new ClientConfiguration();
                configuration.setRegion("cn-hangzhou");
                configuration.setSignatureVersion(version);
                DefaultServiceClient transport = new DefaultServiceClient(configuration) {
                    @Override
                    public ResponseMessage sendRequestCore(Request request, String charset) {
                        assertEquals(HttpMethod.GET, request.getMethod());
                        assertEquals("http://project.example.com/graphstore/store", request.getUri());
                        assertEquals(token, request.getHeaders().get(Consts.CONST_X_ACS_SECURITY_TOKEN));
                        String authorization = request.getHeaders().get(Consts.CONST_AUTHORIZATION);
                        assertTrue(authorization.startsWith(version == SignVersion.V1
                                ? "LOG key:" : "SLS4-HMAC-SHA256 Credential=key/"));
                        ResponseMessage response = new ResponseMessage();
                        response.setStatusCode(200);
                        response.addHeader(Consts.CONST_X_SLS_REQUESTID, "signed-request");
                        response.setContent(new ByteArrayInputStream(STORE_WITH_SHARDS.getBytes(StandardCharsets.UTF_8)));
                        return response;
                    }
                };
                Client client = new Client("http://example.com", new StaticCredentialsProvider(
                        new DefaultCredentials("key", "secret", token)), transport, "127.0.0.1");
                try {
                    GetGraphStoreResponse response = client.getGraphStore(new GetGraphStoreRequest("project", "store"));
                    assertEquals("signed-request", response.GetRequestId());
                    assertEquals(2, response.getGraphStore().getCurrentShardCount());
                } finally {
                    client.shutdown();
                }
            }
        }
    }

    @Test
    public void invalidRequestsDoNotSend() throws Exception {
        CapturingClient client = new CapturingClient();
        expectInvalid(() -> client.createGraphStore(null));
        expectInvalid(() -> client.createGraphStore(new CreateGraphStoreRequest("project", "store", 1, null)));
        expectInvalid(() -> client.createGraphStoreCollectionSchema(
                new CreateGraphStoreCollectionSchemaRequest("project", "store", null)));
        expectInvalid(() -> client.updateGraphStoreCollectionSchema(
                new UpdateGraphStoreCollectionSchemaRequest("project", "store", 1, null)));
        expectInvalid(() -> client.getGraphStore(new GetGraphStoreRequest("", "store")));
        expectInvalid(() -> client.deleteGraphStore(new DeleteGraphStoreRequest("project", "../logstores/store")));
        for (BigInteger hash : new BigInteger[]{BigInteger.valueOf(-1), BigInteger.ONE.shiftLeft(64)}) {
            SplitGraphStoreShardRequest request = new SplitGraphStoreShardRequest("project", "store", 0);
            request.setSplitHash(hash);
            expectInvalid(() -> client.splitGraphStoreShard(request));
        }
        assertEquals(0, client.calls);
    }

    @Test
    public void malformedResponsesFailWithoutCoercion() throws Exception {
        CapturingClient client = new CapturingClient();
        for (String shards : new String[]{"null", "{}", "[null]", "[{}]", "[{\"shardId\":2147483648}]",
                "[{\"shardId\":1},{\"shardId\":1}]", "[{\"shardId\":2},{\"shardId\":1}]", "[]"}) {
            client.responseBody = STORE.substring(0, STORE.length() - 1) + ",\"shards\":" + shards + "}";
            expectBadResponse(() -> client.getGraphStore(new GetGraphStoreRequest("project", "store")));
        }
        for (String body : new String[]{"{}", "null", "[]", STORE + "{}", STORE + "\u0000tail",
                STORE.replace("2147483647", "2147483648"), STORE.replace("2147483647", "1.0"),
                STORE.replace("\"READY\"", "null")}) {
            client.responseBody = body;
            expectBadResponse(() -> client.getGraphStore(new GetGraphStoreRequest("project", "store")));
        }
        for (String id : new String[]{"null", "1.5", "\"42\"", "9223372036854775808"}) {
            client.responseBody = "{\"projectId\":1,\"storeName\":\"store\",\"storeId\":" + id + ",\"baseSchema\":{}}";
            expectBadResponse(() -> client.getGraphStoreBaseSchema(new GetGraphStoreBaseSchemaRequest("project", "store")));
            client.responseBody = "{\"projectId\":1,\"storeName\":\"store\",\"collectionSchema\":{\"collectionSchemaId\":" + id + "}}";
            expectBadResponse(() -> client.getGraphStoreCollectionSchema(new GetGraphStoreCollectionSchemaRequest("project", "store", 1)));
            client.responseBody = "{\"projectId\":1,\"storeName\":\"store\",\"collectionSchemas\":[{\"collectionSchemaId\":" + id + "}]}";
            expectBadResponse(() -> client.listGraphStoreCollectionSchemas(new ListGraphStoreCollectionSchemasRequest("project", "store")));
        }
        client.responseBody = "{\"stores\":[{}],\"count\":1,\"total\":1}";
        expectBadResponse(() -> client.listGraphStores(new ListGraphStoresRequest("project")));
        client.responseBody = "{\"stores\":[],\"count\":-1,\"total\":0}";
        expectBadResponse(() -> client.listGraphStores(new ListGraphStoresRequest("project")));
        client.responseBody = "{\"projectId\":1,\"storeName\":\"store\",\"storeId\":1,\"baseSchema\":[]}";
        expectBadResponse(() -> client.getGraphStoreBaseSchema(new GetGraphStoreBaseSchemaRequest("project", "store")));
        client.responseBody = "{\"projectId\":1,\"storeName\":\"store\",\"collectionSchemas\":[],\"nextPageToken\":null}";
        expectBadResponse(() -> client.listGraphStoreCollectionSchemas(new ListGraphStoreCollectionSchemasRequest("project", "store")));
    }

    @Test
    public void serviceErrorsAreNotRetriedByManagementMethods() throws Exception {
        CapturingClient client = new CapturingClient();
        for (int status : new int[]{400, 403, 404, 409, 413, 500, 503}) {
            client.failure = new LogException(status, "ServiceError", "failed", "error-request");
            int before = client.calls;
            try {
                client.deleteGraphStore(new DeleteGraphStoreRequest("project", "store"));
                fail("expected service error");
            } catch (LogException error) {
                assertSame(client.failure, error);
                assertEquals("error-request", error.GetRequestId());
                assertEquals(status, error.GetHttpCode());
            }
            assertEquals(before + 1, client.calls);
        }
    }

    private interface Call {
        void run() throws LogException;
    }

    private static void expectInvalid(Call call) throws Exception {
        try {
            call.run();
            fail("expected invalid parameter");
        } catch (LogException error) {
            assertEquals(ErrorCodes.INVALID_PARAMETER, error.GetErrorCode());
        }
    }

    private static void expectBadResponse(Call call) throws Exception {
        try {
            call.run();
            fail("expected bad response");
        } catch (LogException error) {
            assertEquals(ErrorCodes.BAD_RESPONSE, error.GetErrorCode());
            assertEquals("request-id", error.GetRequestId());
        }
    }

    private static class CapturingClient extends Client {
        String responseBody = "";
        HttpMethod method;
        String uri;
        Map<String, String> parameters;
        Map<String, String> headers;
        byte[] body;
        int calls;
        LogException failure;

        CapturingClient() {
            super("http://example.com", "key", "secret");
        }

        JSONObject jsonBody() {
            return JSONObject.parseObject(new String(body, StandardCharsets.UTF_8));
        }

        void check(HttpMethod expectedMethod, String expectedUri) {
            assertEquals(expectedMethod, method);
            assertEquals(expectedUri, uri);
            assertEquals(Consts.CONST_SLS_JSON, headers.get(Consts.CONST_CONTENT_TYPE));
            assertEquals(String.valueOf(body.length), headers.get(Consts.CONST_X_SLS_BODYRAWSIZE));
        }

        @Override
        protected ResponseMessage SendData(String project, HttpMethod method, String uri,
                Map<String, String> parameters, Map<String, String> headers, byte[] body) throws LogException {
            calls++;
            if (failure != null) {
                throw failure;
            }
            assertEquals("project", project);
            this.method = method;
            this.uri = uri;
            this.parameters = new HashMap<String, String>(parameters);
            this.headers = headers;
            this.body = body;
            ResponseMessage response = new ResponseMessage();
            response.setStatusCode(200);
            response.addHeader(Consts.CONST_X_SLS_REQUESTID, "request-id");
            response.SetBody(responseBody.getBytes(StandardCharsets.UTF_8));
            return response;
        }
    }
}
