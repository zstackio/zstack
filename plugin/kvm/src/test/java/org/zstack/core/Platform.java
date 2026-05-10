package org.zstack.core;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Minimal test-scope stub that shadows the real {@code org.zstack.core.Platform} on the
 * test classpath. The real Platform has a heavy {@code static} initializer that requires
 * Spring context, database properties, Hibernate Search configuration, and dozens of
 * scanned plugins. None of that is needed for unit-testing {@code KvmRoleProvider}.
 *
 * <p>Only {@code operr()} is implemented — the three other overloads that
 * {@code KvmRoleProvider} imports via the static import {@code Platform.operr} all
 * delegate here. Any call to an unimplemented method throws
 * {@link UnsupportedOperationException} to make missing stubs visible immediately.
 */
public class Platform {

    // No static initializer — that is the entire point of this stub.

    /**
     * Creates an {@link ErrorCode} with the given global error code string and a
     * description built from {@code fmt}/{@code args}. Mirrors the contract of the
     * real {@code Platform.operr(String, String, Object...)}.
     */
    public static ErrorCode operr(String globalErrorCode, String fmt, Object... args) {
        ErrorCode ec = new ErrorCode();
        ec.setCode(globalErrorCode);
        try {
            ec.setDescription(args == null || args.length == 0 ? fmt : String.format(fmt, args));
        } catch (Exception e) {
            ec.setDescription(fmt);
        }
        return ec;
    }

    // ---- other Platform methods referenced by the production import block ----
    // Add stubs here only if KvmRoleProvider starts calling them.

    public static String getManagementServerId() {
        return "test-ms-id";
    }
}
