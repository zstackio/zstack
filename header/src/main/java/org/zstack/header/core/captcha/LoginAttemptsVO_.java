package org.zstack.header.core.captcha;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by kayo on 2018/8/6.
 */
@StaticMetamodel(LoginAttemptsVO.class)
public class LoginAttemptsVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<CaptchaVO, String> targetResourceIdentity;
    public static volatile SingularAttribute<CaptchaVO, Integer> attempts;
    public static volatile SingularAttribute<CaptchaVO, Timestamp> createDate;
    public static volatile SingularAttribute<CaptchaVO, Timestamp> lastOpDate;
}
