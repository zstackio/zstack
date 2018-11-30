package org.zstack.zql;

import java.util.List;
import java.util.Map;

public class ZQLQueryReturn {
    public List inventories;
    public Map<Object, Long> inventoryCounts;
    public Long total;
    public Map returnWith;
    public String name;
}
