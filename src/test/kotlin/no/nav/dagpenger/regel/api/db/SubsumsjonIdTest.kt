package no.nav.dagpenger.regel.api.db

import de.huxhorn.sulky.ulid.ULID
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import java.util.UUID
import no.nav.dagpenger.regel.api.models.IllegalUlidException
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import org.junit.jupiter.api.Test

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
