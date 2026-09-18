package com.aliyun.openservices.log.common;

import java.io.Serializable;

/** Public GraphStore metadata; schema and lifecycle authority remain on the service. */
public class GraphStore implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int projectId;
    private final String storeName;
    private final String state;
    private final int currentShardCount;

    public GraphStore(int projectId, String storeName, String state, int currentShardCount) {
        this.projectId = projectId;
        this.storeName = storeName;
        this.state = state;
        this.currentShardCount = currentShardCount;
    }

    public int getProjectId() {
        return projectId;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getState() {
        return state;
    }

    public int getCurrentShardCount() {
        return currentShardCount;
    }
}
