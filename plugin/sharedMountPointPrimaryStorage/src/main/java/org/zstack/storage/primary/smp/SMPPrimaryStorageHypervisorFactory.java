package org.zstack.storage.primary.smp;

import org.zstack.header.storage.primary.PrimaryStorageVO;

/**
 * Created by frank on 6/30/2015.
 */
public interface SMPPrimaryStorageHypervisorFactory {
    String getHypervisorType();

    SMPPrimaryStorageHypervisorBackend getHypervisorBackend(PrimaryStorageVO vo);
}
