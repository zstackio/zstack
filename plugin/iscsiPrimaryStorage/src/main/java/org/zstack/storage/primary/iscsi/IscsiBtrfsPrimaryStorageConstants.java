package org.zstack.storage.primary.iscsi;

/**
 * Created by frank on 4/19/2015.
 */
public interface IscsiBtrfsPrimaryStorageConstants {
    public static final String BTRFS_TYPE = "btrfs";

    public static final String INIT_PATH = "/init";
    public static final String DOWNLOAD_IMAGE_TO_CACHE_PATH = "/image/sftp/download";
    public static final String CHECK_BITS_EXISTENCE = "/bits/checkifexists";
    public static final String DELETE_BITS_EXISTENCE = "/bits/delete";
    public static final String CREATE_ROOT_VOLUME_PATH = "/volumes/createrootfromtemplate";
    public static final String CREATE_EMPTY_VOLUME_PATH = "/volumes/createempty";
}
