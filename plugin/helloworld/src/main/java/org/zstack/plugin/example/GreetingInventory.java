package org.zstack.plugin.example;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = GreetingVO.class)
public class GreetingInventory {
    private String uuid;
    private String greeting;
    private Timestamp lastOpDate;
    private Timestamp createDate;

    public static GreetingInventory valueOf(GreetingVO vo) {
        GreetingInventory inv = new GreetingInventory();
        inv.uuid = vo.getUuid();
        inv.greeting = vo.getGreeting();
        inv.lastOpDate = vo.getLastOpDate();
        inv.createDate = vo.getCreateDate();
        return inv;
    }

    public static List<GreetingInventory> valueOf(Collection<GreetingVO> vos) {
        return vos.stream().map(GreetingInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}
