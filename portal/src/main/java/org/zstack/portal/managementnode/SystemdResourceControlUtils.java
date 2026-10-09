package org.zstack.portal.managementnode;

import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.header.physicalserver.RoleServiceManifest;
import org.zstack.portal.managementnode.ResourceControlUtils.CommandExecutor;
import org.zstack.utils.path.PathUtil;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.portal.managementnode.ResourceControlUtils.stripRoot;
import static org.zstack.portal.managementnode.ResourceControlUtils.underRoot;

class SystemdResourceControlUtils {
    private static final String SYSTEMD_DROP_IN = "50-zstack-resource-assignment.conf";
    private static final String DIRECTORY_MODE = "0755";
    private static final String DROP_IN_FILE_MODE = "0644";

    private final Path unitRoot;
    private final Path legacyRoot;
    private final ResourceControlUtils commands;

    SystemdResourceControlUtils(Path unitRoot, Path legacyRoot, CommandExecutor commandExecutor) {
        this.unitRoot = unitRoot;
        this.legacyRoot = legacyRoot;
        commands = new ResourceControlUtils(commandExecutor);
    }

    boolean configureSlice(String sliceName, String cpuProperty, String cpuSet, String memoryProperty, Long memory) {
        Path path = dropInPath(sliceName);
        List<String> lines = new ArrayList<>();
        lines.add("[Slice]");
        if (cpuProperty != null && !cpuSet.isEmpty()) {
            lines.add(cpuProperty + "=" + cpuSet);
        }
        if (memory != null && memoryProperty != null) {
            lines.add(String.format("%s=%s", memoryProperty, memory == 0 ? "infinity" : memory));
        } else if (memory != null) {
            for (String line : readDropIn(path).split("\\R")) {
                if (line.startsWith("MemoryMax=") || line.startsWith("MemoryLimit=")) {
                    lines.add(line);
                }
            }
        }
        return writeDropIn(path, String.join("\n", lines) + "\n");
    }

    boolean configureService(String unit, String sliceName) {
        Path path = dropInPath(unit);
        return readDropIn(path).isEmpty() && writeDropIn(path, "[Service]\nSlice=" + sliceName + "\n");
    }

    boolean pruneServices(String sliceName, Collection<String> desiredUnits) {
        if (!Files.isDirectory(unitRoot, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        java.io.File[] directories = unitRoot.toFile().listFiles(file ->
                file.isDirectory() && file.getName().endsWith(".d"));
        if (directories == null) {
            return false;
        }
        boolean changed = false;
        for (java.io.File directory : directories) {
            String name = directory.getName();
            Path dropIn = directory.toPath().resolve(SYSTEMD_DROP_IN);
            if (!sliceName.equals(configuredSlice(dropIn))) {
                continue;
            }
            if (!desiredUnits.contains(name.substring(0, name.length() - 2))) {
                changed = removeDropIn(dropIn) || changed;
            }
        }
        return changed;
    }

    boolean releaseSlice(String sliceName) {
        return removeDropIn(dropInPath(sliceName));
    }

    String configuredSlice(String unit) {
        return configuredSlice(dropInPath(unit));
    }

    private String configuredSlice(Path path) {
        for (String line : readDropIn(path).split("\\R")) {
            String value = line.trim();
            if (value.startsWith("Slice=")) {
                String slice = value.substring("Slice=".length()).trim();
                return slice.matches(RoleServiceManifest.SLICE_NAME_PATTERN) ? slice : null;
            }
        }
        return null;
    }

    Map<String, String> properties(String unit) {
        String output = commands.run(null, "systemctl", "show", unit,
                "--property=LoadState", "--property=ActiveState", "--property=ControlGroup", "--property=MainPID");
        Map<String, String> properties = new HashMap<>();
        for (String line : output.split("\\R")) {
            int separator = line.indexOf('=');
            if (separator > 0) {
                properties.put(line.substring(0, separator), line.substring(separator + 1));
            }
        }
        return properties;
    }

    void reload() {
        commands.run(null, "sudo", "-n", "systemctl", "daemon-reload");
    }

    void start(String unit) {
        commands.run(null, "sudo", "-n", "systemctl", "start", unit);
    }

    void restart(String unit) {
        commands.run(null, "sudo", "-n", "systemctl", "stop", unit);
        start(unit);
    }

    Path legacyControlGroup(String controlGroup) {
        return underRoot(legacyRoot, legacyRoot.resolve(stripRoot(controlGroup)).normalize());
    }

    private Path dropInPath(String unit) {
        return unitRoot.resolve(unit + ".d").resolve(SYSTEMD_DROP_IN);
    }

    private String readDropIn(Path path) {
        return commands.run(null, "sudo", "-n", "sh", "-c", "if [ -f \"$1\" ]; then cat \"$1\"; fi",
                "resource-control-read", path.toString());
    }

    @Deferred
    private boolean writeDropIn(Path path, String content) {
        if (content.equals(readDropIn(path))) {
            return false;
        }
        Path temporary = Paths.get(PathUtil.createTempFileWithContent(content));
        Defer.defer(() -> PathUtil.forceRemoveFile(temporary.toString()));
        commands.run(null, "sudo", "-n", "mkdir", "-p", "-m", DIRECTORY_MODE, path.getParent().toString());
        commands.run(null, "sudo", "-n", "install", "-m", DROP_IN_FILE_MODE, temporary.toString(), path.toString());
        return true;
    }

    private boolean removeDropIn(Path path) {
        if (readDropIn(path).isEmpty()) {
            return false;
        }
        commands.run(null, "sudo", "-n", "rm", "-f", path.toString());
        return true;
    }
}
