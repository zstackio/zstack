package org.zstack.header.errorcode;

import org.zstack.header.exception.CloudRuntimeException;

import java.io.Serializable;

public class ErrorCode implements Serializable, Cloneable {
    private String code;
    private String description;
    private String details;
    private String elaboration;
    private ErrorCode cause;

    public ErrorCode() {
    }

    public ErrorCode(String code, String description) {
        super();
        this.code = code;
        this.description = description;
    }
    
    public ErrorCode(String code, String description, String details) {
        super();
        this.code = code;
        this.description = description;
        this.details = details;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
    
    public ErrorCode copy() {
        try {
            return (ErrorCode) this.clone();
        } catch (CloneNotSupportedException e) {
            throw new CloudRuntimeException(e);
        }
    }
    
    @Override
    public String toString() {
        return cause == null ? String.format("ErrorCode [code = %s, description = %s, details = %s]", this.getCode(), this.getDescription(), this.getDetails()) :
                String.format("ErrorCode [code = %s, description = %s, details = %s, cause = %s]", this.getCode(), this.getDescription(), this.getDetails(), cause.toString());
    }
    
    public static ErrorCode fromString(String err) {
        String arr = err.replace("ErrorCode", "").replace("[", "").replace("]", "").trim();
        try {
            String[] items = arr.split(",");
            ErrorCode code = new ErrorCode(items[0].split("=")[1].trim(), items[1].split("=")[1].trim());
            code.setDetails(items[2].split("=")[1].trim());
            return code;
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Cannot deserialize string[%s] to ErrorCode", err), e);
        }
    }

    public ErrorCode getCause() {
        return cause;
    }

    public void setCause(ErrorCode cause) {
        this.cause = cause;
    }

    public String getElaboration() {
        return elaboration;
    }

    public void setElaboration(String elaboration) {
        this.elaboration = elaboration;
    }

    public boolean isError(Enum...errorEnums) {
        for (Enum e : errorEnums) {
            if (e.toString().equals(getCode())) {
                return true;
            }
        }

        return false;
    }
}
