package org.zstack.hotfix;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xing5 on 2016/10/25.
 */
public class HotFix1169Result {
    public String volumeUuid;
    public String volumeName;
    public boolean success = true;
    public List<String> details;
    public String error;

    public void setError(String error) {
        success = false;
        this.error = error;
    }

    public void addDetail(String detail) {
        if (details == null) {
            details = new ArrayList<String>();
        }
        details.add(detail);
    }
}
