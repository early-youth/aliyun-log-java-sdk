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

## Logical schema contract

All schema inputs and Get/List results are concrete models. This changes source compatibility:
remove raw JSON arguments and migrate custom SDK interface implementations. There is no raw
compatibility overload. HTTP still uses `baseSchema`, `value`, `collectionSchema` and
`collectionSchemas`; BaseSchema's two typed layouts encode as `graphLayouts:[vertex,edge]`.

| Model | Fields and meaning |
| --- | --- |
| BaseSchema | Required timeStateEnable, nonnegative int64 ttlSeconds, indexEnable, vertexLayout and edgeLayout; commonFieldNames defaults to []; optional fullTextIndex. |
| VertexLayout | Fixed read-only name vertex; required label/id **field names**. Optional ck/pk/routeKey and keyIndexConfigs. |
| EdgeLayout | Fixed read-only name edge; required source/target Endpoint and label field name; the same optional keys/index map. |
| Endpoint | Required nonempty label/id field names. |
| CollectionSchema | Required nonempty name/description, graphLayout (vertex/edge), collectionKeyValues and fields arrays. Optional int64 ttlSeconds. |
| CollectionSchemaUpdate | Required name/description and complete replacement fields array; optional ttlSeconds. No layout or CK mutation fields. |
| CollectionKeyValue | Required nonempty name and string value; value may be empty. These are logical constants, not arbitrary binary PB values. |
| FieldSchema | Required nonempty name, type (STRING/INT/DOUBLE/BOOL) and IndexConfig. INT is signed int64. |
| IndexConfig | Required enable; optional FullTextIndex or empty RangeIndex. Enabled with neither selects Exact. |
| FullTextIndex | Required nonempty token string, caseSensitive and chn; optional nonempty optionToken string. |

Base TTL 0 disables expiry. Collection TTL unset/null inherits BaseSchema; 0 disables expiry;
positive values override in seconds. Update is replacement: unset/null TTL restores inheritance.
Required `fields` and `collectionKeyValues` arrays may be empty. Optional keys are omitted when
unset; explicit empty lists remain empty lists. Vertex defaults are ck=[label], pk=[id],
routeKey=[label]; Edge defaults are ck=[source.label,label,target.label],
pk=[source.id,target.id], routeKey=[source.label]. Defaults, valid key projections, index
combinations, token rules, physical IDs/roles/versions and logical-to-physical compilation belong
to the service. The SDK checks required values, empty elements, basic ranges and Update name equality.
Unknown schema fields/enums, wrong known-field types and integer overflow return a parameter or
BadResponse error instead of silently discarding configuration; upgrade the SDK to support new schema fields.

Collection Get/Update/Delete take a string name. Names and pagination tokens remain opaque;
SDK path checks allow safe ASCII characters (including `.`/`@` for collections) and reject `.`/`..`.
The service owns configurable naming limits, the 1000 collection create quota and the 1 MiB
normalized CollectionSchema JSON array cap (BaseSchema excluded).

Models are Java 8 beans with constructors/getters/setters. Required Boolean/Long values use
boxed types so missing values can be rejected; optional fields use null. Create/Update expose
getCollectionSchema/setCollectionSchema; HTTP still wraps them under value. Shared model
objects expand as JSON values without $ref. No arbitrary JSONObject schema input remains.

Store IDs remain long; nullable BigInteger splitHash covers uint64 and distinguishes zero.
Offset/size/pageSize and opaque pageToken share Request's parameter table with SetParam/GetParam.
Typed null setters remove parameters; missing/null nextPageToken means no next page.
GetGraphStore does not recheck shard count, order or uniqueness. Management IDs/counts and Schema
integers reject overflow rather than using truncating getters. BadResponse errors retain the
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
