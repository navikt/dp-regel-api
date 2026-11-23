package no.nav.dagpenger.events

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

val moshiInstance: Moshi =
    Moshi
        .Builder()
        .add(YearMonthJsonAdapter())
        .add(LocalDateJsonAdapter())
        .add(LocalDateTimeJsonAdapter())
        .add(KotlinJsonAdapterFactory())
        .add(BigDecimalJsonAdapter())
        .add(URIJsonAdapter())
        .build()!!

class YearMonthJsonAdapter {
    @ToJson
    fun toJson(yearMonth: YearMonth): String = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))

    @FromJson
    fun fromJson(json: String): YearMonth = YearMonth.parse(json)
}

class LocalDateJsonAdapter {
    @ToJson
    fun toJson(localDate: LocalDate): String = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

    @FromJson
    fun fromJson(json: String): LocalDate = LocalDate.parse(json)
}

class BigDecimalJsonAdapter {
    @ToJson
    fun toJson(bigDecimal: BigDecimal): String = bigDecimal.toString()

    @FromJson
    fun fromJson(json: String): BigDecimal = BigDecimal(json)
}

class LocalDateTimeJsonAdapter {
    @ToJson
    fun toJson(localDateTime: LocalDateTime): String = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    @FromJson
    fun fromJson(json: String): LocalDateTime = LocalDateTime.parse(json)
}

class URIJsonAdapter {
    @ToJson
    fun toJson(uri: URI): String = uri.toString()

    @FromJson
    fun fromJson(json: String): URI = URI.create(json)
}
