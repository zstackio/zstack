package org.zstack.network.securitygroup;

import org.zstack.header.core.Completion;
import org.zstack.header.vm.VmNicVO;

import java.util.List;

public interface SecurityGroupSdnBackend {
    void createSecurityGroup(SecurityGroupInventory sg, Completion completion);

    void updateSecurityGroup(VmNicSecurityGroupTo to, Completion completion);

    List<VmNicVO> getCandidateVmNic(String sgId, String accountUuid);
}
