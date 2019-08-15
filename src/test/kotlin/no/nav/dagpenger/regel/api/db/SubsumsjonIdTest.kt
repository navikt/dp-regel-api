package no.nav.dagpenger.regel.api.db

import de.huxhorn.sulky.ulid.ULID
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
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
        shouldThrow<IllegalSubsumsjonIdException> { SubsumsjonId(id) }
    }
}