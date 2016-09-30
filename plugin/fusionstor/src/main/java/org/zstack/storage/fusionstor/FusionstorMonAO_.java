package org.zstack.storage.fusionstor;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by frank on 7/29/2015.
 */
@StaticMetamodel(FusionstorMonAO.class)
public class FusionstorMonAO_ {
    public static volatile SingularAttribute<FusionstorMonAO, String> uuid;
    public static volatile SingularAttribute<FusionstorMonAO, String> sshUsername;
    public static volatile SingularAttribute<FusionstorMonAO, String> sshPassword;
    public static volatile SingularAttribute<FusionstorMonAO, String> hostname;
    public static volatile SingularAttribute<FusionstorMonAO, Integer> monPort;
    public static volatile SingularAttribute<FusionstorMonAO, MonStatus> status;
    public static volatile SingularAttribute<FusionstorMonAO, Timestamp> createDate;
    public static volatile SingularAttribute<FusionstorMonAO, Timestamp> lastOpDate;
}
