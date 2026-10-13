package com.aliyun.openservices.log.response;

import java.util.Map;
import java.util.List;
import com.aliyun.openservices.log.common.GraphStoreCollectionSchema;

public class ListGraphStoreCollectionSchemasResponse extends Response {
    private static final long serialVersionUID = 1L;
    private final int projectId;
    private final String storeName;
    private final List<GraphStoreCollectionSchema> collectionSchemas;
    private final String nextPageToken;

    public ListGraphStoreCollectionSchemasResponse(Map<String, String> headers, int projectId, String storeName, List<GraphStoreCollectionSchema> collectionSchemas, String nextPageToken) {
        super(headers);
        this.projectId = projectId;
        this.storeName = storeName;
        this.collectionSchemas = collectionSchemas;
        this.nextPageToken = nextPageToken;
    }

    public int getProjectId() {
        return projectId;
    }

    public String getStoreName() {
        return storeName;
    }

    public List<GraphStoreCollectionSchema> getCollectionSchemas() {
        return collectionSchemas;
    }

    public String getNextPageToken() {
        return nextPageToken;
    }
}
