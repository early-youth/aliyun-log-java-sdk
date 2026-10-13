package com.aliyun.openservices.log.request;

import com.aliyun.openservices.log.http.client.HttpMethod;

/** Request for the ListGraphStoreCollectionSchemas management API. Schemas are logical JSON objects. */
public class ListGraphStoreCollectionSchemasRequest extends BasicRequest {
    private static final long serialVersionUID = 1L;
    private String storeName;

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
        return GetParam("pageSize").isEmpty() ? null : Integer.valueOf(GetParam("pageSize"));
    }

    public void setPageSize(Integer pageSize) {
        if (pageSize == null) GetAllParams().remove("pageSize");
        else SetParam("pageSize", pageSize.toString());
    }

    public String getPageToken() {
        return GetAllParams().get("pageToken");
    }

    public void setPageToken(String pageToken) {
        if (pageToken == null || pageToken.isEmpty()) GetAllParams().remove("pageToken");
        else SetParam("pageToken", pageToken);
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.GET;
    }

    @Override
    public String getUri() {
        return "/graphstore/" + storeName + "/collectionschemas";
    }

}
