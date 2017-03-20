package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.core.cascade.CascadeExtensionPoint;
import org.zstack.network.l2.L2NetworkCascadeExtension;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by weiwang on 16/03/2017.
 */
public class L2VxlanNetworkPoolCascadeExtension extends L2NetworkCascadeExtension implements CascadeExtensionPoint {
    private static final CLogger logger = Utils.getLogger(L2VxlanNetworkPoolCascadeExtension.class);

    private static final String NAME = VxlanNetworkPoolVO.class.getSimpleName();

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

}