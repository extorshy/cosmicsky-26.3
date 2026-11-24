# Cosmic Sky - Fabric 26.3 port (work in progress)

## Status
| Part | State |
|---|---|
| Build setup (Loom no-remap, Java 25, Fabric 26.3) | done (versions looked up, not yet built) |
| K key + settings screen | ported to Mojang names / retained-mode GUI, NOT yet compiled |
| Config (config/cosmicsky.json) | unchanged |
| Sky drawing (stars, planets, shooting stars, sun/moon) | NOT ported yet - old code in reference-1.21.11-yarn/ |
| Cloud tint | NOT ported yet |

The sky code used APIs that no longer exist in 26.x (RenderLayer.getCelestial, VertexConsumerProvider).
It needs the real 26.3 signatures, which the build uploads as an "api-dump-and-log" artifact.

## How to get the jar
Upload this folder (incl. hidden .github) to a GitHub repo -> Actions tab -> download:
* `cosmicsky-jar`        -> the mod (only exists if compile succeeded)
* `api-dump-and-log`     -> send me this (or the red error lines) so I can finish the sky port
