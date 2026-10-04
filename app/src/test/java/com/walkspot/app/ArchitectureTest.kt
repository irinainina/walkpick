package com.walkspot.app

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

class ArchitectureTest {

    private val app = Konsist.scopeFromProject(moduleName = "app", sourceSetName = "main")

    private val dataFiles
        get() = app.files.filter { it.packagee?.name.orEmpty().startsWith("$ROOT.data") }

    @Test
    fun dataLayerDoesNotKnowAboutTheUi() {
        dataFiles.assertTrue(additionalMessage = "The data layer must not import from the ui layer") { file ->
            file.imports.none { it.name.startsWith("$ROOT.ui") }
        }
    }

    // Closes the door the rule above leaves open: a ui constant moved up into the root package.
    @Test
    fun dataLayerDoesNotReachTheUiThroughTheRootPackage() {
        val allowed = setOf("$ROOT.R", "$ROOT.BuildConfig", "$ROOT.MainActivity")
        dataFiles.assertTrue(additionalMessage = "The data layer may only name $allowed") { file ->
            file.imports
                .map { it.name }
                .filter { it.startsWith("$ROOT.") }
                .none { !it.removePrefix("$ROOT.").contains('.') && it !in allowed }
        }
    }

    @Test
    fun viewModelsAreNamedAfterTheirKind() {
        app.classes()
            .filter { klass -> klass.parents().any { it.name == "ViewModel" } }
            .assertTrue { it.name.endsWith("ViewModel") }
    }

    @Test
    fun injectedViewModelsAreKnownToHilt() {
        app.classes()
            .filter { it.name.endsWith("ViewModel") }
            .filter { klass -> klass.primaryConstructor?.hasAnnotationWithName("Inject") == true }
            .assertTrue { it.hasAnnotationWithName("HiltViewModel") }
    }

    @Test
    fun roomStaysInTheDataLayer() {
        app.interfaces()
            .filter { it.hasAnnotationWithName("Dao") }
            .assertTrue { it.resideInPackage("$ROOT.data.local") }
    }

    @Test
    fun screensDoNotFetchTheirOwnViewModels() {
        app.files
            .filter { it.packagee?.name.orEmpty().startsWith("$ROOT.ui.screens") }
            .assertTrue(additionalMessage = "A screen takes its state as parameters") { file ->
                file.imports.none { it.name.contains("hiltViewModel") }
            }
    }

    @Test
    fun repositoriesLiveInTheDataLayer() {
        app.interfaces()
            .filter { it.name.endsWith("Repository") }
            .assertTrue { it.resideInPackage("$ROOT.data..") }
    }

    private companion object {
        const val ROOT = "com.walkspot.app"
    }
}
