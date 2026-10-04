plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt)
}

detekt {
    config.setFrom(files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    source.setFrom(files("app/src/main/java", "app/src/test/java"))
}

// gradlew detekt -PdetektFix rewrites imports, wrapping and blank lines in place.
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    autoCorrect = project.hasProperty("detektFix")
    reports {
        html.required.set(true)
        xml.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}
