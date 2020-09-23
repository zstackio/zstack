#!/bin/sh
pip install codemod

mv premium/conf/preconfigurationTemplates/zstack_expert_x86_64_v2.cfg premium/conf/preconfigurationTemplates/cloud_expert_x86_64_v2.cfg
mv premium/conf/preconfigurationTemplates/zstack_host_x86_64_v2.cfg premium/conf/preconfigurationTemplates/cloud_host_x86_64_v2.cfg

mv premium/conf/cloudFormationTemplates/ZStack.System.v1.VlanVPC.json premium/conf/cloudFormationTemplates/Cloud.System.v1.VlanVPC.json
mv premium/conf/cloudFormationTemplates/ZStack.System.v1.VxlanVPC.json premium/conf/cloudFormationTemplates/Cloud.System.v1.VxlanVPC.json
mv premium/conf/cloudFormationTemplates/ZStack.System.v2.LNMP.json premium/conf/cloudFormationTemplates/Cloud.System.v2.LNMP.json
mv premium/conf/cloudFormationTemplates/ZStack.System.v2.MysqlHA.json premium/conf/cloudFormationTemplates/Cloud.System.v2.MysqlHA.json
mv premium/conf/cloudFormationTemplates/ZStack.System.v2.Tomcat.json premium/conf/cloudFormationTemplates/Cloud.System.v2.Tomcat.json
mv premium/conf/cloudFormationTemplates/ZStack.System.v3.EIP.json premium/conf/cloudFormationTemplates/Cloud.System.v3.EIP.json
mv premium/conf/cloudFormationTemplates/ZStack.System.v3.LB.json premium/conf/cloudFormationTemplates/Cloud.System.v3.LB.json
mv premium/conf/cloudFormationTemplates/ZStack.System.v3.PF.json premium/conf/cloudFormationTemplates/Cloud.System.v3.PF.json
mv premium/conf/cloudFormationTemplates/ZStack.System.v3.SG.json premium/conf/cloudFormationTemplates/Cloud.System.v3.SG.json


mv sdk/src/main/java/org/zstack/ sdk/src/main/java/org/cloud/
codemod -m -d sdk/src/main/* --extensions java,groovy org.zstack.heder org.cloud.heder --accept-all
codemod -m -d sdk/src/main/* --extensions java,groovy org.zstack.sdk org.cloud.sdk --accept-all

codemod -m -d test/src/* --extensions java,groovy org.zstack.sdk org.cloud.sdk --accept-all
codemod -m -d testlib/src/* --extensions java,groovy org.zstack.sdk org.cloud.sdk --accept-all
codemod -m -d testlib/src/* --extensions java,groovy org.zstack.heder.storage.volume.backup org.cloud.heder.storage.volume.backup --accept-all

codemod -m -d premium/test-premium/src/* --extensions java,groovy org.zstack.sdk org.cloud.sdk --accept-all
codemod -m -d premium/test-premium/src/* --extensions java,groovy org.zstack.heder.storage.volume.backup org.cloud.heder.storage.volume.backup --accept-all
codemod -m -d premium/testlib-premium/src/* --extensions java,groovy org.zstack.sdk org.cloud.sdk --accept-all


codemod -m -d premium/mevoco/src/* --extensions java org.zstack.sdk org.cloud.sdk --accept-all
codemod -m -d premium/cloudformation/src/* --extensions java,groovy org.zstack.sdk org.cloud.sdk --accept-all
codemod -m -d premium/plugin-premium/externalapiadapter/src/* --extensions java,groovy org.zstack.sdk org.cloud.sdk --accept-all


## cd premium/conf/
## codemod -m -d cloudFormationTemplates/* --extensions json Zstack Cloud --accept-all

## modify mydql contain zstack

mysql -uroot -pzstack.mysql.password <<EOF
use zstack;
update StackTemplateVO set name = replace(name, 'ZStack', 'Cloud');
update StackTemplateVO set content = replace(content, 'ZStack', 'Cloud');

update PreconfigurationTemplateVO set name = replace(name, 'zstack', 'cloud');
update PreconfigurationTemplateVO set description = replace(name, 'zstack', 'cloud');
update PreconfigurationTemplateVO set distribution = replace(name, 'zstack', 'cloud');

EOF