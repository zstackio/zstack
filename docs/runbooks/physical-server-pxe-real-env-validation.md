# Physical Server PXE Real-Environment Validation Runbook

**Audience:** QA, integration tester, pre-release validation engineer.

**Scope:** End-to-end PXE boot and OS installation on real physical hardware for `APIProvisionPhysicalServerMsg` (PhysicalServer-first provision flow). This runbook validates the complete data-plane (DHCP/iPXE/TFTP/HTTP/BMC power control) and installer integration that the focused harness cannot cover.

**Applicability:**
- Feature acceptance before merge to `master`
- Nightly/weekly CI runs in real-hardware lab
- Release gate for v5.5.18+ unified hardware feature
- Reproducing installer issues post-release

**Last updated:** 2026-05-05 (added §11 reference deployment from 172.26.201.160 production install).

---

## 1. Scope And Non-Scope

### 1.1 What This Runbook Validates

- [x] Real PhysicalServer BMC/IPMI connectivity and power control
- [x] Unified ProvisionNetwork data-plane (DHCP/TFTP/iPXE/HTTP) end-to-end
- [x] OS image pull and kickstart rendering per target server
- [x] LongJob state machine (Started → Provisioning → Succeeded/Failed)
- [x] Installed OS IP assignment, SSH accessibility, agent registration
- [x] Error paths: missing OOB, unreachable DHCP, kickstart syntax errors, installer hangs
- [x] Multi-NIC hardware: provision NIC selection, secondary NICs unchanged

### 1.2 What Is NOT Validated Here

- **Focused harness coverage:** `ProvisionPhysicalServerBm2Case`, `TestPhysicalServerProvisionService`, `PhysicalServerOpsCase` are simulator-only, testing contract layer (API/validation/LongJob state/provider dispatch). Passing these does NOT prove real PXE works.
- **Multi-server concurrent provision:** Capacity and scheduling belong in a separate runbook once infrastructure supports parallel provision slots.
- **Upgrade provision paths:** Rollback and OS upgrade orchestration → `v5518-unified-hardware-rollback.md`.
- **KVM role registration:** `APIAttachPhysicalServerRoleMsg` is orthogonal to provision. Provision only installs OS; role registration is user-initiated or orchestrated separately.
- **Non-gateway PXE types:** `STANDALONE_PXE` is phase 2+; this runbook covers `GATEWAY_PXE` only.

### 1.3 Boundary: Simulator vs Real Harness

```
┌─────────────────────────────────────────────────────────────┐
│ Simulator Tests (Focused Harness) — All Pass ≠ Real Works   │
├─────────────────────────────────────────────────────────────┤
│ ✓ Contracts: API signature, LongJob init, provider dispatch │
│ ✓ Validation: missing network, OOB, provision NIC MAC       │
│ ✓ Provider mock: capture PXE config, return synthetic OK     │
│ ✗ Real DHCP / TFTP / HTTP / BMC power / Installer           │
│ ✗ OS boot / network config / agent callback                 │
└─────────────────────────────────────────────────────────────┘
                           │
                  This Runbook Starts Here
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ Real-Environment Validation (This Runbook)                  │
├─────────────────────────────────────────────────────────────┤
│ ✓ Real PhysicalServer, BMC/IPMI, DHCP/TFTP/HTTP services   │
│ ✓ Real iPXE boot sequence, kickstart execution, installer   │
│ ✓ Installed OS SSH login, IP assignment verification        │
│ ✓ Agent callback and status reporting                       │
│ ✓ Failure modes: PXE timeout, installer error, power fail   │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Pre-Environment Setup

### 2.1 Physical Infrastructure

You need a real lab environment with:

1. **One PhysicalServer with:**
   - Reachable BMC/IPMI (IPv4 address, TCP/UDP port 623)
   - BMC user account (username, password)
   - Provision NIC (MAC address, L2 connected to PXE network)
   - At least 1 additional disk for OS installation (≥20 GB)
   - Boot priority set to: Network (PXE) first, then Hard Disk

2. **Provision network L2 reachability:**
   - Physical switch with VLAN trunk configured on port handling the provision NIC
   - VLAN tagging matches `PhysicalServerProvisionNetworkVO.dhcpInterface` VLAN ID (or untagged if no VLAN)
   - No firewall blocking DHCP ports (UDP 67/68)

3. **PXE data-plane node/endpoint** (TBD PRD decision; fill in once decided):
   - **Option A (DHCP/TFTP/HTTP on MN):** dnsmasq + tftp-hpa + HTTP server on management node
   - **Option B (Dedicated PXE node):** Standalone Ubuntu/CentOS VM with dnsmasq + TFTP + HTTP
   - **Option C (Gateway node in BM2 topology):** Reuses existing BM2 gateway if available (transition path)
   
   **v5.5.18 Status:** Provider interface `PhysicalServerGatewayPxeProvisionProvider` is generic; data-plane binding deferred to provider configuration. Recommend **Option A** for lab validation (simplest).

4. **OS image and kickstart template inputs:**
   - `ImageVO` with:
     - `uuid` (discoverable via `APIQueryImageMsg`)
     - `format` = RAW or QCOW2 (actual ISO/img format)
     - `mediaType` = ISO (for installer boot)
     - HTTP-accessible URL (path under image server, e.g., `http://image-server:8080/images/<uuid>/install.iso`)
   - `kickstartTemplate` (passed to API or system default):
     - Plain text, language = kickstart (CentOS/RHEL) or preseed (Debian/Ubuntu)
     - Contains network config, hostname, timezone, repo config, post-install script with agent registration

5. **ZStack management node with unified provision service:**
   - `plugin/physicalServer/` deployed and bean-registered in Spring
   - `PhysicalServerGatewayPxeProvisionProvider` active
   - `PhysicalServerProvisionNetworkVO` table created (Flyway V5.5.18__schema.sql applied)

### 2.2 Network Diagram

```
┌─────────────────┐
│  PhysicalServer │ ← MAC: AA:BB:CC:DD:EE:FF (provision NIC)
│  BMC 192.168.1.5│
│  IP: DHCP       │
└────────┬────────┘
         │ L2 (VLAN 100, trunk)
         │
┌────────▼────────────────────────────────┐
│  PXE Data-Plane (DHCP/TFTP/HTTP)        │
│  IP: 192.168.1.100                      │
│  DHCP Range: 192.168.1.150-192.168.1.200│
│  Netmask: 255.255.255.0                 │
│  Gateway: 192.168.1.1                   │
└────────▲────────────────────────────────┘
         │ (L2 broadcast domain)
         │
┌────────┴────────────────┐
│ ZStack Management Node  │
│ IP: 192.168.1.50        │
│ (calls APIProvision...  │
│  queries LongJob)       │
└─────────────────────────┘
```

---

## 3. Expected DHCP/iPXE/Installer Traffic

### 3.1 Packet Flow Timeline

```
Time  Source            Dest              Protocol  Payload
────  ──────────────    ───────────────   ────────  ────────────────
T0    PS MAC (unknown)  255.255.255.255   DHCP      DISCOVER (no IP yet)
T1    PXE DHCP Server   PS MAC            DHCP      OFFER (IP 192.168.1.150)
T2    PS MAC            255.255.255.255   DHCP      REQUEST (accept 192.168.1.150)
T3    PXE DHCP Server   PS MAC            DHCP      ACK (lease 192.168.1.150)

T4    PS (192.168.1.150) PXE TFTP (port 69) TFTP    GET /pxelinux.0
T5    PXE TFTP Server   PS (192.168.1.150) TFTP    DATA (pxelinux binary)

T6    PS (192.168.1.150) PXE DHCP Server   DHCP     (next-server, boot filename)
T7    PS (192.168.1.150) PXE HTTP (port 80) HTTP    GET /zstack-pxe/<PS-uuid>/boot.ipxe
T8    PXE HTTP Server   PS (192.168.1.150) HTTP    200 OK (iPXE script content)

T9    PS (192.168.1.150) PXE HTTP (port 80) HTTP    GET /images/<image-uuid>/install.iso
T10   PXE HTTP Server   PS (192.168.1.150) HTTP    206 Partial Content (ISO chunks)
      (looped until full ISO downloaded)

T11   PS (192.168.1.150) — local install         — OS installer runs (kernel exec)
T12   PS (new OS IP)    PXE HTTP (port 80) HTTP    GET /zstack-provision-callback?serverUuid=...&status=Succeeded
T13   PXE HTTP Server   PS (new OS IP)     HTTP     200 OK (LongJob updated to Succeeded)
```

**Evidence points for logs:**
- T0-T3: Check DHCP server logs (dnsmasq / systemd-networkd / ISC DHCPD)
- T4-T8: Check TFTP server logs (tftp-hpa / in.tftpd)
- T7-T10: Check HTTP server logs (nginx / Apache / custom)
- T11: Check installer console (IPMI serial/VNC) for kernel boot messages
- T12-T13: Check PXE HTTP callback logs

### 3.2 Expected Port Usage

| Service | Port | Protocol | Direction | Example Command |
|---------|------|----------|-----------|-----------------|
| DHCP Server | UDP 67/68 | DHCP | PS → PXE | `sudo tcpdump -i vlan100 'udp port 67 or udp port 68'` |
| TFTP Server | UDP 69 | TFTP | PS → PXE | `sudo tcpdump -i vlan100 'udp port 69'` |
| HTTP Server | TCP 80 | HTTP | PS → PXE | `sudo tcpdump -i vlan100 'tcp port 80'` |
| BMC IPMI | TCP 623 | IPMI | MN → BMC | `ipmitool -H 192.168.1.5 -U root -P password power status` |

---

## 4. Execution Steps

### 4.1 Step 0: Pre-flight Verification

Run these checks **before** starting provision to ensure environment is healthy.

#### 4.1.1 BMC Reachability

```bash
# Test IPMI connectivity (from ZStack MN)
ipmitool -H <BMC-IP> -U <BMC-USER> -P <BMC-PASSWORD> power status

# Expected output:
# Power is on
# (or "Power is off" — either is OK, we'll power-on during provision)
```

Save output to incident log: `evidence/bmc-status-T0.txt`

#### 4.1.2 PXE Services Health Check

```bash
# From PXE data-plane node: verify DHCP is listening
sudo systemctl status dnsmasq  # or your DHCP daemon
# Expected: active (running)

# From PXE node: verify TFTP is listening
sudo systemctl status tftp  # or in.tftpd
# Expected: active (running)

# From PXE node: verify HTTP server is listening
curl http://localhost/health || curl http://localhost/
# Expected: 200 OK or custom health endpoint response

# From MN: verify reachability to DHCP/TFTP/HTTP
curl -v http://<PXE-IP>:80/health
# Expected: 200 OK
```

Save output: `evidence/pxe-health-check-T0.txt`

#### 4.1.3 Physical Server Hardware Discovery

Ensure `PhysicalServerVO` has hardware info populated (from prior scan/discovery):

```bash
# From ZStack CLI / API / UI:
# APIQueryPhysicalServerMsg with full inventory
# Expected fields:
#   - serverUuid (e.g., "abcd1234...")
#   - hardwareInfo.cpuCount, memoryCapacity, diskList, nicList
#   - nicList[*].mac (must include provision NIC MAC)
#   - hardwareInfo.provisionNicMac (can be NULL if not pre-marked)
#   - oobAddress, oobPort, oobUsername, oobPassword (non-NULL)
#   - serverPoolUuid (non-NULL, pool must exist)
```

Example API call:

```bash
curl -X POST http://zs-api:8080/zstack/api \
  -H 'Content-Type: application/json' \
  -d '{
    "org.zstack.header.server.APIQueryPhysicalServerMsg": {
      "count": false,
      "limit": 1,
      "conditions": [{"name": "uuid", "op": "=", "value": "abcd1234..."}]
    },
    "session": {"uuid": "..."}
  }' | jq '.inventories[0]'
```

Save JSON response: `evidence/physical-server-query-T0.json`

#### 4.1.4 ProvisionNetwork Exists and Linked

```bash
# Verify ProvisionNetwork exists and is attached to the ServerPool
curl -X POST http://zs-api:8080/zstack/api \
  -H 'Content-Type: application/json' \
  -d '{
    "org.zstack.header.server.APIQueryPhysicalServerProvisionNetworkMsg": {
      "conditions": [
        {"name": "type", "op": "=", "value": "GATEWAY_PXE"},
        {"name": "zoneUuid", "op": "=", "value": "<ZONE-UUID>"}
      ]
    },
    "session": {"uuid": "..."}
  }' | jq '.inventories[0]'
```

Expected output includes:
- `uuid` (network UUID)
- `type` = "GATEWAY_PXE"
- `dhcpInterface` (e.g., "vlan100")
- `dhcpRangeStartIp`, `dhcpRangeEndIp`, `dhcpRangeNetmask`, `dhcpRangeGateway`
- `poolRefs` (should list the target ServerPool UUID)

Save JSON: `evidence/provision-network-query-T0.json`

### 4.2 Step 1: Create/Verify OS Image

Ensure a QCOW2 or RAW image is registered with installer kernel and rootfs.

```bash
# Query existing images
curl -X POST http://zs-api:8080/zstack/api \
  -H 'Content-Type: application/json' \
  -d '{
    "org.zstack.header.image.APIQueryImageMsg": {
      "conditions": [
        {"name": "name", "op": "like", "value": "%install%"}
      ]
    },
    "session": {"uuid": "..."}
  }' | jq '.inventories[] | {uuid, name, format, mediaType}'
```

Expected output:
```json
{
  "uuid": "img-uuid-12345",
  "name": "CentOS-7-installer",
  "format": "ISO",
  "mediaType": "ISO"
}
```

**If no image exists:** Upload one (platform/UI-specific; requires storage endpoint). Record the image UUID for next step.

Save UUID to file: `evidence/image-uuid.txt` → write `img-uuid-12345`

### 4.3 Step 2: Call APIProvisionPhysicalServerMsg

Trigger the provision LongJob from ZStack API:

```bash
# Request
curl -X POST http://zs-api:8080/zstack/api \
  -H 'Content-Type: application/json' \
  -d '{
    "org.zstack.header.server.APIProvisionPhysicalServerMsg": {
      "serverUuid": "ps-uuid-abcd1234",
      "networkUuid": "pn-uuid-xyz789",
      "osImageUuid": "img-uuid-12345",
      "osDistribution": "centos7",
      "kickstartTemplate": "# Kickstart template\ninstall\nrebootnetwork --onboot --bootproto=dhcp --device=eth0\nfirewall --enabled --service=ssh\nselinux --disabled\nbootloader --location=mbr\n%post\necho \"Provision complete\"\n%end\n",
      "provisionNicMac": "aa:bb:cc:dd:ee:ff",
      "customParams": {}
    },
    "session": {"uuid": "..."}
  }'

# Expected response (excerpt):
# {
#   "inventory": {
#     "uuid": "longjob-uuid-...",
#     "apiRequestUuid": "req-...",
#     "resourceUuid": "ps-uuid-abcd1234",
#     "jobState": "Started",
#     "progress": 0
#   }
# }
```

**Capture:**
- LongJob UUID (e.g., `longjob-uuid-abc123`)
- API response timestamp
- Request payload (for incident review)

Save to: `evidence/provision-request-T1.json` and `evidence/longjob-uuid.txt`

### 4.4 Step 3: Monitor DHCP/TFTP/HTTP Traffic

**On PXE data-plane node**, start packet capture and log monitoring in parallel:

```bash
# Terminal 1: DHCP traffic
sudo tcpdump -i vlan100 'udp port 67 or udp port 68' -w evidence/dhcp.pcap

# Terminal 2: TFTP traffic
sudo tcpdump -i vlan100 'udp port 69' -w evidence/tftp.pcap

# Terminal 3: HTTP traffic (boot script + ISO)
sudo tcpdump -i vlan100 'tcp port 80' -w evidence/http.pcap

# Terminal 4: DHCP server logs (dnsmasq example)
sudo journalctl -u dnsmasq -f > evidence/dnsmasq.log

# Terminal 5: TFTP server logs
sudo tail -f /var/log/tftp.log > evidence/tftp-server.log  # path varies

# Terminal 6: HTTP server logs
sudo tail -f /var/log/nginx/access.log > evidence/http-access.log  # path varies
```

Allow captures to run for the **full provision duration** (typically 10–30 minutes).

### 4.5 Step 4: Monitor Physical Server Serial Console

**On IPMI serial console** (from BMC or via IPMI session):

```bash
# Via ipmitool (requires SOL feature on BMC)
ipmitool -H <BMC-IP> -U <BMC-USER> -P <BMC-PASSWORD> sol activate

# Or via Redfish VNC/Web console (if BMC supports it)
```

**Capture output:**
```
[Phase 1] PXE ROM starts, DHCP request sent
  Timestamp: 2026-05-01 10:05:30

[Phase 2] iPXE script downloaded, parsing
  Timestamp: 2026-05-01 10:05:45

[Phase 3] ISO download starts
  Timestamp: 2026-05-01 10:06:00

[Phase 4] Installer kernel exec (CentOS boot messages)
  Timestamp: 2026-05-01 10:06:30

[Phase 5] Installer runs (partition, format, install packages)
  Timestamp: 2026-05-01 10:10:00

[Phase 6] System reboots into installed OS
  Timestamp: 2026-05-01 10:15:00

[Phase 7] Network comes up (DHCP lease for new OS)
  Timestamp: 2026-05-01 10:15:30

[Phase 8] OS fully boots, login prompt visible
  Timestamp: 2026-05-01 10:16:00
```

Save console output: `evidence/serial-console.log`

### 4.6 Step 5: Poll LongJob Status

From ZStack MN, poll the LongJob every 30 seconds:

```bash
# In a loop (e.g., bash while loop):
LONGJOB_UUID="longjob-uuid-abc123"
POLL_INTERVAL=30

while true; do
  STATUS=$(curl -s -X POST http://zs-api:8080/zstack/api \
    -H 'Content-Type: application/json' \
    -d "{
      \"org.zstack.header.longjob.APIGetLongJobMsg\": {
        \"uuid\": \"$LONGJOB_UUID\"
      },
      \"session\": {\"uuid\": \"...\"}
    }" | jq -r '.inventory | "\(.jobState) \(.progress)% \(.lastOpDate)"')
  
  TIMESTAMP=$(date -u +'%Y-%m-%d %H:%M:%S')
  echo "[$TIMESTAMP] LongJob $LONGJOB_UUID: $STATUS"
  
  if [[ "$STATUS" == *"Succeeded"* ]] || [[ "$STATUS" == *"Failed"* ]]; then
    echo "LongJob terminal state reached."
    break
  fi
  
  sleep $POLL_INTERVAL
done
```

**Expected state progression:**
```
T1:00  Started   0%
T1:30  Provisioning  20%
T2:00  Provisioning  40%
...
T8:00  Provisioning  95%
T8:30  Succeeded  100%
```

Save polling log: `evidence/longjob-poll.log`

### 4.7 Step 6: SSH Access and Verification

Once LongJob reaches `Succeeded`, test OS reachability:

```bash
# Determine the new OS IP
# (via DHCP logs, or by inspecting IPMI console for login prompt,
#  or by ARP scanning the provision subnet)
NEW_OS_IP=$(arp-scan 192.168.1.0/24 | grep "aa:bb:cc:dd:ee:ff" | awk '{print $1}')
# or manually inspect DHCP lease logs on PXE node

# SSH test (assuming root login and SSH keys pre-configured in kickstart)
ssh root@$NEW_OS_IP "hostname; ip addr; uname -a"

# Expected output:
# <hostname>
# inet 192.168.1.150 (or other assigned IP)
# Linux ... (kernel and OS info)
```

Save output: `evidence/os-ssh-verify.txt`

### 4.8 Step 7: LongJob Result Inspection

Retrieve final LongJob details:

```bash
curl -s -X POST http://zs-api:8080/zstack/api \
  -H 'Content-Type: application/json' \
  -d "{
    \"org.zstack.header.longjob.APIGetLongJobMsg\": {
      \"uuid\": \"$LONGJOB_UUID\"
    },
    \"session\": {\"uuid\": \"...\"}
  }" | jq '.inventory | {uuid, jobState, jobResult, errorCode, errorDescription, progress, lastOpDate}'
```

Expected final state (SUCCESS):
```json
{
  "uuid": "longjob-uuid-abc123",
  "jobState": "Succeeded",
  "jobResult": {
    "result": "success",
    "data": {
      "serverUuid": "ps-uuid-abcd1234",
      "osInstalled": true,
      "ipAddress": "192.168.1.150"
    }
  },
  "errorCode": null,
  "errorDescription": null,
  "progress": 100,
  "lastOpDate": "2026-05-01T10:16:30Z"
}
```

Save JSON: `evidence/longjob-final-state.json`

---

## 5. Pass/Fail Criteria And Evidence

### 5.1 PASS Evidence Checklist

For provision to be marked PASS, **all of the following must exist**:

- [ ] **API Transcript**
  - File: `evidence/provision-request-T1.json`
  - Contents: `APIProvisionPhysicalServerMsg` request body with serverUuid, networkUuid, osImageUuid, kickstartTemplate
  - Signature: Response contains valid longJobUuid

- [ ] **Hardware Discovery Output**
  - File: `evidence/physical-server-query-T0.json`
  - Contents: `PhysicalServerVO` inventory with:
    - `hardwareInfo.provisionNicMac` or `nicList[]` showing provision NIC MAC (e.g., "aa:bb:cc:dd:ee:ff")
    - `oobAddress`, `oobPort`, `oobUsername` (plaintext password should be redacted in log)
    - `serverPoolUuid` (non-null, matches ProvisionNetwork pool ref)

- [ ] **LongJob UUID and Final State**
  - File: `evidence/longjob-final-state.json`
  - Contents: `jobState == "Succeeded"` and `progress == 100`
  - `jobResult.result == "success"`

- [ ] **PXE DHCP Logs**
  - File: `evidence/dhcp.pcap` (pcap file) AND/OR `evidence/dnsmasq.log`
  - Signature: DHCP DISCOVER → OFFER → REQUEST → ACK sequence for provision NIC MAC
  - Assigned IP within `dhcpRangeStartIp`–`dhcpRangeEndIp` range

- [ ] **PXE TFTP Logs**
  - File: `evidence/tftp.pcap` AND/OR `evidence/tftp-server.log`
  - Signature: GET request for boot loader (e.g., pxelinux.0) from provision NIC IP
  - Expected files: `pxelinux.0`, `pxelinux.cfg/<MAC or default>`

- [ ] **PXE HTTP Logs**
  - File: `evidence/http.pcap` AND/OR `evidence/http-access.log`
  - Signatures:
    - GET `/zstack-pxe/<serverUuid>/boot.ipxe` → 200 OK
    - GET `/images/<imageUuid>/install.iso` → 206 Partial Content (multiple requests)
    - GET `/zstack-provision-callback?serverUuid=...&status=Succeeded` → 200 OK

- [ ] **BMC Power-Cycle Log**
  - File: `evidence/ipmi-commands.log`
  - Contents: IPMI SET POWER STATE commands executed at beginning of provision
  - Example: `ipmitool -H 192.168.1.5 power cycle` or similar

- [ ] **Serial Console Output**
  - File: `evidence/serial-console.log`
  - Signatures:
    - PXE ROM banner (BIOS/UEFI)
    - "DHCP..." message
    - Installer kernel boot (CentOS: "Loading linux...", "Loading initrd...", grub/boot messages)
    - Installer running (partitioning, filesystem creation, package install)
    - Reboot message
    - OS login prompt or successful network bringup in new OS

- [ ] **Installed OS Reachability**
  - File: `evidence/os-ssh-verify.txt`
  - Contents: Output of `ssh root@<new-os-ip> "hostname; ip addr; uname -a"`
  - Proof: SSH succeeded, IP assigned (within DHCP range or static as per kickstart), OS kernel visible

### 5.2 FAIL Evidence And Diagnosis

If provision does NOT reach `jobState == "Succeeded"`, capture the failure evidence and follow diagnosis path:

#### 5.2.1 LongJob Failed (jobState == "Failed")

```json
{
  "jobState": "Failed",
  "errorCode": "ORE.1001",
  "errorDescription": "PhysicalServer[uuid:ps-...] has no OOB/IPMI credentials"
}
```

**Diagnosis path:**
- Check `evidence/physical-server-query-T0.json` for `oobAddress`, `oobPassword`
- If NULL: hardware discovery incomplete → re-run discovery or manually set OOB fields
- If non-NULL: call IPMI tool directly to test (see §4.1.1)

#### 5.2.2 LongJob Hangs (No State Change After 30 minutes)

Check PXE logs:

```bash
# DHCP still stuck?
grep "no DHCP OFFER" evidence/dnsmasq.log
# → Check VLAN trunk, L2 connectivity, DHCP config range

# TFTP stuck?
grep "timed out" evidence/tftp-server.log
# → Check TFTP service, port 69 firewall

# HTTP stuck (ISO download never finishes)?
tail evidence/http-access.log | grep install.iso
# → Check HTTP server, bandwidth, disk space on PXE node

# Serial console shows installer prompt but no progress?
tail evidence/serial-console.log
# → Installer hanging; likely kickstart syntax error or repo URL unreachable
```

#### 5.2.3 OS Installed But SSH Fails

LongJob succeeded, but OS not reachable:

```bash
# Check serial console for network error
grep -i "network\|eth0\|bond" evidence/serial-console.log

# Check DHCP logs for post-install callback
grep "zstack-provision-callback" evidence/http-access.log

# Manually inspect system
ipmitool -H <BMC-IP> sol activate
# Look for: IP address assigned? Default route? DNS?
```

---

## 6. Troubleshooting And Failure Paths

### 6.1 BMC Not Reachable

**Error:** `ipmitool: Could not open device at /dev/ipmi0 or /dev/ipmi/0 or /dev/ipmi0: No such file or directory`

**Actions:**
1. Verify BMC IP address and credentials (network reachability from MN)
2. Confirm IPMI service on BMC is enabled (via BMC web UI)
3. Check firewall rules for port 623 (TCP and UDP)
4. Test with `nmap -sU -p 623 <BMC-IP>`

### 6.2 DHCP DISCOVER Never Gets OFFER

**Symptom:** Serial console shows "PXE ROM: Waiting for DHCP..." stuck for >1 minute

**Diagnosis:**
```bash
# Check DHCP server logs for errors
sudo journalctl -u dnsmasq | grep -i "error\|fail"

# Verify DHCP is listening on correct interface
sudo netstat -uln | grep 67

# Check VLAN trunk configuration on switch
# (Consult network team if not obvious)
```

**Fix:**
- DHCP range too small? Expand `dhcpRangeStartIp`–`dhcpRangeEndIp`
- DHCP interface typo? Check `phys-interface` config in dnsmasq
- VLAN mismatch? Ensure switch port is in access mode or trunk mode matching server NIC VLAN

### 6.3 TFTP Timeout During Boot

**Symptom:** Serial console: "TFTP from <IP> <FILE>..." then timeout

**Diagnosis:**
```bash
# Check TFTP server logs
sudo tail /var/log/syslog | grep tftp

# Verify TFTP directory has required files
ls -la /var/lib/tftp/
# Should contain: pxelinux.0, pxelinux.cfg/

# Test TFTP directly from MN
tftp -m binary <PXE-IP> -c get pxelinux.0
```

**Fix:**
- Copy missing boot loader: `cp /usr/lib/syslinux/pxelinux.0 /var/lib/tftp/`
- Check TFTP service status: `sudo systemctl status tftp`

### 6.4 HTTP 404 on Boot Script

**Symptom:** Serial console: "HTTP error 404" or "boot.ipxe not found"

**Evidence:** `evidence/http-access.log` shows `GET /zstack-pxe/<serverUuid>/boot.ipxe 404`

**Diagnosis:**
```bash
# Verify HTTP server is serving ZStack PXE directory
curl http://<PXE-IP>/zstack-pxe/<serverUuid>/boot.ipxe
# If 404: directory doesn't exist or iPXE script not rendered

# Check HTTP server root and symlinks
ls -la /var/www/html/zstack-pxe/
```

**Fix:**
- ProvisionProvider not writing iPXE config? Check provider logs: `grep PhysicalServerGatewayPxeProvisionProvider <zstack-logs>`
- HTTP server misconfigured? Check nginx/Apache vhost config for correct docroot

### 6.5 ISO Download Hangs or Times Out

**Symptom:** Serial console shows ISO download starting, then no progress for 10+ minutes

**Evidence:** `evidence/http-access.log` shows initial GET but no subsequent 206 responses

**Diagnosis:**
```bash
# Check HTTP server bandwidth/load
top | grep nginx / apache2

# Check disk space on PXE node
df -h /var/www/html/

# Verify image file exists and is readable
ls -lh /var/www/html/images/<image-uuid>/

# Try manual download from PXE node
curl -I http://localhost/images/<image-uuid>/install.iso
```

**Fix:**
- Disk full on PXE node? Free space or move images to larger partition
- Image file missing? Re-upload or fix image server endpoint
- Network saturation? Check switch port stats, consider local SSD cache

### 6.6 Installer Fails With Syntax Error

**Symptom:** Installer starts but exits with kickstart parse error; serial console shows "Kickstart syntax error line 42"

**Diagnosis:**
```bash
# Review rendered kickstart template in HTTP logs
grep boot.ipxe evidence/http-access.log
# Extract the boot.ipxe content to inspect syntax

# Test kickstart syntax offline
ksvalidator <(curl http://<PXE-IP>/zstack-pxe/<serverUuid>/boot.ipxe)
```

**Fix:**
- Validate kickstart in `APIProvisionPhysicalServerMsg` request before sending
- Check for unsupported options (e.g., CentOS 7 doesn't support some RHEL 8 directives)

### 6.7 OS Installed But Not Registered

**Symptom:** LongJob succeeded, OS boots, but no agent callback → IP stays unregistered in PhysicalServer

**Evidence:** `evidence/longjob-final-state.json` shows success, but `evidence/serial-console.log` shows installer skipped post-install script

**Diagnosis:**
```bash
# Check if kickstart post-script ran
ssh root@<new-os-ip> "journalctl | grep -i zstack"

# Verify agent is running
ssh root@<new-os-ip> "systemctl status zstack-agent || ps aux | grep zstack"

# Check network from OS perspective
ssh root@<new-os-ip> "ping <zs-mn-ip>"
```

**Fix:**
- `kickstartTemplate` missing `%post` section? Add script to install/start agent
- Agent endpoint unreachable from OS? Check routing, firewall from OS to MN

---

## 7. Artifacts And Evidence Organization

Create the following directory structure for each provision test:

```
evidence/
├── provision-request-T1.json          (API call payload + response)
├── longjob-uuid.txt                   (just the UUID string)
├── longjob-poll.log                   (polling output every 30s)
├── longjob-final-state.json           (final LongJob inventory)
├── physical-server-query-T0.json      (PhysicalServerVO inventory)
├── provision-network-query-T0.json    (ProvisionNetworkVO inventory)
├── bmc-status-T0.txt                  (ipmitool power status)
├── pxe-health-check-T0.txt            (systemctl / curl checks)
├── dhcp.pcap                          (tcpdump DHCP traffic)
├── dhcp.log or dnsmasq.log            (DHCP server logs)
├── tftp.pcap                          (tcpdump TFTP traffic)
├── tftp-server.log                    (TFTP server logs)
├── http.pcap                          (tcpdump HTTP traffic)
├── http-access.log                    (HTTP server access logs)
├── ipmi-commands.log                  (IPMI power/boot commands issued)
├── serial-console.log                 (IPMI serial console output)
├── os-ssh-verify.txt                  (SSH test: hostname, ip, uname)
└── README.md                          (summary: date, server UUID, result)
```

**README template:**

```markdown
# Physical Server PXE Provision Test

**Test Date:** 2026-05-01
**Physical Server UUID:** ps-uuid-abcd1234
**Server Hostname:** server-01
**OS Distro:** CentOS 7
**Image UUID:** img-uuid-12345
**Provision Network UUID:** pn-uuid-xyz789

## Result
**PASS** / **FAIL**

## LongJob Duration
Start: 2026-05-01 10:05:00Z
End: 2026-05-01 10:16:30Z
Duration: 11m 30s

## Final OS IP
192.168.1.150 (DHCP from range 192.168.1.150–192.168.1.200)

## Failure Reason (if FAIL)
[N/A for PASS; describe error code and steps taken for FAIL]

## Notes
- VLAN 100 trunk on switch port Gi0/1
- BMC IP 192.168.1.5 reachable
- PXE node dnsmasq + tftp-hpa + nginx on 192.168.1.100
```

---

## 8. Running Multiple Test Rounds

### 8.1 Regression Matrix

After any change to provision code (provider, validation, LongJob, kickstart defaults), run:

1. **Happy Path:** Bare PhysicalServer → provision succeeds → OS boots, IP assigned
2. **Missing OOB:** PhysicalServer with null `oobAddress` → provision fails with clear error
3. **Missing Network Link:** Server pool not associated with ProvisionNetwork → provision fails
4. **Wrong Provision NIC MAC:** `provisionNicMac` not in hardware discovery → provision fails
5. **Bad Kickstart Syntax:** Malformed template → installer error, visible in serial console

Each test result should generate its own `evidence/` directory (timestamped or named by scenario).

### 8.2 Report Template

```markdown
# Physical Server PXE Validation Report

**Release Version:** v5.5.18
**Test Run Date:** 2026-05-01 to 2026-05-03
**Tester:** Jane Doe
**Lab Environment:** DC-Lab-01

## Test Results Summary

| Test Scenario | Server UUID | Result | LongJob UUID | Notes |
|---|---|---|---|---|
| Happy Path (CentOS 7) | ps-01 | PASS | lj-001 | 11m 30s duration |
| Happy Path (Rocky 9) | ps-02 | PASS | lj-002 | 12m 15s duration |
| Missing OOB | ps-03 | FAIL | lj-003 | Error: no OOB credentials (expected) |
| Missing Pool Link | ps-04 | FAIL | lj-004 | Error: network not attached to pool (expected) |

## Blockers / Issues

None.

## Recommendations

1. Consider reducing DHCP offer timeout from 60s to 30s (faster detection of network issues)
2. Log provider payload to PXE node for easier debugging

## Approval

[Signature / Sign-off by QA lead]
```

---

## 9. Related Documentation

- **Focused Harness Tests (Simulator):** `premium/test-premium/src/test/groovy/org/zstack/test/integration/baremetal2/ProvisionPhysicalServerBm2Case.groovy` (Unit/integration, not real hardware)
- **Provider Interface:** `plugin/physicalServer/src/main/java/org/zstack/server/ProvisionProvider.java`
- **LongJob API:** `APIProvisionPhysicalServerMsg` in `header/`
- **PRD Reference:** `/home/mj/zstack-workspace/cloud_prd/prd/v5.5.18-unified-hardware/provision/feat-unified_provision_network_prd.md` (§2.3 PhysicalServer-first provision)
- **Implementation Plan:** `docs/plans/2026-05-01-physical-server-first-provision-plan.md` (Task 6 scope)
- **Rollback Runbook:** `v5518-unified-hardware-rollback.md` (if provision fails and database rollback is needed)

---

## 10. Sign-Off

This runbook is ready for execution by QA. It assumes:
- Real lab hardware is available (PhysicalServer with BMC, VLAN connectivity)
- ZStack v5.5.18+ unified hardware feature is deployed
- ProvisionProvider (currently `PhysicalServerGatewayPxeProvisionProvider`) is enabled
- PXE data-plane services (DHCP/TFTP/HTTP) are configured per §2

**Test execution should occur before feature merge to `master` and before release tagging.**

---

## 11. Reference Deployment: 2026-05-05 (172.26.201.160)

This section records a single concrete real-environment install used as the v5.5.18 PhysicalServer-first ship-readiness reference. It is **not** a replacement for §1-§10 — those define the methodology. This section is the worked example.

### 11.1 Build Artifact

| Field | Value |
|---|---|
| Bin | `http://storage.zstack.io/mirror/zstack_dev/20260505163928125615/` |
| Source CI | `dev.jenkins.zstack.io/job/build/190` SUCCESS, 22.5min |
| Test gate prior to deploy | 19 cases (10 OSS unit + 4 BM2 lookup + 4 stage + 1 IT) GREEN after `runMavenProfile premium` |
| Implementation parent commits | `dba3ebc107` role-provider classify SPI · `19292e671b` ADD_COLUMN helper for cpuCoreNum · `9a34b170be` import PhysicalServerManager.xml · `68945590b7` STATUS.md correction · `60f7c7c89c` stage-based LongJob · `78fc328d1e` powerOnPxe |
| Implementation premium commits | `d457e0d7ba` gateway-routed ping + path-2 SPI compliance · `406bce4dd9` import PhysicalServerManager.xml · `adbcc52b4c` Bm2GatewayDataPlane stage-based + ping helper |

### 11.2 Install Outcome

- Bin install: all 16 steps PASS (incl. `start ZStack management node` + `start ZStack Web UI`)
- V5.5.18 Flyway migration row written to `schema_version` with `success=1`
- `HostCapacityVO.cpuCoreNum` column present as `INT UNSIGNED NOT NULL DEFAULT 0` in production DB
- PhysicalServer 全家族 8 张表全部建出（`PhysicalServerVO`, `PhysicalServerCapacityVO`, `PhysicalServerHardwareInfoVO`, `PhysicalServerHardwareDetailVO`, `PhysicalServerRoleVO`, `PhysicalServerProvisionNetworkVO`, `PhysicalServerProvisionNetworkPoolVO`, `PhysicalServerProvisionNetworkPoolRefVO`）

### 11.3 PhysicalServer-First Add-Host End-to-End Trace

| Step | API | Result |
|---|---|---|
| 1 | `CreatePhysicalServer` | `PhysicalServerVO` 1 row written |
| 2 | `AttachPhysicalServerRole(KVM_HOST)` via REST `POST /v1/physical-servers/{uuid}/roles` | LongJob accepted, async dispatch |
| 3 | LongJob phase: NotStarted → NetworkPrepared | jobData.phase persisted |
| 4 | LongJob phase: NetworkPrepared → PxeTriggered | `PhysicalServerIpmiPowerExecutor.powerOnPxe` (chassis bootdev pxe + power reset) |
| 5 | LongJob phase: PxeTriggered → Pinging | `Bm2GatewayPingHelper` `bus.send(PingTargetInGatewayMsg)` → gateway agent reachable=true |
| 6 | LongJob phase: Pinging → Done (Succeeded) | RoleVO + HostVO/KVMHostVO + HostCapacityVO + PhysicalServerCapacityVO 全部 created |
| 7 | DB invariant check | `RoleVO.roleUuid == HostCapacityVO.uuid == HostVO.uuid` 持 (NB-22/24, ADR-012) |
| 8 | DB invariant check | `PhysicalServerCapacityVO.uuid == PhysicalServerVO.uuid` 持 (NB-22/30) |

### 11.4 Capacity Population (Real Hardware Values)

```
totalCpu=80
totalMem=16.5G
cpuCoreNum=8     ← new V5.5.18 column populated by hardware discovery
cpuSockets=2
```

`cpuCoreNum` 是 V5.5.18 新增列，在本次部署里被真硬件值填进去，证明 `ADD_COLUMN` helper（commit `19292e671b`）+ Hardware discovery 写路径都通。

### 11.5 Known Issues Surfaced (Not Ship-Blocking)

These are out of scope for this MR but tracked for follow-up:

1. **`zstack-cli` `roleConfig` Map<String,String> argparse**: tried `roleConfig='{...}'` / `roleConfig.username=root` / `roleConfig::username=root` / `roleConfig[username]=root` — all fail. Worked around by using REST directly. Belongs in `zstack-utility` separate PR.
2. **Trial license expired** (2025-08-16): bin ships with expired trial license; manual refresh needed at install time. Belongs in build pipeline (auto-refresh trial license at packaging time).
3. **`CHECK_REPO_VERSION` mismatch**: dev bin `5.5.16.<timestamp>` `.repo_version` vs base 5.5.16 ISO `.repo_version` differ → `bin -D` self-check fails. Workaround: invoke `bash install.sh` directly (skip bin wrapper env-var init). Build infra concern, code-orthogonal.

### 11.6 What This Demonstrates

- **PhysicalServer-first contract holds**: every host VO is born from a PhysicalServerVO + RoleVO write; no path bypasses the SPI dispatch (NB-11, ADR-012).
- **Path-2 SPI compliance**: traditional `AddHost`/`AddChassis`/`AddNode` entrypoints route through `PhysicalServerRoleProvider.classify(HostVO)` (commit `dba3ebc107`); `KvmRoleProvider` catches `BareMetal2GatewayVO` via `instanceof KVMHostVO`, fixing the prior path-2 missing-RoleVO bug.
- **Gateway-agent ping production wiring**: `Bm2GatewayPingHelper` no longer pings from MN; the v1.1+ deferral is withdrawn (AC-PN-14 production-verified).
- **Stage-based LongJob resume safety**: every phase is idempotent and persisted in `jobData.phase`; MN restart mid-provision skips completed stages (AC-PN-15).
- **Schema migration cross-version safety**: `cpuCoreNum` added via `CALL ADD_COLUMN(...)` helper, not raw `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` (which is MariaDB 10.0.2+ only).

### 11.7 Reproducing This Deployment

For a later tester to reproduce, use the same bin URL above (or rebuild from the parent+premium commits listed in §11.1) and follow §2-§5 of this runbook against any real PhysicalServer with reachable BMC/IPMI. The commit set is the same one captured in `docs/brainstorms/next-session.md` 2026-05-05 entry; cross-reference if the bin URL becomes unavailable.

---

## 12. Mixed-Deployment Validation (2026-05-06, 172.26.201.160)

**目标**：在已 ship 的 v5.5.18 真机部署上验证同一 `PhysicalServerVO` 行可同时挂
`KVM_HOST` (INTERNAL_SHARED) + `CONTAINER_HOST` (EXTERNAL_READONLY) 两个 role，
覆盖 capacity PRD §2.9 + role-SPI §2.1 + AC-CM-08 的混部承诺。`PhysicalServerCapacityCase` /
`PhysicalServerRoleCase` IT 在模拟器里跑绿；本节是 IT 同源 fixture 在生产部署上的真机回归。

### 12.1 选哪条路径

| 路径 | 何时用 |
|---|---|
| (A) `AddContainerManagementEndpoint` API | 已知目标 K8s endpoint URL + access key/secret，希望走完整 K8s sync 真路径 |
| (B) DB-direct 模拟 K8s sync | 没 K8s 凭据 / 仅验数据模型；模拟的正是 ContainerRoleProvider 收到 K8s node sync 后的写入路径，与生产 behavior 一致 |

API 设计上拒绝 `AttachPhysicalServerRole(CONTAINER_HOST)` 走 operator 直 attach（EXTERNAL_READONLY
由 K8s sync 拉，不是 user-driven 操作），所以 (A) 路径必须真的有 K8s endpoint，否则
退路 (B)。今天本环境无 K8s 凭据，走 (B)。

### 12.2 (B) DB-direct 模拟 K8s sync

> **mn_host 已 attach KVM_HOST（来自 §5）。下面在同一 `serverUuid` 上模拟 K8s sync 写
> CONTAINER_HOST 行。**

```bash
ssh root@172.26.201.160

# 1) 取目标 PhysicalServer.uuid（KVM_HOST 已挂）
serverUuid=$(mysql -uroot -pzstack.mysql.password zstack -sNe \
  "SELECT serverUuid FROM PhysicalServerRoleVO WHERE roleType='KVM_HOST' LIMIT 1;")
echo serverUuid=$serverUuid

# 2) 模拟 K8s sync：插 PhysicalServerRoleVO + 配套 ResourceVO（缺 ResourceVO ZStack
#    QueryXxxMsg 走 ResourceVO JOIN 做 RBAC 过滤会看不见数据 — 本次实测踩到）
mysql -uroot -pzstack.mysql.password zstack <<SQL
SET @serverUuid := '$serverUuid';
SET @roleVoUuid := REPLACE(UUID(), '-', '');
SET @nativeHostUuid := REPLACE(UUID(), '-', '');

INSERT INTO ResourceVO (uuid, resourceType, concreteResourceType)
VALUES (@roleVoUuid, 'PhysicalServerRoleVO', 'PhysicalServerRoleVO');

INSERT INTO PhysicalServerRoleVO
  (uuid, serverUuid, roleUuid, roleType, schedulingMode, createDate, lastOpDate)
VALUES
  (@roleVoUuid, @serverUuid, @nativeHostUuid,
   'CONTAINER_HOST', 'EXTERNAL_READONLY', NOW(), NOW());

SELECT @roleVoUuid AS roleVoUuid;
SQL
```

记下 `roleVoUuid` for cleanup。

### 12.3 验收

DB 视角：

```sql
SELECT roleType, schedulingMode, roleUuid, createDate
FROM PhysicalServerRoleVO WHERE serverUuid = '<serverUuid>';
```

期望：

```
KVM_HOST       INTERNAL_SHARED      <kvmHostUuid>
CONTAINER_HOST EXTERNAL_READONLY    <fakeNativeHostUuid>
```

Capacity 不变（READONLY 不吃 KVM 容量）：

```sql
SELECT uuid, totalCpu, availableCpu, totalMemory, availableMemory
FROM PhysicalServerCapacityVO WHERE uuid = '<serverUuid>';
```

期望 `totalCpu == availableCpu`、`totalMemory == availableMemory` 不变。

API 视角（必须能查回两条）：

```bash
printf "LogInByAccount accountName=admin password=password\n
QueryPhysicalServerRole serverUuid=<serverUuid>\n
LogOut\n" | zstack-cli
```

`inventories` 应有 2 条：`KVM_HOST/INTERNAL_SHARED` + `CONTAINER_HOST/EXTERNAL_READONLY`。

### 12.4 实测结果（2026-05-06 15:53）

```
serverUuid=d066db930a0041138640fcae28c1514d   (mn_host @ 172.26.201.160)

后插 CONTAINER_HOST 行：
  uuid=8eb2ae6e492011f196f2fa4a1273c900
  roleType=CONTAINER_HOST
  schedulingMode=EXTERNAL_READONLY
  roleUuid=8eb2b282492011f196f2fa4a1273c900   (fake NativeHost uuid)

DB 视角：两 role 共存 ✓
PhysicalServerCapacityVO: totalCpu=80 available=80, totalMem=16.5G available=16.5G — 不变 ✓
QueryPhysicalServerRole API: 返回 2 条 ✓ (KVM_HOST + CONTAINER_HOST)
```

### 12.5 踩坑记录（值得记住）

1. **API 只返一条但 DB 有两条** — 99% 是漏插 `ResourceVO`。ZStack QueryXxxMsg 走
   `ResourceVO` JOIN 做 RBAC 过滤；缺 ResourceVO 行会让新 RoleVO 在 API 视角隐身。
   修法：把 §12.2 第 2 步 SQL 跑齐（INSERT ResourceVO + INSERT PhysicalServerRoleVO）。
2. **`zstack-cli` 用 `LogInByAccount`，不是 `APILogInByAccount`** — v5.5.18 起 API 名字
   去 `API` 前缀；旧文档/cheatsheet 里的 `APIxxx` 会被 server 当作 `not an API message`。
3. **MySQL root 密码**：`zstack.mysql.password`（不是 `zstack.password.example`）。
   `zstack` 用户密码在 `zstack.properties` 里被加密，不能直接用。生产排查走 root 即可。
4. **DB schema**：`PhysicalServerRoleVO` 没有 `containerEndpointUuid` 之类的字段；`roleUuid`
   在 CONTAINER_HOST 语义里指 `NativeHostVO.uuid`（= K8s node 对应的 ZStack 内部
   NativeHost），但 §12.2 模拟时不需要真 NativeHostVO 行 — 只测 RoleVO 共存。

### 12.6 (A) AddContainerManagementEndpoint API 模板（有 K8s endpoint 时用）

```
LogInByAccount accountName=admin password=password

AddContainerManagementEndpoint \
    name=k8s-prod-37 \
    managementIp=172.20.0.37 \
    managementPort=<K8s API port，6443/443/...> \
    vendor=kubernetes \
    containerAccessKeyId=<K8s service-account token name> \
    containerAccessKeySecret=<K8s service-account token>

QueryContainerManagementEndpoint
QueryNativeHost                # K8s sync 周期触发后能看到 node
QueryPhysicalServer            # 每个 K8s node 同步出一个 PhysicalServer
QueryPhysicalServerRole        # 每个 PhysicalServer 自动挂 CONTAINER_HOST role
```

> **service-account token 怎么拿**：在 K8s 上跑
> `kubectl create serviceaccount zstack-mgr -n kube-system` →
> `kubectl create clusterrolebinding zstack-mgr --clusterrole=cluster-admin --serviceaccount=kube-system:zstack-mgr` →
> `kubectl create token zstack-mgr -n kube-system --duration=8760h`，输出当
> `containerAccessKeySecret`，accessKeyId 任填一个 label。

### 12.7 Cleanup

```bash
mysql -uroot -pzstack.mysql.password zstack <<SQL
DELETE FROM ResourceVO WHERE uuid = '<roleVoUuid>';
DELETE FROM PhysicalServerRoleVO WHERE uuid = '<roleVoUuid>';
SQL
```

清理后再 query 应只剩 `KVM_HOST` 一条。本次清理完毕（15:53）— DB 回归到 §5 单 KVM_HOST 状态。

### 12.8 What This Demonstrates

- **混部数据模型成立**：v5.5.18 unified-hardware 的承诺（同 PS 上 KVM + Container 共存）
  在生产环境真表上得到回归。
- **EXTERNAL_READONLY 不影响 KVM 容量**：CONTAINER_HOST 写 RoleVO 后 PSC 容量列零变化，
  capacity ledger 不混合两条调度模式。
- **ResourceVO contract 必走**：任何 K8s sync 写 RoleVO 的地方必须配套写 ResourceVO，
  否则 Query API 拿不到 — 这条踩坑值得在 ContainerRoleProvider K8s-sync 的 production
  代码里也守住（`ContainerRoleProviderIntegrationCase` AC-2 的 `dbf.persistAndRefresh` 路径
  应当也写 ResourceVO；如未写，是潜在 sync-then-invisible bug）。

### 12.A 真机 take-over walkthrough（2026-05-06 16:44，172.26.201.160 ← 172.20.0.37）

凭据来源：从 .37 的 ZStack MN 直读 `ContainerManagementEndpointVO` 表
（schema 用 `accessKeyId/accessKeySecret`，**不带 `container` 前缀** —— 跟
`@APIParam` 名 `containerAccessKeyId/Secret` 不同，DB 与 API 名差一个前缀）。

```bash
# 1) 从 .37 读 endpoint 凭据
ssh root@<MN-IP>
mysql -h172.20.0.37 -uroot -pzstack.mysql.password zstack -e "
SELECT name, managementIp, managementPort, vendor, accessKeyId, accessKeySecret
FROM ContainerManagementEndpointVO\\G"

# 2) 在 201.160 上 take over
printf "LogInByAccount accountName=admin password=password\n
AddContainerManagementEndpoint name=takeover-from-37 \
  managementIp=172.20.9.4 managementPort=80 vendor=zaku \
  containerAccessKeyId=<accessKeyId> \
  containerAccessKeySecret=<accessKeySecret>\n
SyncContainerManagementEndpoint uuid=<新 endpointUuid> zoneUuid=<已有 zoneUuid>\n
LogOut\n" | zstack-cli
```

注意：第一次 sync **必须**走 `APISyncContainerManagementEndpointMsg` 并显式传
`zoneUuid`。只调 `AddContainerManagementEndpoint` 后内部周期 sync 会撞
`No zone found for endpoint` (ORG_ZSTACK_CONTAINER_10002) 因为 NativeClusterVO 还
没创建（`syncContainerManagementEndpoint` Msg handler 在 ContainerEndpointBase
line 225-234 lookup `NativeClusterVO.zoneUuid`，没找到直接 fail）。
`APISyncContainerManagementEndpointMsg` (line 497) 走的是另一分支 — 它接受 msg.zoneUuid
作为 first-sync bootstrap，会根据 vendor provider listClusters 创 NativeClusterVO。

成功后 DB 状态（实测）：

| 实体 | 数量 | 状态 |
|---|---|---|
| `ContainerManagementEndpointVO` | 1 | OK |
| `NativeClusterVO` | 1（k8s-dev-gpu, bizUrl `https://172.20.9.20:6443`, status `Status_Cluster_Running`, zoneUuid=test_zone）| sync 自动落 ✓ |
| `NativeHostVO` | 7（k8s-m-1/2/3, k8s-gpu, k8s-k100-gpu, k8s-910b-aarch64-gpu, k8s-910b-aarch64-gpu-2403）| sync 自动落 ✓ |
| `HostVO`（hypervisorType=Native）| 7 全 status=Connected | sync 自动落 ✓ |
| `PhysicalServerRoleVO(CONTAINER_HOST)` | **0** | **production gap，§12.B 详** |
| `PhysicalServerVO`（CONTAINER 关联）| 0 | **production gap** |

→ `QueryPhysicalServerRole roleType=CONTAINER_HOST` 返空 list，混部不可见。

Endpoint uuid `ef554bb8255d4ce0b891a1367841b88b` 留在 201.160 上等 P1 修完后回归
验证（修完后 `SyncContainerManagementEndpoint` 重跑应自动补出 7 条
`PhysicalServerRoleVO(CONTAINER_HOST)`，serverUuid 自动 auto-association
matched 到 PSV via managementIp/serialNumber）。

### 12.B Open Followup

#### 12.B.1 P1 — K8s sync 不写 PhysicalServerRoleVO（2026-05-06 16:30 调查 + 16:44 真机产证）

**§12.5 ResourceVO 那条踩坑实际牵出更大的 gap**：production code 的 `dbf.persist(vo)`
路径走 Hibernate JOINED 继承，会自动写 ResourceVO 父行 — ResourceVO 不会漏。但
`ContainerEndpointBase.processNodeTransactional` (line 706-747) **根本没在 K8s sync
路径里调用 `dbf.persist(PhysicalServerRoleVO ...)`**。

`grep -r "new PhysicalServerRoleVO\|new PhysicalServerVO\|attachPhysicalServerRole" \
  /premium/plugin-premium/container/` → **0 matches**。

导致：
- v5.5.18 真机 K8s sync 完后，`PhysicalServerRoleVO(roleType=CONTAINER_HOST)` 表对该 K8s
  cluster 永远是空的。
- 容器主机对统一 host 系统不可见 → 混部 capacity reservation / Cordon-aware reserved
  整条链 silent fail。
- `ContainerNodeInfoDiscoveryAdapter` / `ContainerCordonReservedCapacityExtension` 读
  RoleVO 永远空，下游 fallback 路径无声生效。
- `deleteContainerHostRoles` 删的也永远是空集。

**为什么 IT 没暴露**：所有 IT 都用 `dbf.persistAndRefresh(roleVO)` 手插，绕开真实 K8s
sync path。

**修法（Phase 3 fix-plan 候选 U-unit）**：在 `processNodeTransactional` Stage 2.5
（NativeHostVO 之后、PCI/IOMMU 之前）补 PhysicalServer + PhysicalServerRoleVO upsert
（roleType=CONTAINER_HOST, schedulingMode=EXTERNAL_READONLY, roleUuid=NativeHost.uuid），
走 `PhysicalServerManagerImpl.attachRoleVO` 或 `dbf.persist` 接口（自动带 ResourceVO）。
完整描述见 [`docs/brainstorms/next-session.md` 顶部 P1 FOLLOWUP 段](../brainstorms/next-session.md#p1-followup--container-k8s-sync-不写-physicalserverrolevo2026-05-06-1630)。

#### 12.9.2 AddContainerManagementEndpoint API 端到端验证待补

`AddContainerManagementEndpoint` API 的真机验证（§12.6）需 K8s endpoint 凭据，
待 oncall 拿到 K8s 集群后补做。本节模板可直接复用。注意：在 12.9.1 修复落地前，
即使走 (A) 路径，K8s sync 仍不会让 CONTAINER_HOST 出现在 `PhysicalServerRoleVO` —
要先修 12.9.1 才能验。
