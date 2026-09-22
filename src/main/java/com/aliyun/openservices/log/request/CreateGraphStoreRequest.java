package com.aliyun.openservices.log.request;

import com.aliyun.openservices.log.http.client.HttpMethod;
import com.alibaba.fastjson.JSONObject;

/** Request for the CreateGraphStore management API. Schemas are logical JSON objects. */
public class CreateGraphStoreRequest extends BasicRequest {
    private static final long serialVersionUID = 1L;
    private String storeName;
    private int shardCount;
    private JSONObject baseSchema;

    public CreateGraphStoreRequest(String project, String storeName, int shardCount, JSONObject baseSchema) {
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

    public JSONObject getBaseSchema() {
        return baseSchema;
    }

    public void setBaseSchema(JSONObject baseSchema) {
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
        body.put("baseSchema", baseSchema);
        return body;
    }
}
