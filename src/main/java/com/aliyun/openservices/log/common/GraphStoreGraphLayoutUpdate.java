package com.aliyun.openservices.log.common;

import java.io.Serializable;
import java.util.Map;

/** Complete replacement of mutable settings; structural changes are not exposed. */
public class GraphStoreGraphLayoutUpdate implements Serializable {
    private static final long serialVersionUID = 1L;
    private GraphStoreLayoutType name;
    private Map<String, GraphStoreIndexConfig> keyIndexConfigs;

    public GraphStoreLayoutType getName() { return name; }
    public void setName(GraphStoreLayoutType name) { this.name = name; }

    public Map<String, GraphStoreIndexConfig> getKeyIndexConfigs() { return keyIndexConfigs; }
    public void setKeyIndexConfigs(Map<String, GraphStoreIndexConfig> keyIndexConfigs) { this.keyIndexConfigs = keyIndexConfigs; }
}
