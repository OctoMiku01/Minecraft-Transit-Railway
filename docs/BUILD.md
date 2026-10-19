# Build

> How to set up, build, and validate Minecraft Transit Railway from source.

## Prerequisites

| Tool        | Version / notes                                                                                                              |
|-------------|------------------------------------------------------------------------------------------------------------------------------|
| JDK         | **25** is required. Minecraft 26.x runs on Java 25, and the mod is compiled for Java 25.                                     |
| Node.js     | Current LTS recommended for `website/` development tasks.                                                                    |
| npm         | Bundled with Node.js.                                                                                                        |
| Gradle      | Use the wrapper scripts in this repo (`gradlew` / `gradlew.bat`). The wrapper downloads the required Gradle version (9.8+). |
| Stonecutter | Integrated into the Gradle build; no separate installation required.                                                         |

### GitHub Packages access

The build resolves `org.mtr:transport-simulation-core:+` from GitHub Packages. `buildSrc` needs it as well, so no Gradle task works without access. For local development, create a GitHub [personal access token (classic)](https://github.com/settings/tokens) with at least the `read:packages` scope, then add your credentials to your Gradle user-home properties file:

`C:\Users\<you>\.gradle\gradle.properties`

```properties
gpr.user=<your-github-username>
gpr.key=<your-personal-access-token>
```

As an alternative, Gradle will also use the `GITHUB_TOKEN` environment variable when it is set.

## Supported versions

The mod is built for **Minecraft 26.2** with **Fabric**. The build is set up with **Stonecutter**, so further versions can be added as projects in `settings.gradle.kts` with their own folder in `versions/`:

| Minecraft version | Loader | Stonecutter project ID |
|-------------------|--------|------------------------|
| 26.2              | Fabric | `26.2-fabric`          |

Minecraft 26.x is no longer obfuscated, so the build uses the non-remapping Fabric Loom plugin (`net.fabricmc.fabric-loom`) and the source uses the official Mojang names.

## First-time setup

From the repository root:

```powershell
.\gradlew.bat :26.2-fabric:setupFiles
.\gradlew.bat :26.2-fabric:setupWebsiteFiles
```

What these tasks do:

- `setupFiles`
	- Creates tokenised source/resource files from templates (for example `Keys.java`).
	- Pulls translations and supporting build assets where configured (`key.crowdin`, `key.patreon` in `gradle.properties`).
	- Generates Java schema classes and updates relocated imports.
	- Runs webserver setup support used by the mod. This copies the built website from `website/dist/`, so build the website first if you need the in-game web tools (see [Website development](#website-development)).
- `setupWebsiteFiles`
	- Generates TypeScript entities into `website/src/app/entity/generated/`.

## Building

```powershell
.\gradlew.bat :26.2-fabric:build
```

The finished mod jar is `versions/26.2-fabric/build/libs/mtr-<modVersion>+26.2-fabric.jar`. It contains the shadowed libraries (relocated under `org.mtr.libraries`) and the nested mods Elementa, UniversalCraft and the Kotlin standard library. The `-dev` jar next to it is an intermediate file without those dependencies and is not meant to be installed.

To collect the jars under release names in `build/release/`:

```powershell
.\gradlew.bat :26.2-fabric:buildAndCollect
```

The mod version comes from `mod.version` in `gradle.properties`, dependency versions from `gradle.properties` and `versions/26.2-fabric/gradle.properties`.

## Running in development

```powershell
.\gradlew.bat :26.2-fabric:runClient
.\gradlew.bat :26.2-fabric:runServer
```

To read Minecraft's code, `.\gradlew.bat :26.2-fabric:genSources` decompiles it into a sources jar that IDEs pick up automatically.

## Website development

From `website/`:

```powershell
npm install
npm run start
```

Build frontend assets:

```powershell
npm run build
```

## Test and verification

```powershell
.\gradlew.bat :26.2-fabric:test
```

If you only changed docs or comments, a full build is optional, but a quick test run is still recommended before release branches.

## Common failures

- **`Could not resolve org.mtr:transport-simulation-core`** or **HTTP 401 from `maven.pkg.github.com`**
	- The GitHub Packages credentials are missing or the token lacks the `read:packages` scope.
- **`Stonecutter requires at least Gradle …`**
	- Use the Gradle wrapper instead of a locally installed Gradle.
- **`Unsupported class file major version`** or toolchain errors
	- Gradle is not running on JDK 25. Point `JAVA_HOME` to a JDK 25 installation.
- **Generated files missing after checkout**
	- Re-run `setupFiles` and `setupWebsiteFiles`.
- **Website model type errors after schema edits**
	- Re-run `setupWebsiteFiles` and rebuild the website.

## Related docs

- Runtime usage and packaging: [RUNNING.md](RUNNING.md)
- Endpoint details: [API.md](API.md)
- Schema generation details: [SCHEMA.md](SCHEMA.md)
