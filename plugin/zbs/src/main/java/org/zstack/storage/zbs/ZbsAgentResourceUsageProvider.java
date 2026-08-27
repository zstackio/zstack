package org.zstack.storage.zbs;

import org.zstack.core.Platform;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_10000;

public class ZbsAgentResourceUsageProvider implements ZbsResourceUsageProvider {
    public static final String GET_RESOURCE_USAGE_PATH =
            "/zbs/primarystorage/resource/usage";

    @Override
    public String getProviderType() {
        return "ZBS_AGENT";
    }

    @Override
    public boolean isAvailable(ZbsNodeRef nodeRef) {
        return nodeRef != null
                && nodeRef.getSerialNumber() != null
                && !nodeRef.getNodeAddresses().isEmpty();
    }

    @Override
    public void query(
            ZbsNodeRef nodeRef,
            Collection<String> cgroupNames,
            ReturnValueCompletion<List<ZbsCgroupResourceUsage>> completion) {
        List<String> addresses = new ArrayList<>(nodeRef.getNodeAddresses());
        Collections.sort(addresses);
        ResourceUsageCommand command = new ResourceUsageCommand();
        command.setCgroupNames(new ArrayList<>(cgroupNames));
        MdsInfo mds = new MdsInfo();
        mds.setAddr(addresses.get(0));
        new ZbsPrimaryStorageMdsBase(mds).httpCall(
                GET_RESOURCE_USAGE_PATH,
                command,
                ResourceUsageResponse.class,
                new ReturnValueCompletion<ResourceUsageResponse>(completion) {
                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }

                    @Override
                    public void success(ResourceUsageResponse response) {
                        String reportedSerial =
                                Platform.normalizeMachineSerialNumber(
                                        response.getPhysicalServerSerialNumber());
                        if (!Objects.equals(
                                nodeRef.getSerialNumber(), reportedSerial)) {
                            completion.fail(operr(
                                    ORG_ZSTACK_CORE_10000,
                                    "ZBS_RESOURCE_USAGE_IDENTITY_MISMATCH: expected serialNumber[%s], reported[%s]",
                                    nodeRef.getSerialNumber(), reportedSerial));
                            return;
                        }
                        completion.success(response.getUsages());
                    }
                });
    }

    public static class ResourceUsageCommand {
        private List<String> cgroupNames = new ArrayList<>();

        public List<String> getCgroupNames() {
            return cgroupNames;
        }

        public void setCgroupNames(List<String> cgroupNames) {
            this.cgroupNames = cgroupNames;
        }
    }

    public static class ResourceUsageResponse extends ZbsMdsBase.AgentResponse {
        private String physicalServerSerialNumber;
        private List<ZbsCgroupResourceUsage> usages = new ArrayList<>();

        public String getPhysicalServerSerialNumber() {
            return physicalServerSerialNumber;
        }

        public void setPhysicalServerSerialNumber(
                String physicalServerSerialNumber) {
            this.physicalServerSerialNumber = physicalServerSerialNumber;
        }

        public List<ZbsCgroupResourceUsage> getUsages() {
            return usages;
        }

        public void setUsages(List<ZbsCgroupResourceUsage> usages) {
            this.usages = usages;
        }
    }
}
