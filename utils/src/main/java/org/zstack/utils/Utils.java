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

public class Utils {
	private static ArraySpliter arraySpliter = null;
	private static FieldPrinter fieldPrinter = null;
	private static PathUtils pathUtil = null;
	
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
}
