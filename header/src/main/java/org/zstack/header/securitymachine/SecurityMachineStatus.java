package org.zstack.header.securitymachine;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by LiangHanYu on 2021/10/28 13:12
 */
public enum SecurityMachineStatus {
    Synced,
    Unsynced;

    static {
        Synced.transactions(
                new SecurityMachineStatus.Transaction(SecurityMachineStatusEvent.sync, SecurityMachineStatus.Synced),
                new SecurityMachineStatus.Transaction(SecurityMachineStatusEvent.unsync, SecurityMachineStatus.Unsynced)
        );

        Unsynced.transactions(
                new SecurityMachineStatus.Transaction(SecurityMachineStatusEvent.sync, SecurityMachineStatus.Synced),
                new SecurityMachineStatus.Transaction(SecurityMachineStatusEvent.unsync, SecurityMachineStatus.Unsynced)
        );
    }

    private static class Transaction {
        SecurityMachineStatusEvent event;
        SecurityMachineStatus nextStatus;

        private Transaction(SecurityMachineStatusEvent event, SecurityMachineStatus nextStatus) {
            this.event = event;
            this.nextStatus = nextStatus;
        }
    }

    private void transactions(SecurityMachineStatus.Transaction... transactions) {
        for (SecurityMachineStatus.Transaction tran : transactions) {
            transactionMap.put(tran.event, tran);
        }
    }

    private Map<SecurityMachineStatusEvent, Transaction> transactionMap = new HashMap<SecurityMachineStatusEvent, Transaction>();

    public SecurityMachineStatus nextStatus(SecurityMachineStatusEvent event) {
        SecurityMachineStatus.Transaction tran = transactionMap.get(event);
        if (tran == null) {
            throw new CloudRuntimeException(String.format("cannot find next status for current status[%s] on transaction event[%s]",
                    this, event));
        }

        return tran.nextStatus;
    }
}
