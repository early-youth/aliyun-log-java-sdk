package com.aliyun.openservices.log.common;

import java.io.Serializable;
import java.util.List;

/** Logical GraphStore schema; physical IDs, roles and versions remain service owned. */
public class GraphStoreCollectionSchemaUpdate implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Required nonempty name, identical to the request path. */
    private String name;
    /** Required nonempty replacement description. */
    private String description;
    /** Null restores inheritance; zero disables expiry; this is replacement, not patch. */
    private Long ttlSeconds;
    /** Required complete replacement property array; an empty array is valid. */
    private List<GraphStoreFieldSchema> fields;

    public GraphStoreCollectionSchemaUpdate() { }

    public GraphStoreCollectionSchemaUpdate(String name, String description, List<GraphStoreFieldSchema> fields) {
        this.name = name;
        this.description = description;
        this.fields = fields;
    }

    /** Required nonempty name, identical to the request path. */
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    /** Required nonempty replacement description. */
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    /** Null restores inheritance; zero disables expiry; this is replacement, not patch. */
    public Long getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(Long ttlSeconds) { this.ttlSeconds = ttlSeconds; }

    /** Required complete replacement property array; an empty array is valid. */
    public List<GraphStoreFieldSchema> getFields() { return fields; }
    public void setFields(List<GraphStoreFieldSchema> fields) { this.fields = fields; }
}
