package org.zstack.header.encryption;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 11/12/19.
 */
@StaticMetamodel(LuksEncryptedImageVO.class)
public class LuksEncryptedImageVO_ {
    public static volatile SingularAttribute<LuksEncryptedImageVO, Integer> id;
    public static volatile SingularAttribute<LuksEncryptedImageVO, String> encryptUuid;
    public static volatile SingularAttribute<LuksEncryptedImageVO, String> hashValue;
    public static volatile SingularAttribute<LuksEncryptedImageVO, String> bindingVmUuid;
    public static volatile SingularAttribute<LuksEncryptedImageVO, Timestamp> createDate;
    public static volatile SingularAttribute<LuksEncryptedImageVO, Timestamp> lastOpDate;
}
