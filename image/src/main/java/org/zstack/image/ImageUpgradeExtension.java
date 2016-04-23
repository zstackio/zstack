package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.SyncImageSizeMsg;
import org.zstack.header.storage.backup.BackupStorageCanonicalEvents;
import org.zstack.header.storage.backup.BackupStorageCanonicalEvents.BackupStorageStatusChangedData;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;

/**
 * Created by xing5 on 2016/5/6.
 */
public class ImageUpgradeExtension implements Component {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private CloudBus bus;

    @Override
    public boolean start() {
        if (!CoreGlobalProperty.IS_UPGRADE_START) {
            return true;
        }

        String dbVersion = dbf.getDbVersion();
        if ("1.3".equals(dbVersion)) {
            syncImageActualSize();
        }

        return true;
    }

    private void syncImageActualSize() {
        evtf.on(BackupStorageCanonicalEvents.BACKUP_STORAGE_STATUS_CHANGED, new EventCallback() {
            @Transactional(readOnly = true)
            private List<ImageInventory> getImagesForSync(String bsUuid) {
                String sql = "select img from ImageVO img, ImageBackupStorageRefVO ref where img.size = img.actualSize" +
                        " and img.uuid = ref.imageUuid and ref.backupStorageUuid = :bsUuid";
                TypedQuery<ImageVO> q = dbf.getEntityManager().createQuery(sql, ImageVO.class);
                q.setParameter("bsUuid", bsUuid);
                List<ImageVO> vos = q.getResultList();
                return ImageInventory.valueOf(vos);
            }

            @Override
            public void run(Map tokens, Object data) {
                final BackupStorageStatusChangedData d = (BackupStorageStatusChangedData) data;

                if (!BackupStorageStatus.Connected.toString().equals(d.getNewStatus())) {
                    return;
                }

                final List<ImageInventory> imgs = getImagesForSync(d.getBackupStorageUuid());
                if (imgs.isEmpty()) {
                    return;
                }

                List<SyncImageSizeMsg> msgs = CollectionUtils.transformToList(imgs, new Function<SyncImageSizeMsg, ImageInventory>() {
                    @Override
                    public SyncImageSizeMsg call(ImageInventory arg) {
                        SyncImageSizeMsg msg = new SyncImageSizeMsg();
                        msg.setBackupStorageUuid(d.getBackupStorageUuid());
                        msg.setImageUuid(arg.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, arg.getUuid());
                        return msg;
                    }
                });

                bus.send(msgs);
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }
}
