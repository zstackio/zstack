package org.zstack.acl;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.acl.APIAddAccessControlListEntryMsg;
import org.zstack.header.acl.APICreateAccessControlListMsg;
import org.zstack.header.acl.AccessControlListConstants;
import org.zstack.header.acl.AccessControlListVO;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.tag.TagManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.IpRangeSet;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.List;
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
        if (msg.getIpVersion() == null) {
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

    private void validateIp(String ips, AccessControlListVO acl) {
        DebugUtils.Assert(acl != null, "the invalide null AccessControlListVO");
        Integer ipVer = acl.getIpVersion();
        if (!ipVer.equals(IPv6Constants.IPv4)) {
            throw new ApiMessageInterceptionException(argerr("not support the ip version %d", ipVer));
        }
        try {
            RangeSet<Long> ipRanges = IpRangeSet.listAllRanges(ips);
            String[] ipcount = ips.split(IP_SPLIT);
            if (ipRanges.asRanges().size() < ipcount.length) {
                throw new ApiMessageInterceptionException(argerr("%s duplicate/overlap ip entry with access-control-list group:%s", ips, acl.getUuid()));
            }
            for (Range<Long> range : ipRanges.asRanges()) {
                final Range<Long> frange = ContiguousSet.create(range, DiscreteDomain.longs()).range();
                String startIp = NetworkUtils.longToIpv4String(frange.lowerEndpoint());
                String endIp = NetworkUtils.longToIpv4String(frange.upperEndpoint());
                if (!validateIpRange(startIp, endIp)) {
                    throw new ApiMessageInterceptionException(argerr("ip format only supports ip/iprange/cidr, but find %s", ips));
                }
                ipRanges.asRanges().stream().forEach(r -> {
                    if (!frange.equals(r) && NetworkUtils.isIpv4RangeOverlap(startIp, endIp, NetworkUtils.longToIpv4String(r.lowerEndpoint()), NetworkUtils.longToIpv4String(r.upperEndpoint()))) {
                        throw new ApiMessageInterceptionException(argerr("ip range[%s, %s] is overlap with [%s, %s] in access-control-list group:%s",
                                startIp, endIp, NetworkUtils.longToIpv4String(r.lowerEndpoint()), NetworkUtils.longToIpv4String(r.upperEndpoint()), acl.getUuid()));
                    }
                });
            }

        } catch (IllegalArgumentException e) {
            throw new ApiMessageInterceptionException(argerr("Invalid rule expression, the detail: %s", e.getMessage()));
        }

    }

    private void validate (APIAddAccessControlListEntryMsg msg) {
        AccessControlListVO acl = dbf.findByUuid(msg.getAclUuid(), AccessControlListVO.class);

        /*check if the entry is exist*/
        if (acl.getEntries()!= null && !acl.getEntries().isEmpty()) {
            if (acl.getEntries().size() >= AccessControlListConstants.MAX_ENTRY_COUNT_PER_GROUP) {
                throw new ApiMessageInterceptionException(argerr("the access-control-list groups[%s] can't be added more than %d ip entries", acl.getUuid(), AccessControlListConstants.MAX_ENTRY_COUNT_PER_GROUP));
            }

            List<String> ipentries = acl.getEntries().stream().map(entry -> entry.getIpEntries()).collect(Collectors.toList());
            ipentries.add(msg.getEntries());
            /*miaozhanyong to be done*/
            validateIp(StringUtils.join(ipentries.toArray(), ','), acl);
        } else {
            validateIp(msg.getEntries(), acl);
        }
    }
}
