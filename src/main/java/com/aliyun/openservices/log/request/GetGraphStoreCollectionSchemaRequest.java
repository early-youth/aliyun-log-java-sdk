package com.aliyun.openservices.log.request;

import com.aliyun.openservices.log.http.client.HttpMethod;

/** Request for the GetGraphStoreCollectionSchema management API. Schemas are logical JSON objects. */
public class GetGraphStoreCollectionSchemaRequest extends BasicRequest {
    private static final long serialVersionUID = 1L;
    private String storeName;
    private String collectionSchemaName;

    public GetGraphStoreCollectionSchemaRequest(String project, String storeName, String collectionSchemaName) {
        super(project);
        this.storeName = storeName;
        this.collectionSchemaName = collectionSchemaName;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getCollectionSchemaName() {
        return collectionSchemaName;
    }

    public void setCollectionSchemaName(String collectionSchemaName) {
        this.collectionSchemaName = collectionSchemaName;
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.GET;
    }

    @Override
    public String getUri() {
        return "/graphstore/" + storeName + "/collectionschemas/" + collectionSchemaName;
    }
}
