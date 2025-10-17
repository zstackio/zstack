package org.zstack.authentication.checkfile;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(FileVerificationRecordsVO.class)
public class FileVerificationRecordsVO_ {
    public static volatile SingularAttribute<FileVerificationRecordsVO, Long> id;
    public static volatile SingularAttribute<FileVerificationRecordsVO, String> fileVerificationUuid;
    public static volatile SingularAttribute<FileVerificationRecordsVO, String> path;
    public static volatile SingularAttribute<FileVerificationRecordsVO, String> node;
    public static volatile SingularAttribute<FileVerificationRecordsVO, String> currentDigest;
    public static volatile SingularAttribute<FileVerificationRecordsVO, String> targetDigest;
    public static volatile SingularAttribute<FileVerificationRecordsVO, String> reason;
    public static volatile SingularAttribute<FileVerificationRecordsVO, Boolean> recoverFlag;
    public static volatile SingularAttribute<FileVerificationRecordsVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<FileVerificationRecordsVO, Timestamp> createDate;
}
