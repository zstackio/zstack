package org.zstack.network.service.virtualrouter.vyos;

import org.junit.Test;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.header.message.Message;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VyosDeployAgentFlowTest {
    private Map<String, Object> flowDataWithMessage(Message message, VmOperation operation) {
        VmInstanceSpec spec = new VmInstanceSpec();
        spec.setMessage(message);
        spec.setCurrentVmOperation(operation);
        Map<String, Object> data = new HashMap<>();
        data.put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        return data;
    }

    private RebootVmInstanceMsg forwardedApiReboot() {
        RebootVmInstanceMsg message = new RebootVmInstanceMsg();
        message.setFromApi(true);
        return message;
    }

    @Test
    public void optedInApiStartAndRebootUpgradeDuringGrayscale() {
        assertTrue(VyosDeployAgentFlow.shouldUpgradeOnApiStartOrReboot(
                flowDataWithMessage(new APIStartVmInstanceMsg(), VmOperation.Start), true, true));
        assertTrue(VyosDeployAgentFlow.shouldUpgradeOnApiStartOrReboot(
                flowDataWithMessage(new APIRebootVmInstanceMsg(), VmOperation.Reboot), true, true));
        assertTrue(VyosDeployAgentFlow.shouldUpgradeOnApiStartOrReboot(
                flowDataWithMessage(forwardedApiReboot(), VmOperation.Reboot), true, true));
    }

    @Test
    public void otherAppliancesDoNotGainAnApiUpgradeException() {
        assertFalse(VyosDeployAgentFlow.shouldUpgradeOnApiStartOrReboot(
                flowDataWithMessage(new APIStartVmInstanceMsg(), VmOperation.Start), false, true));
        assertFalse(VyosDeployAgentFlow.shouldUpgradeOnApiStartOrReboot(
                flowDataWithMessage(forwardedApiReboot(), VmOperation.Reboot), false, true));
    }

    @Test
    public void nonGrayscaleOperationsKeepTheDeploymentConfig() {
        assertFalse(VyosDeployAgentFlow.shouldUpgradeOnApiStartOrReboot(
                flowDataWithMessage(new APIStartVmInstanceMsg(), VmOperation.Start), true, false));
        assertFalse(VyosDeployAgentFlow.shouldUpgradeOnApiStartOrReboot(
                flowDataWithMessage(forwardedApiReboot(), VmOperation.Reboot), true, false));
    }

    @Test
    public void internalAndHaOperationsDoNotGainAnApiUpgradeException() {
        assertFalse(VyosDeployAgentFlow.shouldUpgradeOnApiStartOrReboot(
                flowDataWithMessage(new StartVmInstanceMsg(), VmOperation.Start), true, true));
        assertFalse(VyosDeployAgentFlow.shouldUpgradeOnApiStartOrReboot(
                flowDataWithMessage(new HaStartVmInstanceMsg(), VmOperation.Start), true, true));
        assertFalse(VyosDeployAgentFlow.shouldUpgradeOnApiStartOrReboot(
                flowDataWithMessage(new RebootVmInstanceMsg(), VmOperation.Reboot), true, true));
    }

    @Test
    public void onlyMatchingStartAndRebootOperationsAreEligible() {
        assertFalse(VyosDeployAgentFlow.shouldUpgradeOnApiStartOrReboot(
                flowDataWithMessage(new APIStartVmInstanceMsg(), VmOperation.NewCreate), true, true));
        assertFalse(VyosDeployAgentFlow.shouldUpgradeOnApiStartOrReboot(
                flowDataWithMessage(new APIStartVmInstanceMsg(), VmOperation.Reboot), true, true));
        assertFalse(VyosDeployAgentFlow.shouldUpgradeOnApiStartOrReboot(
                flowDataWithMessage(forwardedApiReboot(), VmOperation.Start), true, true));
        assertFalse(VyosDeployAgentFlow.shouldUpgradeOnApiStartOrReboot(
                flowDataWithMessage(new APIStartVmInstanceMsg(), null), true, true));
    }

    @Test
    public void reconnectFlagIsNotAStartOrRebootException() {
        Map<String, Object> data = new HashMap<>();
        data.put(ApplianceVmConstant.Params.fromApi.toString(), "true");
        assertFalse(VyosDeployAgentFlow.shouldUpgradeOnApiStartOrReboot(data, true, true));
    }

}
