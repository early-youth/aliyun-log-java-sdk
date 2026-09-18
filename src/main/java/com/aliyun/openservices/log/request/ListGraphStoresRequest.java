package com.aliyun.openservices.log.request;

import com.aliyun.openservices.log.http.client.HttpMethod;
import java.util.HashMap;
import java.util.Map;

/** Request for the ListGraphStores management API. Schemas are logical JSON objects. */
public class ListGraphStoresRequest extends BasicRequest {
    private static final long serialVersionUID = 1L;
    private Integer offset;
    private Integer size;

    public ListGraphStoresRequest(String project) {
        super(project);

    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.GET;
    }

    @Override
    public String getUri() {
        return "/graphstore";
    }

    @Override
    public Map<String, String> GetAllParams() {
        Map<String, String> parameters = new HashMap<String, String>(super.GetAllParams());
        if (offset == null) {
            parameters.remove("offset");
        } else {
            parameters.put("offset", offset.toString());
        }
        if (size == null) {
            parameters.remove("size");
        } else {
            parameters.put("size", size.toString());
        }
        return parameters;
    }
}
