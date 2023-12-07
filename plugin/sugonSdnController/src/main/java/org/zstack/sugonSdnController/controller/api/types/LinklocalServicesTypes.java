//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class LinklocalServicesTypes extends ApiPropertyBase {
    List<LinklocalServiceEntryType> linklocal_service_entry;
    public LinklocalServicesTypes() {
    }
    public LinklocalServicesTypes(List<LinklocalServiceEntryType> linklocal_service_entry) {
        this.linklocal_service_entry = linklocal_service_entry;
    }
    
    public List<LinklocalServiceEntryType> getLinklocalServiceEntry() {
        return linklocal_service_entry;
    }
    
    
    public void addLinklocalServiceEntry(LinklocalServiceEntryType obj) {
        if (linklocal_service_entry == null) {
            linklocal_service_entry = new ArrayList<LinklocalServiceEntryType>();
        }
        linklocal_service_entry.add(obj);
    }
    public void clearLinklocalServiceEntry() {
        linklocal_service_entry = null;
    }
    
}
