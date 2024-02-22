package no.nav.dagpenger.regel.api.db

import de.huxhorn.sulky.ulid.ULID
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.regel.api.models.IllegalUlidException
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import org.junit.jupiter.api.Test
import java.util.UUID

internal class SubsumsjonIdTest {
    @Test
    fun `Subsumsjon id should be in ULID format`() {
        val id = ULID().nextULID()
        val inntektId = SubsumsjonId(id)

        id shouldBe inntektId.id
    }

    @Test
    fun `Subsumsjon id not in ULID format should fail`() {
        val id = UUID.randomUUID().toString()
        shouldThrow<IllegalUlidException> { SubsumsjonId(id) }
    }
}
