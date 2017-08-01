package org.zstack.utils.data;

public enum SizeUnit {
    BYTE {
        public long toByte(long s) {
            return s;
        }

        @Deprecated
        public long toKiloByte(long s) {
            return (s / (k / b));
        }

        @Deprecated
        public long toMegaByte(long s) {
            return (s / (m / b));
        }

        @Deprecated
        public long toGigaByte(long s) {
            return (s / (g / b));
        }

        @Deprecated
        public long toTeraByte(long s) {
            return (s / (t / b));
        }

        public long convert(long s, SizeUnit src) {
            return src.toByte(s);
        }

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

        public double convert(double s, SizeUnit src) {
            return src.toByte(s);
        }
    },
    KILOBYTE {
        public long toByte(long s) {
            return (s * (k / b));
        }

        public long toKiloByte(long s) {
            return s;
        }

        @Deprecated
        public long toMegaByte(long s) {
            return (s / (m / k));
        }

        @Deprecated
        public long toGigaByte(long s) {
            return (s / (g / k));
        }

        @Deprecated
        public long toTeraByte(long s) {
            return (s / (t / k));
        }

        @Deprecated
        public long convert(long s, SizeUnit src) {
            return src.toKiloByte(s);
        }

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

        public double convert(double s, SizeUnit src) {
            return src.toKiloByte(s);
        }
    },
    MEGABYTE {
        public long toByte(long s) {
            return (s * (m / b));
        }

        public long toKiloByte(long s) {
            return (s * (m / k));
        }

        public long toMegaByte(long s) {
            return s;
        }

        @Deprecated
        public long toGigaByte(long s) {
            return (s / (g / m));
        }

        @Deprecated
        public long toTeraByte(long s) {
            return (s / (t / m));
        }

        @Deprecated
        public long convert(long s, SizeUnit src) {
            return src.toMegaByte(s);
        }

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

        public double convert(double s, SizeUnit src) {
            return src.toMegaByte(s);
        }
    },
    GIGABYTE {
        public long toByte(long s) {
            return (s * (g / b));
        }

        public long toKiloByte(long s) {
            return (s * (g / k));
        }

        public long toMegaByte(long s) {
            return (s * (g / m));
        }


        public long toGigaByte(long s) {
            return s;
        }

        @Deprecated
        public long toTeraByte(long s) {
            return (s / (t / g));
        }

        @Deprecated
        public long convert(long s, SizeUnit src) {
            return src.toGigaByte(s);
        }

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

        public double convert(double s, SizeUnit src) {
            return src.toGigaByte(s);
        }
    },
    TERABYTE {
        public long toByte(long s) {
            return (s * (t / b));
        }

        public long toKiloByte(long s) {
            return (s * (t / k));
        }

        public long toMegaByte(long s) {
            return (s * (t / m));
        }

        public long toGigaByte(long s) {
            return (s * (t / g));
        }

        public long toTeraByte(long s) {
            return s;
        }

        @Deprecated
        public long convert(long s, SizeUnit src) {
            return src.toTeraByte(s);
        }

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

        public double convert(double s, SizeUnit src) {
            return src.toTeraByte(s);
        }

    };

    public static SizeUnit fromString(String s) {
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

    private static final long b = 1;
    private static final long k = b * 1024;
    private static final long m = k * 1024;
    private static final long g = m * 1024;
    private static final long t = g * 1024;

    public long toByte(long s) {
        throw new AbstractMethodError();
    }

    @Deprecated
    public long toKiloByte(long s) {
        throw new AbstractMethodError();
    }

    @Deprecated
    public long toMegaByte(long s) {
        throw new AbstractMethodError();
    }

    @Deprecated
    public long toGigaByte(long s) {
        throw new AbstractMethodError();
    }

    @Deprecated
    public long toTeraByte(long s) {
        throw new AbstractMethodError();
    }

    @Deprecated
    public long convert(long s, SizeUnit src) {
        // long type will result in wrong number, please use double
        throw new AbstractMethodError();
    }


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

    public double convert(double s, SizeUnit src) {
        throw new AbstractMethodError();
    }

}
