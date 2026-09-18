package com.aliyun.openservices.log.request;

import com.aliyun.openservices.log.http.client.HttpMethod;

/** Request for the DeleteGraphStoreCollectionSchema management API. Schemas are logical JSON objects. */
public class DeleteGraphStoreCollectionSchemaRequest extends BasicRequest {
    private static final long serialVersionUID = 1L;
    private String storeName;
    private long collectionSchemaId;

    public DeleteGraphStoreCollectionSchemaRequest(String project, String storeName, long collectionSchemaId) {
        super(project);
        this.storeName = storeName;
        this.collectionSchemaId = collectionSchemaId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public long getCollectionSchemaId() {
        return collectionSchemaId;
    }

    public void setCollectionSchemaId(long collectionSchemaId) {
        this.collectionSchemaId = collectionSchemaId;
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.DELETE;
    }

    @Override
    public String getUri() {
        return "/graphstore/" + storeName + "/collectionschemas/" + collectionSchemaId;
    }
}
