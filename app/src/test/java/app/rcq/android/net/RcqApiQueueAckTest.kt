package app.rcq.android.net

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress

class RcqApiQueueAckTest {

    @Test
    fun drainQueueUsesAckModeAndPostsAckIds() = runBlocking {
        var queueQuery: String? = null
        var ackBody = ""
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/") { ex ->
                when (ex.requestMethod to ex.requestURI.path) {
                    "GET" to "/messages/queue" -> {
                        queueQuery = ex.requestURI.rawQuery
                        assertEquals("Bearer test-token", ex.requestHeaders.getFirst("Authorization"))
                        ex.respond("""[{"id":10,"envelope_type":"message","payload":"p","received_at":"now"},{"id":20,"envelope_type":"gmsg","payload":"g","received_at":"now","group_id":7}]""")
                    }
                    "POST" to "/messages/queue/ack" -> {
                        ackBody = ex.requestBody.reader().readText()
                        ex.respond("""{"deleted":2}""")
                    }
                    else -> ex.respond("{}", 404)
                }
            }
            start()
        }
        try {
            val api = RcqApi("http://127.0.0.1:${server.address.port}").apply { setToken("test-token") }

            val rows = api.drainQueue(ack = true)
            val ack = api.ackQueue(directIds = listOf(10), groupIds = listOf(20))

            assertEquals("ack=1", queueQuery)
            assertEquals(2, rows.size)
            assertEquals(2, ack.deleted)
            assertTrue(ackBody.contains(""""direct_ids":[10]"""))
            assertTrue(ackBody.contains(""""group_ids":[20]"""))
        } finally {
            server.stop(0)
        }
    }

    private fun HttpExchange.respond(body: String, code: Int = 200) {
        val bytes = body.toByteArray()
        responseHeaders.add("Content-Type", "application/json")
        sendResponseHeaders(code, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }
}
