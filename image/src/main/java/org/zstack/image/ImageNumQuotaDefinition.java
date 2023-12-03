package org.zstack.image;

import org.zstack.core.db.SQL;
import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.image.ImageVO;

public class ImageNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return ImageQuotaConstant.IMAGE_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return ImageQuotaGlobalConfig.IMAGE_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        String sql = "select count(image) " +
                " from ImageVO image, AccountResourceRefVO ref " +
                " where image.uuid = ref.resourceUuid " +
                " and ref.accountUuid = :auuid " +
                " and ref.resourceType = :rtype ";
        SQL q = SQL.New(sql, Long.class);
        q.param("auuid", accountUuid);
        q.param("rtype", ImageVO.class.getSimpleName());
        Long imageNum = q.find();
        imageNum = imageNum == null ? 0 : imageNum;
        return imageNum;
    }
}
