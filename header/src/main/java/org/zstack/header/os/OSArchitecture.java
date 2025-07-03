package org.zstack.header.os;

import java.util.Locale;

/**
 * Represents the operating system architecture type obtained from System.getProperty("os.arch").
 */
public enum OSArchitecture {
    X86_64(new String[]{"amd64", "x86_64"}, "x86_64"),
    AARCH64(new String[]{"aarch64"}, "aarch64"),
    LOONGARCH64(new String[]{"loongarch64"}, "loongarch64"),
    MIPS64EL(new String[]{"mips64el"}, "mips64el"),
    UNKNOWN(null, "unknown");

    private final String[] archNames;
    private final String normalizedName;

    /**
     * Private constructor for OSArchitecture enum.
     *
     * @param archNames An array of possible raw architecture names from System.getProperty("os.arch"). Can be null for UNKNOWN.
     * @param normalizedName The standardized, simplified name for this architecture.
     */
    OSArchitecture(String[] archNames, String normalizedName) {
        this.archNames = archNames;
        this.normalizedName = normalizedName;
    }

    /**
     * Gets the current OS architecture based on System.getProperty("os.arch").
     * Returns UNKNOWN if no match is found.
     *
     * @return The corresponding OSArchitecture enum constant.
     */
    public static OSArchitecture getCurrent() {
        String currentArch = System.getProperty("os.arch");
        if (currentArch == null) {
            return UNKNOWN;
        }

        String lowerCaseArch = currentArch.toLowerCase(Locale.ROOT);

        for (OSArchitecture arch : values()) {
            if (arch.archNames == null) {
                continue;
            }
            for (String name : arch.archNames) {
                if (lowerCaseArch.equals(name.toLowerCase())) {
                    return arch;
                }
            }
        }
        return UNKNOWN;
    }

    /**
     * Returns a normalized string representation of the architecture, as defined in the enum constant.
     * For example, for X86_64, this will always return "x86_64".
     *
     * @return A normalized string name for the architecture.
     */
    public String normalizedArchName() {
        return this.normalizedName;
    }
}