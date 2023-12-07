package org.zstack.header;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PackageAPIInfo {
    public static String PERMISSION_COMMUNITY_AVAILABLE = "community_available";
    /**
     * If an api is available in ZSV basic version,
     * then the api is also available in ZSV advanced version.
     * 
     * It contains {@link #PERMISSION_ZSV_ADVANCED_AVAILABLE}
     */
    public static String PERMISSION_ZSV_BASIC_AVAILABLE = "zsv_basic_available";
    public static String PERMISSION_ZSV_ADVANCED_AVAILABLE = "zsv_advanced_available";

    String APICategoryName() default "";

    /**
     * If a package with PackageAPIInfo annotation, default permissions of api which in this package is:
     * 1. community_NOT_available (only premium API)
     * 2. zsv_NOT_available
     * 
     * If a package without PackageAPIInfo annotation, the permissions of api which in this package is:
     * 1. community_available ({@link #PERMISSION_COMMUNITY_AVAILABLE})
     * 2. zsv_NOT_available
     */
    String[] permissions() default {};
    String productName() default "";
}
