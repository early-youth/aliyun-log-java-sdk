package com.aliyun.openservices.log.request;

/** Executes a public GraphStore query; no internal engine options are exposed. */
public class GetGraphStoreDataRequest extends Request {
    private static final long serialVersionUID = 1L;
    private final String storeName;
    private final String query;
    private final long from;
    private final long to;
    private String session;

    public GetGraphStoreDataRequest(String project, String storeName, String query, long from, long to) {
        super(project);
        this.storeName = storeName;
        this.query = query;
        this.from = from;
        this.to = to;
    }

    public String getStoreName() { return storeName; }
    public String getQuery() { return query; }
    public long getFrom() { return from; }
    public long getTo() { return to; }
    public String getSession() { return session; }
    public void setSession(String session) { this.session = session; }
}
