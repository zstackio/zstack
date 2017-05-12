package org.zstack.utils.data;

public enum SizeUnit {
    BYTE {
        public long toByte(long s) {
            return s;
        }

        public long toKiloByte(long s) {
            return (s / (k / b));
        }

        public long toMegaByte(long s) {
            return (s / (m / b));
        }

        public long toGigaByte(long s) {
            return (s / (g / b));
        }

        public long toTeraByte(long s) {
            return (s / (t / b));
        }

        public long convert(long s, SizeUnit src) {
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

        public long toMegaByte(long s) {
            return (s / (m / k));
        }

        public long toGigaByte(long s) {
            return (s / (g / k));
        }

        public long toTeraByte(long s) {
            return (s / (t / k));
        }

        public long convert(long s, SizeUnit src) {
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

        public long toGigaByte(long s) {
            return (s / (g / m));
        }

        public long toTeraByte(long s) {
            return (s / (t / m));
        }

        public long convert(long s, SizeUnit src) {
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

        public long toTeraByte(long s) {
            return (s / (t / g));
        }

        public long convert(long s, SizeUnit src) {
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

        public long convert(long s, SizeUnit src) {
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
    public long toKiloByte(long s) {
        throw new AbstractMethodError();
    }
    public long toMegaByte(long s) {
        throw new AbstractMethodError();
    }
    public long toGigaByte(long s) {
        throw new AbstractMethodError();
    }
    public long toTeraByte(long s) {
        throw new AbstractMethodError();
    }

    public long convert(long s, SizeUnit src) {
        throw new AbstractMethodError();
    }


}
