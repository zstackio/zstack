package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.image.*;
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
    public ImageVO createImage(ImageVO vo, APIAddImageMsg msg) {
        return dbf.persistAndRefresh(vo);
    }

    @Override
    public Image getImage(ImageVO vo) {
        return new ImageBase(vo);
    }
}
