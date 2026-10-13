package com.aliyun.openservices.log.request;

import com.aliyun.openservices.log.http.client.HttpMethod;

/** Request for the ListGraphStores management API. Schemas are logical JSON objects. */
public class ListGraphStoresRequest extends BasicRequest {
    private static final long serialVersionUID = 1L;

    public ListGraphStoresRequest(String project) {
        super(project);

    }

    public Integer getOffset() {
        return GetParam("offset").isEmpty() ? null : Integer.valueOf(GetParam("offset"));
    }

    public void setOffset(Integer offset) {
        if (offset == null) GetAllParams().remove("offset");
        else SetParam("offset", offset.toString());
    }

    public Integer getSize() {
        return GetParam("size").isEmpty() ? null : Integer.valueOf(GetParam("size"));
    }

    public void setSize(Integer size) {
        if (size == null) GetAllParams().remove("size");
        else SetParam("size", size.toString());
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.GET;
    }

    @Override
    public String getUri() {
        return "/graphstore";
    }

}
