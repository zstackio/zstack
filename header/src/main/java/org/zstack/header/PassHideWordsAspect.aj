package org.zstack.header;

import org.zstack.utils.Utils;

import java.util.Set;

/**
 * Created by MaJin on 2019/9/25.
 */
public aspect PassHideWordsAspect {
    public Set<String> PassMaskWords.maskWords;

    after(PassMaskWords obj) : target(obj) && execution(org.zstack.header.PassMaskWords+.new(..)) {
        obj.maskWords = Utils.getLogMaskWords();
    }
}
