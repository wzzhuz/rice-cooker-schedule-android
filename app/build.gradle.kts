plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ---------------------------------------------------------------------------
// 版本：唯一手动维护点是 defaultVersionName。发版时改这里。
// CI 可用 -PversionNameOverride=0.5.1 临时覆盖（不会回写文件），
// 发版后请同步修改本文件。
// versionCode 由版本名推导：major*10000 + minor*100 + patch（0.5.0 → 500）。
// 此前恒为 1，连发两版时系统判不出新旧，无法覆盖升级。
// ---------------------------------------------------------------------------
val defaultVersionName = "0.5.0"

fun versionCodeFrom(name: String): Int {
    val parts = name.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    return parts.getOrElse(0) { 0 } * 10000 +
        parts.getOrElse(1) { 0 } * 100 +
        parts.getOrElse(2) { 0 }
}

val versionNameValue =
    (project.findProperty("versionNameOverride") as String?)?.takeIf { it.isNotBlank() }
        ?: defaultVersionName
val versionCodeValue =
    (project.findProperty("versionCodeOverride") as String?)?.toIntOrNull()
        ?: versionCodeFrom(versionNameValue)

println("[版本] $versionNameValue（versionCode=$versionCodeValue）")

// ---------------------------------------------------------------------------
// 签名：4 个环境变量齐全才用正式密钥，否则回退仓库内 app/debug.keystore。
//
// 为什么必须有固定的 debug 密钥：Actions 每次是全新环境，若让 AGP 自动生成
// debug keystore，不同批次 APK 签名不同 → 覆盖安装必须先卸载，应用数据全丢。
// 仓库是公开的，这把密钥也公开，仅供自用；要对外分发请配齐 4 个 Secret。
// ---------------------------------------------------------------------------
fun signingEnv(key: String): String =
    System.getenv(key)?.takeIf { it.isNotBlank() } ?: ""

val releaseStoreFile = signingEnv("RICECOOKER_KEYSTORE_FILE")
val releaseStorePassword = signingEnv("RICECOOKER_STORE_PASSWORD")
val releaseKeyAlias = signingEnv("RICECOOKER_KEY_ALIAS")
// PKCS12 只有一把锁：私钥即用 storepass 加密，keytool 会忽略 -keypass。
// keyPassword 未单独配置时复用 storePassword，否则空值会让 useReleaseSigning
// 判假，release 静默回退 debug 签名且不报错。
val releaseKeyPassword = signingEnv("RICECOOKER_KEY_PASSWORD").ifBlank { releaseStorePassword }

val useReleaseSigning = releaseStoreFile.isNotBlank()
    && file(releaseStoreFile).exists()
    && releaseStorePassword.isNotBlank()
    && releaseKeyAlias.isNotBlank()
    && releaseKeyPassword.isNotBlank()

if (useReleaseSigning) {
    println("[签名] 使用正式密钥：$releaseStoreFile（alias=$releaseKeyAlias）")
} else {
    println("[签名] 回退 app/debug.keystore（自用包，请勿对外分发）")
}

// release 是否启用 R8 混淆与资源压缩（默认关）。
// 本项目纯 Compose、无反射调用，混淆收益有限；默认关可避免
// lintVital 之外再多一层不确定性。需要时传 -PminifyRelease=true。
val minifyRelease: Boolean =
    (project.findProperty("minifyRelease") as String?)?.toBoolean() ?: false

android {
    namespace = "com.wzzhuz.ricecooker.schedule"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wzzhuz.ricecooker.schedule"
        minSdk = 26
        targetSdk = 35
        versionCode = versionCodeValue
        versionName = versionNameValue
    }

    signingConfigs {
        // 固定 debug 签名：保证每次编译出的 APK 签名一致
        create("commonDebug") {
            storeFile = file("debug.keystore")
            storePassword = "ricecooker2026"
            keyAlias = "ricecookerdebug"
            keyPassword = "ricecooker2026"
        }
        if (useReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // 与 release 同策略：配了正式密钥就用它，没配才回退固定 debug 密钥。
            // 这样 debug 包与 release 包签名统一，可互相覆盖安装。
            signingConfig = if (useReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("commonDebug")
            }
            isMinifyEnabled = false
        }
        release {
            signingConfig = if (useReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("commonDebug")
            }
            isMinifyEnabled = minifyRelease
            isShrinkResources = minifyRelease
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/LICENSE*")
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
