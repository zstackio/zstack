package org.zstack.core.logging;

import com.fasterxml.uuid.Generators;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.message.NeedJsonSchema;
import org.zstack.utils.DebugUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2016/5/30.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class Log {
    @Autowired
    protected LogFacade logf;

    protected LogType type;

    @NeedJsonSchema
    public static class Content {
        public LogLevel level;
        public String text;
        public List parameters;
        public String resourceUuid;
        public String uuid;
        public long dateInLong;
        public Date date;
        public Object opaque;

        public Content() {
        }

        public Content(Content other) {
            this.level = other.level;
            this.text = other.text;
            this.parameters = other.parameters;
            this.resourceUuid = other.resourceUuid;
            this.uuid = other.uuid;
            this.dateInLong = other.dateInLong;
            this.date = other.date;
            this.opaque = other.opaque;
        }
    }

    protected Content content;

    public Log() {
        this(null);
    }

    public Log(String resourceUuid) {
        content = new Content();
        content.resourceUuid = resourceUuid;
        content.uuid = Generators.timeBasedGenerator().generate().toString().replace("-", "");
        content.dateInLong = System.currentTimeMillis();
        content.date = new Date(content.dateInLong);
        content.level = LogLevel.INFO;
        type = resourceUuid == null ? LogType.SYSTEM : LogType.RESOURCE;
    }

    public LogType getType() {
        return type;
    }

    public void setType(LogType type) {
        this.type = type;
    }

    public Log setLevel(LogLevel level) {
        content.level = level;
        return this;
    }

    public LogLevel getLevel() {
        return content.level;
    }

    public long getDateInLong() {
        return content.dateInLong;
    }

    public Date getDate() {
        return content.date;
    }

    public String getUuid() {
        return content.uuid;
    }

    public String getResourceUuid() {
        return content.resourceUuid;
    }

    protected Log setText(String label, Collection args) {
        setText(label, args.toArray(new Object[args.size()]));
        return this;
    }

    protected Log setText(String label, Object... args) {
        DebugUtils.Assert(label != null, "label cannot be null");
        content.text = label;
        if (args.length != 0) {
            content.parameters = asList(args).stream().map(Object::toString).collect(Collectors.toList());
        }

        return this;
    }

    public String getText() {
        return content.text;
    }

    public Content getContent() {
        return content;
    }

    public Log setOpaque(Object opaque) {
        content.opaque = opaque;
        return this;
    }

    public Object getOpaque() {
        return content.opaque;
    }

    @ExceptionSafe
    public void log(String label, Object...args) {
        for (int i=0; i<args.length; i++) {
            if (args[i] == null) {
                args[i] = "null";
            }
        }

        setText(label, args).write();
    }

    @ExceptionSafe
    public void log(String label, Collection args) {
        List noNullArgs = new ArrayList<>();
        args.forEach(i -> {
            if (i == null) {
                noNullArgs.add("null");
            } else {
                noNullArgs.add(i);
            }
        });
        setText(label, noNullArgs).write();
    }

    public List getParameters() {
        return content.parameters;
    }

    protected Log write() {
        if (getResourceUuid() != null && !getResourceUuid().equals(Platform.FAKE_UUID)) {
            logf.getBackend().writeLog(this);
        }

        return this;
    }

    @Override
    public String toString() {
        if (content == null) {
            return "";
        }

        if (content.parameters != null) {
            return Platform.i18n(content.text, logf.getBackend().getCurrentLocale(), content.parameters);
        } else {
            return Platform.i18n(content.text, logf.getBackend().getCurrentLocale());
        }
    }
}
