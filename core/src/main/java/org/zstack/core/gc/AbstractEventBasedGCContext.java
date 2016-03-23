package org.zstack.core.gc;

/**
 * Created by xing5 on 2016/3/22.
 */
public abstract class AbstractEventBasedGCContext<T> extends AbstractGCContext<T> {
    protected String eventPath;
    protected String code;
    protected String codeName;

    public AbstractEventBasedGCContext() {
    }

    public AbstractEventBasedGCContext(AbstractEventBasedGCContext other) {
        super(other);
        this.eventPath = other.eventPath;
        this.code = other.code;
        this.codeName = other.codeName;
    }

    public String getCodeName() {
        return codeName;
    }

    public void setCodeName(String codeName) {
        this.codeName = codeName;
    }

    public String getEventPath() {
        return eventPath;
    }

    public void setEventPath(String eventPath) {
        this.eventPath = eventPath;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
