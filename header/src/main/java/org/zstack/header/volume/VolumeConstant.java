package org.zstack.header.volume;

public interface VolumeConstant {
	public static final String SERVICE_ID = "volume";

	public static final String ACTION_CATEGORY = "volume";

	public static final String VOLUME_FORMAT_RAW = "raw";
	public static final String VOLUME_FORMAT_QCOW2 = "qcow2";

    public static final String QUOTA_DATA_VOLUME_NUM = "volume.data.num";

	public static final String QUOTA_VOLUME_SIZE = "volume.capacity";

	enum Capability {
		MigrationInCurrentPrimaryStorage,
		MigrationToOtherPrimaryStorage
	}
}
