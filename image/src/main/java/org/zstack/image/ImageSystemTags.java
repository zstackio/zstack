package org.zstack.image;

import org.zstack.header.image.ImageVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by xing5 on 2016/7/18.
 */
@TagDefinition
public class ImageSystemTags {
    public static String IMAGE_NAME_TOKEN = "imageName";
    public static String PRIMARY_STORAGE_UUID_TOKEN = "primaryStorageUuid";
    public static String IMAGE_INJECT_QEMUGA_TOKEN = "qemuga";
    public static PatternedSystemTag IMAGE_INJECT_QEMUGA = new PatternedSystemTag(String.format("qemuga::{%s}", IMAGE_INJECT_QEMUGA_TOKEN), ImageVO.class);
    public static PatternedSystemTag DELETED_IMAGE_CACHE = new PatternedSystemTag(
            String.format("imageName::{%s}::primaryStorageUuid::{%s}", IMAGE_NAME_TOKEN, PRIMARY_STORAGE_UUID_TOKEN),
            ImageVO.class
    );
}
