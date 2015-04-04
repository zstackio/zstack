# ZStack: the IaaS software you have been looking for

ZStack is open source IaaS(infrastructure as a service) software aiming to automate datacenters, managing resources of compute, storage,
and networking all by APIs. Users can setup ZStack environments in a download-and-run manner, spending 5 minutes building a POC environment
all on a single Linux machine, or 30 minutes building a multi-node production environment that can scale to hundreds of thousands of physical servers.

ZStack provides the capability of managing everything in a datacenter by APIs, fulfilling the goal of software-defined
datacenter. Users control their clouds using either web UI or command line tool both of which interact with ZStack
management nodes; **NO** scattered configurations, dependent software installation, services HA, and thirdparty monitoring
are needed, all of them are taken care of by ZStack itself, which provides a great simplicity for stable long-term operation.

## Why ZStack?

ZStack is designed to tackle two problems -- complexity and stability -- that users building clouds based on open source
IaaS software have been suffered for a long time.

In terms of complexity, ZStack sticks to the design principles of APIs managing everything, full automation, self-managed services,
no hardware lockin, and user-friendly query APIs, providing a software easy to setup and long-term operation.

In terms of stability, ZStack was born with a plugin system that adding or removing features will not impact existing code,
a workflow engine that can rollback completed changes on error, a cascade framework that can spread an operation from
a resource to dependent resources, and three rigorous automated testing systems that guard every single feature,
solving the stability issue in architectural designs.

Besides, ZStack is extremely scale that a single management node is capable of managing hundreds of thousands of
physical servers, managing millions of virtual machines, and serving tens of thousands of concurrent API requests,
particular suitable for building large, scalable public clouds.


## Installation

Installation of ZStack is super easy; users can choose different methods depending on their needs to install the first
ZStack environment:

* For users wanting to try out quickly, see [Quick Installation](http://zstack.org/installation/index.html).

* For users wanting to deploy a production environment, see [Manual Installation](http://zstack.org/installation/manual.html).

* For users wanting to deploy a multi-node environment, see [Multi-node Installation](http://zstack.org/installation/multi-node.html).

Once the installation is done, users can follow one of getting started guides:

* [Getting Started With Quick Installation](http://zstack.org/documentation/getstart-quick.html).

* [Getting Started With Manual Installation](http://zstack.org/documentation/getstart-manual.html).

* [Getting Started With Multi-node Installation](http://zstack.org/documentation/getstart-multi.html).

## Tutorials

Six tutorials are prepared for your first journey in ZStack, building classic cloud deployments all on one single Linux machine:

##### Amazon EC2 classic EIP zone:

* [Web UI](http://zstack.org/tutorials/ec2-ui.html)
* [Command Line Tool](http://zstack.org/tutorials/ec2-cli.html)

##### Flat Network:

* [Web UI](http://zstack.org/tutorials/flat-network-ui.html)
* [Command Line Tool](http://zstack.org/tutorials/flat-network-cli.html)

##### Three Tiered Network:

* [Web UI](http://zstack.org/tutorials/three-tiered-ui.html)
* [Command Line Tool](http://zstack.org/tutorials/three-tiered-cli.html)

##### Security Group:

* [Web UI](http://zstack.org/tutorials/security-group-ui.html)
* [Command Line Tool](http://zstack.org/tutorials/security-group-cli.html)

##### Elastic Port Forwarding:

* [Web UI](http://zstack.org/tutorials/elastic-port-forwarding-ui.html)
* [Command Line Tool](http://zstack.org/tutorials/elastic-port-forwarding-cli.html)

##### Snapshots:

* [Web UI](http://zstack.org/tutorials/snapshot-ui.html)
* [Command Line Tool](http://zstack.org/tutorials/snapshot-cli.html)

## Under the hood 

Under the hood, ZStack is built on an architecture explained by following articles:

##### Scalability:

[ZStack's Scalability Secrets Part 1: Asynchronous Architecture](http://zstack.org/blog/asynchronous-architecture.html)

[ZStack's Scalability Secrets Part 2: Stateless Services](http://zstack.org/blog/stateless-clustering.html)

[ZStack's Scalability Secrets Part 3: Lock-free Architecture](http://zstack.org/blog/lock-free.html)

##### Plugin Architecture:

[The In-Process Microservices Architecture](http://zstack.org/blog/microservices.html)

[The Versatile Plugin System](http://zstack.org/blog/plugin.html)

[The Tag System](http://zstack.org/blog/tag.html)

[The Workflow Engine](http://zstack.org/blog/workflow.html)

[The Cascade Framework](http://zstack.org/blog/cascade.html)

##### Query API:

[The Query API](http://zstack.org/blog/query.html)

##### Automation:

[Full Automation By Ansible](http://zstack.org/blog/ansible.html)

##### Storage And Network:

[Networking Model 1: L2 and L3 Network](http://zstack.org/blog/network-l2.html)

[Networking Model 2: Virtual Router Network Service Provider](http://zstack.org/blog/virtual-router.html)

[Storage Model: Primary Storage and Backup Storage](http://zstack.org/blog/storage.html)

##### Testing:

[The Automation Testing System 1: Integration Testing](http://zstack.org/blog/integration-testing.html)

[The Automation Testing System 2: System Testing](http://zstack.org/blog/system-testing.html)

[The Automation Testing System 3: Model-based Testing](http://zstack.org/blog/model-based-testing.html)

## License

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is
distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and limitations under the License.


  
