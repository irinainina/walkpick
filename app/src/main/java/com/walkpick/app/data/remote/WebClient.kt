package com.walkpick.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

/** One client for the open services that need no key: Wikipedia, the weather. */
internal val webClient by lazy { HttpClient(OkHttp) }
