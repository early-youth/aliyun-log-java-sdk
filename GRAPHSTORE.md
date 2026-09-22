# GraphStore SDK (Java 8)

Use the existing LogService/Client and request/response packages.

```java
GetGraphStoreResponse store = client.getGraphStore(new GetGraphStoreRequest(project, storeName));
GetGraphStoreBaseSchemaResponse base =
    client.getGraphStoreBaseSchema(new GetGraphStoreBaseSchemaRequest(project, storeName));
ListGraphStoreCollectionSchemasRequest request =
    new ListGraphStoreCollectionSchemasRequest(project, storeName);
request.setPageSize(100);
ListGraphStoreCollectionSchemasResponse page = client.listGraphStoreCollectionSchemas(request);
// For another page, explicitly set request.setPageToken(page.getNextPageToken()).
```

Each of the 11 actions takes one BasicRequest subtype and returns its Response subtype.
CreateGraphStoreRequest takes project, storeName, shardCount and a logical JSONObject.
Collection Create/Update take a logical JSONObject; the SDK wraps it as value.
Get/List retain opaque JSONObject schemas; storeId/collectionSchemaId are long.
Nullable BigInteger splitHash supports the entire unsigned 64-bit range; null differs from zero.
Nullable Integer offset/size/pageSize omit the corresponding parameter.

Response.GetRequestId() and LogException preserve native metadata. The existing network retry
budget is unchanged; no GraphStore retry, polling or automatic pagination is added.
Timeouts may leave mutation results unknown.
For V4 on this baseline, set ClientConfiguration's signature version and region before constructing
Client; Client.setSignatureVersion/setRegion do not rebuild an already-created signer.

Compatibility: LogService adds 13 abstract methods (11 management, write and query). Third-party implementations must implement
them. No alternative interface or adapter is added. Maven coordinates/version stay at the source
baseline (com.aliyun.openservices:aliyun-log:0.6.164); no public release is implied.
Console integration is outside this change.

## Write and query

```java
PostGraphStoreDataRequest write = new PostGraphStoreDataRequest(project, storeName, logGroup);
// logGroup is the existing Logs.LogGroup; use protobuf ByteString for exact binary values.
write.setCompressType(Consts.CompressType.LZ4);
PostGraphStoreDataResponse ack = client.postGraphStoreData(write);
GetGraphStoreDataResponse rows = client.getGraphStoreData(
    new GetGraphStoreDataRequest(project, storeName, "MATCH (n) RETURN n", 1700000000L, 1700001000L));
// Optional opaque session: request.setSession("opaque").
// rows.getData().getFields(), rows.getData().getValues(), rows.getMeta(), rows.GetRequestId().
// Each cell is String or null; complex graph values remain strings.
```

The requests extend the existing Request. Write returns a normal Response with headers/requestId,
not a byte-count field. LZ4 is the default; NONE, ZSTD and GZIP (wire name deflate) are the
existing LogStore algorithms. Null compression is invalid. Valid empty groups are still sent.
The standard PB is serialized once and checked by the existing checkBodyRawSize before
compression; neither existing size thresholds nor validation order is changed.
Write uses sendLogBytes/SendData, including native direct-mode/network retries and retryDisabled;
query uses SendData, including its transport retry budget. No GraphStore retry loop is added.
An ACK does not imply query visibility or durability; timeout results may be unknown and
retrying can duplicate delivery. The caller owns query convergence polling.

GetGraphStore returns getShards() with ordered unique signed-int32 shardId; List keeps summary items.
Query requires from/to long values in uint32 range, from < to, with [from,to) semantics.
It returns data.fields/values plus meta (signed-long elapsedMs, truncated and warnings).
Old header/rowCount/peakMemoryBytes/resultBytes are not accepted. Missing/null/mistyped fields, row width mismatches,
invalid UTF-8/JSON or compressed responses fail as BadResponse with HTTP code/requestId,
without partial rows or parser retries. Responses support uncompressed or LZ4 only, with a
256 MiB raw-size check before allocation. The new response boundary restricts FastJSON's
permissive syntax locally; management and global parser/transport settings are unchanged.

Run the hermetic unit tests using Maven's -Dtest selector, then mvn -DskipTests package.
Unrestricted mvn test also includes online function tests and requires their environment.
GraphStoreManagementE2ETool in the existing functiontest tree runs one native management SDK operation for
the GraphStore E2E Runner. It reads credentials only from GRAPHSTORE_E2E_ACCESS_KEY_ID/
GRAPHSTORE_E2E_ACCESS_KEY_SECRET. The Runner owns the lifecycle, assertions and cleanup.
It exposes all 13 management, write and query operations through the same Runner.
