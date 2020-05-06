package org.zstack.network.service.eip;

public interface AdditionalEipOperationExtensionPoint {
    EipStruct getAdditionalEipStruct(EipStruct struct);
}
