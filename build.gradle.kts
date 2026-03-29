/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import de.undercouch.gradle.tasks.download.Download
import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream

plugins {
    id("org.jetbrains.intellij.platform") version "2.7.0"
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("de.undercouch.download") version "5.3.0"
}

data class BuildData(
    val ideaSDKShortVersion: String,
    // https://www.jetbrains.com/intellij-repository/releases
    val ideaSDKVersion: String,
    val sinceBuild: String,
    val untilBuild: String,
    val archiveName: String = "IntelliJ-EmmyLua",
    val jvmTarget: String = "1.8",
    val targetCompatibilityLevel: JavaVersion = JavaVersion.VERSION_11,
    val explicitJavaDependency: Boolean = true,
    val bunch: String = ideaSDKShortVersion,
    // https://github.com/JetBrains/gradle-intellij-plugin/issues/403#issuecomment-542890849
    val instrumentCodeCompilerVersion: String = ideaSDKVersion
)

//./gradlew buildPlugin
//https://www.jetbrains.com/intellij-repository/snapshots
val buildDataList = listOf(
    BuildData(
        ideaSDKShortVersion = "2026.1",
        ideaSDKVersion = "2026.1",
        sinceBuild = "253",
        untilBuild = "261.*",
        bunch = "212",
        targetCompatibilityLevel = JavaVersion.VERSION_21,
        //https://learn.microsoft.com/en-us/java/openjdk/download
        jvmTarget = "21"
    ),
    BuildData(
        ideaSDKShortVersion = "241",
        ideaSDKVersion = "243-EAP-SNAPSHOT",
        sinceBuild = "241",
        untilBuild = "243.*",
        bunch = "212",
        targetCompatibilityLevel = JavaVersion.VERSION_17,
        jvmTarget = "17"
    ),
    BuildData(
        ideaSDKShortVersion = "231",
        ideaSDKVersion = "233-EAP-SNAPSHOT",
        sinceBuild = "232",
        untilBuild = "233.*",
        bunch = "212",
        targetCompatibilityLevel = JavaVersion.VERSION_17,
        jvmTarget = "17"
    )
)

val buildVersion = System.getProperty("IDEA_VER") ?: buildDataList.first().ideaSDKShortVersion
val buildVersionData = buildDataList.find { it.ideaSDKShortVersion == buildVersion }!!

val emmyDebuggerVersion = "1.9.0"
val resDir = "src/main/resources"
val isWin = Os.isFamily(Os.FAMILY_WINDOWS)
val isCI = System.getenv("CI") != null

fun runCommand(vararg command: String) {
    providers.exec {
        commandLine(*command)
    }.result.get()
}
if (isCI) {
    version = System.getenv("CI_BUILD_VERSION")
    runCommand("git", "config", "--global", "user.email", "love.tangzx@qq.com")
    runCommand("git", "config", "--global", "user.name", "tangzx")
}
version = "${version}-IDEA${buildVersion}"

fun getRev(): String {
    return providers.exec {
        commandLine("git", "rev-parse", "HEAD")
    }.standardOutput.asText.get().trim().take(7)
}
tasks.register<Download>("downloadEmmyDebugger") {
    src(
        arrayOf(
            "https://github.com/EmmyLua/EmmyLuaDebugger/releases/download/${emmyDebuggerVersion}/darwin-arm64.zip",
            "https://github.com/EmmyLua/EmmyLuaDebugger/releases/download/${emmyDebuggerVersion}/darwin-x64.zip",
            "https://github.com/EmmyLua/EmmyLuaDebugger/releases/download/${emmyDebuggerVersion}/linux-x64.zip",
            "https://github.com/EmmyLua/EmmyLuaDebugger/releases/download/${emmyDebuggerVersion}/win32-x64.zip",
            "https://github.com/EmmyLua/EmmyLuaDebugger/releases/download/${emmyDebuggerVersion}/win32-x86.zip"
        )
    )
    dest("temp")
}

tasks.register<Copy>("unzipEmmyDebugger") {
    dependsOn("downloadEmmyDebugger")
    from(zipTree("temp/win32-x86.zip")) {
        into("windows/x86")
    }
    from(zipTree("temp/win32-x64.zip")) {
        into("windows/x64")
    }
    from(zipTree("temp/darwin-x64.zip")) {
        into("mac/x64")
    }
    from(zipTree("temp/darwin-arm64.zip")) {
        into("mac/arm64")
    }
    from(zipTree("temp/linux-x64.zip")) {
        into("linux")
    }
    destinationDir = file("temp")
}

tasks.register<Copy>("installEmmyDebugger") {
    dependsOn("unzipEmmyDebugger")
    from("temp/windows/x64/") {
        include("*.*")
        into("debugger/emmy/windows/x64")
    }
    from("temp/windows/x86/") {
        include("*.*")
        into("debugger/emmy/windows/x86")
    }
    from("temp/linux/") {
        include("*.*")
        into("debugger/emmy/linux")
    }
    from("temp/mac/x64") {
        include("*.*")
        into("debugger/emmy/mac/x64")
    }
    from("temp/mac/arm64") {
        include("*.*")
        into("debugger/emmy/mac/arm64")
    }
    destinationDir = file("src/main/resources")
}

allprojects {
    repositories {
        maven(url = "https://maven.aliyun.com/repository/public/")
        mavenCentral()
    }
}

project(":") {
    repositories {
        maven(url = "https://www.jetbrains.com/intellij-repository/releases")
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
    }

    dependencies {
        implementation(fileTree(baseDir = "libs") { include("*.jar") })
        implementation("com.google.code.gson:gson:2.8.6")
        implementation("org.scala-sbt.ipcsocket:ipcsocket:1.3.0")
        implementation("org.luaj:luaj-jse:3.0.1")
        implementation("org.eclipse.mylyn.github:org.eclipse.egit.github.core:2.1.5")
        implementation("com.jgoodies:forms:1.2.1")

        intellijPlatform {
            intellijIdeaUltimate(buildVersionData.ideaSDKVersion)
            bundledModule("intellij.spellchecker")
        }
    }

    sourceSets {
        main {
            java.srcDirs("gen", "src/main/compat")
            resources.exclude("debugger/**")
            resources.exclude("std/**")
        }
    }

    intellijPlatform {
        buildSearchableOptions = false
        sandboxContainer = layout.buildDirectory.dir("${buildVersionData.ideaSDKShortVersion}/idea-sandbox")

        pluginConfiguration {
            ideaVersion {
                sinceBuild = buildVersionData.sinceBuild
                untilBuild = buildVersionData.untilBuild
            }
        }

        publishing {
            token = System.getenv("IDEA_PUBLISH_TOKEN")
        }
    }

    tasks.register("bunch") {
        doLast {
            val rev = getRev()
            runCommand("git", "reset", "HEAD", "--hard")
            runCommand("git", "clean", "-d", "-f")
            runCommand(if (isWin) "bunch/bin/bunch.bat" else "bunch/bin/bunch", "switch", ".", buildVersionData.bunch)
            runCommand("git", "reset", rev)
        }
    }
    tasks {
        named("processResources") {
            dependsOn("installEmmyDebugger")
        }
        named("patchPluginXml") {
            dependsOn("installEmmyDebugger")
        }

        buildPlugin {
            dependsOn("bunch", "installEmmyDebugger")
            archiveBaseName.set(buildVersionData.archiveName)
//            from(fileTree(resDir) { include("!!DONT_UNZIP_ME!!.txt") }) {
//                into("/${project.name}")
//            }
        }

        compileKotlin {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(buildVersionData.jvmTarget))
            }
        }

        withType<org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask> {
            doLast {
                copy {
                    from("src/main/resources/std")
                    into("$destinationDir/${pluginName.get()}/std")
                }
                copy {
                    from("src/main/resources/debugger")
                    into("$destinationDir/${pluginName.get()}/debugger")
                }
            }
        }
    }
}
