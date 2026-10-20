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
requires the expected fields, exact rows and `truncated=false`; HTTP ACK alone is insufficient.

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
// rows.getData().getFields(), getValues(), rows.getMeta(), rows.GetRequestId().
```

Standard LogGroup serialization, exact bytes, checkBodyRawSize and native NONE/LZ4/ZSTD/GZIP
(wire deflate) are preserved. LZ4 defaults; null compression is invalid; valid empty groups
are sent. Writes use sendLogBytes/SendData, queries use SendData; retryDisabled and network
budgets are unchanged. Query start/end are long with closed [start,end], 0 <= start <= end;
the three-argument constructor defaults to [0,4294967295]. Session is optional and opaque.

Query performs ordinary UTF-8 conversion and one FastJSON parse with DisableSpecialKeyDetect.
Additional result fields and unequal row widths are accepted. Required data/meta structures
remain required; cells/elapsedMs/truncated use native FastJSON getters, as LogStore does.
No private lexer, JSONValidator, strict UTF-8 scan or additional integer conversion policy is
used for query values. Responses support uncompressed/LZ4 via LZ4Encoder; raw size must fit
nonnegative int, with no GraphStore 256 MiB cap or extra compressed-input consumption check.
Real decode/decompression errors, including checked LogException from LZ4Encoder, become
BadResponse with HTTP/requestId/cause, without parser retries or partial rows.

ACK does not promise visibility/durability; timeouts may leave mutation results unknown and
retries may duplicate delivery. Callers own convergence polling. All 13 GraphStore methods
stay on LogService; custom implementations must migrate. Each repository retains its own
Maven coordinates/dependencies/version; no release is implied. Run native SDK tests with a
hermetic selector and exclude functiontest sources from test compilation, then package with
-Dmaven.test.skip=true. E2E tools are outside this refinement and need separate migration.

查询结果保留 JSON 原生类型：数字、布尔、数组、对象、null 和字符串；路径为数组，实体为对象。
业务属性中的 JSON 文本仍是字符串，不递归解析。Java 的 `Data.getValues()` 返回 `List<List<Object>>`，数字/对象/数组沿用 FastJSON 的原生类型。
