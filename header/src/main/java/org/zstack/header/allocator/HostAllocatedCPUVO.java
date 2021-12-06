package org.zstack.header.allocator;

import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = HostVO.class, myField = "uuid", targetField = "uuid")
        }
)
public class HostAllocatedCPUVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String uuid;

    // value like nodeID0:CPUID0,CPUID1;nodeID1CPUID10,CPUID11
    @Column
    private String allocatedCPU;


    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setAllocatedCPU(String allocatedCPU) {
        this.allocatedCPU = allocatedCPU;
    }

    public String getUuid() {
        return uuid;
    }

    public String getAllocatedCPU() {
        return allocatedCPU;
    }
}
