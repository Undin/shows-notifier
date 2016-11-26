package com.warrior.shows_notifier.crawlers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.warrior.shows_notifier.ShowEpisode
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

/**
 * Created by warrior on 10/29/16.
 */
class LostFilmCrawler(printLogs: Boolean = false) : AbstractCrawler(printLogs) {

    private val rssPattern = Pattern.compile("(.*?) .*\\(S(\\d)+E(\\d+)\\)")
    private val pagePattern = Pattern.compile("(\\d+)\\.(\\d+)")

    override fun episodes(): List<ShowEpisode> = parsePage()

    private fun parsePage(): List<ShowEpisode> {
        fun Element.isBrClearBoth(): Boolean = tagName() == "br" && attr("clear") == "both"

        val document = try {
            Jsoup.connect("http://www.lostfilm.tv/browse.php").get()
        } catch (e: IOException) {
            e.printStackTrace()
            return emptyList()
        }
        val content = document.select("div.content_body").first()
        val elements = content.children()

        val groups = ArrayList<Elements>()
        var currentGroup = Elements()

        for (i in 1..elements.lastIndex) {
            val prevElem = elements[i - 1]
            val currentElem = elements[i]
            if (prevElem.isBrClearBoth() && currentElem.isBrClearBoth()) {
                groups += currentGroup
                currentGroup = Elements()
            } else {
                currentGroup.add(prevElem)
            }
        }

        val episodes = ArrayList<ShowEpisode>(groups.size)
        groups.forEach { group ->
            val firstDivText = group.select("div").first().text()
            val matcher = pagePattern.matcher(firstDivText)
            if (matcher.find()) {
                val season = matcher.group(1).toInt()
                val episodeNumber = matcher.group(2).toInt()
                val img = group.select("img.category_icon[title]").first()
                val showTitle = img.attr("title")
                val episode = ShowEpisode(showTitle, season, episodeNumber)
                episodes += episode
                log("lostfilm: $episode")
            }
        }
        return episodes
    }

    private fun parseRSS(): List<ShowEpisode> {
        val page = try {
            Jsoup.connect("http://www.lostfilm.tv/rssdd.xml").get().toString()
        } catch (e: IOException) {
            println(e.printStackTrace())
            return emptyList()
        }

        val mapper = XmlMapper()
        val rss = mapper.readValue(page, RSS::class.java)

        val episodeSet = HashSet<ShowEpisode>(rss.channel.items.size)
        for (item in rss.channel.items) {
            val matcher = rssPattern.matcher(item.title)
            if (matcher.find()) {
                val showTitle = matcher.group(1)
                val season = matcher.group(2).toInt()
                val episodeNumber = matcher.group(3).toInt()
                val episode = ShowEpisode(showTitle, season, episodeNumber)
                episodeSet += episode
                log("lostfilm: $episode")
            }
        }

        return episodeSet.toList()
    }

    @JacksonXmlRootElement(localName = "rss")
    @JsonIgnoreProperties(ignoreUnknown = true)
    private class RSS(
        @JacksonXmlProperty(localName = "channel") val channel: Channel
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private class Channel {

        @JacksonXmlProperty(localName = "item")
        @JacksonXmlElementWrapper(useWrapping = false)
        lateinit var items: List<Item>
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private class Item(
            @JacksonXmlProperty(localName = "title") val title: String
    )
}