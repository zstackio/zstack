package org.zstack.authentication.checkfile;

import com.google.common.collect.Lists;

import java.util.List;

public class DefaultFile {
    public static String SystemLocalFileCategory = "mn-default";
    public static String SystemHostFileCategory = "host-default";

    public static List<String> ManagementNodeDefaultFileList = Lists.newArrayList(
        "/usr/bin/zstack-ctl",
        "/usr/local/zstack/zstack.war",
        "/usr/local/zstack/imagestore/bin/zstcli",
        "/usr/local/zstack/ansible/files/kvm/zstack-kvmagent",
        "/boot/vmlinuz-3.10.0-957.el7.x86_64",
        "/usr/lib/modules/3.10.0-957.el7.x86_64/kernel/arch/x86/kvm/kvm.ko.xz"
    );

    public static List<String> ComputeNodeDefaultFileList = Lists.newArrayList(
        "/usr/bin/virsh",
        "/usr/sbin/libvirtd",
        "/usr/libexec/qemu-kvm"
    );
}
