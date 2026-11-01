package org.zstack.core.plugin;

import org.apache.commons.io.FileUtils;
import org.zstack.header.core.external.plugin.FeaturePluginInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.YamlUtils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.util.*;

public class FeaturePlugins {
    private static final CLogger logger = Utils.getLogger(FeaturePlugins.class);
    private static final String FOLDER = "featurePlugins";

    private static List<FeaturePluginInventory> installed;
    private static final Map<String, List<String>> entitiesByPlugin = new HashMap<>();
    // canonical paths of the files zstack-ctl installed for each plugin
    private static final Map<String, Set<String>> filesByPlugin = new HashMap<>();

    public static synchronized List<FeaturePluginInventory> installed() {
        if (installed == null) {
            installed = load();
        }
        return installed;
    }

    public static List<String> loadedEntityClasses() {
        List<String> ret = new ArrayList<>();
        for (FeaturePluginInventory inv : installed()) {
            if (FeaturePluginInventory.STATUS_LOADED.equals(inv.getStatus())) {
                ret.addAll(entitiesByPlugin.get(inv.getId()));
            }
        }
        return ret;
    }

    /**
     * @return the id of the feature plugin whose jar contains the class, null if the class does not come from a plugin
     */
    public static String pluginIdOf(Class<?> clz) {
        installed();
        CodeSource source = clz.getProtectionDomain().getCodeSource();
        if (source == null || source.getLocation() == null) {
            return null;
        }

        String path;
        try {
            path = new File(source.getLocation().toURI()).getCanonicalPath();
        } catch (Exception e) {
            return null;
        }
        for (Map.Entry<String, Set<String>> e : filesByPlugin.entrySet()) {
            if (e.getValue().contains(path)) {
                return e.getKey();
            }
        }
        return null;
    }

    private static Set<String> loadFiles(File record) {
        Set<String> ret = new HashSet<>();
        File files = new File(record.getParentFile(), record.getName().replaceAll("\\.yaml$", ".files"));
        if (!files.exists()) {
            return ret;
        }
        try {
            for (String line : FileUtils.readLines(files, StandardCharsets.UTF_8)) {
                if (!line.trim().isEmpty()) {
                    ret.add(new File(line.trim()).getCanonicalPath());
                }
            }
        } catch (IOException e) {
            logger.warn(String.format("failed to read installed files of feature plugin %s", files.getAbsolutePath()), e);
        }
        return ret;
    }

    private static List<FeaturePluginInventory> load() {
        List<FeaturePluginInventory> ret = new ArrayList<>();
        for (String path : PathUtil.scanFolderOnClassPath(FOLDER)) {
            if (path.endsWith(".yaml")) {
                FeaturePluginInventory inv = loadDescriptor(new File(path));
                filesByPlugin.put(inv.getId(), loadFiles(new File(path)));
                ret.add(inv);
            }
        }
        ret.sort(Comparator.comparing(FeaturePluginInventory::getId));
        return ret;
    }

    @SuppressWarnings("unchecked")
    private static FeaturePluginInventory loadDescriptor(File file) {
        FeaturePluginInventory inv = new FeaturePluginInventory();
        inv.setId(file.getName().replaceAll("\\.yaml$", ""));
        try {
            Map<String, Object> desc = (Map<String, Object>) YamlUtils.load(FileUtils.readFileToString(file, StandardCharsets.UTF_8));
            inv.setId(String.valueOf(desc.get("id")));
            inv.setName(String.valueOf(desc.get("name")));
            inv.setCategory(String.valueOf(desc.get("category")));
            inv.setVersion(String.valueOf(desc.get("version")));
            inv.setSha256((String) desc.get("sha256"));
            Map<String, Object> license = (Map<String, Object>) desc.get("license");
            inv.setLicenseAddon(license == null ? null : (String) license.get("addon"));

            // zstack-ctl leaves a record without files when a plugin cannot be brought to its desired state
            Map<String, Object> skip = (Map<String, Object>) desc.get("skip");
            if (skip != null) {
                inv.setStatus(String.valueOf(skip.get("status")));
                inv.setError(String.valueOf(skip.get("reason")));
                return inv;
            }

            Map<String, Object> mn = desc.get("mn") == null ? new HashMap<>() : (Map<String, Object>) desc.get("mn");
            List<String> entities = mn.get("entities") == null ? new ArrayList<>() : (List<String>) mn.get("entities");
            for (String entity : entities) {
                Class.forName(entity);
            }
            entitiesByPlugin.put(inv.getId(), entities);
            inv.setStatus(FeaturePluginInventory.STATUS_LOADED);
        } catch (Throwable t) {
            inv.setStatus(FeaturePluginInventory.STATUS_FAILED);
            inv.setError(t.toString());
            logger.warn(String.format("failed to load feature plugin descriptor %s", file.getAbsolutePath()), t);
        }
        return inv;
    }
}
