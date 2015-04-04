package org.zstack.core.errorcode;

import org.zstack.header.errorcode.ErrorCode;

/**
 */
public interface ErrorFacade {
    ErrorCode instantiateErrorCode(Enum code, ErrorCode cause);

    ErrorCode instantiateErrorCode(String code, ErrorCode cause);

    ErrorCode instantiateErrorCode(Enum code, String details, ErrorCode cause);

    ErrorCode instantiateErrorCode(String code, String details, ErrorCode cause);

    ErrorCode instantiateErrorCode(Enum code, String details);

    ErrorCode instantiateErrorCode(String code, String details);

    ErrorCode stringToInternalError(String details);

    ErrorCode throwableToInternalError(Throwable t);

    ErrorCode stringToTimeoutError(String details);

    ErrorCode throwableToTimeoutError(Throwable t);

    ErrorCode stringToOperationError(String details);

    ErrorCode stringToOperationError(String details, ErrorCode cause);

    ErrorCode throwableToOperationError(Throwable t);

    ErrorCode stringToInvalidArgumentError(String details);

    ErrorCode throwableToInvalidArgumentError(Throwable t);
}
