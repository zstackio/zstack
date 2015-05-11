package org.zstack.core.safeguard;

import java.util.EmptyStackException;
import java.util.Stack;


class GuardStack<Runnable> extends Stack<Runnable> {
    private Stack<Integer> stackTop = new Stack<Integer>();
    
    void pushTop() {
        stackTop.push(this.size());
    }
    
    int popTop() {
        return stackTop.pop();
    }
    
    int getTop() {
        try {
            return stackTop.peek();
        } catch (EmptyStackException e) {
            return 0;
        }
    }
    
    void revert(int top) {
        if (this.size() > top) {
            this.removeRange(top, this.size()); 
        }
    }
}
