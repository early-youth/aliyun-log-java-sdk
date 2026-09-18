package com.aliyun.openservices.log.response;

import java.util.Map;

/** A normal write acknowledgement with the native response headers and request ID. */
public class PostGraphStoreDataResponse extends Response {
    private static final long serialVersionUID = 1L;

    public PostGraphStoreDataResponse(Map<String, String> headers) {
        super(headers);
    }
}
