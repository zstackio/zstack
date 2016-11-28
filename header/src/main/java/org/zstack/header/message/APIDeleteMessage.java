package org.zstack.header.message;

public abstract class APIDeleteMessage extends APIMessage {
    /**
     * @desc - Permissive: allows check before deletion.
     * If any extension refuses to delete the resource, an error will
     * be returned with reason.
     * - Enforcing: deletes resource forcibly.
     * In this mode, deletion check is bypassed and all deletion errors
     * are omitted.
     * <p>
     * Default value is Permissive.
     * @choices - Permissive
     * - Enforcing
     * @optional
     */
    private String deleteMode = DeletionMode.Permissive.toString();

    public APIDeleteMessage() {
    }

    public DeletionMode getDeletionMode() {
        return DeletionMode.valueOf(deleteMode);
    }

    public void setDeletionMode(DeletionMode deletionMode) {
        this.deleteMode = deletionMode.toString();
    }


    public static enum DeletionMode {
        Enforcing,
        Permissive,
    }
}
