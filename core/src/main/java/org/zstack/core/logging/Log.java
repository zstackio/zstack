package org.zstack.core.logging;

import com.fasterxml.uuid.Generators;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.message.NeedJsonSchema;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by xing5 on 2016/5/30.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class Log {
    @Autowired
    protected LogBackend bkd;

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

    public Log setText(String label, Object...args) {
        content.text = label;
        if (args.length != 0) {
            content.parameters = list(args).stream().map(Object::toString).collect(Collectors.toList());
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

    public Log log(String label, Object...args) {
        setText(label, args).write();
        return this;
    }

    public List getParameters() {
        return content.parameters;
    }

    public void write() {
        bkd.writeLog(this);
    }
}
