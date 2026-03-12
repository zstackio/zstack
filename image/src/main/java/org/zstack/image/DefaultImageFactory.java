package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.image.Image;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageFactory;
import org.zstack.header.image.ImageType;
import org.zstack.header.image.ImageVO;
import org.zstack.header.volume.VolumeFormat;

public class DefaultImageFactory implements ImageFactory {
    static final ImageType type = new ImageType(ImageConstant.ZSTACK_IMAGE_TYPE);
    public static final VolumeFormat ISO_FORMAT = new VolumeFormat(ImageConstant.ISO_FORMAT_STRING, null);

    @Autowired
    private DatabaseFacade dbf;
    
    @Override
    public ImageType getType() {
        return type;
    }

    @Override
    @Transactional
    public ImageVO createImage(ImageVO vo) {
        dbf.getEntityManager().persist(vo);
        dbf.getEntityManager().flush();
        dbf.getEntityManager().refresh(vo);
        return vo;
    }

    @Override
    public Image getImage(ImageVO vo) {
        return new ImageBase(vo);
    }
}
