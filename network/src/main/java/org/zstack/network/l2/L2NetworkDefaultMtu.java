package org.zstack.network.l2;

import org.zstack.header.network.l2.L2NetworkType;

/**
 * Created by weiwang on 17/05/2017.
 */
public interface L2NetworkDefaultMtu {
    String getL2NetworkType();

    Integer getDefaultMtu();
}
