package org.zstack.header.vm;

import java.util.Arrays;

public enum SpiceChannelEnum {
    main, display, inputs, cursor, playback, record, smartcard, usbredir;

    public static boolean contains(String channel) {
        return Arrays.stream(SpiceChannelEnum.values()).anyMatch(c -> c.toString().equals(channel));
    }
}
