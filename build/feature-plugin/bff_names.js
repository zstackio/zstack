// Lists the GraphQL names a feature plugin's BFF module registers, loading it the way the BFF does: @nestjs/* and
// graphql come from the BFF's node_modules, @zstack/bff-plugin-sdk is stubbed. Run by zpkg_tool.py check.
// Usage: node bff_names.js <ui/bff/index.js> <BFF package dir>; prints {"types": [...], "fields": [...]}.
"use strict";
const Module = require("module");
const path = require("path");

const [file, bffDir] = process.argv.slice(2).map((p) => path.resolve(p));
const host = Module.createRequire(path.join(bffDir, "package.json"));
host("reflect-metadata");
const resolved = Object.fromEntries(["@nestjs/common", "@nestjs/graphql", "graphql"].map((r) => [r, host.resolve(r)]));
const SDK = "@zstack/bff-plugin-sdk";
const original = Module._resolveFilename;
Module._resolveFilename = function (request, parent, ...rest) {
  if (request === SDK) return SDK;
  if (resolved[request]) return resolved[request];
  return original.call(this, request, parent, ...rest);
};
class Base {}
require.cache[SDK] = {
  id: SDK, filename: SDK, loaded: true,
  exports: { QueryAdvance: Base, ActionAdvance: Base, ActionService: Base, ActionInput: Base, ActionResult: Base },
};

require(file);
const graphqlDir = path.dirname(resolved["@nestjs/graphql"]);
const { TypeMetadataStorage } = require(resolved["@nestjs/graphql"]);
// input types, enums, unions and resolver fields register only when a schema is built; nothing but the plugin is
// loaded here, so every class with lazy metadata is the plugin's
const { LazyMetadataStorage } = require(path.join(graphqlDir, "schema-builder/storages/lazy-metadata.storage"));
LazyMetadataStorage.load([...LazyMetadataStorage.lazyMetadataByTarget.keys()].filter((k) => typeof k === "function"));
const types = [
  ...TypeMetadataStorage.getObjectTypesMetadata(),
  ...TypeMetadataStorage.getInputTypesMetadata(),
  ...TypeMetadataStorage.getInterfacesMetadata(),
  ...TypeMetadataStorage.getEnumsMetadata(),
  ...TypeMetadataStorage.getUnionsMetadata(),
].map((m) => m.name);
const fields = [
  ...TypeMetadataStorage.getQueriesMetadata(),
  ...TypeMetadataStorage.getMutationsMetadata(),
  ...TypeMetadataStorage.getSubscriptionsMetadata(),
].map((m) => m.schemaName);
process.stdout.write(JSON.stringify({ types: [...new Set(types)], fields: [...new Set(fields)] }));
