package org.zstack.utils.message;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OperationChecker {
    private Map<String, Set<String>> states = new HashMap<String, Set<String>>();
    private final boolean allowedWhenHavingState;

    public OperationChecker() {
        this(true);
    }

    public OperationChecker(boolean allowedWhenHavingState) {
        this.allowedWhenHavingState = allowedWhenHavingState;
    }

    public OperationChecker addState(Enum state, String...opNames) {
        return addState(state.toString(), opNames);
    }

    public OperationChecker addState(String stateName, String...opNames) {
        for (String opName : opNames) {
            Set<String> ss = states.get(opName);
            if (ss == null) {
                ss = new HashSet<String>();
                states.put(opName, ss);
            }

            ss.add(stateName);
        }

        return this;
    }

    public boolean isOperationAllowed(String operationName, String state) {
        return isOperationAllowed(operationName, state, true);
    }

    public boolean isOperationAllowed(String operationName, String state, boolean exceptionIfNoOperation) {
        Set<String> ops = states.get(operationName);
        if (exceptionIfNoOperation) {
            if (ops == null) {
                throw new IllegalArgumentException(String.format("Unable to find allowed states for operation[%s]", operationName));
            }
        } else {
            if (ops == null) {
                return true;
            }
        }
        
        return allowedWhenHavingState ? ops.contains(state) : !ops.contains(state);
    }

    public Set<String> getStatesForOperation(String operationName) {
        Set<String> ops = states.get(operationName);
        if (ops == null) {
            throw new IllegalArgumentException(String.format("Unable to find allowed states for operation[%s]", operationName));
        }
        return ops;
    }
}
