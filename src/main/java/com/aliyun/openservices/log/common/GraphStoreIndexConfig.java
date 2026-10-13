package com.aliyun.openservices.log.common;

import java.io.Serializable;

/** Logical GraphStore schema; physical IDs, roles and versions remain service owned. */
public class GraphStoreIndexConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Required; true without a specialized index selects Exact. */
    private Boolean enable;
    /** Optional FullText configuration. */
    private GraphStoreFullTextIndex fullTextIndex;
    /** Optional Range configuration; the service validates index combinations. */
    private GraphStoreRangeIndex rangeIndex;

    public GraphStoreIndexConfig() { }

    public GraphStoreIndexConfig(Boolean enable) {
        this.enable = enable;
    }

    /** Required; true without a specialized index selects Exact. */
    public Boolean getEnable() { return enable; }
    public void setEnable(Boolean enable) { this.enable = enable; }

    /** Optional FullText configuration. */
    public GraphStoreFullTextIndex getFullTextIndex() { return fullTextIndex; }
    public void setFullTextIndex(GraphStoreFullTextIndex fullTextIndex) { this.fullTextIndex = fullTextIndex; }

    /** Optional Range configuration; the service validates index combinations. */
    public GraphStoreRangeIndex getRangeIndex() { return rangeIndex; }
    public void setRangeIndex(GraphStoreRangeIndex rangeIndex) { this.rangeIndex = rangeIndex; }
}
