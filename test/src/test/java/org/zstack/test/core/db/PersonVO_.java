package org.zstack.test.core.db;

import org.zstack.test.core.db.PersonVO.Sex;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.util.Date;

@StaticMetamodel(PersonVO.class)
public abstract class PersonVO_ {

    public static volatile SingularAttribute<PersonVO, Long> id;
    public static volatile SingularAttribute<PersonVO, String> title;
    public static volatile SingularAttribute<PersonVO, Sex> sex;
    public static volatile SingularAttribute<PersonVO, String> description;
    public static volatile SingularAttribute<PersonVO, Integer> age;
    public static volatile SingularAttribute<PersonVO, String> name;
    public static volatile SingularAttribute<PersonVO, String> uuid;
    public static volatile SingularAttribute<PersonVO, Date> date;
    public static volatile SingularAttribute<PersonVO, Boolean> marriage;

}

