package com.aliyun.openservices.log;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.log.common.*;
import com.aliyun.openservices.log.internal.GraphStoreSchemaCodec;
import java.util.Arrays;
import java.util.Collections;
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
    private static final String SCHEMA = JSONObject.toJSONString(collection("9223372036854775807"));
    private static final String STORE_WITH_SHARDS = STORE.substring(0, STORE.length() - 1)
            + ",\"shards\":[{\"shardId\":2},{\"shardId\":7}]}";

    @Test
    public void mutationsUseNativeRequestAndResponse() throws Exception {
        CapturingClient client = new CapturingClient();
        GraphStoreBaseSchema schema = base();
        Response created = client.createGraphStore(new CreateGraphStoreRequest("project", "store", 2, schema));
        assertEquals("request-id", created.GetRequestId());
        client.check(HttpMethod.POST, "/graphstore");
        JSONObject body = client.jsonBody();
        assertEquals(3, body.size());
        assertEquals("store", body.get("storeName"));
        assertEquals(2, body.get("shardCount"));
        assertTrue(body.get("baseSchema") instanceof JSONObject);
        assertEquals(Long.MAX_VALUE, body.getJSONObject("baseSchema").getLongValue("ttlSeconds"));

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

        client.createGraphStoreCollectionSchema(new CreateGraphStoreCollectionSchemaRequest("project", "store", collection("雪")));
        client.check(HttpMethod.POST, "/graphstore/store/collectionschemas");
        assertEquals(1, client.jsonBody().size());
        assertTrue(client.jsonBody().get("value") instanceof JSONObject);
        client.updateGraphStoreCollectionSchema(new UpdateGraphStoreCollectionSchemaRequest("project", "store", "9223372036854775807", update("9223372036854775807")));
        client.check(HttpMethod.PUT, "/graphstore/store/collectionschemas/9223372036854775807");
        assertFalse(client.jsonBody().getJSONObject("value").containsKey("ttlSeconds"));
        client.deleteGraphStoreCollectionSchema(new DeleteGraphStoreCollectionSchemaRequest("project", "store", "9223372036854775807"));
        client.check(HttpMethod.DELETE, "/graphstore/store/collectionschemas/9223372036854775807");
        assertEquals(0, client.body.length);
        client.deleteGraphStore(new DeleteGraphStoreRequest("project", "store"));
        client.check(HttpMethod.DELETE, "/graphstore/store");
        assertEquals(8, client.calls);
    }

    @Test
    public void readsKeepIntegerPrecisionAndTypedSchemas() throws Exception {
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
                + "\"baseSchema\":" + GraphStoreSchemaCodec.encode(base()).toJSONString() + "}";
        GetGraphStoreBaseSchemaResponse base = client.getGraphStoreBaseSchema(new GetGraphStoreBaseSchemaRequest("project", "store"));
        client.check(HttpMethod.GET, "/graphstore/store/baseschema");
        assertEquals(Long.MAX_VALUE, base.getStoreId());
        assertEquals(Long.valueOf(Long.MAX_VALUE), base.getBaseSchema().getTtlSeconds());

        client.responseBody = "{\"projectId\":1,\"storeName\":\"store\",\"collectionSchema\":" + SCHEMA + "}";
        GetGraphStoreCollectionSchemaResponse collection = client.getGraphStoreCollectionSchema(
                new GetGraphStoreCollectionSchemaRequest("project", "store", "9223372036854775807"));
        client.check(HttpMethod.GET, "/graphstore/store/collectionschemas/9223372036854775807");
        assertEquals("9223372036854775807", collection.getCollectionSchema().getName());

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
        assertEquals("9223372036854775807", result.getCollectionSchemas().get(0).getName());
        page.setPageSize(null);
        page.setPageToken(null);
        client.responseBody = "{\"projectId\":1,\"storeName\":\"store\",\"collectionSchemas\":[]}";
        result = client.listGraphStoreCollectionSchemas(page);
        assertNull(result.getNextPageToken());
        assertTrue(client.parameters.isEmpty());
    }

    @Test
    public void schemaSerializationOmitsOptionalNullsAndExpandsSharedValues() throws Exception {
        CapturingClient client = new CapturingClient();
        GraphStoreIndexConfig index = new GraphStoreIndexConfig(false);
        GraphStoreCollectionSchema schema = collection("aa");
        schema.setFields(Arrays.asList(new GraphStoreFieldSchema("one", GraphStoreFieldType.STRING, index),
                new GraphStoreFieldSchema("two", GraphStoreFieldType.INT, index)));
        client.createGraphStoreCollectionSchema(new CreateGraphStoreCollectionSchemaRequest("project", "store", schema));
        JSONObject value = client.jsonBody().getJSONObject("value");
        assertFalse(value.containsKey("ttlSeconds"));
        assertFalse(new String(client.body, StandardCharsets.UTF_8).contains("$ref"));
        assertEquals(value.getJSONArray("fields").getJSONObject(0).getJSONObject("indexConfig"),
                value.getJSONArray("fields").getJSONObject(1).getJSONObject("indexConfig"));
        GraphStoreCollectionSchemaUpdate update = update("aa");
        update.setTtlSeconds(0L);
        client.updateGraphStoreCollectionSchema(new UpdateGraphStoreCollectionSchemaRequest("project", "store", "aa", update));
        assertEquals(0L, client.jsonBody().getJSONObject("value").getLongValue("ttlSeconds"));
        update.setTtlSeconds(null);
        client.updateGraphStoreCollectionSchema(new UpdateGraphStoreCollectionSchemaRequest("project", "store", "aa", update));
        assertFalse(client.jsonBody().getJSONObject("value").containsKey("ttlSeconds"));
    }

    @Test
    public void schemaBusinessRulesAreLeftToTheService() throws Exception {
        CapturingClient client = new CapturingClient();
        GraphStoreCollectionSchema schema = collection("");
        schema.setDescription("");
        schema.setTtlSeconds(-1L);
        GraphStoreIndexConfig index = new GraphStoreIndexConfig(true);
        index.setFullTextIndex(new GraphStoreFullTextIndex("", false, false));
        schema.setFields(Arrays.asList(new GraphStoreFieldSchema("", GraphStoreFieldType.STRING, index)));
        client.createGraphStoreCollectionSchema(new CreateGraphStoreCollectionSchemaRequest("project", "store", schema));
        JSONObject body = client.jsonBody().getJSONObject("value");
        assertEquals("", body.getString("name"));
        assertEquals("", body.getString("description"));
        assertEquals(-1L, body.getLongValue("ttlSeconds"));
        assertEquals("", body.getJSONArray("fields").getJSONObject(0).getJSONObject("indexConfig").getJSONObject("fullTextIndex").getString("token"));
        assertEquals(1, client.calls);
    }

    @Test
    public void missingSchemaValuesUseNativeNulls() throws Exception {
        CapturingClient client = new CapturingClient();
        client.responseBody = "{\"projectId\":1,\"storeName\":\"store\",\"storeId\":1}";
        assertNull(client.getGraphStoreBaseSchema(new GetGraphStoreBaseSchemaRequest("project", "store")).getBaseSchema());
        assertNull(client.getGraphStoreCollectionSchema(new GetGraphStoreCollectionSchemaRequest("project", "store", "one")).getCollectionSchema());
        assertNull(client.listGraphStoreCollectionSchemas(new ListGraphStoreCollectionSchemasRequest("project", "store")).getCollectionSchemas());
        client.responseBody = "{\"projectId\":1,\"storeName\":\"store\",\"collectionSchema\":{\"newOption\":true}}";
        GraphStoreCollectionSchema schema = client.getGraphStoreCollectionSchema(
                new GetGraphStoreCollectionSchemaRequest("project", "store", "one")).getCollectionSchema();
        assertNull(schema.getName());
        assertNull(schema.getFields());
        client.responseBody = "{\"projectId\":1,\"storeName\":\"store\",\"collectionSchemas\":[null,{}]}";
        java.util.List<GraphStoreCollectionSchema> schemas = client.listGraphStoreCollectionSchemas(
                new ListGraphStoreCollectionSchemasRequest("project", "store")).getCollectionSchemas();
        assertNull(schemas.get(0));
        assertNull(schemas.get(1).getFields());
    }

    @Test
    public void paginationUsesParentParameterTable() {
        ListGraphStoresRequest stores = new ListGraphStoresRequest("project");
        stores.SetParam("offset", "7");
        stores.SetParam("size", "9");
        assertEquals(Integer.valueOf(7), stores.getOffset());
        assertEquals(Integer.valueOf(9), stores.getSize());
        stores.setOffset(2);
        assertEquals("2", stores.GetParam("offset"));
        stores.setOffset(null);
        stores.setSize(null);
        assertTrue(stores.GetAllParams().isEmpty());
        ListGraphStoreCollectionSchemasRequest page = new ListGraphStoreCollectionSchemasRequest("project", "store");
        page.SetParam("pageSize", "3");
        page.SetParam("pageToken", "a+b/=&");
        assertEquals(Integer.valueOf(3), page.getPageSize());
        assertEquals("a+b/=&", page.getPageToken());
        page.setPageToken("雪+=");
        assertEquals("雪+=", page.GetParam("pageToken"));
        page.setPageToken(null);
        page.setPageSize(null);
        assertTrue(page.GetAllParams().isEmpty());
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
        for (String name : new String[]{null, "", ".", "..", "a\\b", "a b", "../other", "a/b", "a%2fb", "a?x=y", "a#b", "雪", "a\u0000b"}) {
            expectInvalid(() -> client.getGraphStoreCollectionSchema(
                    new GetGraphStoreCollectionSchemaRequest("project", "store", name)));
            expectInvalid(() -> client.updateGraphStoreCollectionSchema(
                    new UpdateGraphStoreCollectionSchemaRequest("project", "store", name, update("aa"))));
            expectInvalid(() -> client.deleteGraphStoreCollectionSchema(
                    new DeleteGraphStoreCollectionSchemaRequest("project", "store", name)));
        }
        GraphStoreCollectionSchemaUpdate different = update("bb");
        expectInvalid(() -> client.updateGraphStoreCollectionSchema(
                new UpdateGraphStoreCollectionSchemaRequest("project", "store", "aa", different)));
        expectInvalid(() -> client.createGraphStore(null));
        expectInvalid(() -> client.createGraphStore(new CreateGraphStoreRequest("project", "store", 1, null)));
        expectInvalid(() -> client.createGraphStoreCollectionSchema(
                new CreateGraphStoreCollectionSchemaRequest("project", "store", null)));
        expectInvalid(() -> client.updateGraphStoreCollectionSchema(
                new UpdateGraphStoreCollectionSchemaRequest("project", "store", "aa", null)));
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
    public void collectionNamesRemainOpaque() throws Exception {
        CapturingClient client = new CapturingClient();
        for (String name : new String[]{"a.b", "a@b", "a.@b", "12", "1a", "aa", new String(new char[63]).replace('\0', 'a'),
                "a", new String(new char[64]).replace('\0', 'a')}) {
            // Configurable length/name rules belong to the service, not the SDK.
            client.responseBody = "{\"projectId\":1,\"storeName\":\"store\",\"collectionSchema\":" + JSONObject.toJSONString(collection(name)) + "}";
            GetGraphStoreCollectionSchemaRequest get = new GetGraphStoreCollectionSchemaRequest("project", "store", name);
            get.setCollectionSchemaName(name);
            assertEquals(name, get.getCollectionSchemaName());
            assertEquals(name, client.getGraphStoreCollectionSchema(get).getCollectionSchema().getName());
            client.check(HttpMethod.GET, "/graphstore/store/collectionschemas/" + name);
            GraphStoreCollectionSchemaUpdate value = update(name);
            UpdateGraphStoreCollectionSchemaRequest update = new UpdateGraphStoreCollectionSchemaRequest("project", "store", name, value);
            update.setCollectionSchemaName(name);
            assertEquals(name, update.getCollectionSchemaName());
            client.updateGraphStoreCollectionSchema(update);
            client.check(HttpMethod.PUT, "/graphstore/store/collectionschemas/" + name);
            assertEquals(name, client.jsonBody().getJSONObject("value").getString("name"));
            DeleteGraphStoreCollectionSchemaRequest delete = new DeleteGraphStoreCollectionSchemaRequest("project", "store", name);
            delete.setCollectionSchemaName(name);
            assertEquals(name, delete.getCollectionSchemaName());
            client.deleteGraphStoreCollectionSchema(delete);
            client.check(HttpMethod.DELETE, "/graphstore/store/collectionschemas/" + name);
        }
    }

    @Test
    public void malformedResponsesFailWithoutCoercion() throws Exception {
        CapturingClient client = new CapturingClient();
        for (String body : new String[]{"", "null", "[]", "{"}) {
            client.responseBody = body;
            expectBadResponse(() -> client.getGraphStore(new GetGraphStoreRequest("project", "store")));
            expectBadResponse(() -> client.listGraphStores(new ListGraphStoresRequest("project")));
            expectBadResponse(() -> client.getGraphStoreBaseSchema(new GetGraphStoreBaseSchemaRequest("project", "store")));
            expectBadResponse(() -> client.getGraphStoreCollectionSchema(new GetGraphStoreCollectionSchemaRequest("project", "store", "one")));
            expectBadResponse(() -> client.listGraphStoreCollectionSchemas(new ListGraphStoreCollectionSchemasRequest("project", "store")));
        }
        for (String shards : new String[]{"null", "{}", "[null]", "[{}]", "[{\"shardId\":2147483648}]"}) {
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
        }
        client.responseBody = "{\"stores\":[{}],\"count\":1,\"total\":1}";
        expectBadResponse(() -> client.listGraphStores(new ListGraphStoresRequest("project")));
        client.responseBody = "{\"stores\":[],\"count\":9223372036854775808,\"total\":0}";
        expectBadResponse(() -> client.listGraphStores(new ListGraphStoresRequest("project")));
        client.responseBody = "{\"projectId\":1,\"storeName\":\"store\",\"storeId\":1,\"baseSchema\":[]}";
        expectBadResponse(() -> client.getGraphStoreBaseSchema(new GetGraphStoreBaseSchemaRequest("project", "store")));
        client.responseBody = "{\"projectId\":1,\"storeName\":\"store\",\"collectionSchemas\":[],\"nextPageToken\":null}";
        assertNull(client.listGraphStoreCollectionSchemas(new ListGraphStoreCollectionSchemasRequest("project", "store")).getNextPageToken());
        for (String shards : new String[]{"[]", "[{\"shardId\":2},{\"shardId\":1}]", "[{\"shardId\":1},{\"shardId\":1}]"}) {
            client.responseBody = STORE.substring(0, STORE.length() - 1) + ",\"diagnostic\":true,\"shards\":" + shards + "}";
            client.getGraphStore(new GetGraphStoreRequest("project", "store"));
        }
    }

    @Test
    public void updateNameIsValidatedLocally() throws Exception {
        CapturingClient client = new CapturingClient();
        for (String name : new String[]{"", "renamed"}) {
            expectInvalid(() -> client.updateGraphStoreCollectionSchema(
                    new UpdateGraphStoreCollectionSchemaRequest("project", "store", "a.@b", update(name))));
        }
        assertEquals(0, client.calls);
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
            assertEquals(200, error.GetHttpCode());
            assertNotNull(error.getCause());
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
    static GraphStoreBaseSchema base() {
        return new GraphStoreBaseSchema(false, Long.MAX_VALUE, false,
                new GraphStoreVertexLayout("vl", "vid"), new GraphStoreEdgeLayout(
                new GraphStoreEndpoint("sl", "sid"), "el", new GraphStoreEndpoint("tl", "tid")));
    }

    static GraphStoreCollectionSchema collection(String name) {
        return new GraphStoreCollectionSchema(name, "schema", GraphStoreLayoutType.VERTEX,
                Collections.<GraphStoreCollectionKeyValue>emptyList(), Collections.<GraphStoreFieldSchema>emptyList());
    }

    static GraphStoreCollectionSchemaUpdate update(String name) {
        return new GraphStoreCollectionSchemaUpdate(name, "schema", Collections.<GraphStoreFieldSchema>emptyList());
    }

}
