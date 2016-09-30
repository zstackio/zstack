package org.zstack.network.service.eip;

import org.zstack.header.Service;
import org.zstack.header.core.Completion;

/**
 */
public interface EipManager extends Service {
    EipBackend getEipBackend(String providerType);

    void detachEip(EipStruct struct, String providerType, Completion completion);

    void detachEipAndUpdateDb(EipStruct struct, String providerType, Completion completion);

    void attachEip(EipStruct struct, String providerType, Completion completion);
}
