package org.zstack.utils.path;

import org.apache.commons.codec.digest.DigestUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.File;
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PathUtil {
    private static final CLogger logger = Utils.getLogger(PathUtil.class);
    public static String HOME_DIR_PROPERTY_NAME = "user.home";

    public static String join(String... paths) {
        assert paths != null && paths.length > 0;

        File parent = new File(paths[0]);
        for (int i=1; i<paths.length; i++) {
            parent = new File(parent, paths[i]);
        }
        return parent.getPath();
    }

    public static String absPath(String path) {
        if (path.startsWith("~")) {
            path = path.replaceAll("~", System.getProperty(HOME_DIR_PROPERTY_NAME));
        }

        return new File(path).getAbsolutePath();
    }

    public static String getZStackHomeFolder() {
        String homeDir = System.getProperty(HOME_DIR_PROPERTY_NAME);
        File f = new File(homeDir);
        if (!f.exists()) {
            f.mkdirs();
        }
        return f.getAbsolutePath();
    }

    public static String getFolderUnderZStackHomeFolder(String folder) {
        String path = join(getZStackHomeFolder(), folder);
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }
        return f.getAbsolutePath();
    }

    public static String getFilePathUnderZStackHomeFolder(String path) {
        String folder = getFolderUnderZStackHomeFolder(parentFolder(path));
        return join(folder, new File(path).getName());
    }

    public static String parentFolder(String fullPath) {
        if (! fullPath.contains(File.separator)) {
            return fullPath;
        }

        return fullPath.substring(0, fullPath.lastIndexOf(File.separator));
    }

    public static String fileName(String fullPath) {
        return new File(fullPath).getName();
    }

    public static File findFileOnClassPath(String fileName, boolean exceptionOnNotFound) {
        File f = findFileOnClassPath(fileName);
        if (f == null && exceptionOnNotFound) {
            throw new RuntimeException(String.format("unable to find file[%s] on classpath", fileName));
        }

        return f;
    }

    public static boolean exists(String path) {
        File f = new File(path);
        return f.exists();
    }

    public static File findFolderOnClassPath(String folderName,  boolean exceptionOnNotFound) {
        File folder = findFolderOnClassPath(folderName);
        if (folder == null && exceptionOnNotFound) {
            throw new RuntimeException(String.format("The folder %s is not found on classpath or there is another resource has the same name.", folderName));
        }

        return folder;
    }

    public static File findFolderOnClassPath(String folderName) {
        URL folderUrl = PathUtil.class.getClassLoader().getResource(folderName);
        if (folderUrl == null || !folderUrl.getProtocol().equals("file")) {
            logger.warn(String.format("The folder %s is not found on classpath or there is another resource has the same name.", folderName));
            return null;
        }

        try {
            File folder = new File(folderUrl.toURI());
            if (!folder.isDirectory()) {
                return null;
            }

            return folder;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static File findFileOnClassPath(String fileName) {
        URL fileUrl = PathUtil.class.getClassLoader().getResource(fileName);
        if (fileUrl == null || !fileUrl.getProtocol().equals("file")) {
            logger.warn(String.format("The file %s is not found in classpath or there is another resource has the same name.", fileName));
            return null;
        }

        try {
            return new File(fileUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean compareFileByMd5(File src, File dst) {
        try {
            FileInputStream srcIn = new FileInputStream(src);
            String srcMd5 = DigestUtils.md5Hex(srcIn);
            FileInputStream dstIn = new FileInputStream(dst);
            String dstMd5 = DigestUtils.md5Hex(dstIn);

            return srcMd5.equals(dstMd5);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> scanFolderOnClassPath(String folderName) {
        URL folderUrl = PathUtil.class.getClassLoader().getResource(folderName);
        if (folderUrl == null || !folderUrl.getProtocol().equals("file")) {
            String info = String.format("The folder %s is not found in classpath or there is another resource has the same name.",folderName);
            logger.warn(info);
            return new ArrayList<String>();
        }

        try {
            File folder = new File(folderUrl.toURI());
            List<String> ret = new ArrayList<>();
            scanFolder(ret, folder.getAbsolutePath());
            return ret;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Unable to locate service portal configure files"), e);
        }
    }

    public static List<String> scanFolder(List<String> ret, String folderName) {
        try {
            File folder = new File(folderName);
            if (folder.isDirectory()) {
                File[] fileArray = folder.listFiles();
                for(File f: fileArray) {
                    if (f.isDirectory()) {
                        scanFolder(ret, f.getAbsolutePath());
                    } else {
                        ret.add(PathUtil.join(folder.getAbsolutePath(), f.getName()));
                    }
                }
            }
            return ret;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Unable to locate service portal configure files"), e);
        }
    }
}
