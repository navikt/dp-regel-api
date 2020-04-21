package no.nav.dagpenger.regel.api

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import no.nav.dagpenger.events.URIJsonAdapter
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.SubsumsjonId

val moshiInstance: Moshi = Moshi.Builder()
    .add(BehovIdJsonAdapter())
    .add(SubsumsjonIdJsonAdapter())
    .add(YearMonthJsonAdapter())
    .add(LocalDateTimeJsonAdapter())
    .add(LocalDateJsonAdapter())
    .add(ZonedDateTimeJsonAdapter())
    .add(KotlinJsonAdapterFactory())
    .add(BigDecimalJsonAdapter())
    .add(URIJsonAdapter())
    .build()!!

class YearMonthJsonAdapter {
    @ToJson
    fun toJson(yearMonth: YearMonth): String {
        return yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }

    @FromJson
    fun fromJson(json: String): YearMonth {
        return YearMonth.parse(json)
    }
}

class LocalDateJsonAdapter {
    @ToJson
    fun toJson(localDate: LocalDate): String {
        return localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    @FromJson
    fun fromJson(json: String): LocalDate {
        return LocalDate.parse(json)
    }
}

class LocalDateTimeJsonAdapter {
    @ToJson
    fun toJson(localDateTime: LocalDateTime): String {
        return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    @FromJson
    fun fromJson(json: String): LocalDateTime {
        return LocalDateTime.parse(json)
    }
}

class BigDecimalJsonAdapter {

    @ToJson
    fun toJson(bigDecimal: BigDecimal): String {
        return bigDecimal.toString()
    }

    @FromJson
    fun fromJson(json: String): BigDecimal {
        return BigDecimal(json)
    }
}

class ZonedDateTimeJsonAdapter {
    @ToJson
    fun toJson(zonedDateTime: ZonedDateTime): String {
        return zonedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
    }
    @FromJson
    fun fromJson(json: String): ZonedDateTime {
        return ZonedDateTime.parse(json)
    }
}

class BehovIdJsonAdapter {
    @ToJson
    fun toJson(behovId: BehovId): String {
        return behovId.id
    }

    @FromJson
    fun fromJson(json: String): BehovId {
        return BehovId(json)
    }
}
class SubsumsjonIdJsonAdapter {
    @ToJson
    fun toJson(subsumsjonId: SubsumsjonId): String {
        return subsumsjonId.id
    }

    @FromJson
    fun fromJson(json: String): SubsumsjonId {
        return SubsumsjonId(json)
    }
}
