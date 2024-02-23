# dagpenger-regel-api


API for å kjøre dagpengeregler


## Arkitektur og dokumentasjon

![Arkitektur og dokumentasjon](docs/README.md)


## Utvikling av applikasjonen

For å kjøre enkelte av testene kreves det at Docker kjører.

[Docker Desktop](https://www.docker.com/products/docker-desktop)


### Starte applikasjonen lokalt

Applikasjonen har avhengigheter til Kafka og Postgres som kan kjøres
opp lokalt vha Docker Compose(som følger med Docker Desktop) 


Starte Kafka og Postgres: 
```

docker-compose -f docker-compose.yml up

```
Etter at containerene er startet kan man starte applikasjonen ved å kjøre main metoden.


Stoppe Kafka og Postgres:

```
ctrl-c og docker-compose -f docker-compose.yml down 

```

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
