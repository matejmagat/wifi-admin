# WiFi parametri — zadatak

U aplikaciji MojTelekom pretplatnik može dohvatiti parametre WiFi mreže postavljene na HT routeru koji je instaliran na korisnikovoj adresi.
Trenutno se ti parametri dohvaćaju i postavljaju putem SOAP servisa.

Zadatak je implementirati REST API prema priloženoj Swagger specifikaciji koja će biti wrapper oko SOAP backenda jer je u novoj verziji aplikacije dopuštena samo REST komunikacija.

Ovaj repozitorij sadrži WSDL i Swagger specifikacije servisa te mock vanjske platforme u docker-compose datoteci.

Samu backend aplikaciju potrebno je napraviti kao PR na ovaj GitHub repozitorij.

## Cilj zadatka

Kandidat implementira **REST API** koji:

- izlaže dvije metode opisane u [openapi/openapi.yaml](openapi/openapi.yaml);
- u pozadini, kao **SOAP klijent**, poziva vanjsku platformu opisanu u [wsdl/wifi-platform.wsdl](wsdl/wifi-platform.wsdl);
- mapira isti poslovni model iz REST JSON-a u SOAP poruke i obratno.

Operacija `updateCpeId` u SOAP-u i WSDL-u **mijenja WiFi konfiguraciju** CPE-a (SSID, pojas, šifriranje, lozinka itd.), iako naziv sadrži „CpeId“ — to je naziv operacije na platformi, ne samo promjena identifikatora.

## Arhitektura (tko što radi)

```text
Klijent (Postman / drugi servis)
        │  REST (JSON)
        ▼
  [Backend kandidata]
        │  SOAP 1.1 (XML) + SOAPAction
        ▼
  [Mock platforme — Mockoon u Dockeru]
```

- **REST** je nova definicija: `GET /wifi-parameter/{cpeId}` i `PUT /wifi-parameter`.
- **SOAP** je postojeća definicija prema platformi: operacije `getCpeID` i `updateCpeId` (vidi WSDL i `SOAPAction` zaglavlje).

## Pokretanje mocka (Docker Compose)

```bash
docker compose up -d
```

- Mock je dostupan na **http://localhost:8080/platform** (HTTP POST, SOAP 1.1).

Zaustavljanje:

```bash
docker compose down
```

### SOAP UI / Apache CXF i XML prefiksi

Mockoon čita polja iz **parsiranog XML-a** (isti model kao `xml-js`). Alati poput **SOAP UI** često generiraju **`soapenv:`** omot i **`v1:`** (ili drugi) prefiks za elemente u namespaceu platforme, dok curl primjeri u ovom README-u koriste **`soap:`** + **`tns:`**.

Mock podržava **oba** stila (automatski se grana po sadržaju zahtjeva). Ako i dalje ne dobijete očekivani odgovor nakon što ste promijenili `mockoon/platform-mock.json` ili ga ponovno generirali, učitajte datoteku u Mockoonu:

```bash
docker compose restart platform-mock
```

**SOAPAction** s navodnicima ili bez njih — regex u mocku i dalje prepoznaje `getCpeID` / `updateCpeId`.

## Primjer SOAP poziva (curl)

Zamijenite `CPE_001` jednim od seed `cpeId` iz mocka (vidi sljedeći odlomak).

**getCpeID** (SOAPAction mora odgovarati WSDL-u):

```bash
curl -s -X POST "http://localhost:8080/platform" \
  -H "Content-Type: text/xml; charset=utf-8" \
  -H "SOAPAction: http://wifi-admin.local/platform/v1#getCpeID" \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:tns="http://wifi-admin.local/platform/v1">
  <soap:Body>
    <tns:GetCpeIdRequest>
      <tns:cpeId>CPE_001</tns:cpeId>
    </tns:GetCpeIdRequest>
  </soap:Body>
</soap:Envelope>'
```

**updateCpeId** (mock očekuje sve polja u `<tns:configuration>` radi predloška; za OPEN mreže pošaljite prazne ili dummy vrijednosti za opcionalna polja ako generator SOAP klijenta ne izostavlja elemente):

```bash
curl -s -X POST "http://localhost:8080/platform" \
  -H "Content-Type: text/xml; charset=utf-8" \
  -H "SOAPAction: http://wifi-admin.local/platform/v1#updateCpeId" \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:tns="http://wifi-admin.local/platform/v1">
  <soap:Body>
    <tns:UpdateCpeIdRequest>
      <tns:configuration>
        <tns:cpeId>CPE_001</tns:cpeId>
        <tns:wifiBand>BAND_2_4_GHZ</tns:wifiBand>
        <tns:ssid>Office-2G</tns:ssid>
        <tns:encryptionType>WPA2_PSK</tns:encryptionType>
        <tns:password>moja-lozinka</tns:password>
      </tns:configuration>
    </tns:UpdateCpeIdRequest>
  </soap:Body>
</soap:Envelope>'
```

## Podaci u mocku (seed + in‑memory)

- U [mockoon/platform-mock.json](mockoon/platform-mock.json) postoji **data bucket** s najmanje **12** predefiniranih CPE zapisa (`CPE_001` … `CPE_012`) s različitim kombinacijama pojasa (2.4 / 5 GHz), šifriranja i lozinke.
- **`updateCpeId`** ažurira zapis u memoriji Mockoona (`setData` + `merge`). Nova lozinka vidljiva je na sljedećem **`getCpeID`** dok mock radi.
- **Restart kontejnera** (`docker compose restart`) ili novo pokretanje vraća **početne seed vrijednosti** iz JSON datoteke.
- Mock **ne zapisuje** runtime promjene natrag u datoteku na disku — to je ograničenje Mockoona, prikladno za ovaj zadatak.

## Očekivano ponašanje rješenja

- REST sloj usklađen s OpenAPI 3.0.3 (validacija, smisleni HTTP statusi za greške).
- SOAP klijent usklađen s WSDL-om (ispravna struktura `Envelope`/`Body`, `SOAPAction`, namespace `http://wifi-admin.local/platform/v1`).
- Rukovanje greškama platforme (SOAP fault, mrežni timeout) i mapiranje u REST odgovore.
- Rješenje mora biti u **Spring Boot** ili **Ktor** frameworku, napisano u Javi ili Kotlinu.
- Potiče se korištenje AI alata za generiranje rješenja; provjeravat će se razumljivost.

## Dodatni zadaci

- Izrada sloja baze podataka koji će spremati podatke s platforme te dohvat podataka o WiFi mreži vraćati iz baze, a ne s platforme.
- Izrada schedulera koji će sinkronizirati bazu i podatke s platforme u noćnim satima (konfigurabilno vrijeme i broj CPE-ova).
- Uspostavljanje mehanizama za logiranje, sigurnost i konfiguracijske profile.
- Izrada front-end projekta u Reactu koji poziva REST API.

## Kriteriji ocjene (smjernice)

- Ispravnost kontrakta (REST + SOAP) i čitljivost koda.
- Validacija poslovnih pravila (npr. lozinka vs. tip šifriranja) na REST sloju.
- Struktura projekta, testovi, dokumentacija pokretanja.

## Datoteke u repozitoriju

| Datoteka | Opis |
|----------|------|
| [openapi/openapi.yaml](openapi/openapi.yaml) | OpenAPI **3.0.3** za REST API |
| [wsdl/wifi-platform.wsdl](wsdl/wifi-platform.wsdl) | WSDL platforme (SOAP 1.1, document/literal) |
| [mockoon/platform-mock.json](mockoon/platform-mock.json) | Mockoon okruženje (generirano skriptom) |
| [docker-compose.yml](docker-compose.yml) | Mockoon CLI kontejner |
