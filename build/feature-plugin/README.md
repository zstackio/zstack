# Feature plugins

A feature plugin is a signed `.zpkg` that installs, upgrades and uninstalls on a running cloud from the
License page, without restarting the management nodes or reconnecting hosts. It carries up to three parts,
each optional: a management node part (Java), a kvmagent part (Python) and a UI part (Module Federation).

| Path | What |
| --- | --- |
| `zpkg_tool.py` | builds, checks, packs and signs a plugin (`zpkg_tool.py --help`) |
| `allowlist-v1.txt` | the platform classes a plugin may use (SDK v1) |
| `template/` | a management node part to start from: Maven project woven with AspectJ, `plugin.yaml` |
| `examples/net-probe/` | a complete plugin: management node and kvmagent parts here, the UI part in zstack-ui-next `packages/products/cloud/feature-plugins/net-probe` |

## Start a plugin

1. Copy `template/` and rename the artifact and the `id` in `plugin.yaml`.
2. A kvmagent part goes to `src/main/zpkg/agent/kvmagent/` and is declared as `kvmagent.plugins`; copy
   `examples/net-probe/src/main/zpkg/agent/` to start.
3. A UI part is a package in zstack-ui-next `packages/products/cloud/feature-plugins/<id>`; copy the net-probe
   one, then run `pnpm install` there so that `pnpm-lock.yaml` lists the new package.

## Build

Prerequisites: JDK 8 and Maven with the platform's artifacts installed (`mvn install` in this repository);
`pnpm install` done in zstack-ui-next.

```
python3 build/feature-plugin/zpkg_tool.py build \
    --plugin-dir build/feature-plugin/examples/net-probe \
    --ui-dir ../zstack-ui-next/packages/products/cloud/feature-plugins/net-probe \
    --platform-lib <the platform's WEB-INF/lib> \
    --key <private key> --out <dir>
```

It runs `mvn clean package` and `pnpm run build`, checks the result against the allowlist and the SDK v1
rules, then packs and signs it. Without `--platform-lib` it does not check; without `--key` the package is
unsigned.

## Test

- zpkg_tool: `python3 -m unittest test_zpkg_tool` in this directory.
- kvmagent part of the example:
  `PYTHONPATH=src/main/zpkg/agent/kvmagent python3 -m unittest discover -s src/test/python` in `examples/net-probe`.
- On a test environment: a package signed with a key whose public key is not in the war's
  `WEB-INF/classes/featurePluginKeys/` installs only with `featurePlugin.allowUnsigned = true` in
  `zstack.properties`. Upload and install it on the License page; a higher version upgrades it in place.
- Install and uninstall through the License page or the API: `zstack-ctl plugin install` on a running node
  does not load the plugin until the next plugin operation or restart.
