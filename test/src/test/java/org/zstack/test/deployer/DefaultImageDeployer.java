package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.ImageConfig;

import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DefaultImageDeployer implements ImageDeployer<ImageConfig> {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public Class<ImageConfig> getSupportedDeployerClassType() {
        return ImageConfig.class;
    }

    @Override
    public void deploy(List<ImageConfig> images, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (ImageConfig ic : images) {
            for (String bsref : ic.getBackupStorageRef()) {
                ImageInventory iinv = new ImageInventory();
                BackupStorageInventory bs = deployer.backupStorages.get(bsref);
                if (bs == null) {
                    throw new CloudRuntimeException(String.format("Cannot find BackupStorage with name[%s], unable to add image[name:%s] to it", bsref, ic.getName()));
                }

                iinv.setDescription(ic.getDescription());
                iinv.setMediaType(ic.getMediaType());
                iinv.setGuestOsType(ic.getGuestOsType());
                iinv.setFormat(ic.getFormat());
                iinv.setName(ic.getName());
                iinv.setUrl(ic.getUrl());
                iinv.setPlatform(ic.getPlatform());

                SessionInventory session = ic.getAccountRef() == null ? null : deployer.loginByAccountRef(ic.getAccountRef(), config);

                iinv = deployer.getApi().addImageByFullConfig(iinv, bs.getUuid(), session);
                if (ic.getSize() != null) {
                    ImageVO vo = dbf.findByUuid(iinv.getUuid(), ImageVO.class);
                    long size = deployer.parseSizeCapacity(ic.getSize());
                    vo.setSize(size);
                    vo = dbf.updateAndRefresh(vo);
                    iinv = ImageInventory.valueOf(vo);
                }
                if(ic.getActualSize()!=null){
                    ImageVO vo = dbf.findByUuid(iinv.getUuid(), ImageVO.class);
                    long actualSize = deployer.parseSizeCapacity(ic.getActualSize());
                    vo.setActualSize(actualSize);
                    vo = dbf.updateAndRefresh(vo);
                    iinv = ImageInventory.valueOf(vo);
                }
                deployer.images.put(iinv.getName(), iinv);
            }
        }
    }
}
