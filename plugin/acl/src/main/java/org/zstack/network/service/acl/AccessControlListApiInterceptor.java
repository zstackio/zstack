package org.zstack.network.service.acl;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.network.service.header.acl.APIAddAccessControlListEntryMsg;
import org.zstack.network.service.header.acl.APICreateAccessControlListMsg;
import org.zstack.network.service.header.acl.AccessControlListVO;
import org.zstack.tag.TagManager;
import org.zstack.utils.IpRangeSet;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-12
 **/
public class AccessControlListApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private TagManager tagMgr;

    private static final CLogger logger = CLoggerImpl.getLogger(AccessControlListApiInterceptor.class);
    private static String SPLIT = "-";
    private static String IP_SPLIT = ",";

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateAccessControlListMsg) {
            validate ((APICreateAccessControlListMsg) msg);
        } else if (msg instanceof APIAddAccessControlListEntryMsg) {
            validate ((APIAddAccessControlListEntryMsg) msg);
        }
        return msg;
    }

    private void validate (APICreateAccessControlListMsg msg) {
        if (msg.getIpVersion() == 0) {
            msg.setIpVersion(IPv6Constants.IPv4);
        }
    }

    private boolean validateIpRange(String startIp, String endIp) {
        if (NetworkUtils.isIpv4Address(startIp) && !NetworkUtils.isIpv4Address(endIp)) {
            return false;
        }

        if (IPv6NetworkUtils.isIpv6Address(startIp) && !IPv6NetworkUtils.isIpv6Address(endIp)) {
            return false;
        }

        try {
            if (NetworkUtils.isIpv4Address(startIp)) {
                NetworkUtils.validateIpRange(startIp, endIp);
            } else {
                //IPv6NetworkUtils.validateIpRange(startIp, endIp);
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void validateIp(String ips, Integer ipVer) {
        if (!ipVer.equals(IPv6Constants.IPv4)) {
            throw new ApiMessageInterceptionException(argerr("operation failure, not support the ip version %d", ipVer));
        }
        try {
            RangeSet<Long> ipRanges = IpRangeSet.listAllRanges(ips);
            String[] ipcount = ips.split(IP_SPLIT);
            if (ipRanges.asRanges().size() < ipcount.length) {
                throw new ApiMessageInterceptionException(argerr("operation failure, duplicate/overlap ip entry in %s", ips));
            }
            for (Range<Long> range : ipRanges.asRanges()) {
                final Range<Long> frange = ContiguousSet.create(range, DiscreteDomain.longs()).range();
                String startIp = NetworkUtils.longToIpv4String(frange.lowerEndpoint());
                String endIp = NetworkUtils.longToIpv4String(frange.upperEndpoint());
                if (!validateIpRange(startIp, endIp)) {
                    throw new ApiMessageInterceptionException(argerr("operation failure, ip format only supports ip/iprange/cidr, but find %s", ips));
                }
                ipRanges.asRanges().stream().forEach(r -> {
                    if (!frange.equals(r) && NetworkUtils.isIpv4RangeOverlap(startIp, endIp, NetworkUtils.longToIpv4String(r.lowerEndpoint()), NetworkUtils.longToIpv4String(r.upperEndpoint()))) {
                        throw new ApiMessageInterceptionException(argerr("operation failure, there are overlap ip range[start ip:%s, end ip: %s and start ip:%s, end ip: %s]",
                                startIp, endIp, NetworkUtils.longToIpv4String(r.lowerEndpoint()), NetworkUtils.longToIpv4String(r.upperEndpoint())));
                    }
                });
            }

        } catch (IllegalArgumentException e) {
            throw new ApiMessageInterceptionException(argerr("Invalid rule expression, the detail: %s", e.getMessage()));
        }

    }

    private void validate (APIAddAccessControlListEntryMsg msg) {
        AccessControlListVO acl = dbf.findByUuid(msg.getAclUuid(), AccessControlListVO.class);
        validateIp(msg.getEntries(), acl.getIpVersion());

        /*check if the entry is exist*/
        if (acl.getEntries()!= null && !acl.getEntries().isEmpty()) {
            Set<String> ipentries = acl.getEntries().stream().map(entry -> entry.getIpEntries()).collect(Collectors.toSet());
            ipentries.add(msg.getEntries());
            /*miaozhanyong to be done*/
            //validateIp(ipentries, acl.getIpVersion());
        }
    }
}
