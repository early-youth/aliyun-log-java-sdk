package com.aliyun.openservices.log.request;

import com.aliyun.openservices.log.http.client.HttpMethod;
import java.util.HashMap;
import java.util.Map;

/** Request for the ListGraphStoreCollectionSchemas management API. Schemas are logical JSON objects. */
public class ListGraphStoreCollectionSchemasRequest extends BasicRequest {
    private static final long serialVersionUID = 1L;
    private String storeName;
    private Integer pageSize;
    private String pageToken;

    public ListGraphStoreCollectionSchemasRequest(String project, String storeName) {
        super(project);
        this.storeName = storeName;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getPageToken() {
        return pageToken;
    }

    public void setPageToken(String pageToken) {
        this.pageToken = pageToken;
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.GET;
    }

    @Override
    public String getUri() {
        return "/graphstore/" + storeName + "/collectionschemas";
    }

    @Override
    public Map<String, String> GetAllParams() {
        Map<String, String> parameters = new HashMap<String, String>(super.GetAllParams());
        if (pageSize == null) {
            parameters.remove("pageSize");
        } else {
            parameters.put("pageSize", pageSize.toString());
        }
        if (pageToken == null || pageToken.isEmpty()) {
            parameters.remove("pageToken");
        } else {
            parameters.put("pageToken", pageToken.toString());
        }
        return parameters;
    }
}
