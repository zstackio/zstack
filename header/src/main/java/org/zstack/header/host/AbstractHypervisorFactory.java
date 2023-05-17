package org.zstack.header.host;

import org.zstack.header.AbstractService;
import org.zstack.utils.message.OperationChecker;

public abstract class AbstractHypervisorFactory extends AbstractService implements HypervisorFactory {

    protected static OperationChecker allowedOperations = new OperationChecker(true);

    @Override
    public boolean isAllowedOperation(String opName, String vmState) {
        return allowedOperations.isOperationAllowed(opName, vmState);
    }
}
