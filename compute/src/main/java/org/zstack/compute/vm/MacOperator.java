package org.zstack.compute.vm;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigInteger;

import static org.zstack.core.Platform.operr;

/**
 * Created by camile on 2017/12/14.
 */
public class MacOperator {
    private static final CLogger logger = Utils.getLogger(MacOperator.class);
    private static final Pattern pattern = Pattern.compile("([a-f0-9]{2}:){5}[a-f0-9]{2}");

    class VmMacStruct {
        private String l3Uuid;
        private String mac;

        public VmMacStruct(String l3Uuid, String mac) {
            this.l3Uuid = l3Uuid;
            this.mac = mac;
        }
    }

    @Autowired
    private DatabaseFacade dbf;

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

    private boolean isMulticastMac(String mac) {
        if (!pattern.matcher(mac.toLowerCase()).matches()){
            throw new OperationFailureException(operr("This is not a valid MAC address [%s]", mac));
        }
        String binaryString = new BigInteger(mac.substring(0,2), 16).toString(2);
        return binaryString.substring(binaryString.length() - 1).equals("1");
    }

    public void validateAvailableMac(String mac) {
        String lowercaseMac = mac.toLowerCase();
        Matcher matcher = pattern.matcher(lowercaseMac);
        if (!matcher.matches()) {
            throw new OperationFailureException(operr("Not a valid MAC address [%s]", mac));
        }
        if ("00:00:00:00:00:00".equals(lowercaseMac) || "ff:ff:ff:ff:ff:ff".equals(lowercaseMac)) {
            throw new OperationFailureException(operr("Disallowed address"));
        }
        if (isMulticastMac(lowercaseMac)){
            throw new OperationFailureException(operr("Expected unicast mac address, found multicast MAC address [%s]", mac));
        }
        if (Q.New(VmNicVO.class).eq(VmNicVO_.mac, lowercaseMac).isExists()) {
            throw new OperationFailureException(operr("Duplicate mac address [%s]", lowercaseMac));
        }
    }
}
