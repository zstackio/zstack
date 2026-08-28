package org.zstack.network.service.virtualrouter.vyos;

import org.junit.Test;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.header.message.Message;
import org.zstack.header.vm.APIRebootVmInstanceMsg;
import org.zstack.header.vm.APIStartVmInstanceMsg;
import org.zstack.header.vm.RebootVmInstanceMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VyosDeployAgentFlowTest {
    private Map<String, Object> flowDataWithMessage(Message message) {
        VmInstanceSpec spec = new VmInstanceSpec();
        spec.setMessage(message);

        Map<String, Object> data = new HashMap<>();
        data.put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        return data;
    }

    @Test
    public void apiReconnectUsesExistingFlowFlag() {
        Map<String, Object> data = new HashMap<>();
        data.put(ApplianceVmConstant.Params.fromApi.toString(), Boolean.TRUE.toString());

        assertTrue(VyosDeployAgentFlow.isFromApi(data));
    }

    @Test
    public void apiStartIsRecognizedFromVmSpec() {
        assertTrue(VyosDeployAgentFlow.isFromApi(flowDataWithMessage(new APIStartVmInstanceMsg())));
    }

    @Test
    public void directApiRebootIsRecognizedFromVmSpec() {
        assertTrue(VyosDeployAgentFlow.isFromApi(flowDataWithMessage(new APIRebootVmInstanceMsg())));
    }

    @Test
    public void forwardedApiRebootPreservesApiSource() {
        RebootVmInstanceMsg message = new RebootVmInstanceMsg();
        message.setFromApi(true);

        assertTrue(VyosDeployAgentFlow.isFromApi(flowDataWithMessage(message)));
    }

    @Test
    public void internalRebootIsNotRecognizedAsApi() {
        assertFalse(VyosDeployAgentFlow.isFromApi(flowDataWithMessage(new RebootVmInstanceMsg())));
    }

    @Test
    public void startWithoutAnyDeployTriggerIsSkipped() {
        assertTrue(VyosDeployAgentFlow.shouldSkipDeployOnStart(false, false, false, false));
    }

    @Test
    public void apiStartAndRebootBypassDeployOnStartConfig() {
        assertFalse(VyosDeployAgentFlow.shouldSkipDeployOnStart(false, true, false, false));
    }

    @Test
    public void existingDeployTriggersKeepTheirBehavior() {
        assertFalse(VyosDeployAgentFlow.shouldSkipDeployOnStart(true, false, false, false));
        assertFalse(VyosDeployAgentFlow.shouldSkipDeployOnStart(false, false, true, false));
        assertFalse(VyosDeployAgentFlow.shouldSkipDeployOnStart(false, false, false, true));
    }
}
