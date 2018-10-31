package org.zstack.compute.vm;

import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.VmInstance;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.SystemTagCreator;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static org.zstack.core.Platform.operr;
import static org.zstack.header.vm.VmInstanceConstant.MAXIMUM_MOUNT_ISO_NUMBER;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by xing5 on 2016/5/26.
 */
public class IsoOperator {
    public static List<String> getIsoUuidByVmUuid(String vmUuid) {
        List<String> result = new ArrayList<>();

        List<Map<String, String>> tokenList = VmSystemTags.ISO.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            result.add(tokens.get(VmSystemTags.ISO_TOKEN));
        }

        return result;
    }

    public static List<String> getVmUuidByIsoUuid(String isoUuid) {
        List<String> result = Q.New(SystemTagVO.class)
                .select(SystemTagVO_.resourceUuid)
                .eq(SystemTagVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .like(SystemTagVO_.tag, String.format("%s::%s%s", VmSystemTags.ISO_TOKEN, isoUuid,"%"))
                .listValues();
        return result;
    }

    public static void checkAttachIsoToVm(String vmUuid, String isoUuid) {
        if (!isIsoAttachedToVm(vmUuid)) {
            return;
        }

        List<String> isoList = getIsoUuidByVmUuid(vmUuid);

        if (isoList.size() >= MAXIMUM_MOUNT_ISO_NUMBER) {
            throw new OperationFailureException(operr("VM[uuid:%s] can only attach up to 3 ISOs", vmUuid));
        }

        if (isoList.contains(isoUuid)) {
            throw new OperationFailureException(operr("VM[uuid:%s] has attached ISO[uuid:%s]", vmUuid, isoUuid));
        }
    }

    public void attachIsoToVm(String vmUuid, String isoUuid) {
        Integer isoDeviceId = getIsoDeviceId(vmUuid, isoUuid);
        if (isoDeviceId == null) {
           isoDeviceId = getNextVolumeDeviceId(vmUuid);
        }

        SystemTagCreator creator = VmSystemTags.ISO.newSystemTagCreator(vmUuid);
        creator.setTagByTokens(map(
                e(VmSystemTags.ISO_TOKEN, isoUuid),
                e(VmSystemTags.ISO_DEVICEID_TOKEN, isoDeviceId)
        ));
        creator.inherent = true;
        creator.create();
    }

    public static Integer getIsoDeviceId(String vmUuid, String isoUuid) {
        List<Map<String, String>> tokenList = VmSystemTags.ISO.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            if (tokens.isEmpty()) {
                continue;
            }

            if (isoUuid.equals(tokens.get(VmSystemTags.ISO_TOKEN))) {
                return Integer.parseInt(tokens.get(VmSystemTags.ISO_DEVICEID_TOKEN));
            }
        }

        return null;
    }

    public static int getNextVolumeDeviceId(String vmUuid) {
        List<Integer> deviceIds = new ArrayList<>();

        List<Map<String, String>> tokenList = VmSystemTags.ISO.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            deviceIds.add((Integer.parseInt(tokens.get(VmSystemTags.ISO_DEVICEID_TOKEN))));
        }

        if (deviceIds.size() >= MAXIMUM_MOUNT_ISO_NUMBER) {
            throw new OperationFailureException(operr("VM[uuid:%s] can only attach up to 3 ISOs", vmUuid));
        }

        BitSet full = new BitSet(deviceIds.size() + 1);
        deviceIds.forEach(full::set);

        int result = full.nextClearBit(0);
        if (result >= MAXIMUM_MOUNT_ISO_NUMBER) {
            throw new OperationFailureException(operr("VM[uuid:%s] can only attach up to 3 ISOs", vmUuid));
        }

        return result;
    }

    public static void detachIsoFromVm(String vmUuid, String isoUuid) {
        int deviceId = getIsoDeviceId(vmUuid, isoUuid);
        VmSystemTags.ISO.deleteInherentTag(vmUuid, VmSystemTags.ISO.instantiateTag(map(
                e(VmSystemTags.ISO_TOKEN, isoUuid),
                e(VmSystemTags.ISO_DEVICEID_TOKEN, deviceId)
        )));

    }

    public static boolean isIsoAttachedToVm(String vmUuid) {
        return VmSystemTags.ISO.hasTag(vmUuid);
    }

    public static void checkIsoSystemTag(String vmUuid) {
        if (!isIsoAttachedToVm(vmUuid)) {
            return;
        }

        List<String> isoUuids = getIsoUuidByVmUuid(vmUuid);

        if (isoUuids.size() > MAXIMUM_MOUNT_ISO_NUMBER) {
            throw new OperationFailureException(operr("VM[uuid:%s] can only attach up to 3 ISOs，Please detach the extra iso", vmUuid));
        }

        List<String> repeatedIsoUuids = isoUuids.stream()
                .collect(Collectors.toMap(e -> e, e -> 1, (a, b) -> a + b))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(entry -> entry.getKey())
                .collect(Collectors.toList());

        if (!repeatedIsoUuids.isEmpty()) {
            throw new OperationFailureException(operr("VM[uuid:%s] attached duplicate ios[uuid:%s]，Please detach the extra iso", vmUuid, repeatedIsoUuids));
        }

        List<Integer> repeatedIsoDeviceId = new ArrayList<>();
        List<Map<String, String>> tokenList = VmSystemTags.ISO.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            if (tokens.isEmpty()) {
                continue;
            }

            String isoUuid = tokens.get(VmSystemTags.ISO_TOKEN);
            Integer deviceId = Integer.parseInt(tokens.get(VmSystemTags.ISO_DEVICEID_TOKEN));
            if (repeatedIsoDeviceId.contains(deviceId)) {
                throw new OperationFailureException(operr("VM[uuid:%s] attached ios[%s] deviceId repeat，please detach the iso[%s], and then re-attach iso[%s]", vmUuid, isoUuid, isoUuid, isoUuid));
            }
            repeatedIsoDeviceId.add(deviceId);
        }
    }
}
