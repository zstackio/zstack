package org.zstack.core.cascade;

import java.util.Arrays;
import java.util.List;

/**
 */
public interface CascadeConstant {
    public static final String DELETION_CHECK_CODE = "deletion.check";
    public static final String DELETION_DELETE_CODE = "deletion.delete";
    public static final String DELETION_FORCE_DELETE_CODE = "deletion.forceDelete";
    public static final String DELETION_CLEANUP_CODE = "deletion.cleanup";

    public static final List<String> DELETION_CODES = Arrays.asList(DELETION_CHECK_CODE, DELETION_DELETE_CODE, DELETION_FORCE_DELETE_CODE);
}
