# Running

> Runtime and packaging notes for Minecraft Transit Railway development builds.

## Build artifacts

The mod is built for **Minecraft 26.2** with **Fabric**. A build creates the mod jar at `versions/26.2-fabric/build/libs/mtr-<modVersion>+26.2-fabric.jar`; `buildAndCollect` additionally copies it to `build/release/MTR-fabric-<modVersion>+26.2.jar`.

The code uses the official Mojang names, which Minecraft 26.x ships with.

See [BUILD.md](BUILD.md) for build commands and prerequisites.

## Installing a development build

1. Build the project with Gradle.
2. Copy the mod jar (not the `-dev` or `-sources` jar) into the target Minecraft instance `mods` directory.
3. Make sure Fabric Loader and Fabric API for Minecraft 26.2 are installed. Elementa, UniversalCraft and Kotlin are bundled with the mod.
4. Start Minecraft 26.2 with Java 25.

## Dedicated server considerations

- Install Fabric Loader and Fabric API on the server as well.
- Keep mod, loader, and Minecraft versions aligned with `gradle.properties`.
- If the transport backend becomes unresponsive, the mod currently logs an error and can stop the server to avoid desynchronised state.

## Local web tooling

MTR includes local helper endpoints for map and resource-pack-creator workflows.

- Endpoint behaviour is documented in [API.md](API.md).
- These routes are designed for local/in-game tooling and should not be exposed publicly without your own hardening.

## Troubleshooting runtime startup

- **Game fails with dependency/version mismatch**
  - Confirm loader, Minecraft version, and MTR jar variant match.
- **Missing generated or translated content in a dev build**
  - Re-run setup tasks from [BUILD.md](BUILD.md) before building.
- **Client-side web features unavailable**
  - Confirm client webserver startup in logs and check modpack/network constraints.
