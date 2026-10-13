package com.aliyun.openservices.log.request;

import com.aliyun.openservices.log.http.client.HttpMethod;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.log.common.GraphStoreCollectionSchemaUpdate;
import com.aliyun.openservices.log.internal.GraphStoreSchemaCodec;

/** Request for the UpdateGraphStoreCollectionSchema management API. Schemas use concrete logical models. */
public class UpdateGraphStoreCollectionSchemaRequest extends BasicRequest {
    private static final long serialVersionUID = 1L;
    private String storeName;
    private String collectionSchemaName;
    private GraphStoreCollectionSchemaUpdate collectionSchema;

    public UpdateGraphStoreCollectionSchemaRequest(String project, String storeName, String collectionSchemaName, GraphStoreCollectionSchemaUpdate collectionSchema) {
        super(project);
        this.storeName = storeName;
        this.collectionSchemaName = collectionSchemaName;
        this.collectionSchema = collectionSchema;
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

    public GraphStoreCollectionSchemaUpdate getCollectionSchema() {
        return collectionSchema;
    }

    public void setCollectionSchema(GraphStoreCollectionSchemaUpdate collectionSchema) {
        this.collectionSchema = collectionSchema;
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.PUT;
    }

    @Override
    public String getUri() {
        return "/graphstore/" + storeName + "/collectionschemas/" + collectionSchemaName;
    }

    @Override
    public Object getBody() {
        JSONObject body = new JSONObject();
        body.put("value", GraphStoreSchemaCodec.encode(collectionSchema));
        return body;
    }
}
