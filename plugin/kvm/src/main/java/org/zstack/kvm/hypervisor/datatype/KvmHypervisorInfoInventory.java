package org.zstack.kvm.hypervisor.datatype;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Wenhao.Zhang on 23/02/23
 */
@PythonClassInventory
@Inventory(mappingVOClass = KvmHypervisorInfoVO.class, collectionValueOfMethod = "valueOf1")
public class KvmHypervisorInfoInventory {
    private String uuid;
    private String hypervisor;
    private String version;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public KvmHypervisorInfoInventory() {
    }

    public static KvmHypervisorInfoInventory valueOf(KvmHypervisorInfoVO vo) {
        KvmHypervisorInfoInventory inv = new KvmHypervisorInfoInventory();
        inv.setUuid(vo.getUuid());
        inv.setHypervisor(vo.getHypervisor());
        inv.setVersion(vo.getVersion());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<KvmHypervisorInfoInventory> valueOf1(Collection<KvmHypervisorInfoVO> vos) {
        return vos.stream().map(KvmHypervisorInfoInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
