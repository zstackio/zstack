package org.zstack.test.portal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.propertyvalidator.*;
import org.zstack.header.exception.CloudRuntimeException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestGlobalPropertyValidator {

    @Mock
    private ValidatorTool validatorTool;
    private Map<String, String> validatedMap;
    private Set<String> lengthValidatedSet;
    private Set<String> numberRangeValidatedSet;
    private GlobalPropertyValidator lengthValidate;
    private GlobalPropertyValidator numberRangeValidate;
    private long[] length;
    private long[] range;

    @Before
    public void setup() {
        lengthValidate = new LengthValidator();
        numberRangeValidate = new NumberRangeValidator();
        validatorTool = new ValidatorTool();
        validatedMap = new HashMap<>();
        lengthValidatedSet = new HashSet<>();
        numberRangeValidatedSet = new HashSet<>();

        CoreGlobalProperty.MN_VIP = "0.0.0.0";
        length = new long[]{5, 10};
        range = new long[]{10, 1000};
    }

    @Test
    public void testPublicGlobalPropertyValidator() {

        validatedMap.put("DB.url", "jbdc:mysql://10.0.115.215:3306");
        validatedMap.put("unitTestOn", "other");
        validatedMap.put("RESTFacade.hostname", "0.0.0.0");

        lengthValidatedSet.add("1");
        lengthValidatedSet.add("mockUserNameOverTen");

        numberRangeValidatedSet.add("1");
        numberRangeValidatedSet.add("10000");

        validatedMap.keySet().forEach(name->{
            String value = validatedMap.get(name);
            try {
                assert !validatorTool.checkProperty(name, value);
            } catch (CloudRuntimeException c) {
                c.printStackTrace();
            }
        });

        lengthValidatedSet.forEach(value->{
            try {
                assert !lengthValidate.validate("DB.password", value, length);
            } catch (GlobalPropertyValidatorExecption g) {
                g.printStackTrace();
            }
        });

        numberRangeValidatedSet.forEach(value->{
            try {
                assert !numberRangeValidate.validate("RESTFacade.readTimeout", value, range);
            } catch (GlobalPropertyValidatorExecption g) {
                g.printStackTrace();
            }
        });

        try {
            assert !numberRangeValidate.validate("RESTFacade.readTimeout", "10000", new long[]{10, 20, 30});
        } catch (RuntimeException r) {
            r.printStackTrace();
        }

        assert validatorTool.checkProperty("DB.url", "jdbc:mysql://10.0.115.215:3306");
        assert !validatorTool.checkProperty("D.url", "jdbc:mysql://10.0.115.215:3306");
        assert validatorTool.checkProperty("unitTestOn", "true");
        assert validatorTool.checkProperty("unitTestOn", "false");
        assert validatorTool.checkProperty("RESTFacade.hostname", "127.0.1.1");

        assert lengthValidate.validate("DB.password", "zstack", length);
        assert numberRangeValidate.validate("RESTFacade.readTimeout", "100", range);
    }
}
