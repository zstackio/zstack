package org.zstack.header.image;

import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.List;

/**
 */
public class ImageDeletionStruct {
    private Boolean deleteAll;
    private ImageInventory image;
    private List<String> backupStorageUuids;

    public Boolean getDeleteAll() {
        if (backupStorageUuids == null) {
            return true;
        }

        if (deleteAll == null) {
            List<String> bsUuids = CollectionUtils.transformToList(image.getBackupStorageRefs(), new Function<String, ImageBackupStorageRefInventory>() {
                @Override
                public String call(ImageBackupStorageRefInventory arg) {
                    return arg.getBackupStorageUuid();
                }
            });

            deleteAll = backupStorageUuids.containsAll(bsUuids);
        }

        return deleteAll;
    }

    public ImageInventory getImage() {
        return image;
    }

    public void setImage(ImageInventory image) {
        this.image = image;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }
}
