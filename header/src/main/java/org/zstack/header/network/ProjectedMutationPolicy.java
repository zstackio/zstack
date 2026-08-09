package org.zstack.header.network;

/** Pure validation used by projected updates and subtype conversions before any DB write. */
public final class ProjectedMutationPolicy {
    private ProjectedMutationPolicy() {
    }

    public static void requireSourceType(String actual, String expected) {
        if (expected == null || expected.isEmpty()) {
            return;
        }
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(String.format("stale projected source type: expected %s but found %s", expected, actual));
        }
    }

    public static void requireL2Conversion(String sourceType, String targetType, boolean hasDependencies) {
        requireSourceType(sourceType, "L2NoVlanNetwork");
        if (!"L2VlanNetwork".equals(targetType) && !"L2NoVlanNetwork".equals(targetType)) {
            throw new IllegalArgumentException("unsupported projected L2 conversion target: " + targetType);
        }
        if (hasDependencies) {
            throw new IllegalArgumentException("projected L2 conversion has prohibited dependencies");
        }
    }

    public static void requireL3Conversion(String sourceType, String targetType, boolean hasDependencies) {
        requireSourceType(sourceType, "L3BasicNetwork");
        if (!"L3BasicNetwork".equals(targetType) && !"L3VpcNetwork".equals(targetType)) {
            throw new IllegalArgumentException("unsupported projected L3 conversion target: " + targetType);
        }
        if (hasDependencies) {
            throw new IllegalArgumentException("projected L3 conversion has prohibited dependencies");
        }
    }
}
