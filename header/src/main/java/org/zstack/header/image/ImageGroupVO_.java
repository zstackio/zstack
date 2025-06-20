package org.zstack.header.image;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import java.sql.Timestamp;

public class ImageGroupVO_  extends ResourceVO_ {
    public static volatile SingularAttribute<ImageGroupVO, Integer> imageCount;
    public static volatile SingularAttribute<ImageGroupVO, String> name;
    public static volatile SingularAttribute<ImageGroupVO, String> description;
    public static volatile SingularAttribute<ImageGroupVO, Timestamp> createDate;
    public static volatile SingularAttribute<ImageGroupVO, Timestamp> lastOpDate;
}
