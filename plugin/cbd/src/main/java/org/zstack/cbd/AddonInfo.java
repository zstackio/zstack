package org.zstack.cbd;

import java.util.List;

/**
 * @author Xingwei Yu
 * @date 2024/4/1 18:12
 */
public class AddonInfo {
    public List<MdsInfo> mdsInfos;

    public List<MdsInfo> getMdsInfos() {
        return mdsInfos;
    }

    public void setMdsInfos(List<MdsInfo> mdsInfos) {
        this.mdsInfos = mdsInfos;
    }
}
