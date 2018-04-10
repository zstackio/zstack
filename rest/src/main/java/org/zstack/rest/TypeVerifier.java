package org.zstack.rest;

import org.zstack.core.Platform;

import java.lang.reflect.Field;
import java.util.function.BiFunction;

/**
 * @Author: fubang
 * @Date: 2018/4/2
 */
public class TypeVerifier {

	private static final BiFunction<Field, String, String> verifyInt = (Field f, String source) -> {
		if (f.getType().isAssignableFrom(int.class) || f.getType()
				.isAssignableFrom(Integer.class)|| f.getType().isAssignableFrom(long.class)||f.getType().isAssignableFrom(Long.class)) {
			String error = Platform
					.i18n("[%s] field is excepted an int or long, but was [%s].", f.getName(), source);
			try {
				double value = Double.parseDouble(source);
				if (value != Math.floor(value)) {
					return error;
				}
			} catch (Exception e) {
				return error;
			}
		}
		return null;
	};

	private static final BiFunction<Field, String, String> verifyBoolean = (Field f, String source) -> {
		if (f.getType().isAssignableFrom(boolean.class) || f.getType()
				.isAssignableFrom(Boolean.class)) {
			if (!(source.equalsIgnoreCase("true") || source.equalsIgnoreCase("false"))) {
				return Platform.i18n("Invalid value for boolean field [%s],"
						+ " [%s] is not a valid boolean string[true, false].", f.getName(), source);
			}
		}
		return null;
	};

	private static final BiFunction<Field, String, String>[] functions = new BiFunction[] {
			verifyBoolean, verifyInt };

	public static String verify(Field f, String source) {
		for (BiFunction<Field, String, String> bi : functions) {
			String result = bi.apply(f, source);
			if (result != null) {
				return result.toString();
			}
		}
		return null;
	}
}
