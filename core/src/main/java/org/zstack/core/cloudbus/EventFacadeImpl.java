package org.zstack.core.cloudbus;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Event;
import org.zstack.header.message.NeedJsonSchema;
import org.zstack.utils.TypeUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class EventFacadeImpl implements EventFacade, CloudBusEventListener, Component {
    @Autowired
    private CloudBus bus;

    private ConcurrentMap<Object, CallbackWrapper> callbacks = new ConcurrentHashMap<Object, CallbackWrapper>();
    private final List<Object> toRemove = new ArrayList<Object>();
    private final List<CallbackWrapper> toAdd = new ArrayList<CallbackWrapper>();
    private EventSubscriberReceipt unsubscriber;

    private class CallbackWrapper {
        String path;
        String glob;
        Object callback;
        AtomicBoolean hasRun;

        CallbackWrapper(String path, String glob, Object callback) {
            this.path = path;
            this.glob = glob;
            this.callback = callback;
            if (callback instanceof AutoOffEventCallback) {
                hasRun = new AtomicBoolean(false);
            }
        }

        Object getIdentity() {
            return callback;
        }

        public String getGlob() {
            return glob;
        }

        @AsyncThread
        void call(CanonicalEvent e) {
            if (callback instanceof Runnable) {
                Runnable r = (Runnable)callback;
                r.run();
            } else {
                Map<String, String> tokens = tokenize(e.getPath(), path);
                tokens.put(EventFacade.META_DATA_MANAGEMENT_NODE_ID, e.getManagementNodeId());
                Object data = null;
                if (e.getContent() != null) {
                    data = e.getContent();
                }

                if (hasRun != null && !hasRun.compareAndSet(false, true)) {
                    return;
                }

                if (callback instanceof EventCallback) {
                    ((EventCallback)callback).run(tokens, data);
                } else if (callback instanceof AutoOffEventCallback) {
                    if (((AutoOffEventCallback)callback).run(tokens, data)) {
                        off(callback);
                    }
                }
            }
        }
    }

    public String createRegexFromGlob(String glob) {
        String out = "^";
        for(int i = 0; i < glob.length(); ++i) {
            final char c = glob.charAt(i);
            switch(c) {
                case '*': out += ".*"; break;
                case '?': out += '.'; break;
                case '\\': out += "\\\\"; break;
                default: out += c;
            }
        }
        out += '$';
        return out;
    }

    private Map<String, String> tokenize(String str1, String str2) {
        StringTokenizer token = new StringTokenizer(str1, "/");
        List<String> origins =  new ArrayList<String>();
        while (token.hasMoreElements()) {
            origins.add(token.nextToken());
        }

        token = new StringTokenizer(str2, "/");
        List<String> t = new ArrayList<String>();
        while (token.hasMoreElements()) {
            t.add(token.nextToken());
        }

        Map ret = new HashMap();
        for (int i=0;i<t.size(); i++) {
            String key = t.get(i);
            if (!key.startsWith("{") || !key.endsWith("}")) {
                continue;
            }

            key = key.replaceAll("\\{", "").replaceAll("\\}", "");
            ret.put(key, origins.get(i));
        }

        return ret;
    }


    @Override
    public void on(String path, AutoOffEventCallback cb) {
        synchronized (toAdd) {
            String glob = createRegexFromGlob(path.replaceAll("\\{.*\\}", ".*"));
            toAdd.add(new CallbackWrapper(path, glob, cb));
        }
    }

    @Override
    public void on(String path, final EventCallback cb) {
        synchronized (toAdd) {
            String glob = createRegexFromGlob(path.replaceAll("\\{.*\\}", ".*"));
            toAdd.add(new CallbackWrapper(path, glob, cb));
        }
    }

    @Override
    public void on(String path, Runnable runnable) {
        synchronized (toAdd) {
            String glob = createRegexFromGlob(path.replaceAll("\\{.*\\}", ".*"));
            toAdd.add(new CallbackWrapper(path, glob, runnable));
        }
    }

    @Override
    public void off(Object cb) {
        synchronized (toRemove) {
            toRemove.add(cb);
        }
    }

    @Override
    public void fire(String path, Object data) {
        assert path != null;
        CanonicalEvent evt = new CanonicalEvent();
        evt.setPath(path);
        evt.setManagementNodeId(Platform.getManagementServerId());
        if (data != null) {
            if (!TypeUtils.isPrimitiveOrWrapper(data.getClass()) && !data.getClass().isAnnotationPresent(NeedJsonSchema.class)) {
                throw new CloudRuntimeException(String.format("data[%s] passed to canonical event is not annotated by @NeedJsonSchema", data.getClass().getName()));
            }

            evt.setContent(data);
        }
        bus.publish(evt);
    }

    @Override
    public boolean isFromThisManagementNode(Map tokens) {
        return Platform.getManagementServerId().equals(tokens.get(EventFacade.META_DATA_MANAGEMENT_NODE_ID));
    }

    @Override
    public boolean handleEvent(Event evt) {
        if (!(evt instanceof CanonicalEvent)) {
            return false;
        }

        synchronized (toAdd) {
            if (!toAdd.isEmpty()) {
                for (CallbackWrapper wrapper : toAdd) {
                    callbacks.put(wrapper.getIdentity(), wrapper);
                }

                toAdd.clear();
            }
        }

        synchronized (toRemove) {
            if (!toRemove.isEmpty()) {
                for (Object cb : toRemove) {
                    callbacks.remove(cb);
                }

                toRemove.clear();
            }
        }

        CanonicalEvent cevt = (CanonicalEvent)evt;
        for (CallbackWrapper wrapper : callbacks.values()) {
            if (cevt.getPath().matches(wrapper.getGlob())) {
                wrapper.call(cevt);
            }
        }

        return false;
    }

    @Override
    public boolean start() {
        unsubscriber =  bus.subscribeEvent(this, new CanonicalEvent());
        return true;
    }

    @Override
    public boolean stop() {
        if (unsubscriber != null) {
            unsubscriber.unsubscribeAll();
        }
        return true;
    }
}
