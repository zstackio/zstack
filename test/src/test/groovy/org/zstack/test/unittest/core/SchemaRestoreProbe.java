package org.zstack.test.unittest.core;

import org.zstack.header.message.Message;

/**
 * Transport DTOs may expose plain public fields without JavaBean accessors.
 */
public class SchemaRestoreProbe {
    public enum Phase { PREPARE, QUERY }

    public static class PlainDto {
        public String name;
        public Phase phase;
    }

    public static class HolderMsg extends Message {
        public Object payload;
    }
}
