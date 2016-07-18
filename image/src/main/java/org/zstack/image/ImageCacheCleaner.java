package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.storage.primary.ImageCacheVO;

import java.util.List;

/**
 * Created by xing5 on 2016/7/18.
 */
public abstract class ImageCacheCleaner {
    @Autowired
    protected DatabaseFacade dbf;

    protected abstract String getPrimaryStorageType();

    @Transactional
    protected List<ImageCacheVO> moveStaleImagesInCacheToSystemTag() {
        String sql = "select c from ImageCacheVO c, PrimaryStorageVO pri, ImageEO i where ((c.imageUuid is null) or (i.uuid = c.imageUuid and i.deleted is not null)) and " +
                "pri.type = :ptype";
        return null;
    }
}
