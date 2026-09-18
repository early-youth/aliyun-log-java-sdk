package com.aliyun.openservices.log.request;

import com.aliyun.openservices.log.common.Consts.CompressType;
import com.aliyun.openservices.log.common.Logs;

/** Writes a standard LogGroup. Success acknowledges acceptance, not query visibility. */
public class PostGraphStoreDataRequest extends Request {
    private static final long serialVersionUID = 1L;
    private final String storeName;
    private final Logs.LogGroup logGroup;
    private CompressType compressType = CompressType.LZ4;

    public PostGraphStoreDataRequest(String project, String storeName, Logs.LogGroup logGroup) {
        super(project);
        this.storeName = storeName;
        this.logGroup = logGroup;
    }

    public String getStoreName() { return storeName; }
    public Logs.LogGroup getLogGroup() { return logGroup; }
    public CompressType getCompressType() { return compressType; }
    public void setCompressType(CompressType compressType) { this.compressType = compressType; }
}
