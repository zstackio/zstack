package org.zstack.header.longjob;

/**
 * Created by GuoYi on 11/13/17.
 */
public class LongJobConstants {
    public static final String SERVICE_ID = "longjob";
    public static final String ACTION_CATEGORY = "longjob";

    public enum LongJobOperation {
        Start,
        Resume,
        Rerun,
        Cancel,
    }
}
