# Lucerna Shader Resource Contracts

`layout.json` is a metadata contract for upcoming Lucerna Vulkan work. It is not a compiled shader manifest yet; it gives the native renderer, frame hook, world upload, lighting, denoise, composite, and debug workers stable names to coordinate against.

## Stability Rules

- Pass ids are public once referenced outside this folder. Use `lucerna.<stage>.<name>` and keep `numericId` stable.
- `numericId` is a stable native/debug identifier, not the scheduler key. Use `executionOrder` for pass sequencing.
- Descriptor set and binding numbers are public once consumed by native code. Add new bindings at the end of the owning set unless the controller explicitly coordinates a breaking change.
- Attachment names are public handoff points between passes. Prefer adding a new attachment over changing the meaning or format of an existing one.
- `dependsOn` edges document scheduler order and resource handoff requirements. They must point at declared passes that execute earlier.
- `attachmentWriteSemantics` documents the intended producer and clear/history behavior for attachments written by a pass.
- Placeholder shaders must stay side-effect free: no storage writes, image writes, atomics, barriers, discards, color outputs, or depth outputs.

## Pass Pipeline

The current contract reserves this order:

1. `lucerna.voxel.first_pass`: reads material/chunk dirty-region inputs and stages `VoxelOccupancy` for the main G-buffer traversal path.
2. `lucerna.gbuffer.main`: writes full-resolution depth, normal/roughness, albedo/opacity, material id, emissive, and motion/history buffers using attachment names from `GBufferTargetContract`.
3. `lucerna.lighting.direct`: reads G-buffer and emissive data, writes full-resolution direct lighting.
4. `lucerna.lighting.gi`: reads G-buffer, direct lighting, voxel occupancy, and history, writes half-resolution diffuse GI and cache confidence.
5. `lucerna.denoise.diffuse`: reads lighting, G-buffer, and history, writes full-resolution diffuse lighting plus a rejection mask.
6. `lucerna.debug.overlay`: reads diagnostic resources and writes the debug overlay target.
7. `lucerna.composite.final`: currently represents flat-composite staging into the borrowed Minecraft/Sodium world color target before vanilla HUD and late translucency.

## Descriptor Ownership

- `set 0 frame`: renderer/frame-hook owned. Updated per frame or when quality/debug config changes.
- `set 1 world`: world, material, voxel staging, and native upload workers own the backing data and generation counters.
- `set 2 history`: lighting, denoise, and frame lifecycle workers own invalidation and ping-pong history.
- `set 3 debug`: debug telemetry owns timing, native queue, label, and overlay data.

## Validation Notes

The validation scenarios in `layout.json` are controller-run only. Sub-agents may edit this metadata and placeholder files, but must not run shader compilation, Gradle checks, compiler checks, native builds, Minecraft launches, or render smoke tests.

## Java Metadata Scaffold

`net.lucerna.render.resources` mirrors the descriptive parts of `layout.json` for Java-side validation and future native handoff planning. It parses from a caller-provided reader, models pass ids, dependencies, descriptor sets, descriptor bindings, attachments, attachment write semantics, and validation findings, and does not load renderer state or compile shader sources.
