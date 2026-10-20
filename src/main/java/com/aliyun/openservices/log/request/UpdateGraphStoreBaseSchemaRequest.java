package com.aliyun.openservices.log.request;

import com.aliyun.openservices.log.http.client.HttpMethod;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.log.common.GraphStoreBaseSchemaUpdate;

/** Request for the UpdateGraphStoreBaseSchema management API. Schemas use concrete logical models. */
public class UpdateGraphStoreBaseSchemaRequest extends BasicRequest {
    private static final long serialVersionUID = 1L;
    private String storeName;
    private GraphStoreBaseSchemaUpdate baseSchema;

    public UpdateGraphStoreBaseSchemaRequest(String project, String storeName, GraphStoreBaseSchemaUpdate baseSchema) {
        super(project);
        this.storeName = storeName;
        this.baseSchema = baseSchema;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public GraphStoreBaseSchemaUpdate getBaseSchema() {
        return baseSchema;
    }

    public void setBaseSchema(GraphStoreBaseSchemaUpdate baseSchema) {
        this.baseSchema = baseSchema;
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.PUT;
    }

    @Override
    public String getUri() {
        return "/graphstore/" + storeName + "/baseschema";
    }

    @Override
    public Object getBody() {
        JSONObject body = new JSONObject();
        body.put("value", baseSchema);
        return body;
    }
}
