package org.zstack.header.server;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.HashMap;
import java.util.Map;

public enum PhysicalServerState {
    Enabled,
    Disabled,
    Maintenance;

    static {
        Enabled.transactions(
                new Transaction(PhysicalServerStateEvent.enable, PhysicalServerState.Enabled),
                new Transaction(PhysicalServerStateEvent.disable, PhysicalServerState.Disabled),
                new Transaction(PhysicalServerStateEvent.maintain, PhysicalServerState.Maintenance)
        );

        Disabled.transactions(
                new Transaction(PhysicalServerStateEvent.enable, PhysicalServerState.Enabled),
                new Transaction(PhysicalServerStateEvent.disable, PhysicalServerState.Disabled),
                new Transaction(PhysicalServerStateEvent.maintain, PhysicalServerState.Maintenance)
        );

        Maintenance.transactions(
                new Transaction(PhysicalServerStateEvent.enable, PhysicalServerState.Enabled),
                new Transaction(PhysicalServerStateEvent.disable, PhysicalServerState.Disabled)
        );
    }

    private static class Transaction {
        PhysicalServerStateEvent event;
        PhysicalServerState nextState;

        private Transaction(PhysicalServerStateEvent event, PhysicalServerState nextState) {
            this.event = event;
            this.nextState = nextState;
        }
    }

    private void transactions(Transaction... transactions) {
        for (Transaction tran : transactions) {
            transactionMap.put(tran.event, tran);
        }
    }

    private Map<PhysicalServerStateEvent, Transaction> transactionMap = new HashMap<PhysicalServerStateEvent, Transaction>();

    public PhysicalServerState nextState(PhysicalServerStateEvent event) {
        Transaction tran = transactionMap.get(event);
        if (tran == null) {
            throw new CloudRuntimeException(String.format("cannot find next state for current state[%s] on event[%s]",
                    this, event));
        }

        return tran.nextState;
    }
}
