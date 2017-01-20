package org.zstack.header.storage.backup;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;

public abstract class APIAddBackupStorageMsg extends APICreateMessage {
    /**
     * @desc see :ref:`BackupStorageInventory`
     * <p>
     * max length of 2048 characters
     */
    @APIParam(maxLength = 2048, emptyString = false)
    private String url;
    /**
     * @desc max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc max length of 2048 characters
     * @optional
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;
    /**
     * @desc backup storage type
     * @choices - SftpBackupStorage
     * - SimulatorBackupStorage
     */
	private String type;

	@APIParam(required = false)
	private boolean importImages;

	public boolean isImportImages() {
		return importImages;
	}

	public void setImportImages(boolean importImages) {
		this.importImages = importImages;
	}

	public APIAddBackupStorageMsg() {
	}

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
