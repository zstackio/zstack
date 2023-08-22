package org.zstack.network.service.vip;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.header.configuration.PythonClassInventory;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
public class GetVipFreePortInventory {

    private List<Integer> freeList;

    public List<Integer> getFreeList() {
        return freeList;
    }

    public void setFreeList(List<Integer> freeList) {
        this.freeList = freeList;
    }

}
