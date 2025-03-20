package org.zstack.network.service;

import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.vm.VmDnsVO;
import org.zstack.header.vm.VmDnsVO_;

import javax.persistence.Tuple;
import java.util.List;

public class DnsUtils {
    public static List<Tuple> getTupleOfIdAndDns(String vmNicUuid, Integer ipVersion) {
        return Q.New(VmDnsVO.class).select(VmDnsVO_.id, VmDnsVO_.dns)
                .eq(VmDnsVO_.vmNicUuid, vmNicUuid)
                .eq(VmDnsVO_.ipVersion, ipVersion)
                .orderBy(VmDnsVO_.id, SimpleQuery.Od.ASC)
                .listTuple();
    }

    public static List<String> getVmNicDnsList(String vmNicUuid, Integer ipVersion) {
        return Q.New(VmDnsVO.class).select(VmDnsVO_.dns)
                .eq(VmDnsVO_.vmNicUuid, vmNicUuid)
                .eq(VmDnsVO_.ipVersion, ipVersion)
                .orderBy(VmDnsVO_.id, SimpleQuery.Od.ASC)
                .listValues();
    }

    public static List<String> getVmDnsList(String vmUuid) {
        return Q.New(VmDnsVO.class).select(VmDnsVO_.dns)
                .eq(VmDnsVO_.vmInstanceUuid, vmUuid)
                .orderBy(VmDnsVO_.id, SimpleQuery.Od.ASC)
                .listValues();
    }

    public static List<VmDnsVO> getVmDnsVOList(String vmNicUuid, Integer ipVersion) {
        return Q.New(VmDnsVO.class)
                .eq(VmDnsVO_.vmNicUuid, vmNicUuid)
                .eq(VmDnsVO_.ipVersion, ipVersion)
                .orderBy(VmDnsVO_.id, SimpleQuery.Od.ASC)
                .list();
    }

    public static List<VmDnsVO> getVmDnsVOList(String vmUuid) {
        return Q.New(VmDnsVO.class)
                .eq(VmDnsVO_.vmInstanceUuid, vmUuid)
                .orderBy(VmDnsVO_.id, SimpleQuery.Od.ASC)
                .list();
    }
}
