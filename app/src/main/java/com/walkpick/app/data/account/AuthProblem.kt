package com.walkpick.app.data.account

/** Why a sign-in did not go through, in the app's terms. */
enum class AuthProblem {
    InvalidEmail,
    Offline,
    WrongCode,
    TooOften,
    NoGoogle,
    NoAnswer,
}

// A null problem: the server refused without saying why, and the caller decides what that means.
class AuthRefused(val problem: AuthProblem?, cause: Throwable? = null) : Exception(cause)

// Android's own Patterns.EMAIL_ADDRESS, copied so the check also runs in a test off the phone.
private val emailAddress = Regex(
    """[a-zA-Z0-9+._%\-]{1,256}""" +
        "@" +
        """[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}""" +
        """(\.[a-zA-Z0-9][a-zA-Z0-9\-]{0,25})+""",
)

fun isEmailAddress(value: String): Boolean = emailAddress.matches(value)
