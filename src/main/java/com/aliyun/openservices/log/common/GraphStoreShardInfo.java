package com.aliyun.openservices.log.common;

import java.io.Serializable;

/** A shard from the committed GraphStore routing snapshot. */
public class GraphStoreShardInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int shardId;

    public GraphStoreShardInfo(int shardId) { this.shardId = shardId; }
    public int getShardId() { return shardId; }
}
