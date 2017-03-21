package org.zstack.core.errorcode;

import org.zstack.core.errorcode.schema.Error;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class ErrorFacadeImpl implements ErrorFacade {
    private static final CLogger logger = Utils.getLogger(ErrorFacadeImpl.class);
    private Map<String, ErrorCodeInfo> codes = new HashMap<>();
    private boolean dumpOnError = Boolean.valueOf(System.getProperty("ErrorFacade.dumpOnError"));

    @Override
    public ErrorCode instantiateErrorCode(Enum code, ErrorCode cause) {
        return instantiateErrorCode(code.toString(), cause);
    }

    @Override
    public ErrorCode instantiateErrorCode(String code, ErrorCode cause) {
        return instantiateErrorCode(code, cause.getDetails(), cause);
    }

    @Override
    public ErrorCode instantiateErrorCode(Enum code, String details, ErrorCode cause) {
        return instantiateErrorCode(code.toString(), details, cause);
    }

    @Override
    public ErrorCode instantiateErrorCode(String code, String details, ErrorCode cause) {
        ErrorCode err = instantiateErrorCode(code, details);
        err.setCause(cause);
        return err;
    }

    @Override
    public ErrorCode instantiateErrorCode(Enum code, String details) {
        return instantiateErrorCode(code.toString(), details);
    }

    private ErrorCode doInstantiateErrorCode(String code, String details, List<ErrorCode> causes) {
        ErrorCodeInfo info = codes.get(code);
        if (info == null) {
            throw new CloudRuntimeException(String.format("cannot find error code[%s]", code));
        }

        if (details != null && details.length() > 4096) {
            details = details.substring(0, Math.min(details.length(), 4096));
        }
        ErrorCodeList err = (ErrorCodeList) info.code.copy();
        err.setDetails(details);
        err.setCauses(causes);

        if (dumpOnError) {
            DebugUtils.dumpStackTrace(String.format("An error code%s is instantiated," +
                    " for tracing the place error happened, dump stack as below", err));
        }

        return err;
    }

    @Override
    public ErrorCode instantiateErrorCode(String code, String details) {
        return doInstantiateErrorCode(code, details, null);
    }

    @Override
    public ErrorCode stringToInternalError(String details) {
        return instantiateErrorCode(SysErrors.INTERNAL.toString(), details);
    }

    @Override
    public ErrorCode throwableToInternalError(Throwable t) {
        return instantiateErrorCode(SysErrors.INTERNAL.toString(), t.getMessage());
    }

    @Override
    public ErrorCode stringToTimeoutError(String details) {
        return instantiateErrorCode(SysErrors.TIMEOUT.toString(), details);
    }

    @Override
    public ErrorCode throwableToTimeoutError(Throwable t) {
        return instantiateErrorCode(SysErrors.TIMEOUT.toString(), t.getMessage());
    }

    @Override
    public ErrorCode stringToOperationError(String details) {
        return instantiateErrorCode(SysErrors.OPERATION_ERROR, details);
    }

    @Override
    public ErrorCode stringToOperationError(String details, ErrorCode cause) {
        return instantiateErrorCode(SysErrors.OPERATION_ERROR, details, cause);
    }

    public ErrorCode stringToExternalError(String details, ErrorCode cause) {
        return instantiateErrorCode(SysErrors.EXTERNAL_ERROR, details, cause);
    }

    @Override
    public ErrorCodeList instantiateErrorCode(Enum code, List<ErrorCode> causes) {
        return instantiateErrorCode(code.toString(), causes);
    }

    @Override
    public ErrorCodeList instantiateErrorCode(String code, List<ErrorCode> causes) {
        return instantiateErrorCode(code, null, causes);
    }

    @Override
    public ErrorCodeList instantiateErrorCode(Enum code, String details, List<ErrorCode> causes) {
        return instantiateErrorCode(code.toString(), details, causes);
    }

    @Override
    public ErrorCodeList instantiateErrorCode(String code, String details, List<ErrorCode> causes) {
        return (ErrorCodeList) doInstantiateErrorCode(code, details, causes);
    }

    @Override
    public ErrorCodeList stringToOperationError(String details, List<ErrorCode> causes) {
        return instantiateErrorCode(SysErrors.OPERATION_ERROR, details, causes);
    }

    @Override
    public ErrorCode throwableToOperationError(Throwable t) {
        return instantiateErrorCode(SysErrors.OPERATION_ERROR, t.getMessage());
    }

    @Override
    public ErrorCode stringToInvalidArgumentError(String details) {
        return instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR, details);
    }

    @Override
    public ErrorCode throwableToInvalidArgumentError(Throwable t) {
        return instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR, t.getMessage());
    }

    private class ErrorCodeInfo {
        ErrorCode code;
        String path;
    }

    private void createErrorCode(org.zstack.core.errorcode.schema.Error error, String path) {
        for (Error.Code code : error.getCode()) {
            String codeId = String.format("%s.%s", error.getPrefix(), code.getId());
            ErrorCodeInfo info = codes.get(codeId);
            if (info != null) {
                throw new CloudRuntimeException(String.format("duplicate definition of ErrorCode[%s]," +
                        " file[%s] and file[%s] both define it", codeId, info.path, path));
            }

            ErrorCodeList errorCode = new ErrorCodeList();
            errorCode.setCode(codeId);
            errorCode.setDescription(code.getDescription());
            errorCode.setElaboration(code.getElaboration());
            info = new ErrorCodeInfo();
            info.code = errorCode;
            info.path = path;
            codes.put(codeId, info);
        }
    }

    void init() {
        try {
            JAXBContext context = JAXBContext.newInstance("org.zstack.core.errorcode.schema");
            List<String> paths = PathUtil.scanFolderOnClassPath("errorCodes");
            for (String p : paths) {
                if (!p.endsWith(".xml")) {
                    logger.warn(String.format("ignore %s which is not ending with .xml", p));
                    continue;
                }

                File cfg = new File(p);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                org.zstack.core.errorcode.schema.Error error =
                        (org.zstack.core.errorcode.schema.Error) unmarshaller.unmarshal(cfg);
                createErrorCode(error, p);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
