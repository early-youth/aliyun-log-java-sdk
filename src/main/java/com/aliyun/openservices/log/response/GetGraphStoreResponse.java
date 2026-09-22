package com.aliyun.openservices.log.response;

import java.util.Map;
import java.util.List;
import com.aliyun.openservices.log.common.GraphStore;
import com.aliyun.openservices.log.common.GraphStoreShardInfo;

public class GetGraphStoreResponse extends Response {
    private static final long serialVersionUID = 1L;
    private final GraphStore graphStore;
    private final List<GraphStoreShardInfo> shards;

    public GetGraphStoreResponse(Map<String, String> headers, GraphStore graphStore, List<GraphStoreShardInfo> shards) {
        super(headers);
        this.graphStore = graphStore;
        this.shards = shards;
    }

    public GraphStore getGraphStore() {
        return graphStore;
    }
    public List<GraphStoreShardInfo> getShards() { return shards; }
}
