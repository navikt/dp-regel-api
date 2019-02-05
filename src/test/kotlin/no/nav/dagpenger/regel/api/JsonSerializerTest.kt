package no.nav.dagpenger.regel.api

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class JsonSerializerTest {

    @Test
    fun `serialize dagpenger behov events given input`() {
        val serialized = JsonSerializer().serialize(
            "topic", SubsumsjonsBehov(
                "behovId",
                "aktørId",
                0,
                LocalDate.now()
            )
        )
        assertNotNull(serialized)
    }

    @Test
    fun `Deserialize dagpenger behov events given null input`() {
        val serialized = JsonDeserializer().deserialize("topic", null)
        assertNull(serialized)
    }

    @Test
    fun `Deserialize dagpenger behov events given bad input`() {
        val serialized = JsonDeserializer().deserialize("topic", "bad input".toByteArray())
        assertNull(serialized)
    }

    @Test
    fun `Deserialize dagpenger behov events given input`() {
        val serialized = JsonDeserializer().deserialize("topic", json.toByteArray())
        assertNotNull(serialized)
    }

    private val json = """
        {
            "behovId": "123",
            "aktørId": "123",
            "vedtakId": 0,
            "beregningsDato" : "2019-01-01"
        }
    """.trimIndent()
}