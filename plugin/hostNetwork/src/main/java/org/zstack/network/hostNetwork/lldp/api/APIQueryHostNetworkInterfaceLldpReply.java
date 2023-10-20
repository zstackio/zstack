package org.zstack.network.hostNetwork.lldp.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpRefInventory;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryHostNetworkInterfaceLldpReply extends APIQueryReply {
    private List<HostNetworkInterfaceLldpRefInventory> inventories;

    public List<HostNetworkInterfaceLldpRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostNetworkInterfaceLldpRefInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryHostNetworkInterfaceLldpReply __example__() {
        APIQueryHostNetworkInterfaceLldpReply reply = new APIQueryHostNetworkInterfaceLldpReply();
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

        reply.setInventories(Collections.singletonList(inv));
        return reply;
    }
}