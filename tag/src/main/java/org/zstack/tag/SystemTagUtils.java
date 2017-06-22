package org.zstack.tag;

import java.util.List;
import java.util.Optional;

/**
 * Created by lining on 2017/6/22.
 */
public class SystemTagUtils {

    public static String findTagValue(List<String> systemTags, PatternedSystemTag tag, String tagToken){
        if(systemTags == null || tag == null || tagToken == null ){
            throw new IllegalArgumentException("illegal argument");
        }

        if(systemTags.isEmpty()){
            return null;
        }

        Optional<String> opt = systemTags.stream().filter(s -> tag.isMatch(s)).findAny();
        String result = opt.isPresent() ? tag.getTokenByTag(opt.get(), tagToken) : null;
        return result;
    }
}
