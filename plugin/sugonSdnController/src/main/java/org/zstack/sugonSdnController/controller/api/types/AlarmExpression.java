//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class AlarmExpression extends ApiPropertyBase {
    String operation;
    String operand1;
    AlarmOperand2 operand2;
    List<String> variables;
    public AlarmExpression() {
    }
    public AlarmExpression(String operation, String operand1, AlarmOperand2 operand2, List<String> variables) {
        this.operation = operation;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.variables = variables;
    }
    public AlarmExpression(String operation) {
        this(operation, null, null, null);    }
    public AlarmExpression(String operation, String operand1) {
        this(operation, operand1, null, null);    }
    public AlarmExpression(String operation, String operand1, AlarmOperand2 operand2) {
        this(operation, operand1, operand2, null);    }
    
    public String getOperation() {
        return operation;
    }
    
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    
    public String getOperand1() {
        return operand1;
    }
    
    public void setOperand1(String operand1) {
        this.operand1 = operand1;
    }
    
    
    public AlarmOperand2 getOperand2() {
        return operand2;
    }
    
    public void setOperand2(AlarmOperand2 operand2) {
        this.operand2 = operand2;
    }
    
    
    public List<String> getVariables() {
        return variables;
    }
    
    
    public void addVariables(String obj) {
        if (variables == null) {
            variables = new ArrayList<String>();
        }
        variables.add(obj);
    }
    public void clearVariables() {
        variables = null;
    }
    
}
