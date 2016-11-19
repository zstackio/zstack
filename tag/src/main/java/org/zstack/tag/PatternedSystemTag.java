package org.zstack.tag;

import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.utils.TagUtils;

import java.util.ArrayList;
import java.util.List;
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

    @Override
    public void delete(String resourceUuid, Class resourceClass) {
        tagMgr.deleteSystemTagUseLike(useTagFormat(), resourceUuid, resourceClass.getSimpleName(), false);
    }

    public void delete(String resourceUuid, String tagFormat) {
        tagMgr.deleteSystemTagUseLike(tagFormat, resourceUuid, resourceClass.getSimpleName(), false);
    }

    @Override
    public void delete(String resourceUuid) {
        tagMgr.deleteSystemTagUseLike(useTagFormat(), resourceUuid, resourceClass.getSimpleName(), false);
    }

    @Override
    public void deleteInherentTag(String resourceUuid) {
        tagMgr.deleteSystemTagUseLike(useTagFormat(), resourceUuid, resourceClass.getSimpleName(), true);
    }

    public void deleteInherentTag(String resourceUuid, String tagFormat) {
        tagMgr.deleteSystemTagUseLike(tagFormat, resourceUuid, resourceClass.getSimpleName(), true);
    }

    @Override
    public void deleteInherentTag(String resourceUuid, Class resourceClass) {
        tagMgr.deleteSystemTagUseLike(useTagFormat(), resourceUuid, resourceClass.getSimpleName(), true);
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

    public List<Map<String, String>> getTokensOfTagsByResourceUuid(String resourceUuid) {
        return getTokensOfTagsByResourceUuid(resourceUuid, resourceClass);
    }

    public List<Map<String, String>> getTokensOfTagsByResourceUuid(String resourceUuid, Class resourceClass) {
        List<Map<String, String>> res = new ArrayList<>();

        List<String> tags = getTags(resourceUuid, resourceClass);
        for (String tag : tags) {
            res.add(TagUtils.parseIfMatch(tagFormat, tag));
        }

        return res;
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

    public SystemTagInventory getTagInventory(String resourceUuid) {
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.add(SystemTagVO_.resourceUuid, Op.EQ, resourceUuid);
        q.add(SystemTagVO_.resourceType, Op.EQ, getResourceClass().getSimpleName());
        q.add(SystemTagVO_.tag, Op.LIKE, useTagFormat());
        SystemTagVO vo = q.find();
        return  vo == null ? null : SystemTagInventory.valueOf(vo);
    }
}
