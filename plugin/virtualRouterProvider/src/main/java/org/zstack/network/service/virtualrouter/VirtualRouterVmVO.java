package org.zstack.network.service.virtualrouter;

import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@EO(EOClazz = VmInstanceEO.class, needView = false)
@AutoDeleteTag
public class VirtualRouterVmVO extends ApplianceVmVO {
    private String publicNetworkUuid;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "virtualRouterVmUuid", insertable = false, updatable = false)
    @NoView
    private List<VirtualRouterVipVO> virtualRouterVips = new ArrayList<>();

    public VirtualRouterVmVO(VirtualRouterVmVO other) {
        super(other);
        this.publicNetworkUuid = other.publicNetworkUuid;
        this.virtualRouterVips = other.virtualRouterVips;
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

    public List<VirtualRouterVipVO> getVirtualRouterVips() {
        return virtualRouterVips;
    }

    public void setVirtualRouterVips(List<VirtualRouterVipVO> virtualRouterVips) {
        this.virtualRouterVips = virtualRouterVips;
    }
}
