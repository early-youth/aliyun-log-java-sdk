package com.aliyun.openservices.log.common;

import java.io.Serializable;
import java.util.List;

/** Complete replacement of mutable settings; structural changes are not exposed. */
public class GraphStoreBaseSchemaUpdate implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long ttlSeconds;
    private Boolean indexEnable;
    private GraphStoreFullTextIndex fullTextIndex;
    private List<GraphStoreGraphLayoutUpdate> graphLayouts;

    public Long getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(Long ttlSeconds) { this.ttlSeconds = ttlSeconds; }

    public Boolean getIndexEnable() { return indexEnable; }
    public void setIndexEnable(Boolean indexEnable) { this.indexEnable = indexEnable; }

    public GraphStoreFullTextIndex getFullTextIndex() { return fullTextIndex; }
    public void setFullTextIndex(GraphStoreFullTextIndex fullTextIndex) { this.fullTextIndex = fullTextIndex; }

    public List<GraphStoreGraphLayoutUpdate> getGraphLayouts() { return graphLayouts; }
    public void setGraphLayouts(List<GraphStoreGraphLayoutUpdate> graphLayouts) { this.graphLayouts = graphLayouts; }
}
