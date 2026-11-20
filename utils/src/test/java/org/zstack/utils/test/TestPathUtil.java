package org.zstack.utils.test;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.zstack.utils.Utils;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.path.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.function.UnaryOperator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class TestPathUtil {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void updateTextFileOnlyWhenContentChanges() throws IOException {
        Path file = temp.newFile().toPath();
        Files.write(file, "name=test\nenabled=false\n".getBytes(UTF_8));

        PathUtil.updateTextFile(file, content -> content.replace("enabled=false", "enabled=true"));
        assertEquals("name=test\nenabled=true\n", new String(Files.readAllBytes(file), UTF_8));

        FileTime oldTime = FileTime.fromMillis(1_000_000);
        Files.setLastModifiedTime(file, oldTime);
        PathUtil.updateTextFile(file, UnaryOperator.identity());
        assertEquals(oldTime, Files.getLastModifiedTime(file));
    }

	@Test
	public void test() {
		PathUtils p = Utils.getPathUtil();
		
		System.out.println(p.join("a", "b", "c", "d"));
		System.out.println(p.join("/a", "/b", "c", "d"));
		System.out.println(p.join("/a", "/b", "//c", "d"));

        System.out.println(PathUtil.parentFolder("tmp/a"));
        System.out.println(PathUtil.parentFolder("/tmp/a"));
        System.out.println(PathUtil.parentFolder("tmp/a.xx"));
        System.out.println(PathUtil.parentFolder("/tmp/a.xx"));
        System.out.println(PathUtil.parentFolder("/tmp/a/a.xx"));
        System.out.println(PathUtil.parentFolder("/tmp/a/a"));
	}
}
