package org.zstack.test.unittest.zone

import org.junit.Test

class ZoneCase {
    @Test
    void testZoneNameOfName() {
        // 输入内容只能包含中文汉字、英文字母、数字、空格和以下 7 种英文字符 “-”、“_”、“.”、“(”、“)”、“:”、“+” 且不支持以空格开头或结尾。
        String regex = "^(?! )[\\u4e00-\\u9fa5a-zA-Z0-9\\-_.():+ ]*(?<! )\$"

        assert "区域1abc-_.():+".matches(regex)
        assert !" 区域1".matches(regex)
        assert !" 区域1@@".matches(regex)
    }
}
