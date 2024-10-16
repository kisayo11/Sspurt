pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        //kakao login api
        maven { url = java.net.URI("https://devrepo.kakao.com/nexus/content/groups/public/") }
        maven ( url = "https://jitpack.io" )


    }
}

rootProject.name = "Sspurt"
include(":app")
