package org.zstack.utils;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by kayo on 2018/10/9.
 */
public class BOMUtils {
    public static void skipBOM(InputStream inputStream) throws IOException {
        BOMInputStream bomi = new BOMInputStream(inputStream,
                ByteOrderMark.UTF_16BE,
                ByteOrderMark.UTF_16LE,
                ByteOrderMark.UTF_32BE,
                ByteOrderMark.UTF_32LE);
        if (bomi.hasBOM()) {
            inputStream.skip(bomi.getBOM().length());
        }
    }
}
