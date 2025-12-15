package org.zstack.utils.path;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemotePathValidator {
    // Shell metacharacters that must not appear in paths sent to remote agents.
    // Mirrors zstacklib/utils/linux.py _SHELL_UNSAFE_RE.
    private static final Pattern SHELL_UNSAFE_PATTERN =
            Pattern.compile("[;|&$`'\"\\\\(){}\\[\\]<>!#~\\n\\r\\x00*?]");

    // Protected system directories that must never be used as a target path.
    private static final Set<String> PROTECTED_PATHS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "/", "/bin", "/boot", "/dev", "/etc", "/lib", "/lib64",
                    "/proc", "/run", "/sbin", "/srv", "/sys", "/usr", "/var")));

    /**
     * Validate a remote path to be sent to an agent.
     * Returns null if valid, or an error message if invalid.
     *
     * Checks: non-empty, absolute, no traversal, no shell metacharacters,
     * no protected system directory.
     */
    public static String validateRemotePath(String path, String paramName) {
        if (path == null || path.isEmpty()) {
            return String.format("%s cannot be null or empty", paramName);
        }
        if (!path.startsWith("/")) {
            return String.format("%s [%s] must be an absolute path", paramName, path);
        }
        // Canonicalize by path components: collapse slashes, remove ".", reject ".."
        List<String> segments = new ArrayList<>();
        for (String component : path.split("/+")) {
            if (component.isEmpty() || ".".equals(component)) {
                continue;
            }
            if ("..".equals(component)) {
                return String.format("%s [%s] contains path traversal sequence", paramName, path);
            }
            segments.add(component);
        }
        String normalized = segments.isEmpty() ? "/" : "/" + String.join("/", segments);
        // Reject shell metacharacters (command injection via agent shell calls)
        Matcher m = SHELL_UNSAFE_PATTERN.matcher(path);
        if (m.find()) {
            return String.format("%s [%s] contains unsafe character '%s'", paramName, path, m.group());
        }
        // Reject protected system directories as direct targets
        if (PROTECTED_PATHS.contains(normalized)) {
            return String.format("%s [%s] targets a protected system directory", paramName, path);
        }
        return null;
    }
}
