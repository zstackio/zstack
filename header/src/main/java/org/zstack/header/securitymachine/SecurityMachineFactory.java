package org.zstack.header.securitymachine;

/**
 * Created by LiangHanYu on 2021/11/16 16:05
 */
public interface SecurityMachineFactory {

    String getSecurityMachineModel();

    SecurityMachineVO createSecurityMachine(SecurityMachineVO vo, AddSecurityMachineMessage msg);

    SecurityMachineInventory getSecurityMachineInventory(String uuid);

    SecurityMachine getSecurityMachine(SecurityMachineVO vo);
}
