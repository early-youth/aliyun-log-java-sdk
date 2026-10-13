package com.aliyun.openservices.log.common;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** Logical GraphStore schema; physical IDs, roles and versions remain service owned. */
public class GraphStoreVertexLayout implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Required vertex label field name. */
    private String label;
    /** Required vertex ID field name. */
    private String id;
    /** Null omits the key; an explicit empty list is preserved. Defaults and key rules belong to the service. */
    private List<String> ck;
    /** Null omits the key; an explicit empty list is preserved. Defaults and key rules belong to the service. */
    private List<String> pk;
    /** Null omits the key; an explicit empty list is preserved. Defaults and key rules belong to the service. */
    private List<String> routeKey;
    /** Optional typed key index configuration; null omits the field. */
    private Map<String, GraphStoreIndexConfig> keyIndexConfigs;

    public GraphStoreVertexLayout() { }

    public GraphStoreVertexLayout(String label, String id) {
        this.label = label;
        this.id = id;
    }

    /** Fixed, read-only wire name. */
    public String getName() { return "vertex"; }

    /** Required vertex label field name. */
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    /** Required vertex ID field name. */
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    /** Null omits the key; an explicit empty list is preserved. Defaults and key rules belong to the service. */
    public List<String> getCk() { return ck; }
    public void setCk(List<String> ck) { this.ck = ck; }

    /** Null omits the key; an explicit empty list is preserved. Defaults and key rules belong to the service. */
    public List<String> getPk() { return pk; }
    public void setPk(List<String> pk) { this.pk = pk; }

    /** Null omits the key; an explicit empty list is preserved. Defaults and key rules belong to the service. */
    public List<String> getRouteKey() { return routeKey; }
    public void setRouteKey(List<String> routeKey) { this.routeKey = routeKey; }

    /** Optional typed key index configuration; null omits the field. */
    public Map<String, GraphStoreIndexConfig> getKeyIndexConfigs() { return keyIndexConfigs; }
    public void setKeyIndexConfigs(Map<String, GraphStoreIndexConfig> keyIndexConfigs) { this.keyIndexConfigs = keyIndexConfigs; }
}
