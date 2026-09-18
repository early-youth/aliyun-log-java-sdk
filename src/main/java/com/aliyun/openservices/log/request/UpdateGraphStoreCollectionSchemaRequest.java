package com.aliyun.openservices.log.request;

import com.aliyun.openservices.log.http.client.HttpMethod;
import com.alibaba.fastjson.JSONObject;

/** Request for the UpdateGraphStoreCollectionSchema management API. Schemas are logical JSON objects. */
public class UpdateGraphStoreCollectionSchemaRequest extends BasicRequest {
    private static final long serialVersionUID = 1L;
    private String storeName;
    private long collectionSchemaId;
    private JSONObject value;

    public UpdateGraphStoreCollectionSchemaRequest(String project, String storeName, long collectionSchemaId, JSONObject value) {
        super(project);
        this.storeName = storeName;
        this.collectionSchemaId = collectionSchemaId;
        this.value = value;
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

    public JSONObject getValue() {
        return value;
    }

    public void setValue(JSONObject value) {
        this.value = value;
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.PUT;
    }

    @Override
    public String getUri() {
        return "/graphstore/" + storeName + "/collectionschemas/" + collectionSchemaId;
    }

    @Override
    public Object getBody() {
        JSONObject body = new JSONObject();
        body.put("value", value);
        return body;
    }
}
