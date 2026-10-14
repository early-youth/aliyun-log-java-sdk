package com.aliyun.openservices.log.common;

import java.io.Serializable;
import java.util.List;

/** Logical GraphStore schema; physical IDs, roles and versions remain service owned. */
public class GraphStoreBaseSchema implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Required time state setting. */
    private Boolean timeStateEnable;
    /** Required nonnegative seconds; zero disables expiry. */
    private Long ttlSeconds;
    /** Optional common property names; unset values remain null. */
    private List<String> commonFieldNames;
    /** Required global index setting; configuration combinations are validated by the service. */
    private Boolean indexEnable;
    /** Optional global FullText configuration. */
    private GraphStoreFullTextIndex fullTextIndex;
    /** Required vertex layout; encoded within graphLayouts. */
    private GraphStoreVertexLayout vertexLayout;
    /** Required edge layout; encoded within graphLayouts. */
    private GraphStoreEdgeLayout edgeLayout;

    public GraphStoreBaseSchema() { }

    public GraphStoreBaseSchema(Boolean timeStateEnable, Long ttlSeconds, Boolean indexEnable, GraphStoreVertexLayout vertexLayout, GraphStoreEdgeLayout edgeLayout) {
        this.timeStateEnable = timeStateEnable;
        this.ttlSeconds = ttlSeconds;
        this.indexEnable = indexEnable;
        this.vertexLayout = vertexLayout;
        this.edgeLayout = edgeLayout;
    }

    /** Required time state setting. */
    public Boolean getTimeStateEnable() { return timeStateEnable; }
    public void setTimeStateEnable(Boolean timeStateEnable) { this.timeStateEnable = timeStateEnable; }

    /** Required nonnegative seconds; zero disables expiry. */
    public Long getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(Long ttlSeconds) { this.ttlSeconds = ttlSeconds; }

    /** Optional common property names; unset values remain null. */
    public List<String> getCommonFieldNames() { return commonFieldNames; }
    public void setCommonFieldNames(List<String> commonFieldNames) { this.commonFieldNames = commonFieldNames; }

    /** Required global index setting; configuration combinations are validated by the service. */
    public Boolean getIndexEnable() { return indexEnable; }
    public void setIndexEnable(Boolean indexEnable) { this.indexEnable = indexEnable; }

    /** Optional global FullText configuration. */
    public GraphStoreFullTextIndex getFullTextIndex() { return fullTextIndex; }
    public void setFullTextIndex(GraphStoreFullTextIndex fullTextIndex) { this.fullTextIndex = fullTextIndex; }

    /** Required vertex layout; encoded within graphLayouts. */
    public GraphStoreVertexLayout getVertexLayout() { return vertexLayout; }
    public void setVertexLayout(GraphStoreVertexLayout vertexLayout) { this.vertexLayout = vertexLayout; }

    /** Required edge layout; encoded within graphLayouts. */
    public GraphStoreEdgeLayout getEdgeLayout() { return edgeLayout; }
    public void setEdgeLayout(GraphStoreEdgeLayout edgeLayout) { this.edgeLayout = edgeLayout; }
}
