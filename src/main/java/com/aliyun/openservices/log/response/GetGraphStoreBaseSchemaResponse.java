package com.aliyun.openservices.log.response;

import java.util.Map;
import com.aliyun.openservices.log.common.GraphStoreBaseSchema;

public class GetGraphStoreBaseSchemaResponse extends Response {
    private static final long serialVersionUID = 1L;
    private final int projectId;
    private final String storeName;
    private final long storeId;
    private final GraphStoreBaseSchema baseSchema;

    public GetGraphStoreBaseSchemaResponse(Map<String, String> headers, int projectId, String storeName, long storeId, GraphStoreBaseSchema baseSchema) {
        super(headers);
        this.projectId = projectId;
        this.storeName = storeName;
        this.storeId = storeId;
        this.baseSchema = baseSchema;
    }

    public int getProjectId() {
        return projectId;
    }

    public String getStoreName() {
        return storeName;
    }

    public long getStoreId() {
        return storeId;
    }

    public GraphStoreBaseSchema getBaseSchema() {
        return baseSchema;
    }
}
