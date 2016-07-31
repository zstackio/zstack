package org.zstack.header.storage.primary;

import org.zstack.header.errorcode.ErrorCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xing5 on 2016/8/1.
 */
public class ImageCacheCleanupDetails {
    public static class ImageCacheCleanupInventory {
        private boolean deleted = true;
        private ErrorCode error;
        private ImageCacheInventory inventory;

        public boolean isDeleted() {
            return deleted;
        }

        public void setDeleted(boolean deleted) {
            this.deleted = deleted;
        }

        public ErrorCode getError() {
            return error;
        }

        public void setError(ErrorCode error) {
            deleted = false;
            this.error = error;
        }

        public ImageCacheInventory getInventory() {
            return inventory;
        }

        public void setInventory(ImageCacheInventory inventory) {
            this.inventory = inventory;
        }
    }

    private int numberOfCleanedImageCache;
    private String primaryStorageType;
    private List<ImageCacheCleanupInventory> inventories;

    public int getNumberOfCleanedImageCache() {
        return numberOfCleanedImageCache;
    }

    public void setNumberOfCleanedImageCache(int numberOfCleanedImageCache) {
        this.numberOfCleanedImageCache = numberOfCleanedImageCache;
    }

    public String getPrimaryStorageType() {
        return primaryStorageType;
    }

    public void setPrimaryStorageType(String primaryStorageType) {
        this.primaryStorageType = primaryStorageType;
    }

    public List<ImageCacheCleanupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ImageCacheCleanupInventory> inventories) {
        this.inventories = inventories;
    }

    public synchronized void increaseNumberOfCleanedImageCache() {
        numberOfCleanedImageCache ++;
    }

    public synchronized void addCleanupInventory(ImageCacheCleanupInventory inv) {
        if (inventories == null) {
            inventories = new ArrayList<ImageCacheCleanupInventory>();
        }
        inventories.add(inv);
    }
}
