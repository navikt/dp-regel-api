# dagpenger-regel-api


API for å kjøre dagpengeregler


## Arkitektur og dokumentasjon

![Arkitektur og dokumentasjon](docs/README.md)


## Utvikling av applikasjonen

For å kjøre enkelte av testene kreves det at Docker kjører.


### Personlig tilgang til Postgres databasen

Se [Personlig tilgang](https://docs.nais.io/how-to-guides/persistence/postgres/#personal-database-access)


### Spørringer:
Finne behovene og aktørId for en gitt subsumsjonsId:
```SQL
select
    *
from v2_subsumsjon
where data -> 'minsteinntektResultat' ->> 'subsumsjonsId' = 'subsumsjonsid'
OR data -> 'satsResultat' ->> 'subsumsjonsId' = 'subsumsjonsid'
OR data -> 'periodeResultat' ->> 'subsumsjonsId' = 'subsumsjonsid'
OR data -> 'grunnlagResultat' ->> 'subsumsjonsId' = 'subsumsjonsid';
```

Finne sats, grunnlag og hvilke grunnbeløp brukt gitt en aktør
```SQL
select
data -> 'satsResultat' ->> 'dagsats' as dagsats,
data -> 'faktum' ->> 'beregningsdato' as beregningsdato,
data -> 'faktum' ->> 'regelverksdato' as regelverksdato,
data -> 'faktum' ->> 'manueltGrunnlag' as manueltgrunnlag,
data -> 'grunnlagResultat' as grunnlag,
brukt
from v2_subsumsjon where behov_id in (
select id from v2_behov where aktor_id = 'aktørId');
```
