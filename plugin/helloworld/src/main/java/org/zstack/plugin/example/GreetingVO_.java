package org.zstack.plugin.example;

        import org.zstack.header.vo.ResourceVO_;

        import javax.persistence.metamodel.SingularAttribute;
        import javax.persistence.metamodel.StaticMetamodel;
        import java.sql.Timestamp;

@StaticMetamodel(GreetingVO.class)
public class GreetingVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<GreetingVO, String> greeting;
    public static volatile SingularAttribute<GreetingVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<GreetingVO, Timestamp> createDate;
}
