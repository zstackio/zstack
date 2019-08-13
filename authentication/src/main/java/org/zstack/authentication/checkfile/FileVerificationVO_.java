package org.zstack.authentication.checkfile;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(FileVerificationVO.class)
public class FileVerificationVO_ {
    public static volatile SingularAttribute<FileVerificationVO, String> uuid;
    public static volatile SingularAttribute<FileVerificationVO, String> path;
    public static volatile SingularAttribute<FileVerificationVO, String> node;
    public static volatile SingularAttribute<FileVerificationVO, String> type;
    public static volatile SingularAttribute<FileVerificationVO, String> category;
    public static volatile SingularAttribute<FileVerificationVO, String> digest;
    public static volatile SingularAttribute<FileVerificationVO, String> state;
}
