package org.zstack.compute.vm;

import org.apache.commons.lang.StringUtils;
import org.zstack.utils.TagUtils;

import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class CustomNicOperator {
    public String getCustomNicId(String vmUuid, String l3Uuid) {
        List<Map<String, String>> tokenList = VmSystemTags.CUSTOM_NIC_UUID.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            if (StringUtils.equals(tokens.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN), l3Uuid)) {
                return tokens.get(VmSystemTags.NIC_UUID_TOKEN);
            }
        }
        return null;
    }

    public void deleteNicTags(String vmUuid, String l3NetworkUuid) {
        VmSystemTags.CUSTOM_MAC.delete(vmUuid, TagUtils.tagPatternToSqlPattern(VmSystemTags.CUSTOM_MAC.instantiateTag(
                map(e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3NetworkUuid))
        )));
        VmSystemTags.STATIC_IP.delete(vmUuid, TagUtils.tagPatternToSqlPattern(VmSystemTags.STATIC_IP.instantiateTag(
                map(e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3NetworkUuid))
        )));
        VmSystemTags.CUSTOM_NIC_UUID.delete(vmUuid, TagUtils.tagPatternToSqlPattern(VmSystemTags.CUSTOM_NIC_UUID.instantiateTag(
                map(e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3NetworkUuid))
        )));
    }
}
