package com.aliyun.openservices.log.response;

import java.util.Map;
import com.aliyun.openservices.log.common.GraphStoreCollectionSchema;

public class GetGraphStoreCollectionSchemaResponse extends Response {
    private static final long serialVersionUID = 1L;
    private final int projectId;
    private final String storeName;
    private final GraphStoreCollectionSchema collectionSchema;

    public GetGraphStoreCollectionSchemaResponse(Map<String, String> headers, int projectId, String storeName, GraphStoreCollectionSchema collectionSchema) {
        super(headers);
        this.projectId = projectId;
        this.storeName = storeName;
        this.collectionSchema = collectionSchema;
    }

    public int getProjectId() {
        return projectId;
    }

    public String getStoreName() {
        return storeName;
    }

    public GraphStoreCollectionSchema getCollectionSchema() {
        return collectionSchema;
    }
}
