package org.zstack.zql;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ZQLQueryReturn {
    public List inventories;
    public LinkedHashMap<Object, Long> inventoryCounts;
    public Long total;
    public Map returnWith;
    public String name;
}
