package org.zstack.header.image;

public interface ImageFactory {
    ImageType getType();

    ImageVO createImage(ImageVO vo, APIAddImageMsg msg);

    Image getImage(ImageVO vo);
}
