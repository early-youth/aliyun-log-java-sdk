package com.aliyun.openservices.log.response;

import java.util.Map;
import com.alibaba.fastjson.JSONObject;

public class GetGraphStoreCollectionSchemaResponse extends Response {
    private static final long serialVersionUID = 1L;
    private final int projectId;
    private final String storeName;
    private final JSONObject collectionSchema;

    public GetGraphStoreCollectionSchemaResponse(Map<String, String> headers, int projectId, String storeName, JSONObject collectionSchema) {
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

    public JSONObject getCollectionSchema() {
        return collectionSchema;
    }
}
