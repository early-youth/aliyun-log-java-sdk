package com.aliyun.openservices.log.functiontest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.internal.ErrorCodes;
import com.aliyun.openservices.log.internal.GraphStoreSchemaCodec;
import com.aliyun.openservices.log.common.Consts.CompressType;
import com.aliyun.openservices.log.common.Logs;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.http.client.ClientConfiguration;
import com.aliyun.openservices.log.http.signer.SignVersion;
import com.aliyun.openservices.log.request.*;
import com.aliyun.openservices.log.response.*;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Base64;

// One SDK operation per process; lifecycle, assertions and cleanup belong to the E2E Runner.
public class GraphStoreManagementE2ETool {
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
        if (p.getString("dial_address") != null) {
            java.net.URI proxy = java.net.URI.create("http://" + p.getString("dial_address"));
            configuration.setProxyHost(proxy.getHost());
            configuration.setProxyPort(proxy.getPort());
        }
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
                    if (!p.containsKey("start") || !p.containsKey("end")) throw new IllegalArgumentException("query requires start and end");
                    GetGraphStoreDataRequest request = new GetGraphStoreDataRequest(project, name, p.getString("query"),
                            p.getLongValue("start"), p.getLongValue("end"));
                    request.setSession(p.getString("session"));
                    GetGraphStoreDataResponse r = client.getGraphStoreData(request);
                    response = r;
                    output.put("header", r.getData().getFields()); output.put("data", r.getData().getValues());
                    JSONObject meta = new JSONObject(true);
                    meta.put("row_count", r.getData().getValues().size()); meta.put("truncated", r.getMeta().isTruncated());
                    meta.put("elapsed_ms", r.getMeta().getElapsedMs()); meta.put("warnings", r.getMeta().getWarnings());
                    output.put("meta", meta);
                    break;
                }
                case "CreateGraphStore": {
                    JSONObject value = p.getJSONObject("body_json");
                    response = client.createGraphStore(new CreateGraphStoreRequest(project, value.getString("storeName"),
                        value.getIntValue("shardCount"), GraphStoreSchemaCodec.readBaseSchema(value.getJSONObject("baseSchema"))));
                    break;
                }
                case "GetGraphStore": {
                    GetGraphStoreResponse r = client.getGraphStore(new GetGraphStoreRequest(project, name));
                    response = r; body = (JSONObject) JSON.toJSON(r.getGraphStore()); body.put("shards", r.getShards()); break;
                }
                case "ListGraphStores": {
                    ListGraphStoresRequest request = new ListGraphStoresRequest(project);
                    request.setOffset(p.getInteger("offset")); request.setSize(p.getInteger("size"));
                    ListGraphStoresResponse r = client.listGraphStores(request);
                    response = r;
                    body.put("stores", r.getStores()); body.put("count", r.getCount()); body.put("total", r.getTotal()); break;
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
                    body.put("storeId", r.getStoreId()); body.put("baseSchema", GraphStoreSchemaCodec.encode(r.getBaseSchema())); break;
                }
                case "CreateGraphStoreCollectionSchema":
                    response = client.createGraphStoreCollectionSchema(new CreateGraphStoreCollectionSchemaRequest(project, name, p.getObject("body_json", com.aliyun.openservices.log.common.GraphStoreCollectionSchema.class))); break;
                case "GetGraphStoreCollectionSchema": {
                    GetGraphStoreCollectionSchemaResponse r = client.getGraphStoreCollectionSchema(new GetGraphStoreCollectionSchemaRequest(project, name, p.getString("collection_schema_name")));
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
                    response = client.updateGraphStoreCollectionSchema(new UpdateGraphStoreCollectionSchemaRequest(project, name, p.getString("collection_schema_name"), p.getObject("body_json", com.aliyun.openservices.log.common.GraphStoreCollectionSchemaUpdate.class))); break;
                case "DeleteGraphStoreCollectionSchema":
                    response = client.deleteGraphStoreCollectionSchema(new DeleteGraphStoreCollectionSchemaRequest(project, name, p.getString("collection_schema_name"))); break;
                default: throw new IllegalArgumentException("unsupported GraphStore operation");
            }
            output.put("http_status", 200); output.put("request_id", response.GetRequestId());
            if (!p.getString("action").endsWith("Data")) output.put("body", body);
        } catch (LogException error) {
            if (error.getHttpCode() <= 0 && ErrorCodes.INVALID_PARAMETER.equals(error.getErrorCode())) {
                output.put("http_status", 0); output.put("request_id", "");
                output.put("local_error", true); output.put("error_code", "SDK_LOCAL_ERROR");
                output.put("error_message", error.GetErrorMessage());
            } else {
                if (error.getHttpCode() <= 0) throw error;
                output.put("http_status", error.getHttpCode()); output.put("request_id", error.getRequestId());
                output.put("error_code", error.getErrorCode());
            }
            output.put("body", body);
        } catch (IllegalArgumentException error) {
            output.put("http_status", 0); output.put("request_id", "");
            output.put("local_error", true); output.put("error_code", "SDK_LOCAL_ERROR");
            output.put("error_message", error.getMessage()); output.put("body", body);
        } finally {
            client.shutdown();
        }
        System.out.write(JSON.toJSONString(output).getBytes("UTF-8"));
        System.out.write('\n');
    }
}
