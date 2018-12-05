package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by shixin on 2018/12/05.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DualStackNicSecondaryNetworksOperator {
    private static final CLogger logger = Utils.getLogger(DualStackNicSecondaryNetworksOperator.class);
    @Autowired
    private DatabaseFacade dbf;

    public Map<String, List<String>> getSecondaryNetworksFromSystemTags(List<String> systemTags) {
        Map<String, List<String>> ret = new HashMap<>();

        if (systemTags == null) {
            return ret;
        }

        for (String sysTag : systemTags) {
            if(!VmSystemTags.DUAL_STACK_NIC.isMatch(sysTag)) {
                continue;
            }

            Map<String, String> token = TagUtils.parse(VmSystemTags.DUAL_STACK_NIC.getTagFormat(), sysTag);
            String primaryL3 = token.get(VmSystemTags.DUAL_STACK_NIC_PRIMARY_L3_TOKEN);
            String secondaryL3 = token.get(VmSystemTags.DUAL_STACK_NIC_SECONDARY_L3_TOKEN);
            List<String> l3List = ret.get(primaryL3);
            if (l3List == null) {
                l3List = new ArrayList<>();
                ret.put(primaryL3, l3List);
                l3List.add(primaryL3);
            }
            l3List.add(secondaryL3);
        }

        return ret;
    }

    public Map<String, List<String>> getSecondaryNetworksByVmUuid(String vmUuid) {
        Map<String, List<String>> ret = new HashMap<>();

        List<Map<String, String>> tokenList = VmSystemTags.DUAL_STACK_NIC.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            String primaryL3 = tokens.get(VmSystemTags.DUAL_STACK_NIC_PRIMARY_L3_TOKEN);
            String secondaryL3 = tokens.get(VmSystemTags.DUAL_STACK_NIC_SECONDARY_L3_TOKEN);
            List<String> l3List = ret.get(primaryL3);
            if (l3List == null) {
                l3List = new ArrayList<>();
                ret.put(primaryL3, l3List);
                l3List.add(primaryL3);
            }
            l3List.add(secondaryL3);
        }

        return ret;
    }

    public List<String> getSecondaryNetworksByVmUuidNic(String vmUuid, String l3Uuid) {
        Map<String, List<String>> secondaryNetworksMap = getSecondaryNetworksByVmUuid(vmUuid);
        for (Map.Entry<String, List<String>> e : secondaryNetworksMap.entrySet()) {
            List<String> secondaryNetworksList = e.getValue();
            if (secondaryNetworksList != null && secondaryNetworksList.contains(l3Uuid)) {
                secondaryNetworksList.remove(l3Uuid);
                return secondaryNetworksList;
            }
        }

        return null;
    }

    public void createSecondaryNetworksByVmNic(VmNicInventory nic, String l3Uuid) {
        String vmUuid = nic.getVmInstanceUuid();
        if (vmUuid == null) {
            return;
        }

        List<String> nicL3Uuids = nic.getUsedIps().stream().map(UsedIpInventory::getL3NetworkUuid).collect(Collectors.toList());
        Map<String, List<String>> secondaryNetworksMap = getSecondaryNetworksByVmUuid(vmUuid);

        for (Map.Entry<String, List<String>> e : secondaryNetworksMap.entrySet()) {
            String primaryL3 = e.getKey();
            List<String> secondaryNetworksList = e.getValue();

            /* not for this nic */
            if (!nicL3Uuids.contains(primaryL3)) {
                continue;
            }

            /* secondary networks is organized by nic.getL3NetworkUuid(), just add 1 more systemTags */
            if (primaryL3.equals(nic.getL3NetworkUuid())) {
                if (secondaryNetworksList.contains(l3Uuid)) {
                    return;
                }

                SystemTagCreator creator = VmSystemTags.DUAL_STACK_NIC.newSystemTagCreator(vmUuid);
                creator.setTagByTokens(map(
                        e(VmSystemTags.DUAL_STACK_NIC_PRIMARY_L3_TOKEN, nic.getL3NetworkUuid()),
                        e(VmSystemTags.DUAL_STACK_NIC_SECONDARY_L3_TOKEN, l3Uuid)
                ));
                creator.create();

                return;
            }

            /* secondary networks is organized by other l3 network, delete old systemTag,
             * create systemTags use nic.getL3NetworkUuid() as primary network */
            if (secondaryNetworksList.contains(nic.getL3NetworkUuid())) {
                /* delete old systemTags first */
                for (String uuid : secondaryNetworksList) {
                    if (uuid.equals(primaryL3)) {
                        continue;
                    }

                    VmSystemTags.DUAL_STACK_NIC.delete(vmUuid, TagUtils.tagPatternToSqlPattern(VmSystemTags.DUAL_STACK_NIC.instantiateTag(
                            map(e(VmSystemTags.DUAL_STACK_NIC_PRIMARY_L3_TOKEN, primaryL3),
                                    e(VmSystemTags.DUAL_STACK_NIC_SECONDARY_L3_TOKEN, uuid)))));
                }

                /* create new systemTag */
                secondaryNetworksList.add(l3Uuid);
                secondaryNetworksList = secondaryNetworksList.stream().distinct().collect(Collectors.toList());
                for (String uuid : secondaryNetworksList) {
                    if (uuid.equals(nic.getL3NetworkUuid())) {
                        continue;
                    }
                    SystemTagCreator creator = VmSystemTags.DUAL_STACK_NIC.newSystemTagCreator(vmUuid);
                    creator.setTagByTokens(map(
                            e(VmSystemTags.DUAL_STACK_NIC_PRIMARY_L3_TOKEN, nic.getL3NetworkUuid()),
                            e(VmSystemTags.DUAL_STACK_NIC_SECONDARY_L3_TOKEN, uuid)
                    ));
                    creator.create();
                }

                return;
            }

            /* nic primary l3 changed, add old primary l3 as secondary network */
            SystemTagCreator creator = VmSystemTags.DUAL_STACK_NIC.newSystemTagCreator(vmUuid);
            creator.setTagByTokens(map(
                    e(VmSystemTags.DUAL_STACK_NIC_PRIMARY_L3_TOKEN, nic.getL3NetworkUuid()),
                    e(VmSystemTags.DUAL_STACK_NIC_SECONDARY_L3_TOKEN, primaryL3)
            ));
            creator.create();
            return;
        }


        nicL3Uuids.remove(nic.getL3NetworkUuid());
        for (String uuid : nicL3Uuids) {
            SystemTagCreator creator = VmSystemTags.DUAL_STACK_NIC.newSystemTagCreator(vmUuid);
            creator.setTagByTokens(map(
                    e(VmSystemTags.DUAL_STACK_NIC_PRIMARY_L3_TOKEN, nic.getL3NetworkUuid()),
                    e(VmSystemTags.DUAL_STACK_NIC_SECONDARY_L3_TOKEN, uuid)
            ));
            creator.create();
        }
    }

    public void deleteSecondaryNetworksByVmNic(VmNicInventory nic, String l3Uuid) {
        String vmUuid = nic.getVmInstanceUuid();
        if (vmUuid == null) {
            return;
        }

        Map<String, List<String>> secondaryNetworksMap = getSecondaryNetworksByVmUuid(vmUuid);
        for (Map.Entry<String, List<String>> e : secondaryNetworksMap.entrySet()) {
            String primaryL3 = e.getKey();
            List<String> secondaryNetworksList = e.getValue();

            if (primaryL3.equals(l3Uuid)) {
                /* delete old systemTags */
                for (String uuid : secondaryNetworksList) {
                    VmSystemTags.DUAL_STACK_NIC.delete(vmUuid, TagUtils.tagPatternToSqlPattern(VmSystemTags.DUAL_STACK_NIC.instantiateTag(
                            map(e(VmSystemTags.DUAL_STACK_NIC_PRIMARY_L3_TOKEN, primaryL3),
                                    e(VmSystemTags.DUAL_STACK_NIC_SECONDARY_L3_TOKEN, uuid)))));
                }

                /* if more than 2 secondary network, after delete 1 network, still need to re-organize the systemTags*/
                if (secondaryNetworksList.size() <= 2) {
                    return;
                }

                secondaryNetworksList.remove(primaryL3);
                secondaryNetworksList.remove(nic.getL3NetworkUuid());
                for (String uuid : secondaryNetworksList) {
                    SystemTagCreator creator = VmSystemTags.DUAL_STACK_NIC.newSystemTagCreator(vmUuid);
                    creator.setTagByTokens(map(
                            e(VmSystemTags.DUAL_STACK_NIC_PRIMARY_L3_TOKEN, nic.getL3NetworkUuid()),
                            e(VmSystemTags.DUAL_STACK_NIC_SECONDARY_L3_TOKEN, uuid)
                    ));
                    creator.create();
                }

                return;
            }

            if (secondaryNetworksList.contains(l3Uuid)) {
                VmSystemTags.DUAL_STACK_NIC.delete(vmUuid, TagUtils.tagPatternToSqlPattern(VmSystemTags.DUAL_STACK_NIC.instantiateTag(
                        map(e(VmSystemTags.DUAL_STACK_NIC_PRIMARY_L3_TOKEN, primaryL3),
                                e(VmSystemTags.DUAL_STACK_NIC_SECONDARY_L3_TOKEN, l3Uuid)))));
                return;
            }
        }
    }

    public void deleteSecondaryNetworksByVmNic(VmNicInventory nic) {
        String vmUuid = nic.getVmInstanceUuid();
        if (vmUuid == null) {
            return;
        }

        String l3Uuid = nic.getL3NetworkUuid();
        Map<String, List<String>> secondaryNetworksMap = getSecondaryNetworksByVmUuid(vmUuid);
        for (Map.Entry<String, List<String>> e : secondaryNetworksMap.entrySet()) {
            String primaryL3 = e.getKey();
            List<String> secondaryNetworksList = e.getValue();

            if (primaryL3.equals(l3Uuid) || secondaryNetworksList.contains(l3Uuid)) {
                /* delete old systemTags */
                for (String uuid : secondaryNetworksList) {
                    VmSystemTags.DUAL_STACK_NIC.delete(vmUuid, TagUtils.tagPatternToSqlPattern(VmSystemTags.DUAL_STACK_NIC.instantiateTag(
                            map(e(VmSystemTags.DUAL_STACK_NIC_PRIMARY_L3_TOKEN, primaryL3),
                                    e(VmSystemTags.DUAL_STACK_NIC_SECONDARY_L3_TOKEN, uuid)))));
                }

                return;
            }
        }
    }
}
