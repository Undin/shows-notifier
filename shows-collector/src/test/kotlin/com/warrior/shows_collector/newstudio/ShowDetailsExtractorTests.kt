package com.warrior.shows_collector.newstudio

import com.warrior.shows_collector.newstudio.ShowDetailsExtractor.ShowDetails
import com.warrior.shows_collector.newstudio.TestUtils.mockHtmlResponse
import okhttp3.mockwebserver.*
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Created by warrior on 3/18/17.
 */
class ShowDetailsExtractorTests {

    private val FRINGE = "/viewforum.php?f=133"
    private val GAME_OF_THRONES = "/viewforum.php?f=465"
    private val LONGMIRE = "/viewforum.php?f=246"
    private val REVOLUTION = "/viewforum.php?f=254"
    private val EMERALD_CITY = "/viewforum.php?f=531"

    private var server = MockWebServer()

    init {
        server.setDispatcher(object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    FRINGE -> mockHtmlResponse("fringe.htm")
                    GAME_OF_THRONES -> mockHtmlResponse("game_of_thrones.htm")
                    LONGMIRE -> mockHtmlResponse("longmire.htm")
                    REVOLUTION -> mockHtmlResponse("revolution.htm")
                    else -> MockResponse().setResponseCode(404)
                }
            }
        })
    }

    @Before
    fun setup() {
        server.start()
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun firstItem() {
        val url = server.url(GAME_OF_THRONES).toString()
        val showDetails = ShowDetailsExtractor.getShowDetails(url)
        assertThat(showDetails, equalTo(ShowDetails("Game of Thrones", "Игра Престолов", 6, 10)))
    }

    @Test
    fun notFirstItem() {
        val url = server.url(REVOLUTION).toString()
        val showDetails = ShowDetailsExtractor.getShowDetails(url)
        assertThat(showDetails, equalTo(ShowDetails("Revolution", "Революция", 1, 15)))
    }

    @Test
    fun showWithoutSeparateSeries() {
        val url = server.url(FRINGE).toString()
        val showDetails = ShowDetailsExtractor.getShowDetails(url)
        assertThat(showDetails, nullValue())
    }

    @Test
    fun noElements() {
        val url = server.url(LONGMIRE).toString()
        val showDetails = ShowDetailsExtractor.getShowDetails(url)
        assertThat(showDetails, nullValue())
    }

    @Test
    fun requestError() {
        val url = server.url(EMERALD_CITY).toString()
        val showDetails = ShowDetailsExtractor.getShowDetails(url)
        assertThat(showDetails, nullValue())
    }
}
