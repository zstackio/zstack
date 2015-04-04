package org.zstack.tag;

import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.utils.TagUtils;

import java.util.Map;

import static org.zstack.utils.StringDSL.s;

/**
 */
public class PatternedSystemTag extends SystemTag {
    public PatternedSystemTag(String tagFormat, Class resourceClass) {
        super(tagFormat, resourceClass);
    }

    @Override
    protected String useTagFormat() {
        return TagUtils.tagPatternToSqlPattern(tagFormat);
    }

    @Override
    protected Op useOp() {
        return Op.LIKE;
    }

    @Override
    public boolean isMatch(String tag) {
        return TagUtils.isMatch(tagFormat, tag);
    }

    public Map<String, String> getTokensByTag(String tag) {
        return TagUtils.parseIfMatch(tagFormat, tag);
    }

    public String getTokenByTag(String tag, String tokenName) {
        Map<String, String> tokens = getTokensByTag(tag);
        if (tokens == null) {
            return null;
        }
        return tokens.get(tokenName);
    }

    public Map<String, String> getTokensByResourceUuid(String resourceUuid, Class resourceClass) {
        String tag = getTag(resourceUuid, resourceClass);
        if (tag == null) {
            return null;
        }

        return TagUtils.parseIfMatch(tagFormat, tag);
    }

    public Map<String, String> getTokensByResourceUuid(String resourceUuid) {
        return getTokensByResourceUuid(resourceUuid, resourceClass);
    }

    public String getTokenByResourceUuid(String resourceUuid, Class resourceClass, String tokenName) {
        Map<String, String> tokens = getTokensByResourceUuid(resourceUuid, resourceClass);
        if (tokens == null) {
            return null;
        }
        return tokens.get(tokenName);
    }

    public String getTokenByResourceUuid(String resourceUuid, String tokenName) {
        return getTokenByResourceUuid(resourceUuid, resourceClass, tokenName);
    }

    public String instantiateTag(Map tokens) {
        return s(tagFormat).formatByMap(tokens);
    }

    private SystemTagInventory createTag(String resourceUuid, Class resourceClass, Map tokens, boolean inherent, boolean recreate) {
        if (recreate) {
            tagMgr.deleteSystemTagUseLike(useTagFormat(), resourceUuid, resourceClass.getSimpleName(), inherent);
        }

        if (inherent) {
            return (SystemTagInventory) tagMgr.createSysTag(resourceUuid, instantiateTag(tokens), resourceClass.getSimpleName());
        } else {
            return tagMgr.createNonInherentSystemTag(resourceUuid, instantiateTag(tokens), resourceClass.getSimpleName());
        }
    }

    public SystemTagInventory recreateTag(String resourceUuid, Class resourceClass, Map tokens) {
        return createTag(resourceUuid, resourceClass, tokens, false, true);
    }

    public SystemTagInventory recreateTag(String resourceUuid, Map tokens) {
        return createTag(resourceUuid, resourceClass, tokens, false, true);
    }

    public SystemTagInventory recreateInherentTag(String resourceUuid, Class resourceClass, Map tokens) {
        return createTag(resourceUuid, resourceClass, tokens, true, true);
    }

    public SystemTagInventory recreateInherentTag(String resourceUuid, Map tokens) {
        return createTag(resourceUuid, resourceClass, tokens, true, true);
    }

    public SystemTagInventory createTag(String resourceUuid, Class resourceClass, Map tokens) {
        return createTag(resourceUuid, resourceClass, tokens, false, false);
    }

    public SystemTagInventory createTag(String resourceUuid, Map tokens) {
        return createTag(resourceUuid, resourceClass, tokens, false, false);
    }

    public SystemTagInventory createTag(String resourceUuid, String tag) {
        return createTag(resourceUuid, getTokensByTag(tag));
    }

    public SystemTagInventory createInherentTag(String resourceUuid, Class resourceClass, Map tokens) {
        return createTag(resourceUuid, resourceClass, tokens, true, false);
    }

    public SystemTagInventory createInherentTag(String resourceUuid, Map tokens) {
        return createTag(resourceUuid, resourceClass, tokens, true, false);
    }
}

