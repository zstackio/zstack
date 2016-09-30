package org.zstack.network.service.virtualrouter;

import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vo.EO;

import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@EO(EOClazz = VmInstanceEO.class, needView = false)
@AutoDeleteTag
public class VirtualRouterVmVO extends ApplianceVmVO {
    private String publicNetworkUuid;

    public VirtualRouterVmVO(VirtualRouterVmVO other) {
        super(other);
        this.publicNetworkUuid = other.publicNetworkUuid;
    }

    public VirtualRouterVmVO(ApplianceVmVO other) {
        super(other);
    }

    public VirtualRouterVmVO() {
    }

    public String getPublicNetworkUuid() {
        return publicNetworkUuid;
    }

    public void setPublicNetworkUuid(String publicNetworkUuid) {
        this.publicNetworkUuid = publicNetworkUuid;
    }
}
