import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")

    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

kotlin {
    jvm {
        compilations.configureEach {
            kotlinOptions.jvmTarget = "1.8"
        }
    }

    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }

        nodejs()
    }

    val appleConfig: KotlinNativeTarget.() -> Unit = {
        binaries {
            framework {
                baseName = "lru-cache"
            }
        }
    }

    macosX64(appleConfig)
    macosArm64(appleConfig)

    ios(appleConfig)
    iosSimulatorArm64(appleConfig)

    watchos(appleConfig)
    watchosSimulatorArm64(appleConfig)
    // Not supported by Coroutines
    // Issue: https://github.com/Kotlin/kotlinx.coroutines/issues/3601
    // watchosDeviceArm64(appleConfig)

    tvos(appleConfig)
    tvosSimulatorArm64(appleConfig)

    linuxX64()
    // Not supported by Coroutines
    // Issue: https://github.com/Kotlin/kotlinx.coroutines/issues/855
    // linuxArm64()

    mingwX64()

    // Not supported by Coroutines
    // Issue: https://github.com/Kotlin/kotlinx.coroutines/issues/812
    // androidNativeArm32()
    // androidNativeArm64()
    // androidNativeX86()
    // androidNativeX64()

    // Still experimental and not supported by dependencies
    // wasm()

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.assertions)

                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val nonJvmMain by creating {
            dependsOn(commonMain)

            dependencies {
                implementation(libs.stately.isoCollections)
            }
        }

        val nonJvmTest by creating {
            dependsOn(commonTest)
            dependsOn(nonJvmMain)
        }

        val jsMain by getting {
            dependsOn(nonJvmMain)
        }

        val jsTest by getting {
            dependsOn(nonJvmTest)
        }

        val appleMain by creating {
            dependsOn(nonJvmMain)
        }

        val appleTest by creating {
            dependsOn(nonJvmTest)
        }

        val macosX64Main by getting
        val macosArm64Main by getting
        val macosMain by creating {
            dependsOn(appleMain)

            // There is no built-in macOS target shortcut
            macosX64Main.dependsOn(this)
            macosArm64Main.dependsOn(this)
        }

        val macosX64Test by getting
        val macosArm64Test by getting
        val macosTest by creating {
            dependsOn(appleTest)

            dependsOn(macosMain)
            macosX64Test.dependsOn(this)
            macosArm64Test.dependsOn(this)
        }

        val iosSimulatorArm64Main by getting
        val iosMain by getting {
            dependsOn(appleMain)

            // iOS target shortcut only contains: iosArm64, iosX64
            iosSimulatorArm64Main.dependsOn(this)
        }

        val iosSimulatorArm64Test by getting
        val iosTest by getting {
            dependsOn(appleTest)

            iosSimulatorArm64Test.dependsOn(this)
        }

        val watchosSimulatorArm64Main by getting
        val watchosMain by getting {
            dependsOn(appleMain)

            // watchOS target shortcut only contains: watchosArm32, watchosArm64, watchosX64
            watchosSimulatorArm64Main.dependsOn(this)
        }

        val watchosSimulatorArm64Test by getting
        val watchosTest by getting {
            dependsOn(appleTest)

            watchosSimulatorArm64Test.dependsOn(this)
        }

        val tvosSimulatorArm64Main by getting
        val tvosMain by getting {
            dependsOn(appleMain)

            // tvOS target shortcut only contains: tvosArm64, tvosX64
            tvosSimulatorArm64Main.dependsOn(this)
        }

        val tvosSimulatorArm64Test by getting
        val tvosTest by getting {
            dependsOn(appleTest)

            tvosSimulatorArm64Test.dependsOn(this)
        }

        val linuxX64Main by getting {
            dependsOn(nonJvmMain)
        }

        val linuxX64Test by getting {
            dependsOn(nonJvmTest)
        }

        val mingwX64Main by getting {
            dependsOn(nonJvmMain)
        }

        val mingwX64Test by getting {
            dependsOn(nonJvmTest)
        }
    }

    val publicationsFromMainHost =
        listOf(jvm(), js()).map { it.name } + "kotlinMultiplatform"
    publishing {
        publications {
            matching { it.name in publicationsFromMainHost }.all {
                val targetPublication = this@all
                tasks.withType<AbstractPublishToMaven>()
                    .matching { it.publication == targetPublication }
                    .configureEach { onlyIf { findProperty("isMainHost") == "true" } }
            }
        }
    }
}