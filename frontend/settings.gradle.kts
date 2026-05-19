pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // LiveKit Android SDK and other remote dependencies
        maven { url = java.net.URI("https://jitpack.io") }
    }
}
rootProject.name = "HEVCVideoCall"
include(":app")
