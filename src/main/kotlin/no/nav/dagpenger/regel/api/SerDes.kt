package no.nav.dagpenger.regel.api

import mu.KotlinLogging
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer

val jsonAdapter = moshiInstance.adapter(SubsumsjonsBehov::class.java)

class JsonSerializer : Serializer<SubsumsjonsBehov> {
    override fun serialize(topic: String?, data: SubsumsjonsBehov?): ByteArray? {
        return jsonAdapter.toJson(data)?.toByteArray()
    }

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
}

class JsonDeserializer : Deserializer<SubsumsjonsBehov> {
    private val LOGGER = KotlinLogging.logger {}

    override fun deserialize(topic: String?, data: ByteArray?): SubsumsjonsBehov? {
        return data?.let {
            val json = String(it)
            try {
                jsonAdapter.fromJson(json)
            } catch (ex: Exception) {
                LOGGER.error("'$json' is not valid json", ex)
                null
            }
        }
    }

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
}
