pluginManagement {
	repositories {
		gradlePluginPortal()
		maven("https://maven.kikugie.dev/releases")
		maven("https://maven.kikugie.dev/snapshots")
		maven("https://maven.fabricmc.net/")
	}
}

plugins {
	id("dev.kikugie.stonecutter") version "0.10-beta.1"
	id("org.gradle.toolchains.foojay-resolver-convention") version "+"
}

stonecutter {
	create(rootProject) {
		mapBuilds { _, node ->
			"build-${node.project.substringAfter('-')}.gradle.kts"
		}

		versions(
			"26.2-fabric" to "26.2",
		)

		vcsVersion = "26.2-fabric"
	}
}

rootProject.name = "Minecraft-Transit-Railway"
