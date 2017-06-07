package org.zstack.header.errorcode;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.gson.JSONObjectUtil;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class ErrorCode implements Serializable, Cloneable {
    private String code;
    private String description;
    private String details;
    private String elaboration;
    private ErrorCode cause;
    private LinkedHashMap opaque;

    public LinkedHashMap getOpaque() {
        return opaque;
    }

    public void setOpaque(LinkedHashMap opaque) {
        this.opaque = opaque;
    }

    public void putToOpaque(String key, Object value) {
        if (opaque == null) {
            opaque = new LinkedHashMap();
        }
        opaque.put(key, value);
    }

    public Object getFromOpaque(String key) {
        return opaque == null ? null : opaque.get(key);
    }

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

    public ErrorCode(ErrorCode other) {
        this.code = other.code;
        this.description = other.description;
        this.details = other.details;
        this.elaboration = other.elaboration;
        this.cause = other.cause;
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
        return JSONObjectUtil.toJsonString(this);
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

    public ErrorCode causedBy(ErrorCode cause) {
        setCause(cause);
        return this;
    }

    public ErrorCode causedBy(List<ErrorCode> cause) {
        ((ErrorCodeList) this).setCauses(cause);
        return this;
    }

    public String getElaboration() {
        return elaboration;
    }

    public void setElaboration(String elaboration) {
        this.elaboration = elaboration;
    }

    public boolean isError(Enum... errorEnums) {
        for (Enum e : errorEnums) {
            if (e.toString().equals(getCode())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean equals(Object t) {
        if (this == t){
            return true;
        }
        if (!(t instanceof ErrorCode)){
            return false;
        }

        ErrorCode other = (ErrorCode)t;
        return Objects.equals(this.code, other.code) &&
                // might has endless loop
                Objects.equals(this.cause, other.cause) &&
                Objects.equals(this.details, other.details) &&
                Objects.equals(this.opaque, other.opaque);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = result * 31 + (code == null ? 0 : code.hashCode());
        //TODO: might has endless loop
        //result = result * 31 + (cause == null || cause.details == null ? 0 : cause.details.hashCode());
        result = result * 31 + (cause == null ? 0 : cause.hashCode());
        result = result * 31 + (details == null ? 0 : details.hashCode());
        result = result * 31 + (opaque == null ? 0 : opaque.hashCode());

        return result;
    }
}
