package org.zstack.utils;

import org.zstack.utils.data.ArraySpliter;
import org.zstack.utils.data.ArraySpliterImpl;
import org.zstack.utils.data.FieldPrinter;
import org.zstack.utils.data.FieldPrinterImpl;
import org.zstack.utils.filelocater.FileLocator;
import org.zstack.utils.filelocater.FileLocatorImpl;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;
import org.zstack.utils.path.PathUtilImpl;
import org.zstack.utils.path.PathUtils;
import org.zstack.utils.stopwatch.StopWatch;
import org.zstack.utils.stopwatch.StopWatchImpl;
import org.zstack.utils.tester.ZTester;
import org.zstack.utils.tester.ZTesterImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class Utils {
	private static ArraySpliter arraySpliter = null;
	private static FieldPrinter fieldPrinter = null;
	private static PathUtils pathUtil = null;

    private static ThreadLocal<Set<String>> maskWords = ThreadLocal.withInitial(Collections::emptySet);

    private static final Function<String, String> defaultRewriter = raw -> {
        for (String s : maskWords.get()) {
            raw = raw.replace(String.format(": \"%s\"", s), ": \"*****\"").
                    replace(String.format(":\"%s\"", s), ":\"*****\"");
        }
        return raw;
    };

    public static Set<String> getLogMaskWords() {
        return maskWords.get();
    }

    public static class MaskWords implements AutoCloseable {
        public MaskWords(Set<String> words) {
            maskWords.set(new HashSet<>(words));
        }

        @Override
        public void close() {
            maskWords.remove();
        }
    }

	static {
		arraySpliter = new ArraySpliterImpl();
		fieldPrinter = new FieldPrinterImpl();
		pathUtil = new PathUtilImpl();
	}

    public static StopWatch getStopWatch() {
        return new StopWatchImpl();
    }

    public static CLogger getLogger(String className) {
        return CLoggerImpl.getLogger(className);
     }

    public static CLogger getLogger(Class<?> clazz) {
        return CLoggerImpl.getLogger(clazz);
    }

    public static CLogger getLogger(String className, Function<String, String> rewriter) {
        return CLoggerImpl.getLogger(className,rewriter);
    }

    public static CLogger getLogger(Class<?> clazz, Function<String, String> rewriter) {
        return CLoggerImpl.getLogger(clazz, rewriter);
    }

    public static CLogger getSafeLogger(String className) {
        return CLoggerImpl.getLogger(className, defaultRewriter);
    }

    public static CLogger getSafeLogger(Class<?> clazz) {
        return CLoggerImpl.getLogger(clazz, defaultRewriter);
    }

    public static FileLocator createFileLocator() {
        return new FileLocatorImpl();
    }

    public static ArraySpliter getArraySpliter() {
    	return arraySpliter;
    }

    public static FieldPrinter getFieldPrinter() {
    	return fieldPrinter;
    }

    public static PathUtils getPathUtil() {
    	return pathUtil;
    }

    public static ZTester getTester() {
        return ZTesterImpl.getTester();
    }
}
