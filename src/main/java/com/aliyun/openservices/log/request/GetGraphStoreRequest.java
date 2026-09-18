package com.aliyun.openservices.log.request;

import com.aliyun.openservices.log.http.client.HttpMethod;

/** Request for the GetGraphStore management API. Schemas are logical JSON objects. */
public class GetGraphStoreRequest extends BasicRequest {
    private static final long serialVersionUID = 1L;
    private String storeName;

    public GetGraphStoreRequest(String project, String storeName) {
        super(project);
        this.storeName = storeName;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.GET;
    }

    @Override
    public String getUri() {
        return "/graphstore/" + storeName;
    }
}
