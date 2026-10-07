package com.walkspot.app.data.places

import com.walkspot.app.BuildConfig
import com.walkspot.app.data.local.SyncJson
import com.walkspot.app.data.remote.webClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val SEARCH_URL = "https://en.wikipedia.org/w/api.php"
private const val SUMMARY_URL = "https://en.wikipedia.org/api/rest_v1/page/summary/"

// Wikipedia refuses requests that do not say who is asking.
private const val ASKED_BY = "WalkSpot/${BuildConfig.VERSION_NAME} (Android; ${BuildConfig.APPLICATION_ID})"

/** The head of an article: its title, its first paragraph and where to read on. */
@Serializable
data class WikiSummary(
    val title: String,
    val extract: String = "",
    @SerialName("content_urls") private val urls: Urls? = null,
) {
    val url: String? get() = urls?.desktop?.page

    @Serializable
    data class Urls(val desktop: Page? = null)

    @Serializable
    data class Page(val page: String? = null)
}

/** The summary in an answer of Wikipedia; null when the answer is not one. */
internal fun parseSummary(answer: String): WikiSummary? = try {
    SyncJson.decodeFromString<WikiSummary>(answer)
} catch (_: SerializationException) {
    null
}

/** The English Wikipedia; every call throws [IOException] when it does not answer. */
@Singleton
class Wikipedia @Inject constructor() {

    /** The answer of GeoSearch for the articles about places around a point, the nearest first. */
    suspend fun near(latitude: Double, longitude: Double, radiusM: Int, limit: Int): String {
        val answer = webClient.get(SEARCH_URL) {
            header(HttpHeaders.UserAgent, ASKED_BY)
            parameter("action", "query")
            parameter("list", "geosearch")
            parameter("gscoord", "$latitude|$longitude")
            parameter("gsradius", radiusM)
            parameter("gslimit", limit)
            parameter("format", "json")
        }
        if (!answer.status.isSuccess()) throw IOException("Wikipedia answered ${answer.status}")
        return answer.bodyAsText()
    }

    /** The article called [title], a Latin name included; null when there is none. */
    suspend fun summary(title: String): WikiSummary? {
        val answer = webClient.get(SUMMARY_URL + title.replace(' ', '_').encodeURLPathPart()) {
            header(HttpHeaders.UserAgent, ASKED_BY)
        }
        if (answer.status == HttpStatusCode.NotFound) return null
        if (!answer.status.isSuccess()) throw IOException("Wikipedia answered ${answer.status}")
        return parseSummary(answer.bodyAsText())
    }
}
