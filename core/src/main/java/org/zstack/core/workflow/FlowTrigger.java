package org.zstack.core.workflow;

import org.zstack.header.core.AsyncBackup;
import org.zstack.header.errorcode.ErrorCode;

/**
 */
public interface FlowTrigger extends AsyncBackup {
    void fail(ErrorCode errorCode);

    void next();

    void rollback();

    void skipRestRollbacks();

    void setError(ErrorCode error);

}
