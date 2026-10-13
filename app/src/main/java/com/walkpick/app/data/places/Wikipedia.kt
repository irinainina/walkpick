package com.walkpick.app.data.places

import android.content.Context
import com.walkpick.app.BuildConfig
import com.walkpick.app.data.local.SyncJson
import com.walkpick.app.data.remote.webClient
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val SEARCH_URL = "https://en.wikipedia.org/w/api.php"
private const val SUMMARY_URL = "https://en.wikipedia.org/api/rest_v1/page/summary/"

// Wikipedia refuses requests that do not say who is asking.
private const val ASKED_BY = "WalkPick/${BuildConfig.VERSION_NAME} (Android; ${BuildConfig.APPLICATION_ID})"

// One of the widths Wikipedia keeps ready; a phone screen is about this wide.
private const val CARD_PICTURE_PX = 960
private val ThumbWidth = Regex("/\\d+px-")

/** The head of an article: its title, its first paragraph and where to read on. */
@Serializable
data class WikiSummary(
    val title: String,
    val extract: String = "",
    @SerialName("content_urls") private val urls: Urls? = null,
    @SerialName("pageid") val id: Long = 0,
    private val thumbnail: Picture? = null,
) {
    val url: String? get() = urls?.desktop?.page

    /** The address of the small picture of the article, when it has one. */
    val picture: String? get() = thumbnail?.source

    /** The same picture wide enough for the top of a card; Wikipedia cuts it to the width in its name. */
    val widePicture: String? get() = picture?.replace(ThumbWidth, "/${CARD_PICTURE_PX}px-")

    @Serializable
    data class Picture(val source: String? = null)

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
class Wikipedia(private val pictures: File) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.cacheDir, "wikipedia"))

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

    /** The picture of an article as a file of the cache, fetched once; null when the article has none. */
    suspend fun picture(article: WikiSummary): File? {
        val small = article.picture ?: return null
        val kept = File(pictures, "${article.id}.jpg")
        if (kept.exists()) return kept
        // A picture narrower than the card has no wide cut: the small one is taken then.
        val bytes = listOfNotNull(article.widePicture, small).distinct().firstNotNullOfOrNull { address ->
            val answer = webClient.get(address) { header(HttpHeaders.UserAgent, ASKED_BY) }
            if (answer.status.isSuccess()) answer.bodyAsBytes() else null
        } ?: return null
        return withContext(Dispatchers.IO) {
            pictures.mkdirs()
            kept.writeBytes(bytes)
            kept
        }
    }
}
