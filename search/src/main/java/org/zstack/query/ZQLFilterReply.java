package org.zstack.query;

import java.util.List;

public interface ZQLFilterReply {
    List<String> getFilterResources();

    void setFilteredResult(List result);

    String getInventoryName();
}
