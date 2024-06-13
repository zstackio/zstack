package org.zstack.identity.imports.source;

import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.identity.imports.header.AbstractAccountSourceSpec;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;

/**
 * Created by Wenhao.Zhang on 2024/05/31
 */
public interface AccountSourceFactory {
    String type();
    AbstractAccountSourceBase createBase(ThirdPartyAccountSourceVO vo);
    ErrorableValue<ThirdPartyAccountSourceVO> createAccountSource(AbstractAccountSourceSpec spec);
}
