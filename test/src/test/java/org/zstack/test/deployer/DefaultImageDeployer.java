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
import org.zstack.sdk.AddImageAction;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.ImageConfig;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.List;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;

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
                BackupStorageInventory bs = deployer.backupStorages.get(bsref);
                if (bs == null) {
                    throw new CloudRuntimeException(String.format("Cannot find BackupStorage with name[%s], unable to add image[name:%s] to it", bsref, ic.getName()));
                }

                SessionInventory session = ic.getAccountRef() == null ? deployer.getApi().getAdminSession() : deployer.loginByAccountRef(ic.getAccountRef(), config);

                AddImageAction action = new AddImageAction();
                action.description = ic.getDescription();
                action.mediaType = ic.getMediaType();
                action.guestOsType = ic.getGuestOsType();
                action.format = ic.getFormat();
                action.name = ic.getName();
                action.url = ic.getUrl();
                action.platform = ic.getPlatform();
                action.sessionId = session.getUuid();
                action.backupStorageUuids = asList(bs.getUuid());

                AddImageAction.Result res = action.call().throwExceptionIfError();
                ImageInventory iinv = JSONObjectUtil.rehashObject(res.value.getInventory(), ImageInventory.class);

                if (ic.getSize() != null) {
                    ImageVO vo = dbf.findByUuid(iinv.getUuid(), ImageVO.class);
                    long size = deployer.parseSizeCapacity(ic.getSize());
                    vo.setSize(size);
                    vo = dbf.updateAndRefresh(vo);
                    iinv = ImageInventory.valueOf(vo);
                }
                if (ic.getActualSize() != null) {
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
