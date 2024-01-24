package com.IndianTV

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.mvvm.safeApiCall
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import org.jsoup.nodes.Element

class IndianTVProvider : MainAPI() {
    override var mainUrl = "https://livesportsclub.me"
    override var name = "IndianTV"
    override val supportedTypes = setOf(TvType.Live)
    override var lang = "hi"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "$mainUrl/hls/tata/" to "Channels",
    )

    data class LiveStreamLinks (
        @JsonProperty("title")  val title: String,
        @JsonProperty("poster") val poster: String,
        @JsonProperty("link")   val link: String,
        @JsonProperty("link")   val subtitle: String,
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest
	): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("div.box1").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse {
        //Log.d("Got","got here")
        val titleRaw = this.selectFirst("h2.text-center.text-sm.font-bold")?.text()?.trim()
        val title = if (titleRaw.isNullOrBlank()) "Unknown LiveStream" else titleRaw.toString()
        //Log.d("title", title)
        val subtitleRaw = this.selectFirst("p.text-xs.text-center")?.text()?.trim()
        val subtitle = if (subtitleRaw.isNullOrBlank()) "Unknown LiveStream" else subtitleRaw.toString()
        //Log.d("mybadhref", subtitle)
        val posterUrl = fixUrl(this.select("img").attr("src"))
        //Log.d("posterUrl", posterUrl)
        val href = fixUrl(this.selectFirst("a")?.attr("href").toString())
        //Log.d("mybadhref", href)

        val loadData = LiveStreamLinks(
                title,
                posterUrl,
                href,
                subtitle,
            ).toJson()

        return newMovieSearchResponse(title, loadData, TvType.Live) {
                this.posterUrl = posterUrl
            }
        }

        override suspend fun search(query: String): List<SearchResponse> {
            val doc = app.get("$mainUrl/").document
            //Log.d("document", document.toString())
    
            return doc.select("div.col-6").mapNotNull {
                it.toSearchResult()
            }.filter { it.name.contains(query, true) }
        }

        override suspend fun load(url: String): LoadResponse {
            val data = parseJson<LiveStreamLinks>(url)
            val title = data.title
            val poster = data.poster
            val link = data.link
            val subtitle=data.subtitle

            return newMovieLoadResponse(title, link, TvType.Live, link) {
                this.posterUrl = poster
            }
        }
}