package com.aliyun.openservices.log.common;

import java.io.Serializable;
import java.util.List;

/** Logical GraphStore schema; physical IDs, roles and versions remain service owned. */
public class GraphStoreCollectionSchema implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Required nonempty logical collection name; no numeric collection ID is exposed. */
    private String name;
    /** Required nonempty description. */
    private String description;
    /** Required fixed vertex or edge layout. */
    private GraphStoreLayoutType graphLayout;
    /** Required complete user CK values; system CK values belong to the service. */
    private List<GraphStoreCollectionKeyValue> collectionKeyValues;
    /** Null inherits BaseSchema; explicit seconds must be positive. */
    private Long ttlSeconds;
    /** Required property array; an empty array is valid. */
    private List<GraphStoreFieldSchema> fields;

    public GraphStoreCollectionSchema() { }

    public GraphStoreCollectionSchema(String name, String description, GraphStoreLayoutType graphLayout, List<GraphStoreCollectionKeyValue> collectionKeyValues, List<GraphStoreFieldSchema> fields) {
        this.name = name;
        this.description = description;
        this.graphLayout = graphLayout;
        this.collectionKeyValues = collectionKeyValues;
        this.fields = fields;
    }

    /** Required nonempty logical collection name; no numeric collection ID is exposed. */
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    /** Required nonempty description. */
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    /** Required fixed vertex or edge layout. */
    public GraphStoreLayoutType getGraphLayout() { return graphLayout; }
    public void setGraphLayout(GraphStoreLayoutType graphLayout) { this.graphLayout = graphLayout; }

    /** Required complete user CK values; system CK values belong to the service. */
    public List<GraphStoreCollectionKeyValue> getCollectionKeyValues() { return collectionKeyValues; }
    public void setCollectionKeyValues(List<GraphStoreCollectionKeyValue> collectionKeyValues) { this.collectionKeyValues = collectionKeyValues; }

    /** Null inherits BaseSchema; explicit seconds must be positive. */
    public Long getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(Long ttlSeconds) { this.ttlSeconds = ttlSeconds; }

    /** Required property array; an empty array is valid. */
    public List<GraphStoreFieldSchema> getFields() { return fields; }
    public void setFields(List<GraphStoreFieldSchema> fields) { this.fields = fields; }
}
