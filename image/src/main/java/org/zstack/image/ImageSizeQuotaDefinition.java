package org.zstack.image;

import org.zstack.core.db.SQL;
import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.image.ImageVO;

public class ImageSizeQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return ImageQuotaConstant.IMAGE_SIZE;
    }

    @Override
    public Long getDefaultValue() {
        return ImageQuotaGlobalConfig.IMAGE_SIZE.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        String sql = "select sum(image.actualSize) " +
                " from ImageVO image ,AccountResourceRefVO ref " +
                " where image.uuid = ref.resourceUuid " +
                " and ref.accountUuid = :auuid " +
                " and ref.resourceType = :rtype ";
        SQL q = SQL.New(sql, Long.class);
        q.param("auuid", accountUuid);
        q.param("rtype", ImageVO.class.getSimpleName());
        Long imageSize = q.find();
        imageSize = imageSize == null ? 0 : imageSize;
        return imageSize;
    }
}
