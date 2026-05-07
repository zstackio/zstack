# ZStack Local Storage QCOW2 Backing Chain Rule

When implementing or reviewing ZStack local storage logic related to qcow2 volumes, always account for image cache and backing chain semantics:

1. Do not modify the template image directly when creating a root volume from an image. ZStack local storage normally caches the image under `imagecache/template/<imageUuid>/<imageUuid>.qcow2`, then creates a root volume qcow2 overlay whose backing file points to that image cache.
2. Snapshot, clone, migration, backup, and encryption conversion logic must inspect and preserve or intentionally flatten the qcow2 backing chain. Do not operate only on the current qcow2 file unless a flattened independent view is explicitly intended.
3. Use `qemu-img convert` when an independent full image is required. This reads the complete logical view and removes backing dependencies.
4. Use `qemu-img create -b` when fast creation or incremental dependency is intended. In that case, the backing file lifecycle, path accessibility, and reference relationship must remain valid.
5. Do not infer the real image format from a `.qcow2` filename suffix. Use `qemu-img info` or an equivalent mechanism to inspect the actual format, backing file, and encryption metadata.
6. If encrypted qcow2 or LUKS is introduced, consider the encryption semantics of the whole backing chain and the QEMU/libvirt secret passing path. Encrypting only the top overlay must not be described as encrypting the complete disk history.
