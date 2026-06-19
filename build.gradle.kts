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
val nativeStagedRuntimeDirOverride = providers.gradleProperty("lucernaNativeRuntimeDir")
    .orElse(providers.environmentVariable("LUCERNA_NATIVE_RUNTIME_DIR"))
    .orNull
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
val nativeStagedRuntimeDir = nativeStagedRuntimeDirOverride
    ?.let { file(it) }
    ?: layout.projectDirectory.dir("run/native/lucerna_renderer").asFile
val nativeStagedLibrary = nativeStagedRuntimeDir.resolve("lucerna_renderer.dll")
val useExplicitNativePath = providers.gradleProperty("lucernaNativeUseExplicitPath")
    .map(String::toBoolean)
    .orElse(false)
    .get()
val nativeSignThumbprint = providers.gradleProperty("lucernaNativeSignThumbprint")
    .orElse(providers.environmentVariable("LUCERNA_NATIVE_SIGN_THUMBPRINT"))
    .orNull
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
val nativeSignTool = providers.gradleProperty("lucernaNativeSignTool")
    .orElse(providers.environmentVariable("LUCERNA_NATIVE_SIGN_TOOL"))
    .orElse("C:/Program Files (x86)/Windows Kits/10/bin/10.0.26100.0/x64/signtool.exe")
    .get()

loom {
    runs {
        named("client") {
            client()
            configName = "Lucerna Fabric Client"
            runDir("run")
            vmArg("-Djava.library.path=${nativeStagedRuntimeDir.absolutePath}")
            if (useExplicitNativePath) {
                vmArg("-Dlucerna.native.path=${nativeStagedLibrary.absolutePath}")
            }
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

tasks.register<Exec>("signNativeRuntime") {
    group = "lucerna native"
    description = "Optionally sign the staged Lucerna native renderer for local development policies."
    dependsOn("stageNativeRuntime")
    onlyIf { nativeSignThumbprint != null }
    commandLine(
        nativeSignTool,
        "sign",
        "/fd",
        "SHA256",
        "/sha1",
        nativeSignThumbprint ?: "",
        nativeStagedLibrary.absolutePath
    )
}

tasks.named("runClient") {
    dependsOn("signNativeRuntime")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = property("archives_base_name") as String
            from(components["java"])
        }
    }
}
