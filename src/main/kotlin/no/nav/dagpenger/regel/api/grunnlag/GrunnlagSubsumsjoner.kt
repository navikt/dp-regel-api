package no.nav.dagpenger.regel.api.grunnlag

interface GrunnlagSubsumsjoner {
    fun getGrunnlagSubsumsjon(subsumsjonsId: String): GrunnlagSubsumsjon

    fun setGrunnlagSubsumsjon(dagpengegrunnlagSubsumsjon: GrunnlagSubsumsjon)
}

class GrunnlagSubsumsjonNotFoundException(override val message: String) : RuntimeException(message)