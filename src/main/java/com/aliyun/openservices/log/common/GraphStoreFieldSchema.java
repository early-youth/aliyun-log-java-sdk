package com.aliyun.openservices.log.common;

import java.io.Serializable;

/** Logical GraphStore schema; physical IDs, roles and versions remain service owned. */
public class GraphStoreFieldSchema implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Required nonempty property field name. */
    private String name;
    /** Required property type; INT is signed int64. */
    private GraphStoreFieldType type;
    /** Required field index configuration. */
    private GraphStoreIndexConfig indexConfig;

    public GraphStoreFieldSchema() { }

    public GraphStoreFieldSchema(String name, GraphStoreFieldType type, GraphStoreIndexConfig indexConfig) {
        this.name = name;
        this.type = type;
        this.indexConfig = indexConfig;
    }

    /** Required nonempty property field name. */
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    /** Required property type; INT is signed int64. */
    public GraphStoreFieldType getType() { return type; }
    public void setType(GraphStoreFieldType type) { this.type = type; }

    /** Required field index configuration. */
    public GraphStoreIndexConfig getIndexConfig() { return indexConfig; }
    public void setIndexConfig(GraphStoreIndexConfig indexConfig) { this.indexConfig = indexConfig; }
}
