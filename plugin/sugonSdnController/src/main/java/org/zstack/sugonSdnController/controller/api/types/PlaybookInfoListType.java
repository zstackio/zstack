//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class PlaybookInfoListType extends ApiPropertyBase {
    List<PlaybookInfoType> playbook_info;
    public PlaybookInfoListType() {
    }
    public PlaybookInfoListType(List<PlaybookInfoType> playbook_info) {
        this.playbook_info = playbook_info;
    }
    
    public List<PlaybookInfoType> getPlaybookInfo() {
        return playbook_info;
    }
    
    
    public void addPlaybookInfo(PlaybookInfoType obj) {
        if (playbook_info == null) {
            playbook_info = new ArrayList<PlaybookInfoType>();
        }
        playbook_info.add(obj);
    }
    public void clearPlaybookInfo() {
        playbook_info = null;
    }
    
}
