package org.zstack.test.core.defer;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestDefer {
    CLogger logger = Utils.getLogger(TestDefer.class);
    boolean success = false;
    boolean success21 = false;
    boolean success22 = false;
    boolean success31 = false;
    boolean success41 = false;
    boolean success51 = false;
    boolean success52 = false;
    boolean success61 = false;
    boolean success62 = false;
    boolean success63 = false;

    @Deferred
    private void case1() {
        Defer.defer(new Runnable() {
            @Override
            public void run() {
                success = true;
            }
        });
    }

    @Deferred
    private void case21() {
        Defer.defer(new Runnable() {
            @Override
            public void run() {
                success21 = true;
            }
        });

        case22();
    }

    @Deferred
    private void case22() {
        Defer.defer(new Runnable() {
            @Override
            public void run() {
                success22 = true;
            }
        });
    }

    private void case2() {
        case21();
        Assert.assertTrue(success21);
        Assert.assertTrue(success22);
    }

    @Deferred
    private void case31() {
        Defer.defer(new Runnable() {
            @Override
            public void run() {
                success31 = true;
            }
        });

        throw new RuntimeException("case 31");
    }

    private void case3() {
        try {
            case31();
        } catch (RuntimeException e) {
            //ignore
        }

        Assert.assertTrue(success31);
    }

    private void throwExceptionForCase41() {
        throw new RuntimeException("case 41");
    }

    @Deferred
    private void case41() {
        throwExceptionForCase41();
        Defer.defer(new Runnable() {
            @Override
            public void run() {
                success41 = true;
            }
        });
    }

    private void case4() {
        try {
            case41();
        } catch (RuntimeException e) {
            //ignore
        }
        Assert.assertFalse(success41);
    }

    @Deferred
    private void case51() {
        Defer.defer(new Runnable() {
            @Override
            public void run() {
                success51 = true;
            }
        });

        Defer.guard(new Runnable() {
            @Override
            public void run() {
                success52 = true;
            }
        });

        throw new RuntimeException("case 31");
    }

    private void case5() {
        try {
            case51();
        } catch (RuntimeException e) {
            //ignore
        }

        Assert.assertTrue(success51);
        Assert.assertTrue(success52);
    }

    @Deferred
    private void case62() {
        Defer.guard(new Runnable() {
            @Override
            public void run() {
                success62 = true;
            }
        });

        throw new RuntimeException("case 61");
    }

    @Deferred
    private void case61() {
        Defer.defer(new Runnable() {
            @Override
            public void run() {
                success61 = true;
            }
        });

        Defer.guard(new Runnable() {
            @Override
            public void run() {
                success63 = true;
            }
        });

        case62();
    }

    private void case6() {
        try {
            case61();
        } catch (RuntimeException e) {
            //ignore
        }

        Assert.assertTrue(success61);
        Assert.assertTrue(success62);
        Assert.assertTrue(success63);
    }

    @Test
    public void test() {
        case1();
        Assert.assertTrue(success);
        case2();
        case3();
        case4();
        case5();
        case6();
    }
}
