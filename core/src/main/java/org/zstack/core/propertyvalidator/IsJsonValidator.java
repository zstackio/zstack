package org.zstack.core.propertyvalidator;

import com.google.gson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import java.io.IOException;

@ValidateTarget(target = IsJson.class)
public class IsJsonValidator implements GlobalPropertyValidator {
    @Override
    public boolean validate(String name, String value, Object rule) throws GlobalPropertyValidatorExecption {
    //when value is null, isJSONStringParsable will return true
    if (!isJSONStringParsable(value)) {
            throw new GlobalPropertyValidatorExecption(String.format("invalid json value %s for %s", value, name));
        }
        return true;
    }

    private static boolean isJSONStringParsable(String jsonString) {
        try {
            JsonParser parser = new ObjectMapper().getJsonFactory().createJsonParser(jsonString);
            while (parser.nextToken() != null) {
            }
            return true;
        } catch (JsonParseException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
