package org.zstack.core.safeguard;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class SafeGuard {
    private static ThreadLocal<GuardStack<Runnable>> stack = new ThreadLocal<GuardStack<Runnable>>();
    private static final CLogger logger = Utils.getLogger(SafeGuard.class);
    
    private static GuardStack<Runnable> getStack()  {
        GuardStack<Runnable> sg = stack.get(); 
        if (sg == null) {
            sg = new GuardStack<Runnable>(); 
            stack.set(sg);
        }    
        return sg;
    }
    
    public static void guard(Runnable r) {
        GuardStack<Runnable> sg = getStack();
        sg.push(r);
    }
    
    static void pushTop() {
        GuardStack<Runnable> sg = getStack();
        sg.pushTop();
    }
    
    static int popTop() {
        GuardStack<Runnable> sg = getStack();
        return sg.popTop();
    }
    
    static void rollback(String sourceLocation, int top) {
        GuardStack<Runnable> sg = getStack();
        int count = sg.size() - top;
        for (int i=0; i<count; i++) {
            try {
                Runnable r = sg.pop();
                r.run();
            } catch (Throwable e) {
                logger.warn("A unhandled exception happened during calling safeguard in " + sourceLocation, e);
            }
        }
    }
    
    static void revert(int top) {
        GuardStack<Runnable> sg = getStack();
        sg.revert(top);
    }
    
    static int getTop() {
        GuardStack<Runnable> sg = getStack();
        return sg.getTop();
    }
    
    static int getSize() {
        GuardStack<Runnable> sg = getStack();
        return sg.size();
    }
}
