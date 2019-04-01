package no.nav.dagpenger.regel.api

import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals

class SenesteInntektsmånedTest {
    @Test
    fun `senesteInntektsMåned is two months ago when beregningsdato is before the 5th`() {
        assertEquals(YearMonth.of(2019, 1), senesteInntektsmåned(LocalDate.of(2019, 3, 1)))
        assertEquals(YearMonth.of(2019, 1), senesteInntektsmåned(LocalDate.of(2019, 3, 2)))
        assertEquals(YearMonth.of(2019, 1), senesteInntektsmåned(LocalDate.of(2019, 3, 5)))
    }

    @Test
    fun `senesteInntektsMåned is one month ago when beregningsdato is after the 5th where the 6th is a working day`() {
        assertEquals(YearMonth.of(2019, 2), senesteInntektsmåned(LocalDate.of(2019, 3, 6)))
        assertEquals(YearMonth.of(2019, 7), senesteInntektsmåned(LocalDate.of(2019, 8, 15)))
    }

    @Test
    fun `senesteInntektsMåned is two months ago when beregningsdato is after the 5th and but before there has been a working day`() {
        assertEquals(YearMonth.of(2018, 11), senesteInntektsmåned(LocalDate.of(2019, 1, 6)))
        assertEquals(YearMonth.of(2023, 2), senesteInntektsmåned(LocalDate.of(2023, 4, 10)))
    }
}