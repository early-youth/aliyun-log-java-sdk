package com.aliyun.openservices.log.functiontest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.common.GraphStore;
import com.aliyun.openservices.log.common.Consts.CompressType;
import com.aliyun.openservices.log.common.Logs;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.http.client.ClientConfiguration;
import com.aliyun.openservices.log.http.signer.SignVersion;
import com.aliyun.openservices.log.request.*;
import com.aliyun.openservices.log.response.*;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;

// One SDK operation per process; lifecycle, assertions and cleanup belong to the E2E Runner.
public class GraphStoreManagementE2ETool {
    private static JSONObject store(GraphStore value) {
        JSONObject result = new JSONObject(true);
        result.put("projectId", value.getProjectId());
        result.put("storeName", value.getStoreName());
        result.put("state", value.getState());
        result.put("currentShardCount", value.getCurrentShardCount());
        return result;
    }

    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream input = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = System.in.read(buffer)) != -1) input.write(buffer, 0, count);
        JSONObject p = JSON.parseObject(new String(input.toByteArray(), "UTF-8"));
        String key = System.getenv("GRAPHSTORE_E2E_ACCESS_KEY_ID");
        String secret = System.getenv("GRAPHSTORE_E2E_ACCESS_KEY_SECRET");
        int timeout = p.getIntValue("timeout_ms");
        if (key == null || secret == null || timeout <= 0) throw new IllegalArgumentException("missing E2E credentials or timeout");
        if (p.getBooleanValue("bad_credentials")) secret = "intentionally-invalid-e2e-secret";
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setConnectionTimeout(timeout);
        configuration.setSocketTimeout(timeout);
        configuration.setRequestTimeoutEnabled(true);
        configuration.setRequestTimeout(timeout);
        configuration.setProxyHost(p.getString("dial_address").split(":")[0]);
        configuration.setProxyPort(Integer.parseInt(p.getString("dial_address").split(":")[1]));
        if ("v4".equals(p.getString("signature_version"))) {
            configuration.setSignatureVersion(SignVersion.V4);
            configuration.setRegion(p.getString("region"));
        }
        Client client = new Client(p.getString("endpoint"), key, secret, configuration);
        String project = p.getString("project"), name = p.getString("store_name");
        JSONObject output = new JSONObject(true);
        JSONObject body = new JSONObject(true);
        Response response;
        try {
            switch (p.getString("action")) {
                case "PostGraphStoreData": {
                    Logs.LogGroup group = Logs.LogGroup.parseFrom(Base64.getDecoder().decode(p.getString("request_base64")));
                    if (group.getLogsCount() == 0) throw new IllegalArgumentException("E2E write requires a nonempty LogGroup");
                    PostGraphStoreDataRequest request = new PostGraphStoreDataRequest(project, name, group);
                    String compression = p.getString("compression");
                    request.setCompressType("deflate".equals(compression) ? CompressType.GZIP
                            : CompressType.valueOf(compression.toUpperCase(java.util.Locale.ROOT)));
                    response = client.postGraphStoreData(request);
                    break;
                }
                case "GetGraphStoreData": {
                    GetGraphStoreDataResponse r = client.getGraphStoreData(new GetGraphStoreDataRequest(project, name, p.getString("query")));
                    response = r;
                    output.put("header", r.getHeader()); output.put("data", r.getData());
                    JSONObject meta = new JSONObject(true);
                    meta.put("row_count", r.getMeta().getRowCount()); meta.put("truncated", r.getMeta().isTruncated());
                    meta.put("elapsed_ms", r.getMeta().getElapsedMs()); meta.put("peak_memory_bytes", r.getMeta().getPeakMemoryBytes());
                    meta.put("result_bytes", r.getMeta().getResultBytes()); meta.put("warnings", r.getMeta().getWarnings());
                    output.put("meta", meta);
                    break;
                }
                case "CreateGraphStore": {
                    JSONObject value = p.getJSONObject("body_json");
                    response = client.createGraphStore(new CreateGraphStoreRequest(project, value.getString("storeName"),
                        value.getIntValue("initialShardCount"), value.getJSONObject("baseSchema")));
                    break;
                }
                case "GetGraphStore": {
                    GetGraphStoreResponse r = client.getGraphStore(new GetGraphStoreRequest(project, name));
                    response = r; body = store(r.getGraphStore()); break;
                }
                case "ListGraphStores": {
                    ListGraphStoresRequest request = new ListGraphStoresRequest(project);
                    request.setOffset(p.getInteger("offset")); request.setSize(p.getInteger("size"));
                    ListGraphStoresResponse r = client.listGraphStores(request);
                    response = r;
                    List<JSONObject> stores = new ArrayList<JSONObject>();
                    for (GraphStore value : r.getStores()) stores.add(store(value));
                    body.put("stores", stores); body.put("count", r.getCount()); body.put("total", r.getTotal()); break;
                }
                case "DeleteGraphStore": response = client.deleteGraphStore(new DeleteGraphStoreRequest(project, name)); break;
                case "SplitGraphStoreShard": {
                    SplitGraphStoreShardRequest request = new SplitGraphStoreShardRequest(project, name, p.getIntValue("shard_id"));
                    if (p.containsKey("split_hash")) request.setSplitHash(new BigInteger(p.getString("split_hash")));
                    response = client.splitGraphStoreShard(request); break;
                }
                case "GetGraphStoreBaseSchema": {
                    GetGraphStoreBaseSchemaResponse r = client.getGraphStoreBaseSchema(new GetGraphStoreBaseSchemaRequest(project, name));
                    response = r; body.put("projectId", r.getProjectId()); body.put("storeName", r.getStoreName());
                    body.put("storeId", r.getStoreId()); body.put("baseSchema", r.getBaseSchema()); break;
                }
                case "CreateGraphStoreCollectionSchema":
                    response = client.createGraphStoreCollectionSchema(new CreateGraphStoreCollectionSchemaRequest(project, name, p.getJSONObject("body_json"))); break;
                case "GetGraphStoreCollectionSchema": {
                    GetGraphStoreCollectionSchemaResponse r = client.getGraphStoreCollectionSchema(new GetGraphStoreCollectionSchemaRequest(project, name, p.getLongValue("collection_schema_id")));
                    response = r; body.put("projectId", r.getProjectId()); body.put("storeName", r.getStoreName());
                    body.put("collectionSchema", r.getCollectionSchema()); break;
                }
                case "ListGraphStoreCollectionSchemas": {
                    ListGraphStoreCollectionSchemasRequest request = new ListGraphStoreCollectionSchemasRequest(project, name);
                    request.setPageSize(p.getInteger("page_size")); request.setPageToken(p.getString("page_token"));
                    ListGraphStoreCollectionSchemasResponse r = client.listGraphStoreCollectionSchemas(request);
                    response = r; body.put("projectId", r.getProjectId()); body.put("storeName", r.getStoreName());
                    body.put("collectionSchemas", r.getCollectionSchemas());
                    if (r.getNextPageToken() != null && !r.getNextPageToken().isEmpty()) body.put("nextPageToken", r.getNextPageToken()); break;
                }
                case "UpdateGraphStoreCollectionSchema":
                    response = client.updateGraphStoreCollectionSchema(new UpdateGraphStoreCollectionSchemaRequest(project, name, p.getLongValue("collection_schema_id"), p.getJSONObject("body_json"))); break;
                case "DeleteGraphStoreCollectionSchema":
                    response = client.deleteGraphStoreCollectionSchema(new DeleteGraphStoreCollectionSchemaRequest(project, name, p.getLongValue("collection_schema_id"))); break;
                default: throw new IllegalArgumentException("unsupported GraphStore operation");
            }
            output.put("http_status", 200); output.put("request_id", response.GetRequestId());
            if (!p.getString("action").endsWith("Data")) output.put("body", body);
        } catch (LogException error) {
            if (error.getHttpCode() <= 0) throw error;
            output.put("http_status", error.getHttpCode()); output.put("request_id", error.getRequestId());
            output.put("error_code", error.getErrorCode()); output.put("body", body);
        } finally {
            client.shutdown();
        }
        System.out.write(JSON.toJSONString(output, com.alibaba.fastjson.serializer.SerializerFeature.WriteMapNullValue).getBytes("UTF-8"));
        System.out.write('\n');
    }
}
