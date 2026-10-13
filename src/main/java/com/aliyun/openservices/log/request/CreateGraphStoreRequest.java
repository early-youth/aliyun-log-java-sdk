package com.aliyun.openservices.log.request;

import com.aliyun.openservices.log.http.client.HttpMethod;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.log.common.GraphStoreBaseSchema;
import com.aliyun.openservices.log.internal.GraphStoreSchemaCodec;

/** Request for the CreateGraphStore management API. Schemas use concrete logical models. */
public class CreateGraphStoreRequest extends BasicRequest {
    private static final long serialVersionUID = 1L;
    private String storeName;
    private int shardCount;
    private GraphStoreBaseSchema baseSchema;

    public CreateGraphStoreRequest(String project, String storeName, int shardCount, GraphStoreBaseSchema baseSchema) {
        super(project);
        this.storeName = storeName;
        this.shardCount = shardCount;
        this.baseSchema = baseSchema;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public int getShardCount() {
        return shardCount;
    }

    public void setShardCount(int shardCount) {
        this.shardCount = shardCount;
    }

    public GraphStoreBaseSchema getBaseSchema() {
        return baseSchema;
    }

    public void setBaseSchema(GraphStoreBaseSchema baseSchema) {
        this.baseSchema = baseSchema;
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.POST;
    }

    @Override
    public String getUri() {
        return "/graphstore";
    }

    @Override
    public Object getBody() {
        JSONObject body = new JSONObject();
        body.put("storeName", storeName);
        body.put("shardCount", shardCount);
        body.put("baseSchema", GraphStoreSchemaCodec.encode(baseSchema));
        return body;
    }
}
