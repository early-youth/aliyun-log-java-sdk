package com.aliyun.openservices.log.request;

import com.aliyun.openservices.log.http.client.HttpMethod;
import com.alibaba.fastjson.JSONObject;

/** Request for the CreateGraphStoreCollectionSchema management API. Schemas are logical JSON objects. */
public class CreateGraphStoreCollectionSchemaRequest extends BasicRequest {
    private static final long serialVersionUID = 1L;
    private String storeName;
    private JSONObject value;

    public CreateGraphStoreCollectionSchemaRequest(String project, String storeName, JSONObject value) {
        super(project);
        this.storeName = storeName;
        this.value = value;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public JSONObject getValue() {
        return value;
    }

    public void setValue(JSONObject value) {
        this.value = value;
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
        body.put("value", value);
        return body;
    }
}
