package com.aliyun.openservices.log.sample;

import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.common.*;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.http.client.ClientConfiguration;
import com.aliyun.openservices.log.request.*;
import com.aliyun.openservices.log.response.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** 在已有 Project 中演示图存储 CRUD，运行结束清理本次创建的 Store；配置和运行方式见 GRAPHSTORE.md。 */
public class GraphStoreSample {
    // ID 使用非空字符串，无需固定长度；边的 sid/tid 必须与对应顶点的 ID 保持一致。
    private static final String ALICE_ID = "alice";
    private static final String BOB_ID = "bob";
    private static final String PERSON_QUERY =
            "MATCH (n:person) RETURN n.name AS name, n.age AS age ORDER BY n.name";
    private static final String EDGE_QUERY =
            "MATCH (a:person)-[:knows]->(b:person) RETURN a.name AS source, b.name AS target";
    private static final long WAIT_NANOS = TimeUnit.SECONDS.toNanos(120);

    private final Client client;
    private final String project;
    private final String store = "graphstore-sample-" + UUID.randomUUID().toString().replace("-", "");

    private GraphStoreSample(Client client, String project) {
        this.client = client;
        this.project = project;
    }

    public static void main(String[] args) throws Exception {
        // 从环境变量读取配置和凭据，避免把 AK/SK 写入源码或命令行参数。
        String endpoint = requiredEnv("GRAPHSTORE_ENDPOINT");
        String project = requiredEnv("GRAPHSTORE_PROJECT");
        String accessKeyId = requiredEnv("ALIBABA_CLOUD_ACCESS_KEY_ID");
        String accessKeySecret = requiredEnv("ALIBABA_CLOUD_ACCESS_KEY_SECRET");
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setConnectionTimeout(10_000);
        configuration.setSocketTimeout(30_000);
        configuration.setRequestTimeoutEnabled(true);
        configuration.setRequestTimeout(30_000);
        // 默认使用 V1；V4 需在创建 Client 前设置 setSignatureVersion(SignVersion.V4) 和 setRegion(region)。
        // 全流程复用同一个 Client 和连接池，并在 finally 中关闭。
        Client client = new Client(endpoint, accessKeyId, accessKeySecret, configuration);
        GraphStoreSample sample = new GraphStoreSample(client, project);
        System.out.println("Sample resources: project=" + project + ", store=" + sample.store);
        int exitCode = 0;
        try {
            sample.run();
            System.out.println("GraphStore sample passed; Store deletion confirmed.");
        } catch (Exception error) {
            printFailure(error);
            for (Throwable cleanupError : error.getSuppressed()) printFailure(cleanupError);
            System.err.println("Check the Store above if Create or cleanup had an unknown result.");
            exitCode = 1;
        } finally {
            client.shutdown();
        }
        if (exitCode != 0) System.exit(exitCode);
    }

    private void run() throws Exception {
        // TTL 必须为正数，本例设置为 86400 秒（1 天），同时关闭时态和索引。
        // vl/vid 等指定写入字段名，person/knows 才是后续写入的标签值。
        GraphStoreBaseSchema base = new GraphStoreBaseSchema(false, 86400L, false,
                new GraphStoreVertexLayout("vl", "vid"),
                new GraphStoreEdgeLayout(new GraphStoreEndpoint("sl", "sid"), "el",
                        new GraphStoreEndpoint("tl", "tid")));
        // Create 失败时服务端仍可能已创建资源，先输出唯一名称，便于结果未知时核对。
        printResponse("CreateGraphStore", client.createGraphStore(
                new CreateGraphStoreRequest(project, store, 1, base)));
        // 只有明确创建成功才进入自动清理范围，业务失败时也清理这个专用 Store。
        try {
            waitUntilReady();
            ListGraphStoresRequest list = new ListGraphStoresRequest(project);
            list.setOffset(0);
            list.setSize(100);
            ListGraphStoresResponse stores = client.listGraphStores(list);
            printResponse("ListGraphStores", stores);
            for (GraphStore item : stores.getStores()) System.out.println("Store: " + item.getStoreName());
            GetGraphStoreBaseSchemaResponse baseResponse = client.getGraphStoreBaseSchema(
                    new GetGraphStoreBaseSchemaRequest(project, store));
            printResponse("GetGraphStoreBaseSchema", baseResponse);
            System.out.println("Vertex label field: " + baseResponse.getBaseSchema().getVertexLayout().getLabel());
            System.out.println("Base TTL seconds: " + baseResponse.getBaseSchema().getTtlSeconds());

            createSchemas();
            GetGraphStoreCollectionSchemaResponse person = client.getGraphStoreCollectionSchema(
                    new GetGraphStoreCollectionSchemaRequest(project, store, "person"));
            printResponse("GetGraphStoreCollectionSchema", person);
            // 分页 token 直接透传服务端返回值，直到为空；不要自行解析或拼接。
            ListGraphStoreCollectionSchemasRequest page = new ListGraphStoreCollectionSchemasRequest(project, store);
            page.setPageSize(1);
            long pageDeadline = System.nanoTime() + WAIT_NANOS;
            do {
                if (System.nanoTime() >= pageDeadline) throw new IllegalStateException("Timed out listing CollectionSchemas");
                ListGraphStoreCollectionSchemasResponse schemas = client.listGraphStoreCollectionSchemas(page);
                printResponse("ListGraphStoreCollectionSchemas", schemas);
                for (GraphStoreCollectionSchema schema : schemas.getCollectionSchemas()) {
                    System.out.println("CollectionSchema: " + schema.getName());
                }
                page.setPageToken(schemas.getNextPageToken());
            } while (page.getPageToken() != null && !page.getPageToken().isEmpty());

            // shortcut: 当前 stg 在 READY 后仍需等待路由/Schema 缓存收敛；有公开可写保证后替换固定等待。
            System.out.println("Waiting 65 seconds before the first write for stg cache convergence.");
            Thread.sleep(65_000);
            writeAndQuery();
            updateSchema(person.getCollectionSchema());
            printResponse("DeleteGraphStoreCollectionSchema(knows)", client.deleteGraphStoreCollectionSchema(
                    new DeleteGraphStoreCollectionSchemaRequest(project, store, "knows")));
            printResponse("DeleteGraphStoreCollectionSchema(person)", client.deleteGraphStoreCollectionSchema(
                    new DeleteGraphStoreCollectionSchemaRequest(project, store, "person")));
        } catch (Exception error) {
            try {
                deleteStore();
            } catch (Exception cleanupError) {
                // 保留业务主异常，同时附带清理错误，避免排查时丢失最初的失败原因。
                error.addSuppressed(cleanupError);
            }
            throw error;
        }
        deleteStore();
    }

    private void createSchemas() throws LogException {
        // 两个 Collection 未显式设置 TTL，默认继承 BaseSchema 的 86400 秒。
        // CollectionSchema 通过标签键值匹配数据；这里为 vl=person 的顶点定义 name/age 属性。
        List<GraphStoreFieldSchema> fields = Arrays.asList(
                new GraphStoreFieldSchema("name", GraphStoreFieldType.STRING, new GraphStoreIndexConfig(false)),
                new GraphStoreFieldSchema("age", GraphStoreFieldType.INT, new GraphStoreIndexConfig(false)));
        GraphStoreCollectionSchema person = new GraphStoreCollectionSchema("person", "People",
                GraphStoreLayoutType.VERTEX,
                Collections.singletonList(new GraphStoreCollectionKeyValue("vl", "person")), fields);
        printResponse("CreateGraphStoreCollectionSchema(person)", client.createGraphStoreCollectionSchema(
                new CreateGraphStoreCollectionSchemaRequest(project, store, person)));
        // 当前 Worker 要求非 DELETE 事件携带非空属性，因此 knows 边增加真实的 weight 属性。
        GraphStoreCollectionSchema knows = new GraphStoreCollectionSchema("knows", "Person knows person",
                GraphStoreLayoutType.EDGE, Arrays.asList(
                        new GraphStoreCollectionKeyValue("sl", "person"),
                        new GraphStoreCollectionKeyValue("el", "knows"),
                        new GraphStoreCollectionKeyValue("tl", "person")),
                Collections.singletonList(new GraphStoreFieldSchema("weight", GraphStoreFieldType.DOUBLE,
                        new GraphStoreIndexConfig(false))));
        printResponse("CreateGraphStoreCollectionSchema(knows)", client.createGraphStoreCollectionSchema(
                new CreateGraphStoreCollectionSchemaRequest(project, store, knows)));
    }

    private void writeAndQuery() throws Exception {
        // 同一个标准 LogGroup 可包含顶点和边；INT/DOUBLE 属性仍以十进制字符串传入。
        write(log("vertex", "create", "vl", "person", "vid", ALICE_ID, "name", "Alice", "age", "30"),
                log("vertex", "create", "vl", "person", "vid", BOB_ID, "name", "Bob", "age", "28"),
                log("edge", "create", "sl", "person", "sid", ALICE_ID, "el", "knows", "tl", "person", "tid", BOB_ID, "weight", "1.0"));
        waitForRows(PERSON_QUERY, Arrays.asList("name", "age"),
                Arrays.asList(Arrays.asList("Alice", "30"), Arrays.asList("Bob", "28")));
        waitForRows(EDGE_QUERY, Arrays.asList("source", "target"),
                Collections.singletonList(Arrays.asList("Alice", "Bob")));

        // 实体 update 携带完整属性集合：只改 age 时仍需保留 name。
        write(log("vertex", "update", "vl", "person", "vid", ALICE_ID, "name", "Alice", "age", "31"));
        waitForRows(PERSON_QUERY, Arrays.asList("name", "age"),
                Arrays.asList(Arrays.asList("Alice", "31"), Arrays.asList("Bob", "28")));

        // delete 只需标识实体的键字段；本例先删边，再删顶点，并分别查询确认。
        write(log("edge", "delete", "sl", "person", "sid", ALICE_ID, "el", "knows", "tl", "person", "tid", BOB_ID));
        waitForRows(EDGE_QUERY, Arrays.asList("source", "target"), Collections.<List<String>>emptyList());
        write(log("vertex", "delete", "vl", "person", "vid", ALICE_ID),
                log("vertex", "delete", "vl", "person", "vid", BOB_ID));
        waitForRows(PERSON_QUERY, Arrays.asList("name", "age"), Collections.<List<String>>emptyList());
    }

    private void updateSchema(GraphStoreCollectionSchema person) throws LogException {
        // Schema 更新替换 fields 和 TTL；新增 email 时显式带回原字段，避免误删 name/age。
        List<GraphStoreFieldSchema> fields = new ArrayList<GraphStoreFieldSchema>(person.getFields());
        fields.add(new GraphStoreFieldSchema("email", GraphStoreFieldType.STRING, new GraphStoreIndexConfig(false)));
        GraphStoreCollectionSchemaUpdate update = new GraphStoreCollectionSchemaUpdate("person", "People with email", fields);
        // 显式覆盖为 3600 秒（1 小时）；不设置或设为 null 则恢复继承 Base TTL。
        update.setTtlSeconds(3600L);
        printResponse("UpdateGraphStoreCollectionSchema", client.updateGraphStoreCollectionSchema(
                new UpdateGraphStoreCollectionSchemaRequest(project, store, "person", update)));
        GetGraphStoreCollectionSchemaResponse result = client.getGraphStoreCollectionSchema(
                new GetGraphStoreCollectionSchemaRequest(project, store, "person"));
        printResponse("GetGraphStoreCollectionSchema(updated)", result);
        GraphStoreCollectionSchema actual = result.getCollectionSchema();
        List<String> names = new ArrayList<String>();
        for (GraphStoreFieldSchema field : actual.getFields()) names.add(field.getName());
        // 服务端可能调整字段顺序，按字段集合核对，同时确认描述和 TTL 已更新。
        if (!update.getDescription().equals(actual.getDescription()) || !Long.valueOf(3600L).equals(actual.getTtlSeconds())
                || names.size() != 3 || !names.containsAll(Arrays.asList("name", "age", "email"))) {
            throw new IllegalStateException("CollectionSchema update did not match the requested replacement");
        }
        System.out.println("Updated CollectionSchema: description=" + actual.getDescription()
                + ", ttlSeconds=" + actual.getTtlSeconds() + ", fields=" + names);
    }

    private static Logs.Log log(String layout, String method, String... fields) {
        // Log.time 使用 Unix 秒；两个保留字段声明布局和操作，后续参数按字段名/值成对传入。
        Logs.Log.Builder log = Logs.Log.newBuilder().setTime((int) (System.currentTimeMillis() / 1000L));
        log.addContents(Logs.Log.Content.newBuilder().setKey("__layout__").setValue(layout));
        log.addContents(Logs.Log.Content.newBuilder().setKey("__method__").setValue(method));
        for (int i = 0; i < fields.length; i += 2) {
            log.addContents(Logs.Log.Content.newBuilder().setKey(fields[i]).setValue(fields[i + 1]));
        }
        return log.build();
    }

    private void write(Logs.Log... logs) throws LogException {
        // 直接构造标准 LogGroup，序列化、LZ4 压缩、签名和 HTTP 请求交给原生 SDK。
        Logs.LogGroup group = Logs.LogGroup.newBuilder().addAllLogs(Arrays.asList(logs)).build();
        PostGraphStoreDataRequest request = new PostGraphStoreDataRequest(project, store, group);
        request.setCompressType(Consts.CompressType.LZ4);
        printResponse("PostGraphStoreData(" + logs.length + " events)", client.postGraphStoreData(request));
    }

    private void waitUntilReady() throws Exception {
        long deadline = System.nanoTime() + WAIT_NANOS;
        do {
            GetGraphStoreResponse response = client.getGraphStore(new GetGraphStoreRequest(project, store));
            printResponse("GetGraphStore", response);
            System.out.println("Store state: " + response.getGraphStore().getState() + ", shards=" + response.getShards().size());
            if ("READY".equals(response.getGraphStore().getState())) return;
            Thread.sleep(1000);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("Timed out waiting for Store READY");
    }

    private void waitForRows(String query, List<String> header, List<List<String>> expected) throws Exception {
        // 写入 ACK 不保证查询立即可见；有限轮询，严格核对列名、行值及 truncated=false。
        long deadline = System.nanoTime() + WAIT_NANOS;
        String observed = "no response";
        do {
            // start/end 单位为 Unix 秒，本例查询完整的 uint32 时间范围。
            GetGraphStoreDataResponse response = client.getGraphStoreData(
                    new GetGraphStoreDataRequest(project, store, query, 0L, 0xffffffffL));
            printResponse("GetGraphStoreData", response);
            observed = "fields=" + response.getData().getFields() + ", rows=" + response.getData().getValues()
                    + ", truncated=" + response.getMeta().isTruncated();
            if (!response.getMeta().isTruncated() && header.equals(response.getData().getFields())
                    && expected.equals(response.getData().getValues())) {
                System.out.println("Query: " + query);
                System.out.println("Rows: " + response.getData().getValues());
                return;
            }
            Thread.sleep(1000);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("Timed out waiting for query rows: " + expected + "; last " + observed);
    }

    private void deleteStore() throws Exception {
        printResponse("DeleteGraphStore", client.deleteGraphStore(new DeleteGraphStoreRequest(project, store)));
        long deadline = System.nanoTime() + WAIT_NANOS;
        do {
            try {
                GetGraphStoreResponse response = client.getGraphStore(new GetGraphStoreRequest(project, store));
                printResponse("GetGraphStore(deleting)", response);
            } catch (LogException error) {
                // 只有明确的 Store 不存在才表示清理完成，不能把任意 404 都当作删除成功。
                if (error.getHttpCode() != 404 || !"GraphStoreNotExist".equals(error.getErrorCode())) throw error;
                System.out.println("Store absent; requestId=" + error.getRequestId());
                return;
            }
            Thread.sleep(1000);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("Timed out waiting for Store deletion");
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Set environment variable " + name);
        return value;
    }

    private static void printResponse(String operation, Response response) {
        System.out.println(operation + ": requestId=" + response.GetRequestId());
    }

    private static void printFailure(Throwable error) {
        // SDK 异常仅输出状态、错误码和 request ID，避免原始错误正文或堆栈暴露凭据。
        if (error instanceof LogException) {
            LogException serviceError = (LogException) error;
            System.err.println("SDK failure: httpStatus=" + serviceError.getHttpCode()
                    + ", errorCode=" + serviceError.getErrorCode() + ", requestId=" + serviceError.getRequestId());
        } else {
            System.err.println("Sample failure: " + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }
}
