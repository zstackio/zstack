package org.zstack.xinfini;

import java.sql.Timestamp;
import java.time.OffsetDateTime;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:42 2024/5/29
 */
public class XInfiniTimeUtil {
    public static Timestamp translateToTimeStamp(String time) {
        return Timestamp.from(OffsetDateTime.parse(time).toInstant());
    }
}
