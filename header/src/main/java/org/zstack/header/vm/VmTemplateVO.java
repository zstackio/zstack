package org.zstack.header.vm;

import org.zstack.header.vo.ToInventory;

import javax.persistence.*;

/**
 * @ Author : yh.w
 * @ Date   : Created in 9:55 2022/12/7
 */
@Entity
@Table
@Inheritance(strategy = InheritanceType.JOINED)
public class VmTemplateVO extends VmTemplateAO implements ToInventory {
   public VmTemplateVO() {

   }

    public VmTemplateVO(VmTemplateVO other) {
       super(other);
    }
}
