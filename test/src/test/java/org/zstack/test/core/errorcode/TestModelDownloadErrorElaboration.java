package org.zstack.test.core.errorcode;

import org.junit.Test;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.JobResultError;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.string.ElaborationSearchMethod;
import org.zstack.utils.string.ErrorCodeElaboration;
import org.zstack.utils.string.StringSimilarity;

import java.util.Locale;

import static org.junit.Assert.*;

public class TestModelDownloadErrorElaboration {
    private static final String SOURCE_UPDATED = "源文件已更新，需重新下载";

    @Test
    public void sourceUpdateRetainsBothLanguagesInLongJobResult() {
        ErrorCodeElaboration template = StringSimilarity.findSimilar(SOURCE_UPDATED);
        assertNotNull(template);
        assertTrue(StringSimilarity.matched(template));
        assertEquals(ElaborationSearchMethod.regex, template.getMethod());

        ErrorCode error = new ErrorCode();
        error.setDetails(SOURCE_UPDATED);
        error.setMessages(new ErrorCodeElaboration(template, Locale.SIMPLIFIED_CHINESE));
        JobResultError result = JSONObjectUtil.toObject(ErrorCode.getJobResult(error), JobResultError.class);

        assertEquals(SOURCE_UPDATED, result.getMessage().getMessage_cn());
        assertEquals("The source file has been updated. Download it again.", result.getMessage().getMessage_en());
        assertEquals(SOURCE_UPDATED, result.getDetail());
        assertEquals(SOURCE_UPDATED, result.getCause());
    }

    @Test
    public void sourceUpdateTemplateDoesNotMatchCacheLoss() {
        ErrorCodeElaboration template = StringSimilarity.findSimilar(SOURCE_UPDATED);
        assertNotNull(template);
        assertTrue(StringSimilarity.isRegexMatched(template.getRegex(), SOURCE_UPDATED));
        assertFalse(StringSimilarity.isRegexMatched(template.getRegex(), "缓存文件丢失，需重新下载"));
    }

    @Test
    public void untranslatedErrorsKeepTheirExistingJobResult() {
        ErrorCode error = new ErrorCode();
        error.setDetails("An unrelated download error");
        JobResultError result = JSONObjectUtil.toObject(ErrorCode.getJobResult(error), JobResultError.class);
        assertNull(result.getMessage());
        assertEquals("An unrelated download error", result.getDetail());
        assertEquals("An unrelated download error", result.getCause());
    }
}
