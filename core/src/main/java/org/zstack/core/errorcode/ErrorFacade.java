package org.zstack.core.errorcode;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;

import java.util.List;

/**
 */
public interface ErrorFacade {
    ErrorCode instantiateErrorCode(Enum code, ErrorCode cause);

    ErrorCode instantiateErrorCode(String code, ErrorCode cause);

    ErrorCode instantiateErrorCode(Enum code, String details, ErrorCode cause);

    ErrorCode instantiateErrorCode(String code, String details, ErrorCode cause);

    ErrorCode stringToOperationError(String details, ErrorCode cause);

    ErrorCode stringToExternalError(String details, ErrorCode cause);

    ErrorCodeList instantiateErrorCode(Enum code, List<ErrorCode> causes);

    ErrorCodeList instantiateErrorCode(String code, List<ErrorCode> causes);

    ErrorCodeList instantiateErrorCode(Enum code, String details, List<ErrorCode> causes);

    ErrorCodeList instantiateErrorCode(String code, String details, List<ErrorCode> causes);

    ErrorCodeList stringToOperationError(String details, List<ErrorCode> causes);

    ErrorCode instantiateErrorCode(Enum code, String details);

    ErrorCode instantiateErrorCode(String code, String details);

    ErrorCode stringToInternalError(String details);

    ErrorCode throwableToInternalError(Throwable t);

    ErrorCode stringToTimeoutError(String details);

    ErrorCode throwableToTimeoutError(Throwable t);

    ErrorCode stringToOperationError(String details);

    ErrorCode throwableToOperationError(Throwable t);

    ErrorCode stringToInvalidArgumentError(String details);

    ErrorCode throwableToInvalidArgumentError(Throwable t);
}
