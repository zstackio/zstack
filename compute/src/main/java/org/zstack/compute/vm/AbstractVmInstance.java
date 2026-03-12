package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;
import org.zstack.header.vm.APIAttachIsoToVmInstanceMsg;
import org.zstack.header.vm.APIAttachL3NetworkToVmMsg;
import org.zstack.header.vm.APIAttachVmNicToVmMsg;
import org.zstack.header.vm.APIChangeInstanceOfferingMsg;
import org.zstack.header.vm.APIChangeVmNicNetworkMsg;
import org.zstack.header.vm.APIChangeVmNicStateMsg;
import org.zstack.header.vm.APIDeleteVmStaticIpMsg;
import org.zstack.header.vm.APIDestroyVmInstanceMsg;
import org.zstack.header.vm.APIDetachIsoFromVmInstanceMsg;
import org.zstack.header.vm.APIDetachL3NetworkFromVmMsg;
import org.zstack.header.vm.APIExpungeVmInstanceMsg;
import org.zstack.header.vm.APIFlattenVmInstanceMsg;
import org.zstack.header.vm.APIGetVmConsoleAddressMsg;
import org.zstack.header.vm.APIGetVmMigrationCandidateHostsMsg;
import org.zstack.header.vm.APIGetVmStartingCandidateClustersHostsMsg;
import org.zstack.header.vm.APIMigrateVmMsg;
import org.zstack.header.vm.APIPauseVmInstanceMsg;
import org.zstack.header.vm.APIRebootVmInstanceMsg;
import org.zstack.header.vm.APIRecoverVmInstanceMsg;
import org.zstack.header.vm.APIResumeVmInstanceMsg;
import org.zstack.header.vm.APISetVmBootVolumeMsg;
import org.zstack.header.vm.APISetVmStaticIpMsg;
import org.zstack.header.vm.APIStartVmInstanceMsg;
import org.zstack.header.vm.APIStopVmInstanceMsg;
import org.zstack.header.vm.APIUpdateConsolePasswordMsg;
import org.zstack.header.vm.AddL3NetworkToVmNicMsg;
import org.zstack.header.vm.AttachDataVolumeToVmMsg;
import org.zstack.header.vm.AttachIsoToVmInstanceMsg;
import org.zstack.header.vm.AttachNicToVmMsg;
import org.zstack.header.vm.CancelFlattenVmInstanceMsg;
import org.zstack.header.vm.CreateTemplateFromRootVolumeSnapShotVmMsg;
import org.zstack.header.vm.CreateTemplateFromRootVolumeVmMsg;
import org.zstack.header.vm.CreateVmCdRomMsg;
import org.zstack.header.vm.DeleteL3NetworkFromVmNicMsg;
import org.zstack.header.vm.DestroyVmInstanceMsg;
import org.zstack.header.vm.DetachDataVolumeFromVmMsg;
import org.zstack.header.vm.DetachNicFromVmMsg;
import org.zstack.header.vm.ExpungeVmMsg;
import org.zstack.header.vm.FlattenVmInstanceMsg;
import org.zstack.header.vm.GetVmMigrationTargetHostMsg;
import org.zstack.header.vm.GetVmStartingCandidateClustersHostsMsg;
import org.zstack.header.vm.HaStartVmInstanceMsg;
import org.zstack.header.vm.InstantiateNewCreatedVmInstanceMsg;
import org.zstack.header.vm.MigrateVmInnerMsg;
import org.zstack.header.vm.MigrateVmMsg;
import org.zstack.header.vm.RebootVmInstanceMsg;
import org.zstack.header.vm.RecoverVmInstanceMsg;
import org.zstack.header.vm.RestoreVmInstanceMsg;
import org.zstack.header.vm.StartVmInstanceMsg;
import org.zstack.header.vm.StopVmInstanceMsg;
import org.zstack.header.vm.VmAttachNicMsg;
import org.zstack.header.vm.VmErrors;
import org.zstack.header.vm.VmInstance;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceStateEvent;
import org.zstack.header.vm.cdrom.DeleteVmCdRomMsg;
import org.zstack.utils.message.OperationChecker;

import java.util.Set;

import static org.zstack.core.Platform.canerr;
import static org.zstack.core.Platform.err;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractVmInstance implements VmInstance {
    protected static OperationChecker allowedOperations = new OperationChecker(true);
    protected static OperationChecker stateChangeChecker = new OperationChecker(false);

    @Autowired
    protected ErrorFacade errf;

    static {
        allowedOperations.addState(VmInstanceState.Created,
                InstantiateNewCreatedVmInstanceMsg.class.getName(),
                APIStartVmInstanceMsg.class.getName(),
                StartVmInstanceMsg.class.getName()
        );

        allowedOperations.addState(VmInstanceState.NoState,
                APIStopVmInstanceMsg.class.getName(),
                APIRebootVmInstanceMsg.class.getName(),
                RebootVmInstanceMsg.class.getName(),
                StopVmInstanceMsg.class.getName()
        );

        allowedOperations.addState(VmInstanceState.Running,
                APIStopVmInstanceMsg.class.getName(),
                StopVmInstanceMsg.class.getName(),
                APIStartVmInstanceMsg.class.getName(),
                StartVmInstanceMsg.class.getName(),
                APIRebootVmInstanceMsg.class.getName(),
                RebootVmInstanceMsg.class.getName(),
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName(),
                APIMigrateVmMsg.class.getName(),
                MigrateVmInnerMsg.class.getName(),
                MigrateVmMsg.class.getName(),
                AttachDataVolumeToVmMsg.class.getName(),
                DetachDataVolumeFromVmMsg.class.getName(),
                AttachNicToVmMsg.class.getName(),
                VmAttachNicMsg.class.getName(),
                APIAttachL3NetworkToVmMsg.class.getName(),
                APIAttachVmNicToVmMsg.class.getName(),
                GetVmMigrationTargetHostMsg.class.getName(),
                APIChangeInstanceOfferingMsg.class.getName(),
                APIGetVmMigrationCandidateHostsMsg.class.getName(),
                APIDetachL3NetworkFromVmMsg.class.getName(),
                APIChangeVmNicStateMsg.class.getName(),
                DetachNicFromVmMsg.class.getName(),
                APIAttachIsoToVmInstanceMsg.class.getName(),
                AttachIsoToVmInstanceMsg.class.getName(),
                APIDetachIsoFromVmInstanceMsg.class.getName(),
                APIGetVmConsoleAddressMsg.class.getName(),
                APIUpdateConsolePasswordMsg.class.getName(),
                APIDeleteVmStaticIpMsg.class.getName(),
                APIPauseVmInstanceMsg.class.getName(),
                CreateTemplateFromRootVolumeSnapShotVmMsg.class.getName(),
                CreateTemplateFromRootVolumeVmMsg.class.getName(),
                AddL3NetworkToVmNicMsg.class.getName(),
                DeleteL3NetworkFromVmNicMsg.class.getName(),
                APIChangeVmNicNetworkMsg.class.getName(),
                FlattenVmInstanceMsg.class.getName(),
                APIFlattenVmInstanceMsg.class.getName(),
                CancelFlattenVmInstanceMsg.class.getName(),
                APISetVmStaticIpMsg.class.getName()
        );

        allowedOperations.addState(VmInstanceState.Stopped,
                APIStopVmInstanceMsg.class.getName(),
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName(),
                APIStartVmInstanceMsg.class.getName(),
                StartVmInstanceMsg.class.getName(),
                AttachDataVolumeToVmMsg.class.getName(),
                DetachDataVolumeFromVmMsg.class.getName(),
                CreateTemplateFromRootVolumeSnapShotVmMsg.class.getName(),
                CreateTemplateFromRootVolumeVmMsg.class.getName(),
                VmAttachNicMsg.class.getName(),
                APIAttachL3NetworkToVmMsg.class.getName(),
                APIChangeVmNicNetworkMsg.class.getName(),
                APIAttachVmNicToVmMsg.class.getName(),
                APIChangeInstanceOfferingMsg.class.getName(),
                StopVmInstanceMsg.class.getName(),
                APIDetachL3NetworkFromVmMsg.class.getName(),
                APIChangeVmNicStateMsg.class.getName(),
                DetachNicFromVmMsg.class.getName(),
                APIAttachIsoToVmInstanceMsg.class.getName(),
                AttachIsoToVmInstanceMsg.class.getName(),
                APIDetachIsoFromVmInstanceMsg.class.getName(),
                APISetVmStaticIpMsg.class.getName(),
                APIDeleteVmStaticIpMsg.class.getName(),
                StartVmInstanceMsg.class.getName(),
                HaStartVmInstanceMsg.class.getName(),
                APIGetVmStartingCandidateClustersHostsMsg.class.getName(),
                GetVmStartingCandidateClustersHostsMsg.class.getName(),
                DeleteVmCdRomMsg.class.getName(),
                CreateVmCdRomMsg.class.getName(),
                RestoreVmInstanceMsg.class.getName(),
                FlattenVmInstanceMsg.class.getName(),
                APIFlattenVmInstanceMsg.class.getName(),
                CancelFlattenVmInstanceMsg.class.getName(),
                APISetVmBootVolumeMsg.class.getName()
        );

        allowedOperations.addState(VmInstanceState.Unknown,
                APIMigrateVmMsg.class.getName(),
                MigrateVmInnerMsg.class.getName(),
                MigrateVmMsg.class.getName(),
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName(),
                APIStopVmInstanceMsg.class.getName(),
                StopVmInstanceMsg.class.getName());

        allowedOperations.addState(VmInstanceState.Crashed,
                APIStopVmInstanceMsg.class.getName(),
                StopVmInstanceMsg.class.getName(),
                APIStartVmInstanceMsg.class.getName(),
                StartVmInstanceMsg.class.getName(),
                APIRebootVmInstanceMsg.class.getName(),
                RebootVmInstanceMsg.class.getName(),
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName(),
                APIGetVmConsoleAddressMsg.class.getName());

        allowedOperations.addState(VmInstanceState.Starting,
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName());

        allowedOperations.addState(VmInstanceState.Migrating,
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName());

        allowedOperations.addState(VmInstanceState.VolumeMigrating,
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName());

        allowedOperations.addState(VmInstanceState.VolumeRecovering,
                StartVmInstanceMsg.class.getName(),
                StopVmInstanceMsg.class.getName());

        allowedOperations.addState(VmInstanceState.Stopping,
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName());

        allowedOperations.addState(VmInstanceState.Rebooting,
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName());

        allowedOperations.addState(VmInstanceState.Destroyed,
                ExpungeVmMsg.class.getName(),
                APIExpungeVmInstanceMsg.class.getName(),
                APIRecoverVmInstanceMsg.class.getName(),
                RecoverVmInstanceMsg.class.getName());

        allowedOperations.addState(VmInstanceState.Paused,
                APIResumeVmInstanceMsg.class.getName(),
                APIStopVmInstanceMsg.class.getName(),
                StopVmInstanceMsg.class.getName(),
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName(),
                APIMigrateVmMsg.class.getName(),
                MigrateVmInnerMsg.class.getName(),
                MigrateVmMsg.class.getName(),
                AttachDataVolumeToVmMsg.class.getName(),
                DetachDataVolumeFromVmMsg.class.getName(),
                AttachNicToVmMsg.class.getName(),
                VmAttachNicMsg.class.getName(),
                APIAttachL3NetworkToVmMsg.class.getName(),
                APIAttachVmNicToVmMsg.class.getName(),
                GetVmMigrationTargetHostMsg.class.getName(),
                APIChangeInstanceOfferingMsg.class.getName(),
                APIGetVmMigrationCandidateHostsMsg.class.getName(),
                APIDetachL3NetworkFromVmMsg.class.getName(),
                APIChangeVmNicStateMsg.class.getName(),
                DetachNicFromVmMsg.class.getName(),
                APIAttachIsoToVmInstanceMsg.class.getName(),
                AttachIsoToVmInstanceMsg.class.getName(),
                APIDetachIsoFromVmInstanceMsg.class.getName(),
                CreateTemplateFromRootVolumeSnapShotVmMsg.class.getName(),
                CreateTemplateFromRootVolumeVmMsg.class.getName(),
                FlattenVmInstanceMsg.class.getName(),
                APIFlattenVmInstanceMsg.class.getName(),
                CancelFlattenVmInstanceMsg.class.getName(),
                APIDeleteVmStaticIpMsg.class.getName());

        allowedOperations.addState(VmInstanceState.Pausing,
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName());

        allowedOperations.addState(VmInstanceState.Resuming,
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName());


        stateChangeChecker.addState(VmInstanceStateEvent.unknown.toString(),
                VmInstanceState.Created.toString(),
                VmInstanceState.Stopped.toString(),
                VmInstanceState.Destroyed.toString(),
                VmInstanceState.Expunging.toString());
    }

    public static Set<String> getAllowedStatesForOperation(Class<? extends Message> clz) {
        return allowedOperations.getStatesForOperation(clz.getName());
    }

    private ErrorCode validateOperationByState(OperationChecker checker, Message msg, VmInstanceState currentState, Enum errorCode) {
        if (checker.isOperationAllowed(msg.getMessageName(), currentState.toString())) {
            return null;
        } else {
            ErrorCode cause = err(ORG_ZSTACK_COMPUTE_VM_10019, VmErrors.NOT_IN_CORRECT_STATE, "current vm instance state[%s] doesn't allow to proceed message[%s], allowed states are %s", currentState,
                    msg.getMessageName(), checker.getStatesForOperation(msg.getMessageName()));
            if (errorCode != null) {
                return err(ORG_ZSTACK_COMPUTE_VM_10021, errorCode, cause, cause.getDetails());
            } else {
                return cause;
            }
        }
    }
    
    public ErrorCode validateOperationByState(Message msg, VmInstanceState currentState, Enum errorCode) {
        return validateOperationByState(allowedOperations, msg, currentState, errorCode);
    }
    
    public static boolean needChangeState(OperationChecker checker, VmInstanceStateEvent stateEvent, VmInstanceState currentState) {
        return checker.isOperationAllowed(stateEvent.toString(), currentState.toString(), false);
    }
    
    public static boolean needChangeState(VmInstanceStateEvent stateEvent, VmInstanceState currentState) {
        return needChangeState(stateChangeChecker, stateEvent, currentState);
    }
    
}
