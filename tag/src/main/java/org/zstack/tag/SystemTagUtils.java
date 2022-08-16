package org.zstack.tag;

import org.zstack.header.tag.SystemTagInventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

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

    public static void cloneTag(PatternedSystemTag tag, String tagToken, String srcUuid, String destUuid){
        if (tagToken == null) {
            return;
        }
        List<SystemTagInventory> srcTags = tag.getTagInventories(srcUuid);

        if (srcTags == null || srcTags.isEmpty()) {
            return;
        }

        for(SystemTagInventory srcTag: srcTags) {
            SystemTagCreator creator = tag.newSystemTagCreator(destUuid);
            creator.setTagByTokens(map(e(tagToken, tag.getTokenByResourceUuid(srcUuid, tagToken))));
            creator.inherent = srcTag.isInherent();
            creator.ignoreIfExisting = true;
            creator.create();
        }
    }

    public static void cloneTag(PatternedSystemTag tag, String srcUuid, String destUuid, String... tokens){
        List<SystemTagInventory> srcTags = tag.getTagInventories(srcUuid);

        if (srcTags == null || srcTags.isEmpty()) {
            return;
        }

        Map<String, String> tokenMap = new HashMap<>();
        for (String token : tokens) {
            tokenMap.put(token, tag.getTokenByResourceUuid(srcUuid, token));
        }

        for(SystemTagInventory srcTag: srcTags) {
            SystemTagCreator creator = tag.newSystemTagCreator(destUuid);
            creator.setTagByTokens(tokenMap);
            creator.inherent = srcTag.isInherent();
            creator.ignoreIfExisting = true;
            creator.create();
        }
    }
}
