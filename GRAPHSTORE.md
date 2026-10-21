# GraphStore SDK (Java 8)

Use the existing `LogService/Client` with `common`, `request` and `response` models.
The following method accepts an existing client and throws the native LogException:

```java
import com.aliyun.openservices.log.LogService;
import com.aliyun.openservices.log.common.*;
import com.aliyun.openservices.log.request.*;
import com.aliyun.openservices.log.response.*;
import com.aliyun.openservices.log.exception.LogException;
import java.util.Arrays;
import java.util.Collections;

static void configureGraphStore(LogService client, String project, String store) throws LogException {
    GraphStoreFullTextIndex fullText = new GraphStoreFullTextIndex(" ,", false, false);
    GraphStoreBaseSchema base = new GraphStoreBaseSchema(false, 86400L, true,
        new GraphStoreVertexLayout("vl", "vid"),
        new GraphStoreEdgeLayout(new GraphStoreEndpoint("sl", "sid"), "el", new GraphStoreEndpoint("tl", "tid")));
    base.setFullTextIndex(fullText);
    client.createGraphStore(new CreateGraphStoreRequest(project, store, 2, base));
    GraphStoreIndexConfig full = new GraphStoreIndexConfig(true);
    full.setFullTextIndex(fullText);
    GraphStoreIndexConfig range = new GraphStoreIndexConfig(true);
    range.setRangeIndex(new GraphStoreRangeIndex());
    java.util.List<GraphStoreFieldSchema> fields = Arrays.asList(
        new GraphStoreFieldSchema("text", GraphStoreFieldType.STRING, full),
        new GraphStoreFieldSchema("count", GraphStoreFieldType.INT, range),
        new GraphStoreFieldSchema("exact", GraphStoreFieldType.BOOL, new GraphStoreIndexConfig(true)));
    client.createGraphStoreCollectionSchema(new CreateGraphStoreCollectionSchemaRequest(project, store,
        new GraphStoreCollectionSchema("vertices", "example", GraphStoreLayoutType.VERTEX,
            Arrays.asList(new GraphStoreCollectionKeyValue("vl", "person")), fields)));
    client.createGraphStoreCollectionSchema(new CreateGraphStoreCollectionSchemaRequest(project, store,
        new GraphStoreCollectionSchema("edges", "example", GraphStoreLayoutType.EDGE,
            Arrays.asList(new GraphStoreCollectionKeyValue("sl", "person"),
                new GraphStoreCollectionKeyValue("el", "knows"), new GraphStoreCollectionKeyValue("tl", "person")), fields)));
    // Null TTL restores inheritance; the empty array replaces the property list.
    client.updateGraphStoreCollectionSchema(new UpdateGraphStoreCollectionSchemaRequest(project, store, "vertices",
        new GraphStoreCollectionSchemaUpdate("vertices", "updated", Collections.<GraphStoreFieldSchema>emptyList())));
    GraphStoreBaseSchema read = client.getGraphStoreBaseSchema(new GetGraphStoreBaseSchemaRequest(project, store)).getBaseSchema();
    String vertexIdField = read.getVertexLayout().getId();
    ListGraphStoreCollectionSchemasRequest request = new ListGraphStoreCollectionSchemasRequest(project, store);
    request.setPageSize(100);
    ListGraphStoreCollectionSchemasResponse page = client.listGraphStoreCollectionSchemas(request);
    for (GraphStoreCollectionSchema schema : page.getCollectionSchemas()) { schema.getFields(); }
    // Explicitly set request.setPageToken(page.getNextPageToken()) to request another page.
}
```

## Runnable CRUD sample (Java 8)

[`GraphStoreSample.java`](src/main/java/com/aliyun/openservices/log/sample/GraphStoreSample.java)
is a complete `main` program using one native `Client` in an existing Project. It creates its
own unique `graphstore-sample-*` Store, waits for READY, reads Store/BaseSchema, creates
`person` and `knows` CollectionSchemas, and follows CollectionSchema pagination with
`pageSize=1` and the returned opaque token.

The sample constructs standard `Logs.LogGroup` messages directly in Java. Each Log carries
`__layout__` (`vertex`/`edge`), `__method__` (`create`/`update`/`delete`), and the logical key
and property fields defined by BaseSchema. The person properties are name (STRING) and age
(INT); the edge has a weight (DOUBLE) property set to 1.0. The current Worker requires a
nonempty property payload for non-DELETE events, so this sample includes a real edge
property. An invalid event can cause its entire Worker package to be rejected after the
HTTP ACK; empty-property edge creation is outside this runnable example.
It writes Alice, Bob and a knows edge, verifies exact query results, updates Alice's age from 30 to 31 using the complete property set,
deletes the edge and vertices, and verifies empty results after each deletion. Values such
as INT properties use decimal text; SDK compression is LZ4. Queries use Unix seconds and
this example reads the full `[0,4294967295]` range with TimeState disabled and Base TTL=86400
seconds (one day). Both CollectionSchemas initially inherit the Base TTL by leaving their TTL unset.

It then replaces the person CollectionSchema with all original fields plus email and
TTL=3600, reads it back, deletes both CollectionSchemas, deletes the Store and observes
`404/GraphStoreNotExist`. Store management has no Update method; Split is outside this sample.
No Project is created or deleted.

Set `GRAPHSTORE_ENDPOINT` (including `https://`) and `GRAPHSTORE_PROJECT` to your test
environment. Inject `ALIBABA_CLOUD_ACCESS_KEY_ID` and `ALIBABA_CLOUD_ACCESS_KEY_SECRET`
through your existing credential setup; do not put credentials in source or command arguments.
From this SDK repository root:

```bash
mvn -DskipTests compile dependency:build-classpath \
  -DincludeScope=runtime -Dmdep.outputFile=target/graphstore-sample-classpath.txt
java -cp "target/classes:$(cat target/graphstore-sample-classpath.txt)" \
  com.aliyun.openservices.log.sample.GraphStoreSample
```

The external and internal SDK repositories contain identical sample source and use their own
Maven dependencies/classpath. Do not combine both SDKs in one classpath.
`GRAPHSTORE_SIGNATURE_VERSION` defaults to `v1` and accepts only `v1` or `v4`.
V4 requires an explicit `GRAPHSTORE_REGION`; the sample does not infer it from the endpoint.
Run both versions without changing source:

```bash
GRAPHSTORE_SIGNATURE_VERSION=v1 java -cp "target/classes:$(cat target/graphstore-sample-classpath.txt)" \
  com.aliyun.openservices.log.sample.GraphStoreSample
GRAPHSTORE_SIGNATURE_VERSION=v4 GRAPHSTORE_REGION=cn-wulanchabu-stg-pg2 \
  java -cp "target/classes:$(cat target/graphstore-sample-classpath.txt)" \
  com.aliyun.openservices.log.sample.GraphStoreSample
```

Expected output includes a unique Store name, each operation's request ID, rows
`[[Alice, 30], [Bob, 28]]`, `[[Alice, Bob]]`, updated rows `[[Alice, 31], [Bob, 28]]`,
empty results after deletions, the updated Schema fields/TTL, and
`GraphStore sample passed; Store deletion confirmed.` The process exits nonzero on failure.

READY is control-plane readiness. The sample has one explicit 65-second delay before its
first write for the currently observed stg routing/Schema cache convergence; this is an
environment workaround, not a general SDK readiness guarantee. Readiness/query/deletion
polls allow 120 seconds plus an in-flight request (30-second request timeout); service
errors fail immediately and mutations have no sample-level automatic replay. Query success
requires the expected fields, exact rows and `status=success`; HTTP ACK alone is insufficient.

After a successful Create, failures also attempt to delete this sample's Store, preserving
both the original and cleanup error codes/request IDs. Create timeout/transport/5xx can
leave an unknown result before that cleanup path is entered: use the printed Project/Store
name to inspect and clean up as needed. Likewise inspect that name if cleanup fails. The
Client always shuts down; credentials and raw SDK exception stacks are not printed.

## Logical schema contract

All schema inputs and Get/List results are concrete models. This changes source compatibility:
remove raw JSON arguments and migrate custom SDK interface implementations. There is no raw
compatibility overload. HTTP still uses `baseSchema`, `value`, `collectionSchema` and
`collectionSchemas`; BaseSchema's two typed layouts encode as `graphLayouts:[vertex,edge]`.

| Model | Fields and meaning |
| --- | --- |
| BaseSchema | Required timeStateEnable, positive int64 ttlSeconds, indexEnable, vertexLayout and edgeLayout; commonFieldNames defaults to []; optional fullTextIndex. |
| VertexLayout | Fixed read-only name vertex; required label/id **field names**. Optional ck/pk/routeKey and keyIndexConfigs. |
| EdgeLayout | Fixed read-only name edge; required source/target Endpoint and label field name; the same optional keys/index map. |
| Endpoint | Required nonempty label/id field names. |
| CollectionSchema | Required nonempty name/description, graphLayout (vertex/edge), collectionKeyValues and fields arrays. Optional positive int64 ttlSeconds. |
| CollectionSchemaUpdate | Required name/description and complete replacement fields array; optional positive ttlSeconds. No layout or CK mutation fields. |
| CollectionKeyValue | Required nonempty name and string value; value may be empty. These are logical constants, not arbitrary binary PB values. |
| FieldSchema | Required nonempty name, type (STRING/INT/DOUBLE/BOOL) and IndexConfig. INT is signed int64. |
| IndexConfig | Required enable; optional FullTextIndex or empty RangeIndex. Enabled with neither selects Exact. |
| FullTextIndex | Required nonempty token string, caseSensitive and chn; optional nonempty optionToken string. |

Base TTL must be positive. Collection TTL unset/null inherits BaseSchema; an explicit value
must be positive and overrides the Base TTL in seconds. Zero and negative TTLs are invalid.
Update is replacement: unset/null TTL restores inheritance.
Required `fields` and `collectionKeyValues` arrays may be empty. Optional keys are omitted when
unset; explicit empty lists remain empty lists. Vertex defaults are ck=[label], pk=[id],
routeKey=[label]; Edge defaults are ck=[source.label,label,target.label],
pk=[source.id,target.id], routeKey=[source.label]. Defaults, valid key projections, index
combinations, token rules, physical IDs/roles/versions and logical-to-physical compilation belong
to the service. Schema values use native FastJSON binding; missing members remain null and
unknown members are ignored. Schema validation belongs to the service. Update name equality
is checked when assembling the request path.

Collection Get/Update/Delete take a string name. Names and pagination tokens remain opaque;
SDK path checks allow safe ASCII characters (including `.`/`@` for collections) and reject `.`/`..`.
The service owns configurable naming limits, the 1000 collection create quota and the 1 MiB
normalized CollectionSchema JSON array cap (BaseSchema excluded).

Models are Java 8 beans with constructors/getters/setters. Required Boolean/Long values use
boxed types so missing values remain null. Create/Update expose
getCollectionSchema/setCollectionSchema; HTTP still wraps them under value. Shared model
objects expand as JSON values without $ref. No arbitrary JSONObject schema input remains.

Schema conversion uses native FastJSON bean/collection mapping and the SDK's JSONSerializable
enum pattern for lowercase vertex/edge values. Only BaseSchema's graphLayouts array needs
adaptation. Missing schemas, members and collection-schema lists remain null; commonFieldNames
also remains null when unset. Optional null values are omitted by native HTTP JSON serialization.
Actual JSON parsing/conversion errors retain the existing BadResponse handling.

Store IDs remain long; nullable BigInteger splitHash covers uint64 and distinguishes zero.
Offset/size/pageSize and opaque pageToken share Request's parameter table with SetParam/GetParam.
Typed null setters remove parameters; missing/null nextPageToken means no next page.
GetGraphStore does not recheck shard count, order or uniqueness. Management IDs/counts
reject overflow rather than using truncating getters. BadResponse errors retain the
actual HTTP code, requestId and cause. Native transport/network retry budgets remain unchanged.
For V4, set ClientConfiguration signature version and region before constructing Client.

## Write and query

```java
PostGraphStoreDataRequest write = new PostGraphStoreDataRequest(project, storeName, logGroup);
write.setCompressType(Consts.CompressType.LZ4);
PostGraphStoreDataResponse ack = client.postGraphStoreData(write);
GetGraphStoreDataResponse rows = client.getGraphStoreData(
    new GetGraphStoreDataRequest(project, storeName, "MATCH (n) RETURN n", 1700000000L, 1700001000L));
// rows.getData().getFields(), getValues(), rows.getStatus(), rows.getWarnings(), rows.getStats() (nullable), rows.GetRequestId().
```

Standard LogGroup serialization, exact bytes, checkBodyRawSize and native NONE/LZ4/ZSTD/GZIP
(wire deflate) are preserved. LZ4 defaults; null compression is invalid; valid empty groups
are sent. Writes use sendLogBytes/SendData, queries use SendData; retryDisabled and network
budgets are unchanged. Query start/end are long with closed [start,end], 0 <= start <= end;
the three-argument constructor defaults to [0,4294967295]. Session is optional and opaque.

Query performs ordinary UTF-8 conversion and one FastJSON parse with DisableSpecialKeyDetect.
Additional result fields and unequal row widths are accepted. Absent/null data, fields, values,
status and warnings stay null. Cells and stats use native FastJSON conversions; elapsedMs defaults
to zero, rowCount/peakMemoryBytes use BigInteger without additional range validation.
No private lexer, JSONValidator, strict UTF-8 scan or additional integer conversion policy is
used for query values. Responses support uncompressed/LZ4 via LZ4Encoder; raw size must fit
nonnegative int, with no GraphStore 256 MiB cap or extra compressed-input consumption check.
Real decode/decompression errors, including checked LogException from LZ4Encoder, become
BadResponse with HTTP/requestId/cause, without parser retries or partial rows.

ACK does not promise visibility/durability; timeouts may leave mutation results unknown and
retries may duplicate delivery. Callers own convergence polling. All 14 GraphStore methods
stay on LogService; custom implementations must migrate. Each repository retains its own
Maven coordinates/dependencies/version; no release is implied. Run native SDK tests with a
hermetic selector and exclude functiontest sources from test compilation, then package with
-Dmaven.test.skip=true. The native E2E bridge exposes the same query outcome fields.

查询请求可设置 `resultFormat`：`json` 返回原生 JSON 值，`typed_json` 返回带标签的值；
省略时服务端默认 `json`。使用 `request.setResultFormat("typed_json")` 显式选择 Typed；
样例已显式选择 Typed。SDK 仅序列化参数，合法取值由 Front 校验，两种格式共用响应模型。
Typed 模式原样保留每个值的 `$type/_value` 和嵌套结构。
例如整数 42 是 `{"$type":"Integer","_value":"42"}`，字符串 42 使用 `String` 标签；
Null 使用 `{"$type":"Null","_value":null}`。List、Map、Node、Relationship、Path 和 temporal
沿用底层标签；未知标签同样透传，由消费者按显式类型读取，不根据文本或对象形状猜测。
Java 的 `Data.getValues()` 返回 `List<List<Object>>`，标签和 payload 沿用 FastJSON 的原生对象/数组/字符串。

Query models expose `status`, `warnings`, and `stats` with `rowCount/elapsedMs/peakMemoryBytes`.
The SDK passes status strings through without enum or error-field combination checks. Missing/null
fields use native defaults; stats may be null, empty or partial, and warnings may be null or empty. Statistics require session `stats_enable=true`
and available execution statistics; absent stats stay absent. HTTP 200 with `incomplete` is partial
and must not pass a caller's complete-result check. The old query Meta model is removed.
Query HTTP errors carry `status=error`, `error_code` and `error`; the existing SDK error type
retains the public code/message, HTTP status and request ID. Ordinary LogStore errors keep their
existing format and retry behavior.

## BaseSchema update

`UpdateGraphStoreBaseSchema` (Java: `updateGraphStoreBaseSchema`) sends
`PUT /graphstore/{storeName}/baseschema` with a `GraphStoreBaseSchemaUpdate` under `value`.
The model contains positive `ttlSeconds`, `indexEnable`, optional `fullTextIndex`, and
`graphLayouts` containing one vertex and one edge `GraphStoreGraphLayoutUpdate`. Each
layout has only `name` and a required `keyIndexConfigs` map. Use an empty map to clear
explicit key index settings; false is preserved. In Go, use a non-nil empty map.
This is complete replacement of mutable settings, without structural mutation or CAS.
The service validates keys, reserved names, TTL and index combinations. Success has
an empty body and does not promise immediate convergence or historical reindexing.
