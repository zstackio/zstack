package org.zstack.network.service;

import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.network.service.NetworkServiceCentralizedDnsBackend;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

/**
 * Created by AlanJager on 2017/7/8.
 */
public class CentralizedDnsExtension extends AbstractNetworkServiceExtension implements Component {
    private static final CLogger logger = Utils.getLogger(CentralizedDnsExtension.class);
    private Map<NetworkServiceProviderType, NetworkServiceCentralizedDnsBackend> cDnsBackends = new HashMap<NetworkServiceProviderType, NetworkServiceCentralizedDnsBackend>();

    private final String RESULT = String.format("result.%s", CentralizedDnsExtension.class.getName());

    @Override
    public NetworkServiceType getNetworkServiceType() {
        return NetworkServiceType.Centralized_DNS;
    }

    @Override
    public void applyNetworkService(VmInstanceSpec spec, Map<String, Object> data, Completion completion) {
        Map<NetworkServiceCentralizedDnsBackend, List<ForwardDnsStruct>> entries = workoutForwardDns(spec);
        data.put(RESULT, entries);
        doForwardDns(entries.entrySet().iterator(), spec, completion);
    }


    private void doForwardDns(final Iterator<Map.Entry<NetworkServiceCentralizedDnsBackend, List<ForwardDnsStruct>>> it, final VmInstanceSpec spec, final Completion complete) {
        if (!it.hasNext()) {
            complete.success();
            return;
        }

        Map.Entry<NetworkServiceCentralizedDnsBackend, List<ForwardDnsStruct>> e = it.next();
        NetworkServiceCentralizedDnsBackend bkd = e.getKey();
        List<ForwardDnsStruct> structs = e.getValue();
        logger.debug(String.format("%s is applying centralized dns service", bkd.getClass().getName()));
        bkd.applyForwardDnsService(structs, spec, new Completion(complete) {
            @Override
            public void success() {
                doForwardDns(it, spec, complete);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                complete.fail(errorCode);
            }
        });
    }

    private Map<NetworkServiceCentralizedDnsBackend, List<ForwardDnsStruct>> workoutForwardDns(VmInstanceSpec spec) {
        Map<NetworkServiceCentralizedDnsBackend, List<ForwardDnsStruct>> map = new HashMap<NetworkServiceCentralizedDnsBackend, List<ForwardDnsStruct>>();
        Map<NetworkServiceProviderType, List<L3NetworkInventory>> providerMap = getNetworkServiceProviderMap(NetworkServiceType.Centralized_DNS, spec.getL3Networks());

        for (Map.Entry<NetworkServiceProviderType, List<L3NetworkInventory>> e : providerMap.entrySet()) {
            NetworkServiceProviderType ptype = e.getKey();
            List<ForwardDnsStruct> lst = new ArrayList<ForwardDnsStruct>();

            for (L3NetworkInventory l3 : e.getValue()) {
                lst.add(makeForwardDnsStruct(spec, l3));
            }

            NetworkServiceCentralizedDnsBackend bkd = cDnsBackends.get(ptype);
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

    private ForwardDnsStruct makeForwardDnsStruct(VmInstanceSpec spec, final L3NetworkInventory l3) {
        VmNicInventory nic = null;
        for (VmNicInventory inv : spec.getDestNics()) {
            if (inv.getL3NetworkUuid().equals(l3.getUuid())) {
                nic = inv;
                break;
            }
        }

        ForwardDnsStruct struct = new ForwardDnsStruct();
        struct.setL3Network(l3);
        struct.setMac(nic.getMac());

        return struct;
    }

    @Override
    public void releaseNetworkService(VmInstanceSpec spec, Map<String, Object> data, NoErrorCompletion completion) {
        Map<NetworkServiceCentralizedDnsBackend, List<ForwardDnsStruct>> entries = (Map<NetworkServiceCentralizedDnsBackend, List<ForwardDnsStruct>>) data.get(RESULT);
        if (entries == null) {
            entries = workoutForwardDns(spec);
        }
        releaseForwardDns(entries.entrySet().iterator(), spec, completion);
    }

    private void releaseForwardDns(final Iterator<Map.Entry<NetworkServiceCentralizedDnsBackend, List<ForwardDnsStruct>>> it, final VmInstanceSpec spec, final NoErrorCompletion complete) {
        if (!it.hasNext()) {
            complete.done();
            return;
        }

        Map.Entry<NetworkServiceCentralizedDnsBackend, List<ForwardDnsStruct>> e = it.next();
        NetworkServiceCentralizedDnsBackend bkd = e.getKey();
        List<ForwardDnsStruct> structs = e.getValue();
        logger.debug(String.format("%s is applying centralized dns service", bkd.getClass().getName()));
        bkd.releaseForwardDnsService(structs, spec, new NoErrorCompletion(complete) {
            @Override
            public void done() {
                complete.done();
            }
        });
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

    private void populateExtensions() {
        for (NetworkServiceCentralizedDnsBackend extp : pluginRgty.getExtensionList(NetworkServiceCentralizedDnsBackend.class)) {
            NetworkServiceCentralizedDnsBackend old = cDnsBackends.get(extp.getProviderType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate NetworkServiceDnsBackend[%s, %s] for type[%s]",
                        extp.getClass().getName(), old.getClass().getName(), extp.getProviderType()));
            }
            cDnsBackends.put(extp.getProviderType(), extp);
        }
    }
}
