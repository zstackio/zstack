package org.zstack.compute.vm;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.l3.UsedIpVO_;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import java.util.List;

public class VmNicManagerImpl implements VmNicManager, VmNicExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmNicManagerImpl.class);

    @Override
    public void afterAddIpAddress(String vmNicUUid, String usedIpUuid) {
        /* update UsedIpVO */
        SQL.New(UsedIpVO.class).eq(UsedIpVO_.uuid, usedIpUuid).set(UsedIpVO_.vmNicUuid, vmNicUUid).update();

        VmNicVO nic = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicUUid).find();

        UsedIpVO temp = null;
        /* if there is ipv4 addresses, we put the first attached ipv4 address to VmNic.ip
         * or we put the first attached ipv6 address to vmNic.Ip */
        List<UsedIpVO> refs = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipVersion, IPv6Constants.IPv4)
                .eq(UsedIpVO_.vmNicUuid, vmNicUUid).orderBy(UsedIpVO_.createDate, SimpleQuery.Od.ASC).list();
        if (refs != null && !refs.isEmpty()) {
            temp = refs.get(0);
        } else {
            refs = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipVersion, IPv6Constants.IPv6)
                    .eq(UsedIpVO_.vmNicUuid, vmNicUUid).orderBy(UsedIpVO_.createDate, SimpleQuery.Od.ASC).list();
            if (refs != null && !refs.isEmpty()) {
                temp = refs.get(0);
            }
        }

        if (!temp.getUuid().equals(nic.getUsedIpUuid())) {
            SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicUUid)
                    .set(VmNicVO_.ip, temp.getIp())
                    .set(VmNicVO_.netmask, temp.getNetmask())
                    .set(VmNicVO_.gateway, temp.getGateway())
                    .set(VmNicVO_.usedIpUuid, temp.getUuid())
                    .set(VmNicVO_.ipVersion, temp.getIpVersion())
                    .set(VmNicVO_.l3NetworkUuid, temp.getL3NetworkUuid()).update();
        }
    }

    @Override
    public void afterDelIpAddress(String vmNicUUid, String usedIpUuid) {
        VmNicVO nic = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicUUid).find();
        if (nic.getUsedIpUuid() != null && !nic.getUsedIpUuid().equals(usedIpUuid)) {
            return;
        }

        UsedIpVO temp = null;
        /* if there is ipv4 addresses, we put the first attached ipv4 address to VmNic.ip
         * or we put the first attached ipv6 address to vmNic.Ip */
        List<UsedIpVO> refs = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipVersion, IPv6Constants.IPv4)
                .eq(UsedIpVO_.vmNicUuid, vmNicUUid).orderBy(UsedIpVO_.createDate, SimpleQuery.Od.ASC).list();
        if (refs != null && !refs.isEmpty()) {
            temp = refs.get(0);
        } else {
            refs = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipVersion, IPv6Constants.IPv6)
                    .eq(UsedIpVO_.vmNicUuid, vmNicUUid).orderBy(UsedIpVO_.createDate, SimpleQuery.Od.ASC).list();
            if (refs != null && !refs.isEmpty()) {
                temp = refs.get(0);
            }
        }

        if (temp != null) {
            SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicUUid)
                    .set(VmNicVO_.ip, temp.getIp())
                    .set(VmNicVO_.netmask, temp.getNetmask())
                    .set(VmNicVO_.gateway, temp.getGateway())
                    .set(VmNicVO_.usedIpUuid, temp.getUuid())
                    .set(VmNicVO_.ipVersion, temp.getIpVersion())
                    .set(VmNicVO_.l3NetworkUuid, temp.getL3NetworkUuid()).update();
        }
    }
}
