package org.zstack.compute.vm;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmNicState;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

/**
 * Created by camile on 2017/12/14.
 */
public class MacOperator {
    private static final CLogger logger = Utils.getLogger(MacOperator.class);
    private static final Pattern pattern = Pattern.compile("([a-f0-9]{2}:){5}[a-f0-9]{2}");
    private static final Random random = new Random();

    class VmMacStruct {
        private String l3Uuid;
        private String mac;

        public VmMacStruct(String l3Uuid, String mac) {
            this.l3Uuid = l3Uuid;
            this.mac = mac;
        }
    }

    private PatternedSystemTag that = VmSystemTags.CUSTOM_MAC;

    public List<VmMacStruct> getMacInfobyVmUuid(String vmUuid) {
        List<VmMacStruct> structs = Lists.newArrayList();
        List<Map<String, String>> tokenList = that.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            structs.add(new VmMacStruct(tokens.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN),
                    VmSystemTags.MAC_TOKEN));

        }
        return structs;
    }

    public String getMac(String vmUuid, String l3Uuid) {
        List<Map<String, String>> tokenList = that.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            if (StringUtils.equals(tokens.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN), l3Uuid)) {
                return  tokens.get(VmSystemTags.MAC_TOKEN);
            }
        }
        return null;
    }

    public void deleteCustomMacSystemTag(String vmUuid, String l3uuid, String mac) {
        String str = VmSystemTags.CUSTOM_MAC.instantiateTag(map(
                e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3uuid),
                e(VmSystemTags.MAC_TOKEN, mac)));
        that.delete(vmUuid, str);
    }

    private boolean isMulticastMac(String mac) {
        if (!pattern.matcher(mac.toLowerCase()).matches()){
            throw new OperationFailureException(operr(ORG_ZSTACK_COMPUTE_VM_10225, "This is not a valid MAC address [%s]", mac));
        }
        String binaryString = new BigInteger(mac.substring(0,2), 16).toString(2);
        return binaryString.substring(binaryString.length() - 1).equals("1");
    }

    public void validateAvailableMac(String mac) {
        String lowercaseMac = mac.toLowerCase();
        Matcher matcher = pattern.matcher(lowercaseMac);
        if (!matcher.matches()) {
            throw new OperationFailureException(operr(ORG_ZSTACK_COMPUTE_VM_10226, "Not a valid MAC address [%s]", mac));
        }
        if ("00:00:00:00:00:00".equals(lowercaseMac) || "ff:ff:ff:ff:ff:ff".equals(lowercaseMac)) {
            throw new OperationFailureException(operr(ORG_ZSTACK_COMPUTE_VM_10227, "Disallowed address"));
        }
        if (isMulticastMac(lowercaseMac)){
            throw new OperationFailureException(operr(ORG_ZSTACK_COMPUTE_VM_10228, "Expected unicast mac address, found multicast MAC address [%s]", mac));
        }
    }

    public boolean checkDuplicateMac(String hypervisorType, String l3Uuid, String mac) {
        if (!VmInstanceConstant.KVM_HYPERVISOR_TYPE.equals(hypervisorType)) {
            return false;
        }

        L3NetworkVO l3vo = Q.New(L3NetworkVO.class)
                .eq(L3NetworkVO_.uuid, l3Uuid).find();
        if (l3vo == null) {
            return false;
        }
        
        List<String> l3Uuids = Q.New(L3NetworkVO.class)
                .select(L3NetworkVO_.uuid)
                .eq(L3NetworkVO_.l2NetworkUuid, l3vo.getL2NetworkUuid())
                .listValues();

        if (l3Uuids.isEmpty()) {
            return false;
        }

        return Q.New(VmNicVO.class)
                .eq(VmNicVO_.hypervisorType, hypervisorType)
                .eq(VmNicVO_.mac, mac.toLowerCase())
                .in(VmNicVO_.l3NetworkUuid, l3Uuids)
                .notEq(VmNicVO_.state, VmNicState.disable)
                .isExists();
    }

    public static String generateMacWithDeviceId(short deviceId) {
        VmMacAddressSchemaType type;
        try {
            type = VmMacAddressSchemaType.valueOf(VmGlobalProperty.vmMacAddressSchema);
        } catch (Exception e) {
            type = VmMacAddressSchemaType.Random;
        }

        switch (type) {
            case Ip:
                return generateMacWithDeviceIdIp(deviceId);
            case Random:
            default:
                return generateMacWithDeviceIdRandom(deviceId);
        }
    }

    public static String generateMacWithDeviceIdIp(short deviceId) {
        String consoleProxyIp = CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IP;//Platform.getManagementServerIp();
        if (!NetworkUtils.isIpv4Address(consoleProxyIp) || "0.0.0.0".equals(consoleProxyIp)) {
            return generateMacWithDeviceIdRandom(deviceId);
        }

        /* encode mgt ip address into mac address: for example,
        * mgt ip is: 172.24.0.81, its hex string: AC 18 00 51,
        * so mac address will look like: fa:00:51:xx:xx:yy
        * xx:xx are random. yy is device ID */
        int mgtIpL = (int)NetworkUtils.ipv4StringToLong(consoleProxyIp);
        String mgtIpStr = Integer.toHexString(mgtIpL);
        if (mgtIpStr.length() < 8) {
            String compensate = StringUtils.repeat("0", 8 - mgtIpStr.length());
            mgtIpStr = compensate + mgtIpStr;
        }

        StringBuilder sb = new StringBuilder("fa").append(":");
        sb.append(mgtIpStr, 4, 6).append(":");
        sb.append(mgtIpStr, 6, 8).append(":");

        int seed = random.nextInt();
        String seedStr = Integer.toHexString(seed);
        if (seedStr.length() < 4) {
            String compensate = StringUtils.repeat("0", 4 - seedStr.length());
            seedStr = compensate + seedStr;
        }

        sb.append(seedStr, 0, 2).append(":");
        sb.append(seedStr, 2, 4).append(":");
        String deviceIdStr = Integer.toHexString(deviceId);
        if (deviceIdStr.length() < 2) {
            deviceIdStr = "0" + deviceIdStr;
        }
        sb.append(deviceIdStr);
        return sb.toString();
    }

    public static String generateMacWithDeviceIdRandom(short deviceId) {
        int seed = random.nextInt();
        String seedStr = Integer.toHexString(seed);
        if (seedStr.length() < 8) {
            String compensate = StringUtils.repeat("0", 8 - seedStr.length());
            seedStr = compensate + seedStr;
        }
        String octet2 = seedStr.substring(0, 2);
        String octet3 = seedStr.substring(2, 4);
        String octet4 = seedStr.substring(4, 6);
        String octet5 = seedStr.substring(6, 8);
        StringBuilder sb = new StringBuilder("fa").append(":");
        sb.append(octet2).append(":");
        sb.append(octet3).append(":");
        sb.append(octet4).append(":");
        sb.append(octet5).append(":");
        String deviceIdStr = Integer.toHexString(deviceId);
        if (deviceIdStr.length() < 2) {
            deviceIdStr = "0" + deviceIdStr;
        }
        sb.append(deviceIdStr);
        return sb.toString();
    }
}
