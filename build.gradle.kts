plugins {
    id("net.fabricmc.fabric-loom") version "1.17.11"
    id("maven-publish")
}

version = property("mod_version") as String
group = property("maven_group") as String

base {
    archivesName.set(property("archives_base_name") as String)
}

val sodiumApiStub = sourceSets.create("sodiumApiStub")

sourceSets {
    named("sodiumApiStub") {
        compileClasspath += named("main").get().compileClasspath
    }

    named("main") {
        compileClasspath += sodiumApiStub.output
    }
}

tasks.jar {
    exclude("net/caffeinemc/**")
}

dependencies {
    add("minecraft", "com.mojang:minecraft:${property("minecraft_version")}")

    implementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val nativeSourceDir = layout.projectDirectory.dir("native/lucerna_renderer")
val nativeBuildDir = layout.buildDirectory.dir("native/lucerna_renderer")
val nativeRuntimeDir = nativeBuildDir.map { it.dir("RelWithDebInfo") }
val nativeStagedRuntimeDir = layout.projectDirectory.dir("run/native/lucerna_renderer")
val nativeStagedLibrary = nativeStagedRuntimeDir.file("lucerna_renderer.dll")

loom {
    runs {
        named("client") {
            client()
            configName = "Lucerna Fabric Client"
            runDir("run")
            vmArg("-Djava.library.path=${nativeStagedRuntimeDir.asFile.absolutePath}")
            vmArg("-Dlucerna.native.path=${nativeStagedLibrary.asFile.absolutePath}")
        }
    }
}

tasks.register<Exec>("configureNative") {
    group = "lucerna native"
    description = "Configure the Lucerna native renderer with CMake."
    commandLine("cmake", "-S", nativeSourceDir.asFile.absolutePath, "-B", nativeBuildDir.get().asFile.absolutePath)
}

tasks.register<Exec>("buildNative") {
    group = "lucerna native"
    description = "Build the Lucerna native renderer with CMake."
    dependsOn("configureNative")
    commandLine("cmake", "--build", nativeBuildDir.get().asFile.absolutePath, "--config", "RelWithDebInfo")
}

tasks.register<Copy>("stageNativeRuntime") {
    group = "lucerna native"
    description = "Stage the Lucerna native renderer into the runtime directory used by runClient."
    dependsOn("buildNative")
    from(nativeRuntimeDir) {
        include("lucerna_renderer.dll")
    }
    into(nativeStagedRuntimeDir)
}

tasks.named("runClient") {
    dependsOn("stageNativeRuntime")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = property("archives_base_name") as String
            from(components["java"])
        }
    }
}
