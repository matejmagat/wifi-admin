# WiFi Admin — rješenje zadatka

Rješenje zadatka za prijavu na posao: REST API (Spring Boot) koji služi kao wrapper oko postojećeg SOAP servisa za dohvat i izmjenu WiFi parametara na HT routeru (CPE uređaju), uz React frontend i podršku bazu podataka. Izvorni opis zadatka nalazi se u [TASK.md](TASK.md) (bivši `README.md`).

## Sadržaj

- [Riješeni zadaci](#riješeni-zadaci)
- [Arhitektura i struktura projekta](#arhitektura-i-struktura-projekta)
- [Preduvjeti](#preduvjeti)
- [Pokretanje SOAP mock platforme](#pokretanje-soap-mock-platforme)
- [Pokretanje Spring Boot backenda](#pokretanje-spring-boot-backenda)
- [Pokretanje Spring testova](#pokretanje-spring-testova)
- [Pokretanje React frontenda](#pokretanje-react-frontenda)
- [Autentikacija](#autentikacija)
- [Moguća poboljšanja](#moguća-poboljšanja)
- [Korišteni alati](#korišteni-alati)

## Riješeni zadaci

**Osnovni zadatak:**

- REST API prema [openapi/openapi.yaml](openapi/openapi.yaml) izložen kroz `WifiController`: `GET /wifi-parameter/{cpeId}` i `PUT /wifi-parameter`.
- SOAP klijent (`SoapClient`, Spring-WS + JAXB, klase generirane iz WSDL-a preko `wsdl2java` Gradle plugina) koji poziva operacije `getCpeID` i `updateCpeId` prema [wsdl/wifi-platform.wsdl](wsdl/wifi-platform.wsdl).
- Dvosmjerno mapiranje REST ⇄ SOAP ⇄ entitet baze kroz `WifiConfigurationMapper`.
- Validacija poslovnog pravila: šifrirane mreže moraju imati lozinku, OPEN mreže je ne smiju imati (`WifiService.validateWifiConfiguration`).
- Rukovanje greškama: SOAP fault i komunikacijske greške mapirane su u smislene HTTP statuse (404, 400, 502) preko `GlobalExceptionHandler`.
- Normalizacija SOAP envelopea i detekcija faulta u odgovoru kroz `SoapClientInterceptor`.

**Dodatni zadaci (svi implementirani):**

- **Baza podataka** — `WifiConfigurationEntity` + `WifiConfigurationRepository` (Spring Data JPA, PostgreSQL za dev/prod, H2 za testove). `GET` prvo čita iz baze, a tek ako zapis ne postoji ide na SOAP i sprema rezultat; `PUT` uvijek ide na SOAP i zatim ažurira bazu.
- **Scheduler za noćnu sinkronizaciju** — `WifiSyncService.syncAllKnownWifiConfigurations()`, konfigurabilan preko `wifi.sync.*` property-ja (uključeno/isključeno, initial delay, fixed delay), prolazi kroz sve poznate CPE-ove iz baze i osvježava ih sa SOAP platforme.
- **Logiranje** — konfigurirano u `application.properties` (log u konzolu i datoteku, po profilu različita razina; SOAP message tracing).
- **Sigurnost** — HTTP Basic Auth preko `SecurityConfig` (Spring Security), `/wifi-parameter/**` zahtijeva prijavu, `/actuator/health` i `/actuator/info` su javni, ostali actuator endpointi traže rolu `ADMIN`.
- **Konfiguracijski profili** — `dev`, `test`, `prod` (`application-*.properties`), različiti izvori baze, razine logiranja i sigurnosnih parametara po profilu.
- **React frontend** — forma za dohvat i izmjenu WiFi parametara po `cpeId`, poziva REST API (`wifi-admin-frontend`).

## Arhitektura i struktura projekta

```text
Frontend (React, :3000)
        │  REST + Basic Auth
        ▼
Backend (Spring Boot, :8081)
        │  SOAP 1.1 + SOAPAction
        ▼
Mock platforme (Mockoon u Dockeru, :8080)
        │
        ▼
PostgreSQL (:5432) ← baza s posljednjim poznatim stanjem CPE-ova
```

```text
wifi-admin-main/
├── openapi/openapi.yaml           REST kontrakt
├── wsdl/wifi-platform.wsdl        SOAP kontrakt platforme
├── mockoon/platform-mock.json     Mock SOAP platforme
├── docker-compose.yml             Pokretanje mocka
├── wifi-admin-service/            Spring Boot backend
│   └── src/main/java/.../
│       ├── controller/            WifiController (REST)
│       ├── service/                WifiService, WifiSyncService
│       ├── client/                 SoapClient, SoapClientInterceptor
│       ├── mapper/                 WifiConfigurationMapper
│       ├── model/                  WifiConfiguration (REST DTO)
│       ├── entity/                 WifiConfigurationEntity (JPA)
│       ├── repository/             WifiConfigurationRepository
│       ├── config/                 SoapConfig, SecurityConfig, SchedulerConfig
│       └── exception/               GlobalExceptionHandler
└── wifi-admin-frontend/            React frontend
    └── src/
        ├── api/                     client.js, wifiApi.js
        ├── components/               WifiForm, WifiLookup, ErrorMessage
        ├── hooks/                     useWifiConfiguration, useWifiMetadata
        └── config/env.js             API URL i auth (dev postavke)
```

## Preduvjeti

- Java 21 (JDK)
- Node.js 18+ i npm (za frontend)
- Docker i Docker Compose (za SOAP mock)
- PostgreSQL (lokalno instaliran ili u kontejneru) — potreban za `dev` profil backend

## Pokretanje SOAP mock platforme

```bash
docker compose up -d
```

Mock je dostupan na `http://localhost:8080/platform`. Zaustavljanje:

```bash
docker compose down
```

Detalji o mocku (seed podaci, SOAP primjeri, kompatibilnost sa SOAP UI) opisani su u izvornom [TASK.md](TASK.md).

## Pokretanje Spring Boot backenda

1. Pripremiti PostgreSQL bazu za `dev` profil (podaci iz `application-dev.properties`):

   ```sql
   CREATE DATABASE wifi_admin_db;
   CREATE USER wifi_admin_user WITH PASSWORD 'wifi_admin_user';
   GRANT ALL PRIVILEGES ON DATABASE wifi_admin_db TO wifi_admin_user;
   ```

2. Pokrenuti SOAP mock (vidi gore) — backend na startu i pri svakom dohvatu/izmjeni komunicira s njim na `http://localhost:8080/platform`.

3. Pokrenuti backend iz `wifi-admin-service/`:

   ```bash
   ./gradlew bootRun
   ```

   Aktivan je `dev` profil po defaultu (`spring.profiles.active=dev` u `application.properties`). Backend sluša na `http://localhost:8081`.

   Za pokretanje s drugim profilom (npr. `prod`, uz odgovarajuće env varijable `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `APP_SECURITY_USERNAME`, `APP_SECURITY_PASSWORD`):

   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=prod'
   ```

U IntelliJ IDEA-i dovoljno je otvoriti `wifi-admin-service` kao Gradle projekt i pokrenuti `WifiAdminServiceApplication` (uz Active profile `dev` u Run Configuration ako se ne koristi Gradle task).

## Pokretanje Spring testova

Testovi (`WifiServiceTest`, `WifiControllerTest`, `WifiSyncServiceTest`, `WifiPersistenceIntegrationTest`, `WifiAdminServiceApplicationTests`) koriste `test` profil s H2 in-memory bazom, pa ne zahtijevaju pokrenutu vanjsku bazu ni SOAP mock.

```bash
cd wifi-admin-service
./gradlew test
```

Izvještaj o testovima nakon izvođenja: `wifi-admin-service/build/reports/tests/test/index.html`.

U IntelliJ IDEA-i: desni klik na `src/test/java` → *Run All Tests*, ili pokretanje pojedinačne test klase.

## Pokretanje React frontenda

```bash
cd wifi-admin-frontend
npm install
npm start
```

Frontend se pokreće na `http://localhost:3000` i poziva backend na `http://localhost:8081` (vidi `src/config/env.js`). Backend mora biti pokrenut prije korištenja frontenda; CORS je na backendu dopušten upravo za `http://localhost:3000` (`@CrossOrigin` u `WifiController`).

## Autentikacija

Backend zahtijeva HTTP Basic Auth za sve `/wifi-parameter/**` pozive. Zadane dev vjerodajnice (`application-dev.properties`):

- korisničko ime: `admin`
- lozinka: `admin123`

Frontend (`src/config/env.js`) već sadrži iste dev vjerodajnice pa forma radi bez dodatne konfiguracije u lokalnom razvoju.

## Moguća poboljšanja

- **Config properties umjesto praznih placeholder klasa** — `SyncProperties` i `WifiSyncScheduler` trenutno su prazne klase bez implementacije; logika sinkronizacije zapravo živi u `WifiSyncService` s `@Scheduled` SpEL izrazima. Vezati `wifi.sync.*` property-je na pravi `@ConfigurationProperties` bean radi tipizacije i validacije, te ili implementirati ili ukloniti prazne klase.
- **Maskiranje osjetljivih podataka u logovima** — `logging.level...MessageTracing=TRACE` loga cijeli SOAP envelope, uključujući WiFi lozinku u čistom tekstu; vrijedilo bi dodati maskiranje/redakciju prije zapisivanja u produkciji.
- **Lozinka u bazi u čistom tekstu** — `WifiConfigurationEntity.password` sprema se nekriptirano; razmisliti o enkripciji polja (npr. Jasypt ili aplikacijski `AttributeConverter`).
- **Frontend konfiguracija kroz env varijable** — `config/env.js` trenutno hardkodira `API_BASE_URL` i dev vjerodajnice; prebaciti na `.env` / `REACT_APP_*` varijable radi razdvajanja okruženja i lakšeg deploya.
- **Jači auth model** — Basic Auth s jednim in-memory korisnikom je u redu za zadatak, ali za produkciju bi imalo smisla OAuth2/JWT te prisilan HTTPS.
- **Postgres u docker-compose** — trenutno je dockeriziran samo SOAP mock; dodavanjem PostgreSQL servisa u `docker-compose.yml` pokretanje `dev` okruženja bilo bi jednim korakom, bez ručnog postavljanja lokalne baze.
- **Swagger/OpenAPI UI uživo** — izložiti `springdoc-openapi` na backendu radi interaktivne dokumentacije usklađene s `openapi/openapi.yaml` i lakšeg ručnog testiranja.
- **Paginacija/listanje CPE-ova** — trenutni API radi samo po pojedinačnom `cpeId`; endpoint za listanje/pretragu svih poznatih uređaja u bazi bio bi koristan za administraciju.
- **CI pipeline** — dodati GitHub Actions (build + `./gradlew test` + `npm test`) radi automatske provjere PR-ova.
- **Frontend testovi** — trenutno je prisutan samo generirani `App.test.js`; dodati testove za `WifiForm`, `useWifiConfiguration` i `api/client.js` (mock fetch odgovora, uključujući greške).

## Korišteni alati

- OS: Linux (Fedora 44)
- IDE: IntelliJ IDEA (backend), WebStorm (frontend)
- AI alati: Perplexity (istraživanje/provjera), Claude (generiranje i revizija dijelova rješenja)
