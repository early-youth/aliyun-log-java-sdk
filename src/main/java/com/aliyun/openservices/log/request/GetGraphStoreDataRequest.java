package com.aliyun.openservices.log.request;

/** Executes a public GraphStore query; no internal engine options are exposed. */
public class GetGraphStoreDataRequest extends Request {
    private static final long serialVersionUID = 1L;
    private final String storeName;
    private final String query;

    public GetGraphStoreDataRequest(String project, String storeName, String query) {
        super(project);
        this.storeName = storeName;
        this.query = query;
    }

    public String getStoreName() { return storeName; }
    public String getQuery() { return query; }
}
