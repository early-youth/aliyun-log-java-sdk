package com.aliyun.openservices.log.response;

import java.util.Map;
import java.util.List;
import com.aliyun.openservices.log.common.GraphStore;

public class ListGraphStoresResponse extends Response {
    private static final long serialVersionUID = 1L;
    private final List<GraphStore> stores;
    private final long count;
    private final long total;

    public ListGraphStoresResponse(Map<String, String> headers, List<GraphStore> stores, long count, long total) {
        super(headers);
        this.stores = stores;
        this.count = count;
        this.total = total;
    }

    public List<GraphStore> getStores() {
        return stores;
    }

    public long getCount() {
        return count;
    }

    public long getTotal() {
        return total;
    }
}
