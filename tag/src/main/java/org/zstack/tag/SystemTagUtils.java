package org.zstack.tag;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public static String findTagValue(List<String> systemTags, PatternedSystemTag tag){
        if(systemTags == null || tag == null){
            throw new IllegalArgumentException("illegal argument");
        }

        if(systemTags.isEmpty()){
            return null;
        }

        Optional<String> opt = systemTags.stream().filter(s -> tag.isMatch(s)).findAny();
        String result = opt.isPresent() ? opt.get() : null;
        return result;
    }

    public static List<String> findTagValues(List<String> systemTags, PatternedSystemTag tag, String tagToken) {
        if(systemTags == null || tag == null || tagToken == null ){
            throw new IllegalArgumentException("illegal argument");
        }

        if(systemTags.isEmpty()){
            return null;
        }

        return systemTags.stream().filter(tag::isMatch).map(t -> tag.getTokenByTag(t, tagToken)).collect(Collectors.toList());
    }

    public static List<String> findTagValues(List<String> systemTags, PatternedSystemTag tag) {
        if(systemTags == null || tag == null){
            throw new IllegalArgumentException("illegal argument");
        }

        if(systemTags.isEmpty()){
            return null;
        }

        return systemTags.stream().filter(tag::isMatch).collect(Collectors.toList());
    }
}
