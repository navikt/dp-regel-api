package no.nav.dagpenger.events

import com.squareup.moshi.JsonEncodingException
import io.kotest.matchers.shouldBe
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PacketTest {
    @Test
    fun `create packet from default JSON string`() {
        assertNotNull(Packet())
    }

    @Test
    fun `create packet with JSON`() {
        val jsonString =
            """
            {
                "key1": "value1"
            }
            """.trimIndent()

        assertEquals("value1", Packet(jsonString).getNullableStringValue("key1"))
    }

    @Test
    fun ` packet throws exception on invalid JSON`() {
        val invalidJson =
            """
            {
                "key1" "value1"
                "key2": "value1",
            }
            """.trimIndent()
        assertThrows<JsonEncodingException> { Packet(invalidJson) }
    }

    @Test
    fun `packet keeps fields`() {
        val jsonString =
            """
            {
                "key1": 1,
                "key2": "value1",
                "key3": true
            }
            """.trimIndent()
        val jsonObject = JSONObject(Packet(jsonString).toJson())
        assertEquals(1, jsonObject.getInt("key1"))
        assertEquals("value1", jsonObject.getString("key2"))
        assertEquals(true, jsonObject.getBoolean("key3"))
    }

    @Test
    fun `packet gets system readcount`() {
        val jsonString =
            """
            {
                "key1": "value1"
            }
            """.trimIndent()

        assertTrue(JSONObject(Packet(jsonString).toJson()).has("system_read_count"))
        assertTrue(JSONObject(Packet(jsonString).toJson()).has("system_started"))
        assertEquals(0, JSONObject(Packet(jsonString).toJson()).getInt("system_read_count"))
    }

    @Test
    fun `packet increments system readcount`() {
        val jsonString =
            """
            {
                "system_read_count": 5,
                "key1": "value1"
            }
            """.trimIndent()

        val packet = Packet(jsonString)

        assertEquals(6, packet.getReadCount())
    }

    @Test
    fun `packet does not allow rewrite`() {
        val jsonString =
            """
            {
                "system_read_count": 5,
                "key1": "value1"
            }
            """.trimIndent()

        assertThrows<IllegalArgumentException> { Packet(jsonString).putValue("key1", "awe") }
    }

    @Test
    fun `can write and get BigDecimal to packet`() {
        val jsonString =
            """
            {
                "system_read_count": 5,
                "key1": "value1"
            }
            """.trimIndent()
        val packet = Packet(jsonString)

        packet.putValue("dec", BigDecimal(5))
        packet.putValue("dec2", BigDecimal(5.3))
        packet.putValue("rubbish", "rubbish")

        assertEquals(BigDecimal(5), packet.getNullableBigDecimalValue("dec"))
        assertEquals(BigDecimal(5), packet.getBigDecimalValue("dec"))
        assertEquals(BigDecimal(5.3), packet.getNullableBigDecimalValue("dec2"))
        assertEquals(BigDecimal(5.3), packet.getBigDecimalValue("dec2"))
        assertEquals(null, packet.getNullableBigDecimalValue("notExisting"))
        assertThrows<IllegalArgumentException> { packet.getBigDecimalValue("notExisting") }
        assertThrows<NumberFormatException> { packet.getNullableLongValue("rubbish") }
        assertThrows<NumberFormatException> { packet.getLongValue("rubbish") }
    }

    @Test
    fun `can write and get Number to packet`() {
        val jsonString =
            """
            {
                "system_read_count": 5,
                "key1": "value1",
                "key2": 5
            }
            """.trimIndent()
        val packet = Packet(jsonString)

        packet.putValue("int", 1)
        packet.putValue("long", 1L)
        packet.putValue("rubbish", "rubbish")

        assertEquals(5, packet.getNullableIntValue("key2"))
        assertEquals(5, packet.getIntValue("key2"))
        assertEquals(1, packet.getNullableIntValue("int"))
        assertEquals(1, packet.getIntValue("int"))
        assertEquals(1L, packet.getNullableLongValue("long"))
        assertEquals(1L, packet.getLongValue("long"))
        assertEquals(null, packet.getNullableIntValue("noExisting"))
        assertThrows<IllegalArgumentException> { packet.getIntValue("noExisting") }
        assertEquals(null, packet.getNullableLongValue("noExisting"))
        assertThrows<IllegalArgumentException> { packet.getLongValue("noExisting") }
        assertThrows<NumberFormatException> { packet.getNullableIntValue("rubbish") }
        assertThrows<NumberFormatException> { packet.getIntValue("rubbish") }
        assertThrows<NumberFormatException> { packet.getNullableLongValue("rubbish") }
        assertThrows<NumberFormatException> { packet.getLongValue("rubbish") }
    }

    @Test
    fun `can write and get Boolean to packet`() {
        val jsonString =
            """
            {
                "system_read_count": 5,
                "key1": "value1"
            }
            """.trimIndent()
        val packet = Packet(jsonString)

        packet.putValue("booleanValue1", true)
        packet.putValue("booleanValue2", false)
        packet.putValue("notAnBoolean", "rubbish")

        assertEquals(true, packet.getNullableBoolean("booleanValue1"))
        assertEquals(true, packet.getBoolean("booleanValue1"))
        assertEquals(false, packet.getNullableBoolean("booleanValue2"))
        assertEquals(false, packet.getBoolean("booleanValue2"))
        assertEquals(null, packet.getNullableBoolean("notExistiing"))
        assertThrows<IllegalArgumentException> { packet.getBoolean("notExistiing") }
        assertThrows<IllegalArgumentException> { packet.getNullableBoolean("notAnBoolean") }
        assertThrows<IllegalArgumentException> { packet.getBoolean("notAnBoolean") }
    }

    @Test
    fun `can write and get Map values from Packet`() {
        val jsonString =
            """
            {
                "system_read_count": 5,
                "key1": "value1"
            }
            """.trimIndent()
        val packet = Packet(jsonString)

        packet.putValue("map1", mapOf("key1" to "11222", "key2" to 124))
        packet.putValue("map2", mapOf("key1" to "11222", "key2" to false))
        packet.putValue("notAmap", "rubbish")

        assertEquals(mapOf("key1" to "11222", "key2" to 124), packet.getMapValue("map1"))
        assertEquals(mapOf("key1" to "11222", "key2" to false), packet.getMapValue("map2"))
        assertThrows<IllegalArgumentException> { packet.getMapValue("notAmap") }

        val packet2 = Packet(packet.toJson()!!)
        assertEquals(mapOf("key1" to "11222", "key2" to 124.0), packet2.getMapValue("map1"))
        assertEquals(mapOf("key1" to "11222", "key2" to false), packet2.getMapValue("map2"))
    }

    @Test
    fun `can write and get LocalDate to packet`() {
        val jsonString =
            """
            {
                "system_read_count": 5,
                "key1": "value1"
            }
            """.trimIndent()
        val packet = Packet(jsonString)

        packet.putValue("localdate", LocalDate.of(2019, 1, 15))
        packet.putValue("notALocalDate", "rubbish")

        assertEquals(LocalDate.of(2019, 1, 15), packet.getNullableLocalDate("localdate"))
        assertEquals(LocalDate.of(2019, 1, 15), packet.getLocalDate("localdate"))
        assertEquals(null, packet.getNullableLocalDate("notExistiing"))
        assertThrows<IllegalArgumentException> { packet.getLocalDate("notExistiing") }
        assertThrows<DateTimeParseException> { packet.getNullableLocalDate("notALocalDate") }
    }

    @Test
    fun `can write and get YearMonth to packet`() {
        val jsonString =
            """
            {
                "system_read_count": 5,
                "key1": "value1"
            }
            """.trimIndent()
        val packet = Packet(jsonString)

        packet.putValue("yearmonth", YearMonth.of(2019, 2))
        packet.putValue("notAYearMonth", "rubbish")

        assertEquals(YearMonth.of(2019, 2), packet.getNullableYearMonth("yearmonth"))
        assertEquals(YearMonth.of(2019, 2), packet.getYearMonth("yearmonth"))
        assertEquals(null, packet.getNullableYearMonth("notExistiing"))
        assertThrows<IllegalArgumentException> { packet.getYearMonth("notExistiing") }
        assertThrows<DateTimeParseException> { packet.getNullableYearMonth("notAYearMonth") }
    }

    @Test
    fun `hasField `() {
        val jsonString =
            """
            {
                "system_read_count": 5,
                "key1": "value1",
                "anotherKey": "qwe"
            }
            """.trimIndent()
        val packet = Packet(jsonString)
        assertTrue(packet.hasField("key1"))
        assertTrue(packet.hasField("anotherKey"))
        assertFalse(packet.hasField("some other key"))
        assertFalse(packet.hasField("key2"))
    }

    @Test
    fun `hasFields `() {
        val jsonString =
            """
            {
                "system_read_count": 5,
                "key1": "value1",
                "anotherKey": "qwe",
                "thirdKey": "qwe"
            }
            """.trimIndent()
        val packet = Packet(jsonString)
        assertTrue(packet.hasFields("key1"))
        assertTrue(packet.hasFields("anotherKey"))
        assertFalse(packet.hasFields("some other key"))
        assertTrue(packet.hasFields("thirdKey", "key1", "anotherKey"))
        assertFalse(packet.hasFields("thirdKey", "key1", "non existing", "anotherKey"))
    }

    @Test
    fun `Should be able to add problem `() {
        val jsonString =
            """
            {
                "key1": "value1"
            }
            """.trimIndent()

        val packet = Packet(jsonString)

        packet.addProblem(Problem(title = "A problem"))
        assertTrue(packet.hasProblem())
    }

    @Test
    fun `Should be able serialize problems to json `() {
        val jsonString =
            """
            {
                "key1": "value1"
            }
            """.trimIndent()

        val packet = Packet(jsonString)

        val problem =
            Problem(
                type = URI.create("urn:error"),
                title = "A problem",
                status = 404,
                detail = "An detailed error message",
                instance = URI.create("urn:error:404"),
            )
        packet.addProblem(problem)
        val serializedPacket = Packet(packet.toJson()!!)
        assertTrue(serializedPacket.hasProblem())
        assertEquals(problem, packet.getProblem())
    }

    @Test
    fun `Should contain no problems `() {
        val jsonString =
            """
            {
                "key1": "value1"
            }
            """.trimIndent()

        val packet = Packet(jsonString)
        assertFalse(packet.hasProblem())
    }

    @Test
    fun `can put complex object`() {
        val jsonString =
            """
            {
                "system_read_count": 0,
                "key1": "value1",
                "anotherKey": "qwe"
            }
            """.trimIndent()
        val packet = Packet(jsonString)
        val complex =
            ClassA(
                "id",
                listOf(
                    ClassB(
                        YearMonth.of(2019, 2),
                        listOf(ClassC(BigDecimal.ZERO, AnEnum.BBB), ClassC(BigDecimal.TEN, AnEnum.AAA)),
                    ),
                ),
            )
        val adapter = moshiInstance.adapter<ClassA>(ClassA::class.java)

        packet.putValue("complex", adapter.toJsonValue(complex)!!)
        assertEquals(complex, packet.getNullableObjectValue("complex", adapter::fromJsonValue))

        val snapshot = Packet(packet.toJson()!!)
        assertEquals(complex, snapshot.getNullableObjectValue("complex", adapter::fromJsonValue))
        assertEquals("qwe", snapshot.getNullableStringValue("anotherKey"))
        assertEquals(null, packet.getNullableObjectValue("notExisting", adapter::fromJsonValue))
        assertThrows<IllegalArgumentException> {
            packet.getObjectValue("notExisting") { string ->
                adapter.fromJsonValue(
                    string,
                ) ?: throw java.lang.IllegalArgumentException()
            }
        }
    }

    @Test
    fun `toString should hide non system Packet values`() {
        val packetString =
            Packet()
                .apply {
                    putValue(Packet.PROBLEM, "problemValue")
                    putValue("secret", "secretValue")
                }.toString()

        assertFalse(packetString.contains("secretValue"))
        assertFalse(packetString.contains("problemValue"))
    }

    @Test
    fun `Packet should have system-correlation-id`() {
        val packet = Packet()
        packet.hasField("system_correlation_id") shouldBe true
    }

    @Test
    fun `system-correlation-id should be preserved on serialization and deserialization`() {
        val packet = Packet()
        Packet(packet.toJson()!!).getCorrelationId() shouldBe packet.getCorrelationId()
    }

    @Test
    fun `Initialization of Packet should increment summary count`() {
        packetPayloadByteSize.clear()
        val packet = Packet()
        packetPayloadByteSize.get().count shouldBe 1.0
    }

    data class ClassA(
        val id: String,
        val list: List<ClassB>,
    )

    data class ClassB(
        val yearMonth: YearMonth,
        val list: List<ClassC>,
    )

    data class ClassC(
        val bigDecimal: BigDecimal,
        val enums: AnEnum,
    )

    enum class AnEnum {
        AAA,
        BBB,
    }
}
