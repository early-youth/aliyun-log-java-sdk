package com.aliyun.openservices.log.response;

import java.util.Map;
import com.aliyun.openservices.log.common.GraphStore;

public class GetGraphStoreResponse extends Response {
    private static final long serialVersionUID = 1L;
    private final GraphStore graphStore;

    public GetGraphStoreResponse(Map<String, String> headers, GraphStore graphStore) {
        super(headers);
        this.graphStore = graphStore;
    }

    public GraphStore getGraphStore() {
        return graphStore;
    }
}
