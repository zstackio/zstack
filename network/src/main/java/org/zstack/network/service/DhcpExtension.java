package org.zstack.network.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.GlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.notification.N;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.DhcpStruct;
import org.zstack.header.network.service.NetworkServiceDhcpBackend;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceSpec.HostName;
import org.zstack.network.l2.L2NetworkDefaultMtu;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 7:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class DhcpExtension extends AbstractNetworkServiceExtension implements Component, VmDefaultL3NetworkChangedExtensionPoint {
    private static final CLogger logger = Utils.getLogger(DhcpExtension.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private CloudBus bus;

    private Map<NetworkServiceProviderType, NetworkServiceDhcpBackend> dhcpBackends = new HashMap<NetworkServiceProviderType, NetworkServiceDhcpBackend>();

    private final String RESULT = String.format("result.%s", DhcpExtension.class.getName());

    public NetworkServiceType getNetworkServiceType() {
        return NetworkServiceType.DHCP;
    }

    private void doDhcp(final Iterator<Map.Entry<NetworkServiceDhcpBackend, List<DhcpStruct>>> it, final VmInstanceSpec spec, final Completion complete) {
        if (!it.hasNext()) {
            complete.success();
            return;
        }

        Map.Entry<NetworkServiceDhcpBackend, List<DhcpStruct>> e = it.next();
        NetworkServiceDhcpBackend bkd = e.getKey();
        List<DhcpStruct> structs = e.getValue();
        logger.debug(String.format("%s is applying DHCP service", bkd.getClass().getName()));
        bkd.applyDhcpService(structs, spec, new Completion(complete) {
            @Override
            public void success() {
                doDhcp(it, spec, complete);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                complete.fail(errorCode);
            }
        });
    }

    private void releaseDhcp(final Iterator<Map.Entry<NetworkServiceDhcpBackend, List<DhcpStruct>>> it, final VmInstanceSpec spec, final NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        Map.Entry<NetworkServiceDhcpBackend, List<DhcpStruct>> e = it.next();
        NetworkServiceDhcpBackend bkd = e.getKey();
        List<DhcpStruct> structs = e.getValue();
        logger.debug(String.format("%s is releasing DHCP service", bkd.getClass().getName()));
        bkd.releaseDhcpService(structs, spec, new NoErrorCompletion(completion) {
            @Override
            public void done() {
                releaseDhcp(it, spec, completion);
            }
        });
    }

    @Override
    public void applyNetworkService(VmInstanceSpec spec, Map<String, Object> data, Completion complete) {
        Map<NetworkServiceDhcpBackend, List<DhcpStruct>> entries = workoutDhcp(spec);
        data.put(RESULT, entries);
        doDhcp(entries.entrySet().iterator(), spec, complete);
    }

    @Override
    public void releaseNetworkService(VmInstanceSpec spec, Map<String, Object> data, NoErrorCompletion completion) {
        Map<NetworkServiceDhcpBackend, List<DhcpStruct>> entries = (Map<NetworkServiceDhcpBackend, List<DhcpStruct>>) data.get(RESULT);
        if (entries == null) {
            entries = workoutDhcp(spec);
        }
        releaseDhcp(entries.entrySet().iterator(), spec, completion);
    }

    private void populateExtensions() {
        for (NetworkServiceDhcpBackend bkd : pluginRgty.getExtensionList(NetworkServiceDhcpBackend.class)) {
            NetworkServiceDhcpBackend old = dhcpBackends.get(bkd.getProviderType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate NetworkServiceDhcpBackend[%s, %s] for type[%s]",
                        bkd.getClass().getName(), old.getClass().getName(), bkd.getProviderType()));
            }
            dhcpBackends.put(bkd.getProviderType(), bkd);
        }
    }


    private DhcpStruct makeDhcpStruct(VmInstanceSpec spec, final L3NetworkInventory l3) {
        VmNicInventory nic = null;
        for (VmNicInventory inv : spec.getDestNics()) {
            if (inv.getL3NetworkUuid().equals(l3.getUuid())) {
                nic = inv;
                break;
            }
        }

        DhcpStruct struct = new DhcpStruct();
        struct.setGateway(nic.getGateway());
        String hostname = CollectionUtils.find(spec.getHostnames(), new Function<String, HostName>() {
            @Override
            public String call(HostName arg) {
                return arg.getL3NetworkUuid().equals(l3.getUuid()) ? arg.getHostname() : null;
            }
        });
        if (hostname != null && l3.getDnsDomain() != null) {
            hostname = String.format("%s.%s", hostname, l3.getDnsDomain());
        }
        struct.setHostname(hostname);
        struct.setIp(nic.getIp());
        struct.setDnsDomain(l3.getDnsDomain());
        struct.setL3Network(l3);
        struct.setDefaultL3Network(spec.getVmInventory().getDefaultL3NetworkUuid() != null &&
                spec.getVmInventory().getDefaultL3NetworkUuid().equals(l3.getUuid()));
        struct.setMac(nic.getMac());
        struct.setNetmask(nic.getNetmask());
        struct.setMtu(new MtuGetter().getMtu(l3.getUuid()));

        return struct;
    }

    private Map<NetworkServiceDhcpBackend, List<DhcpStruct>> workoutDhcp(VmInstanceSpec spec) {
        Map<NetworkServiceDhcpBackend, List<DhcpStruct>> map = new HashMap<NetworkServiceDhcpBackend, List<DhcpStruct>>();
        Map<NetworkServiceProviderType, List<L3NetworkInventory>> providerMap = getNetworkServiceProviderMap(NetworkServiceType.DHCP, spec.getL3Networks());

        for (Map.Entry<NetworkServiceProviderType, List<L3NetworkInventory>> e : providerMap.entrySet()) {
            NetworkServiceProviderType ptype = e.getKey();
            List<DhcpStruct> lst = new ArrayList<DhcpStruct>();

            for (L3NetworkInventory l3 : e.getValue()) {
                lst.add(makeDhcpStruct(spec, l3));
            }

            NetworkServiceDhcpBackend bkd = dhcpBackends.get(ptype);
            if (bkd == null) {
                throw new CloudRuntimeException(String.format("unable to find NetworkServiceDhcpBackend[provider type: %s]", ptype));
            }
            map.put(bkd, lst);

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("DHCP Backend[%s] is about to apply entries: \n%s", bkd.getClass().getName(), lst));
            }
        }

        return map;
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void vmDefaultL3NetworkChanged(VmInstanceInventory vm, String previousL3, String nowL3) {
        List<String> l3Uuids = new ArrayList<String>();
        if (previousL3 != null) {
            l3Uuids.add(previousL3);
        }
        if (nowL3 != null) {
            l3Uuids.add(nowL3);
        }

        SimpleQuery<L3NetworkVO> q = dbf.createQuery(L3NetworkVO.class);
        q.add(L3NetworkVO_.uuid, Op.IN, l3Uuids);
        List<L3NetworkVO> vos = q.list();
        List<L3NetworkInventory> invs = L3NetworkInventory.valueOf(vos);
        Map<NetworkServiceProviderType, List<L3NetworkInventory>> providerMap = getNetworkServiceProviderMap(NetworkServiceType.DHCP, invs);
        for (Map.Entry<NetworkServiceProviderType, List<L3NetworkInventory>> e : providerMap.entrySet()) {
            NetworkServiceProviderType ptype = e.getKey();

            NetworkServiceDhcpBackend bkd = dhcpBackends.get(ptype);
            if (bkd == null) {
                throw new CloudRuntimeException(String.format("unable to find NetworkServiceDhcpBackend[provider type: %s]", ptype));
            }

            bkd.vmDefaultL3NetworkChanged(vm, previousL3, nowL3, new Completion(null) {
                @Override
                public void success() {
                    // pass
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    N.New(VmInstanceVO.class, vm.getUuid()).warn_("unable to change the VM[uuid:%s]'s default L3 network in the DHCP backend, %s. You may need to reboot" +
                            " the VM to use the new default L3 network setting", vm.getUuid(), errorCode);
                }
            });
        }
    }
}
