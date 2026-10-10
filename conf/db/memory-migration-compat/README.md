# Memory migration compatibility sources

The normal product build uses `conf/db/upgrade`: the existing official
`V5.5.38.1__schema.sql` and memory migrations `.1001`–`.1010`, with the checked-in
`memory-migration-cohort.json` declaration. Normal installation, `upgrade_db`
and HA upgrade read this declaration without an internal selection argument.
The declaration and every selected SQL byte are checked before database writes.

`legacy/` preserves the exact historical `.1`–`.5` experimental files and the
append-only `.1006`–`.1010` files. Do not edit their contents or rewrite applied
Flyway history. They are excluded from a normal WAR. For a verified experimental
database only, build the same product with the explicit Maven profile
`-Ppremium,memory-legacy-compat` and use its matching explicit ctl compatibility
selection. A normal official package cannot upgrade that experimental history.

Both paths use the standard Maven WAR resources. No external ZIP rewriting is
needed to produce either migration set. Use a clean build for each profile, and
verify that the resulting WAR has only one selected declaration/SQL set; do not
reuse the expanded WAR directory from a different profile without cleaning it.

The history checks reject unknown, mixed, failed, partial and checksum-mismatched
feature history. A failed/missing history query is never treated as a fresh DB.
