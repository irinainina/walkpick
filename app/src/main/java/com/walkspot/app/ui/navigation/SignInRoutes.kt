package com.walkspot.app.ui.navigation

import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkspot.app.ui.account.AuthViewModel
import com.walkspot.app.ui.account.rememberGoogleSignIn
import com.walkspot.app.ui.screens.SignInCodeScreen
import com.walkspot.app.ui.screens.SignInEmailScreen
import com.walkspot.app.ui.screens.SignInScreen
import com.walkspot.app.ui.screens.messageRes

private const val EMAIL = "SignInEmail"
private const val CODE = "SignInCode"

internal fun NavGraphBuilder.signInRoutes(navController: NavHostController, auth: AuthViewModel) {
    // The sign-in is left behind, or the back gesture walks out of a signed-in app onto it.
    val enter = {
        navController.navigate(Screen.Today.route) {
            popUpTo(Screen.SignIn.route) { inclusive = true }
        }
    }
    val back: () -> Unit = {
        auth.forgetProblem()
        navController.popBackStack()
    }
    composable(Screen.SignIn.route) {
        val google = rememberGoogleSignIn()
        SignInScreen(
            onGoogle = { auth.signInWithGoogle(google, enter) },
            onEmail = {
                auth.forgetProblem()
                navController.navigate(EMAIL)
            },
            problem = auth.problem?.let { stringResource(it.messageRes()) },
        )
    }
    composable(EMAIL) {
        SignInEmailScreen(
            email = auth.email,
            onEmailChange = auth::editEmail,
            onContinue = { auth.sendCode { navController.navigate(CODE) } },
            onBack = back,
            busy = auth.busy,
            problem = auth.problem,
        )
    }
    composable(CODE) {
        SignInCodeScreen(
            email = auth.email.trim(),
            code = auth.code,
            onCodeChange = auth::editCode,
            onCodeComplete = { auth.verifyCode(enter) },
            onResend = { auth.sendCode() },
            onBack = back,
            problem = auth.problem,
        )
    }
}
