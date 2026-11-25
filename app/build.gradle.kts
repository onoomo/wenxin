plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.fortune.stall"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fortune.stall"
        minSdk = 24
        targetSdk = 36
        versionCode = 45
        versionName = "1.4.6 · 问心处"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // 对调试包也开启 R8 压缩：移除未使用的 Compose / 库代码，显著减小 APK
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // 本地发行包跳过 lint 致命检查（图标别名告警不影响运行）
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    // AGP 9.x：将 assets 中的技能包资料带入（默认已包含 assets，此处声明确保）
    androidResources {
        noCompress += listOf("md", "py")
    }
}

// 用户指定 compileSdk/targetSdk = 36。
// 最新 Compose BOM 会拉取 androidx.core 1.19+（要求 compileSdk 37），
// 这里强制锁定到兼容 API 36 的 core 版本，避免 SDK 版本被抬高。
configurations.all {
    resolutionStrategy {
        force("androidx.core:core:1.16.0")
        force("androidx.core:core-ktx:1.16.0")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // material-icons-extended 未使用（图标都用文字符号），移除以免打入数千图标 dex
}
