package org.zstack.storage.cdp;

import org.zstack.core.Platform;
import org.zstack.core.statemachine.StateMachine;
import org.zstack.header.storage.backup.*;
import org.zstack.utils.message.OperationChecker;

public abstract class AbstractCdpBackupStorage implements BackupStorage {
    protected final static StateMachine<BackupStorageState, BackupStorageStateEvent> states;
    protected final static OperationChecker statusChecker = new OperationChecker();
    protected final static OperationChecker stateChecker = new OperationChecker();

    static {
        states = Platform.<BackupStorageState, BackupStorageStateEvent>createStateMachine();
        states.addTranscation(BackupStorageState.Enabled, BackupStorageStateEvent.disable, BackupStorageState.Disabled);
        states.addTranscation(BackupStorageState.Enabled, BackupStorageStateEvent.enable, BackupStorageState.Enabled);
        states.addTranscation(BackupStorageState.Disabled, BackupStorageStateEvent.disable, BackupStorageState.Disabled);
        states.addTranscation(BackupStorageState.Disabled, BackupStorageStateEvent.enable, BackupStorageState.Enabled);

        statusChecker.addState(BackupStorageStatus.Connecting.toString());
        statusChecker.addState(BackupStorageStatus.Disconnected.toString());

        stateChecker.addState(BackupStorageState.Disabled.toString());
    }

    public static BackupStorageState getNextState(BackupStorageState curr, BackupStorageStateEvent evt) {
        return states.getNextState(curr, evt);
    }
}
