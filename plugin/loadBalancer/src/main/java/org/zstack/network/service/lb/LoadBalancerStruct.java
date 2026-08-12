package org.zstack.network.service.lb;

import com.google.common.collect.Lists;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.vip.VipInventory;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by frank on 8/8/2015.
 */
public class LoadBalancerStruct implements Serializable {
    private LoadBalancerInventory lb;
    private VipInventory vip;
    private VipInventory ipv6Vip;
    private Map<String, VmNicInventory> vmNics;
    private List<LoadBalancerListenerInventory> listeners;
    private Map<String, List<LoadBalancerServerGroupInventory>> listenerServerGroupMap = new HashMap<>();
    private Map<String, List<LoadBalancerServerGroupInventory>> deletedListenerServerGroupMap = new HashMap<>();
    private Set<String> disabledVmNics = new HashSet<>();
    private Set<String> disabledServerIps = new HashSet<>();
    private Map<String, List<String>> tags;
    private boolean init;
    private boolean rollback;

    public Map<String, VmNicInventory> getVmNics() {
        return vmNics;
    }

    public boolean isInit() {
        return init;
    }

    public void setInit(boolean init) {
        this.init = init;
    }

    public boolean isRollback() {
        return rollback;
    }

    public void setRollback(boolean rollback) {
        this.rollback = rollback;
    }

    public Map<String, List<String>> getTags() {
        return tags;
    }

    public void setTags(Map<String, List<String>> tags) {
        this.tags = tags;
    }

    public List<LoadBalancerListenerInventory> getListeners() {
        return listeners;
    }

    public void setListeners(List<LoadBalancerListenerInventory> listeners) {
        this.listeners = listeners;
    }

    public LoadBalancerInventory getLb() {
        return lb;
    }

    public void setLb(LoadBalancerInventory lb) {
        this.lb = lb;
    }

    public void setVmNics(Map<String, VmNicInventory> vmNics) {
        this.vmNics = vmNics;
    }

    public VipInventory getVip() {
        return vip;
    }

    public void setVip(VipInventory vip) {
        this.vip = vip;
    }

    public VipInventory getIpv6Vip() {
        return ipv6Vip;
    }

    public void setIpv6Vip(VipInventory ipv6Vip) {
        this.ipv6Vip = ipv6Vip;
    }

    public Map<String, List<LoadBalancerServerGroupInventory>> getListenerServerGroupMap() {
        return listenerServerGroupMap;
    }

    public void setListenerServerGroupMap(Map<String, List<LoadBalancerServerGroupInventory>> listenerServerGroupMap) {
        this.listenerServerGroupMap = listenerServerGroupMap;
    }

    public Map<String, List<LoadBalancerServerGroupInventory>> getDeletedListenerServerGroupMap() {
        return deletedListenerServerGroupMap;
    }

    public void setDeletedListenerServerGroupMap(Map<String, List<LoadBalancerServerGroupInventory>> deletedListenerServerGroupMap) {
        this.deletedListenerServerGroupMap = deletedListenerServerGroupMap;
    }

    public Set<String> getDisabledVmNics() {
        return disabledVmNics;
    }

    public void setDisabledVmNics(Set<String> disabledVmNics) {
        this.disabledVmNics = disabledVmNics;
    }

    public boolean isVmNicDisabled(String listenerUuid, String serverGroupUuid, String vmNicUuid) {
        return disabledVmNics.contains(vmNicStateKey(listenerUuid, serverGroupUuid, vmNicUuid));
    }

    public static String vmNicStateKey(String listenerUuid, String serverGroupUuid, String vmNicUuid) {
        return String.format("%s:%s:%s", listenerUuid, serverGroupUuid, vmNicUuid);
    }

    public Set<String> getDisabledServerIps() {
        return disabledServerIps;
    }

    public void setDisabledServerIps(Set<String> disabledServerIps) {
        this.disabledServerIps = disabledServerIps;
    }

    public boolean isServerIpDisabled(String listenerUuid, String serverGroupUuid, long serverIpId) {
        return disabledServerIps.contains(serverIpStateKey(listenerUuid, serverGroupUuid, serverIpId));
    }

    public static String serverIpStateKey(String listenerUuid, String serverGroupUuid, long serverIpId) {
        return String.format("%s:%s:%d", listenerUuid, serverGroupUuid, serverIpId);
    }

    public List<String> getActiveVmNics() {
        List<String> attachedVmNicUuids = new ArrayList<>();
        for (LoadBalancerListenerInventory ll : lb.getListeners()) {
            List<LoadBalancerServerGroupInventory> groupInvs = listenerServerGroupMap.get(ll.getUuid());
            if (groupInvs == null || groupInvs.isEmpty()) {
                continue;
            }

            for (LoadBalancerServerGroupInventory group : groupInvs) {
                attachedVmNicUuids.addAll(group.getVmNicRefs().stream()
                        .filter(r -> !LoadBalancerVmNicStatus.Pending.toString().equals(r.getStatus()))
                        .map(LoadBalancerServerGroupVmNicRefInventory::getVmNicUuid).collect(Collectors.toList()));
            }
        }

        return attachedVmNicUuids;
    }

    public List<String> getAllVmNics() {
        List<String> attachedVmNicUuids = new ArrayList<>();
        for (LoadBalancerListenerInventory ll : lb.getListeners()) {
            List<LoadBalancerServerGroupInventory> groupInvs = listenerServerGroupMap.get(ll.getUuid());
            if (groupInvs == null || groupInvs.isEmpty()) {
                continue;
            }

            for (LoadBalancerServerGroupInventory group : groupInvs) {
                attachedVmNicUuids.addAll(group.getVmNicRefs().stream()
                        .map(LoadBalancerServerGroupVmNicRefInventory::getVmNicUuid).collect(Collectors.toList()));
            }
        }

        return attachedVmNicUuids;
    }

    public List<VmNicInventory> getAllVmNicsInventory() {
        if (vmNics == null || vmNics.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        Collection<VmNicInventory> vmNicsIn = vmNics.values();
        return Lists.newArrayList(vmNicsIn);
    }

    public List<String> getAllVmNicsOfListener(LoadBalancerListenerInventory listener) {
        List<String> attachedVmNicUuids = new ArrayList<>();
        List<LoadBalancerServerGroupInventory> groupInvs = deletedListenerServerGroupMap.get(listener.getUuid());
        if (groupInvs == null || groupInvs.isEmpty()) {
            return attachedVmNicUuids;
        }

        for (LoadBalancerServerGroupInventory group : groupInvs) {
            attachedVmNicUuids.addAll(group.getVmNicRefs().stream()
                    .map(LoadBalancerServerGroupVmNicRefInventory::getVmNicUuid).collect(Collectors.toList()));
        }

        return attachedVmNicUuids;
    }
}
