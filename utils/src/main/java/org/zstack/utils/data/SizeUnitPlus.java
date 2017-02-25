package org.zstack.utils.data;

public enum SizeUnitPlus {
    BYTE {
        public double toByte(double s) {
            return s;
        }

        public double toKiloByte(double s) {
            return (s / (k / b));
        }

        public double toMegaByte(double s) {
            return (s / (m / b));
        }

        public double toGigaByte(double s) {
            return (s / (g / b));
        }

        public double toTeraByte(double s) {
            return (s / (t / b));
        }

        public double convert(double s, SizeUnitPlus src) {
            return src.toByte(s);
        }
    },
    KILOBYTE {
        public double toByte(double s) {
            return (s * (k / b));
        }

        public double toKiloByte(double s) {
            return s;
        }

        public double toMegaByte(double s) {
            return (s / (m / k));
        }

        public double toGigaByte(double s) {
            return (s / (g / k));
        }

        public double toTeraByte(double s) {
            return (s / (t / k));
        }

        public double convert(double s, SizeUnitPlus src) {
            return src.toKiloByte(s);
        }
    },
    MEGABYTE {
        public double toByte(double s) {
            return (s * (m / b));
        }

        public double toKiloByte(double s) {
            return (s * (m / k));
        }

        public double toMegaByte(double s) {
            return s;
        }

        public double toGigaByte(double s) {
            return (s / (g / m));
        }

        public double toTeraByte(double s) {
            return (s / (t / m));
        }

        public double convert(double s, SizeUnitPlus src) {
            return src.toMegaByte(s);
        }
    },
    GIGABYTE {
        public double toByte(double s) {
            return (s * (g / b));
        }

        public double toKiloByte(double s) {
            return (s * (g / k));
        }

        public double toMegaByte(double s) {
            return (s * (g / m));
        }

        public double toGigaByte(double s) {
            return s;
        }

        public double toTeraByte(double s) {
            return (s / (t / g));
        }

        public double convert(double s, SizeUnitPlus src) {
            return src.toGigaByte(s);
        }
    },
    TERABYTE {
        public double toByte(double s) {
            return (s * (t / b));
        }

        public double toKiloByte(double s) {
            return (s * (t / k));
        }

        public double toMegaByte(double s) {
            return (s * (t / m));
        }

        public double toGigaByte(double s) {
            return (s * (t / g));
        }

        public double toTeraByte(double s) {
            return s;
        }

        public double convert(double s, SizeUnitPlus src) {
            return src.toTeraByte(s);
        }

    };

    public static SizeUnitPlus fromString(String s) {
        if ("b".equalsIgnoreCase(s)) {
            return BYTE;
        } else if ("k".equalsIgnoreCase(s)) {
            return KILOBYTE;
        } else if ("m".equalsIgnoreCase(s)) {
            return MEGABYTE;
        } else if ("g".equalsIgnoreCase(s)) {
            return GIGABYTE;
        } else if ("t".equalsIgnoreCase(s)) {
            return TERABYTE;
        } else {
            throw new IllegalArgumentException(String.format("unknown size unit[%s]", s));
        }
    }

    private static final double b = 1;
    private static final double k = b * 1024;
    private static final double m = k * 1024;
    private static final double g = m * 1024;
    private static final double t = g * 1024;

    public double toByte(double s) {
        throw new AbstractMethodError();
    }
    public double toKiloByte(double s) {
        throw new AbstractMethodError();
    }
    public double toMegaByte(double s) {
        throw new AbstractMethodError();
    }
    public double toGigaByte(double s) {
        throw new AbstractMethodError();
    }
    public double toTeraByte(double s) {
        throw new AbstractMethodError();
    }

    public double convert(double s, SizeUnitPlus src) {
        throw new AbstractMethodError();
    }
}
