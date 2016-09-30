package org.zstack.network.l2;

import org.zstack.header.host.HypervisorType;
import org.zstack.header.network.l2.L2NetworkFactory;
import org.zstack.header.network.l2.L2NetworkRealizationExtensionPoint;
import org.zstack.header.network.l2.L2NetworkType;

public interface L2NetworkManager {
    L2NetworkFactory getL2NetworkFactory(L2NetworkType type);
    
    L2NetworkRealizationExtensionPoint getRealizationExtension(L2NetworkType l2Type, HypervisorType hvType);
}
