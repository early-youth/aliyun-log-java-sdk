package com.aliyun.openservices.log.request;

/** Executes a public GraphStore query; no internal engine options are exposed. */
public class GetGraphStoreDataRequest extends Request {
    private static final long serialVersionUID = 1L;
    private final String storeName;
    private final String query;
    private final long start;
    private final long end;
    private String session;

    public GetGraphStoreDataRequest(String project, String storeName, String query) {
        this(project, storeName, query, 0, 0xffffffffL);
    }

    public GetGraphStoreDataRequest(String project, String storeName, String query, long start, long end) {
        super(project);
        this.storeName = storeName;
        this.query = query;
        this.start = start;
        this.end = end;
    }

    public String getStoreName() { return storeName; }
    public String getQuery() { return query; }
    public long getStart() { return start; }
    public long getEnd() { return end; }
    public String getSession() { return session; }
    public void setSession(String session) { this.session = session; }
}
