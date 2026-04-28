package org.zstack.header.allocator;

/**
 * Purpose of a host allocation request. Filter extension points may relax some
 * checks (e.g. PCI device owner-RBAC) when the purpose is LIST_CANDIDATES so
 * that callers can list "what could be available" without enforcing all access
 * restrictions that would apply to a real allocation.
 *
 * Default is ALLOCATE so existing call sites keep their current behavior.
 *
 * Callers must restrict who is allowed to set LIST_CANDIDATES (e.g. admin only).
 * Filters trust the value carried by HostAllocatorSpec.
 */
public enum HostAllocationPurpose {
    ALLOCATE,
    LIST_CANDIDATES
}
