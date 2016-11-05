package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;
import org.zstack.header.vm.*;
import org.zstack.utils.message.OperationChecker;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractVmInstance implements VmInstance {
    protected static OperationChecker allowedOperations = new OperationChecker(true);
    protected static OperationChecker stateChangeChecker = new OperationChecker(false);

    @Autowired
    protected ErrorFacade errf;

    static {
        allowedOperations.addState(VmInstanceState.Created,
                StartNewCreatedVmInstanceMsg.class.getName(),
                APIStartVmInstanceMsg.class.getName(),
                StartVmInstanceMsg.class.getName()
        );

        allowedOperations.addState(VmInstanceState.Running,
                APIStopVmInstanceMsg.class.getName(),
                StopVmInstanceMsg.class.getName(),
                APIRebootVmInstanceMsg.class.getName(),
                RebootVmInstanceMsg.class.getName(),
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName(),
                APIMigrateVmMsg.class.getName(),
                MigrateVmMsg.class.getName(),
                AttachDataVolumeToVmMsg.class.getName(),
                DetachDataVolumeFromVmMsg.class.getName(),
                AttachNicToVmMsg.class.getName(),
                VmAttachNicMsg.class.getName(),
                APIAttachL3NetworkToVmMsg.class.getName(),
                GetVmMigrationTargetHostMsg.class.getName(),
                APIChangeInstanceOfferingMsg.class.getName(),
                APIGetVmMigrationCandidateHostsMsg.class.getName(),
                APIDetachL3NetworkFromVmMsg.class.getName(),
                DetachNicFromVmMsg.class.getName(),
                APIAttachIsoToVmInstanceMsg.class.getName(),
                APIDetachIsoFromVmInstanceMsg.class.getName(),
                APIGetVmConsoleAddressMsg.class.getName(),
                APIDeleteVmStaticIpMsg.class.getName(),
                APISuspendVmInstanceMsg.class.getName()
        );

        allowedOperations.addState(VmInstanceState.Stopped,
                APIStopVmInstanceMsg.class.getName(),
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName(),
                APIStartVmInstanceMsg.class.getName(),
                StartVmInstanceMsg.class.getName(),
                AttachDataVolumeToVmMsg.class.getName(),
                DetachDataVolumeFromVmMsg.class.getName(),
                CreateTemplateFromVmRootVolumeMsg.class.getName(),
                VmAttachNicMsg.class.getName(),
                APIAttachL3NetworkToVmMsg.class.getName(),
                APIChangeInstanceOfferingMsg.class.getName(),
                StopVmInstanceMsg.class.getName(),
                APIDetachL3NetworkFromVmMsg.class.getName(),
                DetachNicFromVmMsg.class.getName(),
                APIAttachIsoToVmInstanceMsg.class.getName(),
                APIDetachIsoFromVmInstanceMsg.class.getName(),
                APISetVmStaticIpMsg.class.getName(),
                APIDeleteVmStaticIpMsg.class.getName(),
                StartVmInstanceMsg.class.getName(),
                HaStartVmInstanceMsg.class.getName(),
                APIGetVmStartingCandidateClustersHostsMsg.class.getName()
        );

        allowedOperations.addState(VmInstanceState.Unknown,
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName(),
                APIStopVmInstanceMsg.class.getName(),
                StopVmInstanceMsg.class.getName());

        allowedOperations.addState(VmInstanceState.Starting,
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName());

        allowedOperations.addState(VmInstanceState.Migrating,
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName());

        allowedOperations.addState(VmInstanceState.Stopping,
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName());

        allowedOperations.addState(VmInstanceState.Rebooting,
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName());

        allowedOperations.addState(VmInstanceState.Destroyed,
                ExpungeVmMsg.class.getName(),
                APIExpungeVmInstanceMsg.class.getName(),
                APIRecoverVmInstanceMsg.class.getName());

        allowedOperations.addState(VmInstanceState.Suspended,
                APIResumeVmInstanceMsg.class.getName(),
                APIStopVmInstanceMsg.class.getName(),
                StopVmInstanceMsg.class.getName(),
                APIDestroyVmInstanceMsg.class.getName(),
                DestroyVmInstanceMsg.class.getName());

        allowedOperations.addState(VmInstanceState.Suspending,
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


    private ErrorCode validateOperationByState(OperationChecker checker, Message msg, VmInstanceState currentState, Enum errorCode) {
        if (checker.isOperationAllowed(msg.getMessageName(), currentState.toString())) {
            return null;
        } else {
            String details = String.format("current vm instance state[%s] doesn't allow to proceed message[%s], allowed states are %s", currentState,
                    msg.getMessageName(), checker.getStatesForOperation(msg.getMessageName()));
            ErrorCode cause = errf.instantiateErrorCode(VmErrors.NOT_IN_CORRECT_STATE, details);
            if (errorCode != null) {
                return errf.instantiateErrorCode(errorCode, cause);
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
