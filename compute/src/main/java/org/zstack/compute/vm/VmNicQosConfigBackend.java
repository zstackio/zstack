package org.zstack.compute.vm;

import java.util.List;

public interface VmNicQosConfigBackend {
    String getVmInstanceType();
    void addNicQos(String vmUuid, String vmNicUuid, Long outboundBandwidth, Long inboundBandwidth);
    void deleteNicQos(String vmUuid, String vmNicUuid,String direction);
    VmNicQosStruct getNicQos(String vmUuid, String vmNicUuid);

    /**
     * Batch version: return QoS struct for provided NIC UUIDs in the same order.
     */
    default List<VmNicQosStruct> getNicQosBatch(List<String> vmNicUuids) {
        throw new UnsupportedOperationException("getNicQosBatch is not supported");
    }

    void addVmQos(String vmUuid, Long outboundBandwidth, Long inboundBandwidth);
    void deleteVmQos(String vmUuid, String direction);
    VmNicQosStruct getVmQos(String vmUuid);
}
