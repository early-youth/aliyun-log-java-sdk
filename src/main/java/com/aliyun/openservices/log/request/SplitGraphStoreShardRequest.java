package com.aliyun.openservices.log.request;

import com.aliyun.openservices.log.http.client.HttpMethod;
import com.alibaba.fastjson.JSONObject;
import java.math.BigInteger;

/** Request for the SplitGraphStoreShard management API. Schemas are logical JSON objects. */
public class SplitGraphStoreShardRequest extends BasicRequest {
    private static final long serialVersionUID = 1L;
    private String storeName;
    private int shardId;
    private BigInteger splitHash;

    public SplitGraphStoreShardRequest(String project, String storeName, int shardId) {
        super(project);
        this.storeName = storeName;
        this.shardId = shardId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public int getShardId() {
        return shardId;
    }

    public void setShardId(int shardId) {
        this.shardId = shardId;
    }

    public BigInteger getSplitHash() {
        return splitHash;
    }

    public void setSplitHash(BigInteger splitHash) {
        this.splitHash = splitHash;
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.POST;
    }

    @Override
    public String getUri() {
        return "/graphstore/" + storeName + "/shards/" + shardId + "/split";
    }

    @Override
    public Object getBody() {
        if (splitHash == null) {
            return null;
        }
        JSONObject body = new JSONObject();
        body.put("splitHash", splitHash);
        return body;
    }
}
