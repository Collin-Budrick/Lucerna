# Lucerna Shader Layout

These shader files are placeholders for the Sodium Vulkan integration milestones.

## Pass Directories

- `gbuffer/`: Lucerna-controlled visibility outputs: depth-compatible data, normals, albedo, material id, emissive, and motion/history metadata.
- `lighting/`: direct sun/moon, emissive sampling, first diffuse GI, and cache confidence passes.
- `denoise/`: temporal rejection, spatial denoise, variance-aware filtering, and history repair passes.
- `composite/`: final world composite into the active Minecraft target without corrupting vanilla HUD or late translucency.
- `debug/`: overlays used by controller-run verification: backend state, dirty regions, material ids, timings, native queues, and cache views.

## Naming

Use lowercase pass names with the stage suffix when real shaders land:

- `<pass>.vert.glsl`
- `<pass>.frag.glsl`
- `<pass>.comp.glsl`

Keep placeholder files side-effect free. Sub-agents may add or refine shader assets in their owned scope, but they must not run shader compilation, Gradle checks, Minecraft launches, or render smoke tests.

## Descriptor Contract Draft

The current no-op layout reserves stable names for later native integration:

- `set 0`: frame constants, camera matrices, quality/debug flags.
- `set 1`: world resources, material tables, voxel/chunk occupancy buffers.
- `set 2`: history resources, temporal accumulation targets, variance/confidence maps.
- `set 3`: debug overlays and readback/telemetry resources.

Resource workers should update `layout.json` when adding a pass, target, descriptor binding, or expected controller-run validation scenario.
