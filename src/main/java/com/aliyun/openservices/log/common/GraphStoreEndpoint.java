package com.aliyun.openservices.log.common;

import java.io.Serializable;

/** Logical GraphStore schema; physical IDs, roles and versions remain service owned. */
public class GraphStoreEndpoint implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Required source or target label field name. */
    private String label;
    /** Required source or target ID field name. */
    private String id;

    public GraphStoreEndpoint() { }

    public GraphStoreEndpoint(String label, String id) {
        this.label = label;
        this.id = id;
    }

    /** Required source or target label field name. */
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    /** Required source or target ID field name. */
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
