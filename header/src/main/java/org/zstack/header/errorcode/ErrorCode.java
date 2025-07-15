package org.zstack.header.errorcode;

import org.apache.commons.collections.CollectionUtils;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.rest.APINoSee;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.string.ErrorCodeElaboration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class ErrorCode implements Serializable, Cloneable {
    private String code;
    private String description;
    private String details;
    private String elaboration;
    @APINoSee
    private ErrorCodeElaboration messages;
    @APINoSee
    private String cost;
    /**
     * TODO: merge cause to causes
     */
    @Deprecated
    private ErrorCode cause;
    protected final List<ErrorCode> causes = Collections.synchronizedList(new ArrayList<>());
    @NoJsonSchema
    private LinkedHashMap<String, Object> opaque;

    public static final String OPAQUE_KEY_LOCATION = "error.location";

    public LinkedHashMap getOpaque() {
        return opaque;
    }

    public void setOpaque(LinkedHashMap opaque) {
        this.opaque = opaque;
    }

    public ErrorCode withOpaque(String key, Object value) {
        if (opaque == null) {
            opaque = new LinkedHashMap<>();
        }
        opaque.put(key, value);
        return this;
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
        this.messages = other.messages;
        this.cause = other.cause;
        this.causes.addAll(other.causes);
        if (other.opaque != null) {
            this.opaque = new LinkedHashMap<>(other.opaque);
        }
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
        return new ErrorCode(this);
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

    public List<ErrorCode> getCauses() {
        return causes;
    }

    public void setCauses(List<ErrorCode> causes) {
        this.causes.clear();
        if (!CollectionUtils.isEmpty(causes))
            this.causes.addAll(causes);
    }

    public ErrorCode withCause(ErrorCode cause) {
        causes.add(cause);
        return this;
    }

    public ErrorCode withCause(Collection<ErrorCode> causes) {
        this.causes.addAll(causes);
        return this;
    }

    public ErrorCode causedBy(ErrorCode cause) {
        setCause(cause);
        return this;
    }

    /**
     * use "withCause"
     */
    @Deprecated
    public ErrorCode causedBy(List<ErrorCode> cause) {
        return withCause(cause);
    }

    public String getElaboration() {
        return elaboration;
    }

    public void setElaboration(String elaboration) {
        this.elaboration = elaboration;
    }

    public boolean isError(Enum<?>... errorEnums) {
        for (Enum<?> e : errorEnums) {
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
                Objects.equals(this.cause, other.cause) &&
                Objects.equals(this.details, other.details) &&
                Objects.equals(this.opaque, other.opaque) &&
                Objects.equals(this.causes, other.causes);
    }

    @Override
    public int hashCode() {
        StringBuilder sb = new StringBuilder();
        sb.append(code == null ? "" : code);
        sb.append(details == null ? "" : details);
        sb.append(opaque == null ? "" : opaque);
        sb.append(cause == null ? "" : cause.toString());
        sb.append(causes.isEmpty() ? "" : causes.toString());
        return sb.toString().hashCode();
    }

    public String getReadableDetails() {
        ErrorCode root = this;
        StringBuffer errorBuf = new StringBuffer();
        if (CollectionUtils.isNotEmpty(root.causes)) {
            root.causes.forEach(cause -> {
                if (errorBuf.length() > 0) {
                    errorBuf.append(",");
                }
                errorBuf.append(cause.getReadableDetails());
            });
            return errorBuf.toString().trim();
        }

        return getRootCauseDetails();
    }

    public ErrorCode getRootCause() {
        ErrorCode root = this;
        do {
            if (root.cause != null) {
                root = root.cause;
            } else {
                break;
            }
        } while (true);
        return root;
    }

    public String getRootCauseDetails() {
        if (cause == null) {
            return getDetails() != null ? getDetails() : getDescription();
        }

        ErrorCode root = getRootCause();
        return root.getReadableDetails();
    }

    public ErrorCodeElaboration getMessages() {
        return messages;
    }

    public void setMessages(ErrorCodeElaboration messages) {
        this.messages = messages;
    }

    public String getCost() {
        return cost;
    }

    public void setCost(String cost) {
        this.cost = cost;
    }

    public String getLocation() {
        return (String) getFromOpaque(OPAQUE_KEY_LOCATION);
    }

    public void setLocation(String location) {
        withOpaque(OPAQUE_KEY_LOCATION, location);
    }
}
