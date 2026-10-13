package com.aliyun.openservices.log.request;

import com.aliyun.openservices.log.http.client.HttpMethod;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.log.common.GraphStoreCollectionSchema;
import com.aliyun.openservices.log.internal.GraphStoreSchemaCodec;

/** Request for the CreateGraphStoreCollectionSchema management API. Schemas use concrete logical models. */
public class CreateGraphStoreCollectionSchemaRequest extends BasicRequest {
    private static final long serialVersionUID = 1L;
    private String storeName;
    private GraphStoreCollectionSchema collectionSchema;

    public CreateGraphStoreCollectionSchemaRequest(String project, String storeName, GraphStoreCollectionSchema collectionSchema) {
        super(project);
        this.storeName = storeName;
        this.collectionSchema = collectionSchema;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public GraphStoreCollectionSchema getCollectionSchema() {
        return collectionSchema;
    }

    public void setCollectionSchema(GraphStoreCollectionSchema collectionSchema) {
        this.collectionSchema = collectionSchema;
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.POST;
    }

    @Override
    public String getUri() {
        return "/graphstore/" + storeName + "/collectionschemas";
    }

    @Override
    public Object getBody() {
        JSONObject body = new JSONObject();
        body.put("value", GraphStoreSchemaCodec.encode(collectionSchema));
        return body;
    }
}
