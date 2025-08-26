package org.zstack.network.hostNetworkInterface;

import java.util.Arrays;
import java.util.List;

public interface HostNetworkInterfaceConstant {
    String NIC_DRIVER_TYPE_VFIO_PCI = "vfio-pci";
    String NIC_DRIVER_TYPE_UIO_PCI = "uio_pci_generic";

    List<String> VFIO_DRIVER_TYPES = Arrays.asList(NIC_DRIVER_TYPE_VFIO_PCI, NIC_DRIVER_TYPE_UIO_PCI);
}
