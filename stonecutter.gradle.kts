plugins {
	id("dev.kikugie.stonecutter")
	id("net.fabricmc.fabric-loom") version "1.19.1" apply false
}

stonecutter active "26.2-fabric"

stonecutter parameters {
	constants.match(node.metadata.project.substringAfterLast("-"), "fabric", "neoforge")
}
