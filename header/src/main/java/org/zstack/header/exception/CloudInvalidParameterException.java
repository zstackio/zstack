package org.zstack.header.exception;

public class CloudInvalidParameterException extends CloudRuntimeException implements CloudException {
    private static final long serialVersionUID = SerialVersionUID.CloudInvalidParameterException;

    public CloudInvalidParameterException(String parameterName, String value, String msg) {
        super(new StringBuilder("[Invalid Parameter: ").append(parameterName).append(" has value ").append(value).append("]: ").append(msg).toString());
    }
}
