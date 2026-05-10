package org.zstack.header.allocator;

/**
 * Phase 3 Wave 2 U9 — dynamic reserved-capacity contributor SPI for the unified
 * {@code PhysicalServerVO} layer. Mirrors {@link HostReservedCapacityExtensionPoint} for cognitive
 * symmetry, but keyed by {@code physicalServerUuid} (not hypervisor type) because PhysicalServer
 * is hardware-type-agnostic.
 *
 * <p>Implementors return a {@link ReservedHostCapacity} delta that
 * {@code PhysicalServerCapacityUpdater.recalculate} sums on top of the static safety buffer.
 * Examples of dynamic contributors:
 * <ul>
 *   <li>{@code ContainerNodeCordonService} — cordoned node reserves remaining capacity (U7).</li>
 *   <li>Pending BM2 maintenance-mode marker — reserves full capacity during reimage.</li>
 * </ul>
 *
 * <p><b>Contract</b>: return {@code null} or a zero-valued struct to opt out for a given server.
 * Negative values are not honoured (callers clamp). The method is invoked under a PSC pessimistic
 * write lock — implementors must not perform long-running I/O or attempt to re-enter the capacity
 * pipeline (would deadlock).
 */
public interface ServerReservedCapacityExtensionPoint {
    ReservedHostCapacity getReservedCapacityForPhysicalServer(String physicalServerUuid);
}
