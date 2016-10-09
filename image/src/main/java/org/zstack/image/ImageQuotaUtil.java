package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.BypassWhenUnitTest;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.Quota;
import org.zstack.header.image.APIAddImageMsg;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.GetImageSizeOnBackupStorageMsg;
import org.zstack.header.storage.backup.GetImageSizeOnBackupStorageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.Map;

/**
 * Created by miao on 16-10-9.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ImageQuotaUtil {
    private static final CLogger logger = Utils.getLogger(ImageQuotaUtil.class);

    @Autowired
    public DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;
    @Autowired
    protected RESTFacade restf;

    public class ImageQuota {
        public long imageNum;
        public long imageSize;
    }

    @Transactional(readOnly = true)
    public ImageQuota getUsed(String accountUUid) {
        ImageQuota quota = new ImageQuota();

        quota.imageSize = getUsedImageSize(accountUUid);
        quota.imageNum = getUsedImageNum(accountUUid);

        return quota;
    }

    @Transactional(readOnly = true)
    public long getUsedImageNum(String accountUuid) {
        String sql = "select count(image) " +
                " from ImageVO image, AccountResourceRefVO ref " +
                " where image.uuid = ref.resourceUuid " +
                " and ref.accountUuid = :auuid " +
                " and ref.resourceType = :rtype ";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("auuid", accountUuid);
        q.setParameter("rtype", ImageVO.class.getSimpleName());
        Long imageNum = q.getSingleResult();
        imageNum = imageNum == null ? 0 : imageNum;
        return imageNum;
    }

    @Transactional(readOnly = true)
    public long getUsedImageSize(String accountUuid) {
        String sql = "select sum(image.size) " +
                " from ImageVO image, AccountResourceRefVO ref " +
                " where image.uuid = ref.resourceUuid " +
                " and ref.accountUuid = :auuid " +
                " and ref.resourceType = :rtype ";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("auuid", accountUuid);
        q.setParameter("rtype", ImageVO.class.getSimpleName());
        Long imageSize = q.getSingleResult();
        imageSize = imageSize == null ? 0 : imageSize;
        return imageSize;
    }

    @BypassWhenUnitTest
    public void checkImageSizeQuotaUseHttpHead(APIAddImageMsg msg, Map<String, Quota.QuotaPair> pairs) {
        long imageSizeQuota = pairs.get(ImageConstant.QUOTA_IMAGE_SIZE).getValue();
        long imageSizeUsed = new ImageQuotaUtil().getUsedImageSize(msg.getSession().getAccountUuid());

        long imageSizeAsked;
        String url = msg.getUrl();
        url = url.trim();

        if (url.startsWith("file:///")) {
            GetImageSizeOnBackupStorageMsg cmsg = new GetImageSizeOnBackupStorageMsg();
            cmsg.setBackupStorageUuid(msg.getBackupStorageUuids().get(0));
            cmsg.setImageUrl(url);
            cmsg.setImageUuid(msg.getResourceUuid());
            bus.makeTargetServiceIdByResourceUuid(cmsg, BackupStorageConstant.SERVICE_ID,
                    msg.getBackupStorageUuids().get(0));
            MessageReply reply = bus.call(cmsg);
            if (!reply.isSuccess()) {
                throw new OperationFailureException(reply.getError());
            }

            imageSizeAsked = ((GetImageSizeOnBackupStorageReply) reply).getSize();
        } else if (url.startsWith("http") || url.startsWith("https")) {
            String len;
            try {
                HttpHeaders header = restf.getRESTTemplate().headForHeaders(url);
                len = header.getFirst("Content-Length");
            } catch (Exception e) {
                logger.warn(String.format("cannot get image.  The image url : %s. description: %s.name: %s",
                        url, msg.getDescription(), msg.getName()));
                return;
            }

            imageSizeAsked = len == null ? 0 : Long.valueOf(len);
        } else {
            // bypass the check
            imageSizeAsked = 0;
        }

        if ((imageSizeQuota == 0) || (imageSizeUsed + imageSizeAsked > imageSizeQuota)) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                    String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                            msg.getSession().getAccountUuid(), ImageConstant.QUOTA_IMAGE_SIZE, imageSizeQuota)
            ));
        }
    }
}
