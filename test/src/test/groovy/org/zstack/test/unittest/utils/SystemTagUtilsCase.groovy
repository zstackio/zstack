package org.zstack.test.unittest.utils

import org.junit.Test
import org.zstack.tag.PatternedSystemTag
import org.zstack.tag.SystemTagUtils

import static org.junit.Assert.assertThrows

class SystemTagUtilsCase {
    PatternedSystemTag poolTag = new PatternedSystemTag("pool::{name}", Object.class)

    @Test
    void testFindTokenWithAbsentTags() {
        [null, []].each { tags ->
            assert SystemTagUtils.findTagValue(tags, poolTag, "name") == null :
                    "Absent systemTags must return null for token lookup: systemTags=${tags}"
            assert SystemTagUtils.findTagValue(tags, null, null) == null :
                    "Absent systemTags require no pattern or token: systemTags=${tags}"
        }
    }

    @Test
    void testFindTagWithAbsentTags() {
        [null, []].each { tags ->
            assert SystemTagUtils.findTagValue(tags, poolTag) == null :
                    "Absent systemTags must return null for tag lookup: systemTags=${tags}"
            assert SystemTagUtils.findTagValue(tags, null) == null :
                    "Absent systemTags require no pattern: systemTags=${tags}"
        }
    }

    @Test
    void testFindTokensWithAbsentTags() {
        [null, []].each { tags ->
            assert SystemTagUtils.findTagValues(tags, poolTag, "name") == null :
                    "Absent systemTags must return null for token list lookup: systemTags=${tags}"
            assert SystemTagUtils.findTagValues(tags, null, null) == null :
                    "Absent systemTags require no pattern or token: systemTags=${tags}"
        }
    }

    @Test
    void testFindTagsWithAbsentTags() {
        [null, []].each { tags ->
            assert SystemTagUtils.findTagValues(tags, poolTag) == null :
                    "Absent systemTags must return null for tag list lookup: systemTags=${tags}"
            assert SystemTagUtils.findTagValues(tags, null) == null :
                    "Absent systemTags require no pattern: systemTags=${tags}"
        }
    }

    @Test
    void testFindMatchingTags() {
        List<String> tags = ["other::value", "pool::one"]
        assert SystemTagUtils.findTagValue(tags, poolTag, "name") == "one" : "Token lookup must preserve matching"
        assert SystemTagUtils.findTagValue(tags, poolTag) == "pool::one" : "Tag lookup must preserve matching"
        tags.add("pool::two")
        assert SystemTagUtils.findTagValues(tags, poolTag, "name") == ["one", "two"] : "All tokens must be returned"
        assert SystemTagUtils.findTagValues(tags, poolTag) == ["pool::one", "pool::two"] : "All tags must be returned"
    }

    @Test
    void testNonemptyTagsWithoutMatch() {
        List<String> tags = ["other::value"]
        assert SystemTagUtils.findTagValue(tags, poolTag, "name") == null : "Unmatched token lookup must return null"
        assert SystemTagUtils.findTagValue(tags, poolTag) == null : "Unmatched tag lookup must return null"
        assert SystemTagUtils.findTagValues(tags, poolTag, "name") == [] : "Unmatched token list must remain empty"
        assert SystemTagUtils.findTagValues(tags, poolTag) == [] : "Unmatched tag list must remain empty"
    }

    @Test
    void testNonemptyTagsStillValidatePatternAndToken() {
        List<String> tags = ["pool::one"]
        assertThrows("Token lookup requires a pattern", IllegalArgumentException) {
            SystemTagUtils.findTagValue(tags, null, "name")
        }
        assertThrows("Token lookup requires a token", IllegalArgumentException) {
            SystemTagUtils.findTagValue(tags, poolTag, null)
        }
        assertThrows("Tag lookup requires a pattern", IllegalArgumentException) {
            SystemTagUtils.findTagValue(tags, null)
        }
        assertThrows("Token list lookup requires a pattern", IllegalArgumentException) {
            SystemTagUtils.findTagValues(tags, null, "name")
        }
        assertThrows("Token list lookup requires a token", IllegalArgumentException) {
            SystemTagUtils.findTagValues(tags, poolTag, null)
        }
        assertThrows("Tag list lookup requires a pattern", IllegalArgumentException) {
            SystemTagUtils.findTagValues(tags, null)
        }
    }
}
