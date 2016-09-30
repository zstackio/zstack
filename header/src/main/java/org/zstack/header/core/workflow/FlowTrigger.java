package org.zstack.header.core.workflow;

import org.zstack.header.core.AsyncBackup;
import org.zstack.header.errorcode.ErrorCode;

/**
 */
public interface FlowTrigger extends AsyncBackup {
    void fail(ErrorCode errorCode);

    void next();

    void setError(ErrorCode error);

}
