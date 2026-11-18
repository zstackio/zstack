package org.zstack.portal.managementnode;

import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.utils.Linux;
import org.zstack.utils.path.PathUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class ResourceControlUtils {
    private static final long COMMAND_TIMEOUT_SECONDS = 30;
    private final CommandExecutor commandExecutor;

    ResourceControlUtils(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    static String read(Path path) {
        return PathUtil.readFileToString(path.toString(), StandardCharsets.US_ASCII);
    }

    @Deferred
    void write(Path path, String value) {
        byte[] input = value.getBytes(StandardCharsets.US_ASCII);
        if (commandExecutor != null) {
            run(input, "sudo", "-n", "tee", path.toString());
            return;
        }
        String temporary = PathUtil.createTempFileWithContent(value);
        Defer.defer(() -> PathUtil.forceRemoveFile(temporary));
        run(null, "timeout", String.valueOf(COMMAND_TIMEOUT_SECONDS), "sudo", "-n", "sh", "-c",
                "cat \"$1\" > \"$2\"", "resource-control-write", temporary, path.toString());
    }

    String run(byte[] input, String... command) {
        if (commandExecutor != null) {
            return commandExecutor.run(input, command);
        }
        List<String> timedCommand = new ArrayList<>();
        if (!"timeout".equals(command[0])) {
            timedCommand.add("timeout");
            timedCommand.add(String.valueOf(COMMAND_TIMEOUT_SECONDS));
        }
        timedCommand.addAll(Arrays.asList(command));
        Linux.ShellResult result = Linux.shell(timedCommand);
        if (result.getExitCode() != 0) {
            throw new ResourceControlException(String.format(
                    "Command[%s] failed: %s", command[0], result.getStderr().trim()));
        }
        return result.getStdout();
    }

    static Path underRoot(Path root, Path path) {
        if (!path.equals(root) && !path.startsWith(root)) {
            throw new ResourceControlException(String.format("Control group path[%s] is outside root[%s]", path, root));
        }
        return path;
    }

    static String stripRoot(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    interface CommandExecutor {
        String run(byte[] input, String... command);
    }

    public static class ResourceControlException extends RuntimeException {
        public ResourceControlException(String message) {
            super(message);
        }
    }
}
