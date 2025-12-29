# 📦 EyeSeeTea SDK - JitPack Publishing and Local Development Guide

This guide explains how the EyeSeeTea fork of the DHIS2 Android SDK is published to JitPack, how to configure it in your app, and how to use it for local development.

---

## 📋 Table of Contents

1. [Publishing to JitPack](#publishing-to-jitpack)
2. [Configuring the SDK in Your App](#configuring-the-sdk-in-your-app)
3. [Local Development with Composite Build](#local-development-with-composite-build)
4. [Workflows](#workflows)
5. [Troubleshooting](#troubleshooting)

---

## Publishing to JitPack

### Overview

The SDK is automatically published to JitPack when you push code to GitHub. JitPack builds and publishes libraries from GitHub repositories without requiring manual Maven repository setup.

### Requirements

1. **Public GitHub repository**: JitPack works with public repositories (free) or private repositories with a paid plan
2. **`maven-publish` plugin** (required for this setup): The SDK must have the Maven publication plugin configured because the `jitpack.yml` uses `publishToMavenLocal` which requires this plugin
3. **`jitpack.yml` file**: Optional configuration file to customize the build (used in this project)

**Note about `maven-publish`**:
- **Required in this setup**: Since `jitpack.yml` uses `publishToMavenLocal`, the `maven-publish` plugin is required
- **Alternative**: You could use the older `maven` plugin instead, but `maven-publish` is the modern standard
- **Without `jitpack.yml`**: JitPack can automatically detect and publish modules with `maven-publish` plugin, but using `jitpack.yml` gives you more control over the build process

### Configuration Steps

#### Step 1: Configure the Group

In `build.gradle.kts` (root of the SDK):

```kotlin
// build.gradle.kts
// EyeSeeTea customization
group = "com.github.EyeSeeTea"  // Format: com.github.Username
version = libs.versions.dhis2AndroidSdkVersion.get()
```

**Important**: 
- The format must be `com.github.Username` where `Username` is your GitHub user/organization
- JitPack uses this group to build the artifact URL

#### Step 2: Configure Props.kt

In `buildSrc/src/main/kotlin/Props.kt`:

```kotlin
object Props {
    const val POM_NAME = "Core"
    const val POM_ARTIFACT_ID = "android-core"
    const val POM_PACKAGING = "aar"
    const val POM_DESCRIPTION = "Android SDK for DHIS 2."
    
    // EyeSeeTea customization
    const val POM_URL = "https://github.com/EyeSeeTea/dhis2-android-sdk"
    const val POM_SCM_URL = "https://github.com/EyeSeeTea/dhis2-android-sdk"
    const val POM_SCM_CONNECTION = "scm:git:git://github.com/EyeSeeTea/dhis2-android-sdk.git"
    const val POM_SCM_DEV_CONNECTION = "scm:git:ssh://git@github.com/EyeSeeTea/dhis2-android-sdk.git"

    const val POM_LICENCE_NAME = "BSD"
    const val POM_LICENCE_URL = "https://opensource.org/licenses/BSD-3-Clause"
    const val POM_LICENCE_DIST = "repo"
    const val POM_DEVELOPER_ID = "DHIS 2"
    const val POM_DEVELOPER_NAME = "DHIS 2"
}
```

#### Step 3: Create `jitpack.yml`

In the root of the repository, create `jitpack.yml`:

```yaml
jdk:
  - openjdk17

before_install:
  - echo "Java version:"
  - java -version
  - echo "Gradle version:"
  - ./gradlew --version

# JitPack automatically detects and publishes modules with maven-publish plugin
# Use 'assemble' instead of 'build' to skip all verification tasks (ktlint, detekt, etc.)
# Build dependencies first, then assemble release and publish to Maven Local
# JitPack will find the published artifacts in ~/.m2
install:
  - ./gradlew :annotations:assemble :processor:assemble --no-daemon
  - ./gradlew :core:assembleRelease :core:publishToMavenLocal --no-daemon
```

**Explanation**:
- `jdk`: Specifies Java version (SDK requires Java 17)
- `before_install`: Optional commands for debugging
- `install`: Build commands
  - First builds dependencies (`annotations` and `processor`)
  - Then builds and publishes the `core` module (the one being published)
  - Uses `assemble` instead of `build` to avoid running code verification tasks (ktlint, detekt, etc.)
  - `publishToMavenLocal` publishes to `~/.m2` where JitPack looks for artifacts

### Publishing a Version

JitPack builds automatically when:
- You create a tag: `git tag v1.0.0 && git push origin v1.0.0`
- You use a commit SHA directly
- You use a branch (less recommended)

**Verify the build**:
1. Go to https://jitpack.io/#EyeSeeTea/dhis2-android-sdk
2. Find your tag or commit SHA
3. Wait for the build to finish (may take 5-10 minutes)
4. If successful (✅ green), you can use it in your app

---

## Configuring the SDK in Your App

### Step 1: Add JitPack Repository

In `settings.gradle.kts` (or `build.gradle`):

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }  // ✅ JitPack
    }
}
```

### Step 2: Add the Dependency

In your module's `build.gradle.kts` (e.g., `app/build.gradle.kts`):

#### Option A: By Tag (Recommended for production)

```kotlin
dependencies {
    implementation("com.github.EyeSeeTea:dhis2-android-sdk:android-core:v1.13.0-eyeseetea-fork-1")
}
```

**Format**: `com.github.Username:Repository:Module:Tag`

#### Option B: By Commit SHA (Recommended for development/testing)

```kotlin
dependencies {
    implementation("com.github.EyeSeeTea:dhis2-android-sdk:android-core:abc123def456")
}
```

**Format**: `com.github.Username:Repository:Module:SHA`

**Get the SHA**:
```bash
cd dhis2-android-sdk
git rev-parse HEAD
# Output: abc123def456789...
```

#### Option C: By Branch (Only for active development)

```kotlin
dependencies {
    implementation("com.github.EyeSeeTea:dhis2-android-sdk:android-core:feature-new-feature-SNAPSHOT")
}
```

**⚠️ Warning**: Branches can change, not reproducible. Only for development.

### Step 3: Sync and Build

```bash
./gradlew assembleDebug
```

Gradle will automatically download the SDK from JitPack.

---

#### 4. Keep the Same Dependency in `build.gradle.kts`

```kotlin
dependencies {
    // ✅ Works automatically with local SDK (if exists) or JitPack
    implementation("com.github.EyeSeeTea:dhis2-android-sdk:android-core:v1.13.0-eyeseetea-fork-1")
}
```

## References

- **SDK on JitPack**: https://jitpack.io/#EyeSeeTea/dhis2-android-sdk
- **JitPack Documentation**: https://jitpack.io/docs/
- **Composite Build Documentation**: https://docs.gradle.org/current/userguide/composite_builds.html

