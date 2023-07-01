package org.zstack.sugonSdnController.controller;

import org.zstack.header.core.Completion;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.*;

import java.util.List;

public interface TfSdnController {

    void createL2Network(L2NetworkVO l2NetworkVO, APICreateL2NetworkMsg msg, List<String> systemTags, Completion completion);
    void updateL2Network(L2NetworkVO l2NetworkVO, List<String> systemTags, Completion completion);
    void deleteL2Network(L2NetworkVO l2NetworkVO, List<String> systemTags, Completion completion);

    // 账号同步：zstack->tf
    void createAccount(AccountInventory account);
    void deleteAccount(AccountInventory account);
    void updateAccount(AccountInventory account);

    // L3网络：zstack->tf
    void deleteL3Network(L3NetworkVO l3NetworkVO, Completion completion);

    void updateL3Network(L3NetworkVO l3NetworkVO, APIUpdateL3NetworkMsg msg, Completion completion);

    void addL3IpRangeByCidr(L3NetworkVO l3NetworkVO, APIAddIpRangeByNetworkCidrMsg msg, Completion completion);

    void addL3HostRoute(L3NetworkVO l3NetworkVO, APIAddHostRouteToL3NetworkMsg msg, Completion completion);

    void deleteL3HostRoute(L3NetworkVO l3NetworkVO, APIRemoveHostRouteFromL3NetworkMsg msg, Completion completion);

    void addL3Dns(L3NetworkVO l3NetworkVO, APIAddDnsToL3NetworkMsg msg, Completion completion);

    void deleteL3Dns(L3NetworkVO l3NetworkVO, APIRemoveDnsFromL3NetworkMsg msg, Completion completion);
}
