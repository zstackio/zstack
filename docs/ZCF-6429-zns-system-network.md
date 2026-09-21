# Marketplace ZNS on a system management network

ZCF-6429 now requires allowing ZNS installation on a system management L3,
superseding the issue's original suggestion to hide that network.

The VM remains UserVm and the image remains system=false. The exception to
the system-network creation restriction requires both:

- APICreateVmInstanceMsg containing the existing `marketplace::true` tag;
- the selected ImageVO carrying `marketplaceApp::zstack_io_zns`.

All other network, image, resource-access and placement checks remain.
Creation from volumes/snapshots and later NIC attachment/network changes are
outside this exception. The tag identifies the application; it does not
replace Cloud authorization.

Only administrators can write the ZNS image identity tag, using the existing
AdminOnlyTag enforcement. A tenant cannot mark an arbitrary image as ZNS.

The companion Marketplace change supplies the identity from app metadata
and handles both new and reused images. Every instance creation already
rebuilds and applies the import workspace, so there is no migration or new
retry mechanism. Deploy the Cloud support before the Marketplace change.

ImageConstant shares the tag string because image depends on compute;
compute must not acquire a reverse module dependency. ImageSystemTags
registers the tag, and compute queries its exact resource type, UUID and tag.

Validation includes CreateVmInPubL3Case: ordinary/public network behavior,
untagged image rejection, Marketplace marker alone rejection, ZNS image
alone rejection, successful ZNS UserVm creation, and disabled L3 rejection.
The real Marketplace test must also verify boot, IP/SSH and ZNS initialization.

## Validation on 2026-09-20

The standard `./runMavenProfile premium` build passed. CreateVmInPubL3Case passed
in the operator's terminal: one test, zero failures/errors/skips, BUILD SUCCESS
at 2026-09-20 17:31:59 +08:00. Its tenant permission assertion checks the stable
globalErrorCode ORG_ZSTACK_TAG_10008 rather than localized error text.

Real validation used the first nested Cloud, 172.25.116.50 (5.5.38.681).
Before deploying the fix, Marketplace-style creation on a System L3 failed
with the existing system-network rejection. After deployment:

- Marketplace reused the existing 1.3.0 ZNS image and added exactly one identity
  tag. The image remained system=false.
- A UserVm was created on system=true/category=System L3 with Flat DHCP, DNS
  and Userdata. Marketplace installation reached Installed.
- Node 172.25.116.187 and VIP 172.25.116.188 returned the same active cluster;
  the node was healthy/realized, SSH worked, and zstack-zns was active.
- Compute Manager connected to Cloud .50 with Up/realized state. The Cloud
  ZNS UI shell returned HTTP 200.
- Ordinary image creation, another Marketplace application, and a tagged ZNS
  image without marketplace::true were all rejected by the system-network
  check. None created a VM.

The image was deliberately reused to exercise backfilling; this test does not
claim that the image contains the latest ZNS commit. Local incremental review
passed after adding AdminOnlyTag. No attach/change-network behavior changed.

Browser verification confirmed the management network appears under Dedicated
Network / Management Network when the selected zone is ZSTAC-88655-Zone2.
The original default zone, nested-5.5.38-zone, has no management network, so its
empty list is expected. The tested VM and network belong to Zone2, where Host
172.25.116.34 had sufficient memory for the ZNS VM. This is a zone filter, not
a separate management-network tag missing from the tested network.
