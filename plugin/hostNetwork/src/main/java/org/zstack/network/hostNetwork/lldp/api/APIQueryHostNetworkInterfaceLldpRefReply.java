package org.zstack.network.hostNetwork.lldp.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpRefInventory;

@RestResponse(allTo = "inventory")
public class APIQueryHostNetworkInterfaceLldpRefReply extends APIQueryReply {
    private HostNetworkInterfaceLldpRefInventory inventory;

    public HostNetworkInterfaceLldpRefInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostNetworkInterfaceLldpRefInventory inventory) {
        this.inventory = inventory;
    }

    public static APIQueryHostNetworkInterfaceLldpRefReply __example__() {
        APIQueryHostNetworkInterfaceLldpRefReply reply = new APIQueryHostNetworkInterfaceLldpRefReply();
        HostNetworkInterfaceLldpRefInventory inv = new HostNetworkInterfaceLldpRefInventory();

        inv.setInterfaceUuid(uuid());
        inv.setChassisId("mac 00:1e:08:1d:05:ba");
        inv.setTimeToLive(120);
        inv.setManagementAddress("172.25.2.4");
        inv.setSystemName("BM-MN-3");
        inv.setSystemDescription(" CentecOS software, E530, Version 7.4.7 Copyright (C) 2004-2021 Centec Networks Inc.  All Rights Reserved.");
        inv.setSystemCapabilities("Bridge, on  Router, on");
        inv.setPortId("ifname eth-0-5");
        inv.setPortDescription("eth-0-4");
        inv.setVlanId(3999);
        inv.setAggregationPortId(4294965248L);
        inv.setMtu(9600);

        reply.setInventory(inv);

        return reply;
    }
}