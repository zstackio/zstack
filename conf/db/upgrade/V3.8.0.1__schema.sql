alter table AliyunProxyVpcVO modify vpcName varchar(128) not null;

alter table AliyunProxyVpcVO
    drop foreign key fkAliyunProxyVpcVOVmInstanceEO;
alter table AliyunProxyVpcVO
    add constraint fkAliyunProxyVpcVOVmInstanceEO foreign key (vRouterUuid) references VmInstanceEO (uuid);

alter table AliyunProxyVSwitchVO
    drop foreign key fkAliyunProxyVSwitchVOAliyunProxyVpcVO;
alter table AliyunProxyVSwitchVO
    add constraint fkAliyunProxyVSwitchVOAliyunProxyVpcVO foreign key (aliyunProxyVpcUuid) references AliyunProxyVpcVO (uuid);
alter table AliyunProxyVSwitchVO
    drop foreign key fkAliyunProxyVSwitchVOL3NetworkEO;
alter table AliyunProxyVSwitchVO
    add constraint fkAliyunProxyVSwitchVOL3NetworkEO foreign key (vpcL3NetworkUuid) references L3NetworkEO (uuid);
