# Kylin V10/V11 Python3 host onboarding patch

This directory contains the tested source patch for the ZStack utility
Python3 host onboarding flow.

## Scope

The patch modifies `zstack-utility`, not the Java source tree in this repository.
It was generated from:

- base branch: `origin/feature-py3-5320`
- base commit: `8f4f8838ae3a8056a1d49e418aec616c7af1d49f`
- tested branch: `review/py3-kylin-v11`

## Apply

```bash
git fetch origin feature-py3-5320
git checkout -b hotfix/kylin-v10-v11-python3-onboarding 8f4f8838ae3a8056a1d49e418aec616c7af1d49f
git apply --check py3-kylin-v10-v11-review.patch
git apply py3-kylin-v10-v11-review.patch
```

## Verified hosts

- Kylin V11 Swan25, QEMU 8.2.0, libvirt 9.10.0, Python 3.11.6: Connected
- Kylin V10 Halberd, QEMU 4.1.0, libvirt 6.2.0, Python 3.7.9: Connected
