# Kiadáskezelő – backend órai mintaprojekt

Java 25, Spring Boot 4.1.1, Spring MVC, Spring Data JPA, MapStruct és H2.
A REST-példák a felhasználókezelésen mutatják be a backend működését.
A pénzügyi domain service- és REST-rétege is elkészült; az adatbázist induláskor
automatikus mintaadat-feltöltő készíti elő az órai feladatokhoz.

## Indítás és tesztek

A `JAVA_HOME` egy Java 25 JDK-ra mutasson. Windows alatt:

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```

Az alkalmazás címe: `http://localhost:9090`. Swagger UI: `/swagger-ui/index.html`.
A H2 konzol: `/h2`; JDBC URL: `jdbc:h2:mem:testdb`, felhasználó: `sa`, üres jelszó.
Az adatbázis memóriában működik, újraindításkor az adatok elvesznek.

## HTTP-szerződés

| Művelet | Végpont | Sikeres válasz |
| --- | --- | --- |
| Létrehozás | `POST /api/felhasznalo` | `201 Created`, `Location` fejléc és `FelhasznaloResponse` |
| Lapozott lista, opcionális névszűrés | `GET /api/felhasznalo` | `200 OK`, `content` és `page` |
| Lekérés azonosító alapján | `GET /api/felhasznalo/{id}` | `200 OK`, `FelhasznaloResponse` |
| Profilmezők teljes cseréje | `PUT /api/felhasznalo/{id}` | `200 OK`, `FelhasznaloResponse` |
| Törlés azonosító alapján | `DELETE /api/felhasznalo/{id}` | `204 No Content` |
| Összes felhasználó törlése, órai példa | `DELETE /api/felhasznalo/felhasznalok` | `204 No Content` |

A korábbi `GET /api/felhasznalo/felhasznalok` cím a lapozott lista aliasa.
A hiányzó felhasználó lekérése, módosítása és törlése `404` választ ad.
A sikertelen névszűrés `200` és üres `content` lista.

Változások a korábbi példákhoz képest:

- A módosítás azonosítója az URL-be került; nincs `PUT /api/felhasznalo`.
- A törléshez nincs szükség teljes DTO-ra; a kérés törzsével működő `DELETE /api/felhasznalo` megszűnt.
- A korábbi adatot módosító `GET /api/felhasznalo/init` helyett a létrehozó POST használható.
- A listák válasza a lapozás miatt objektum, benne `content` tömbbel.
- A közös mentési DTO helyett külön create, update és response típusok vannak.

## Külön kérés- és válasz DTO-k

Létrehozáskor a `FelhasznaloCreateRequest` kötelező mezői: `nev`,
`szuletesiDatum`, `felhasznalonev`, `jelszo`, `email`. A `nem` opcionális.

```http
POST http://localhost:9090/api/felhasznalo
Content-Type: application/json

{
  "nev": "Minta Anna",
  "szuletesiDatum": "2000-01-01",
  "felhasznalonev": "anna01",
  "jelszo": "oratartasi-pelda",
  "email": "anna@example.com",
  "nem": null
}
```

A `Location` fejléc a létrehozott felhasználó lekérési címére mutat.
A `FelhasznaloResponse` mezői: `id`, `nev`, `szuletesiDatum`, `felhasznalonev`,
`email`, `nem`, `jogNev`. Jelszó nincs benne; jogosultság nélkül a `jogNev` értéke `null`.

A `FelhasznaloUpdateRequest` az összes módosítható profilmezőt tartalmazza.
A kötelező mezők elhagyása hibás PUT kérés, ez nem részleges PATCH művelet.

```http
PUT http://localhost:9090/api/felhasznalo/1
Content-Type: application/json

{
  "nev": "Minta Anna új neve",
  "szuletesiDatum": "2000-01-01",
  "felhasznalonev": "anna01",
  "email": "anna.uj@example.com",
  "nem": null
}
```

Az `id` és a jogosultság egyik kérésben sem adható meg; a jelszó csak létrehozáskor.
Az ismeretlen JSON-mezők `400` hibát okoznak, így például a módosító kérésbe tett
`id`, `jog` vagy `jelszo` sem kerül csendben elfogadásra.
A módosítás megőrzi a meglévő jelszót és jogosultságot.
A mintában a jelszó tárolása még a korábbi módon történik; hitelesítés és
`PasswordEncoder` bevezetése külön tananyagrész lehet.

## Validáció három szinten

1. **Kérés:** a `@Valid` és a Jakarta Validation annotációk ellenőrzik a DTO-t.
   A név legfeljebb 100, a felhasználónév 60, az e-mail 254, a jelszó 100,
   az opcionális `nem` 10 karakter. A születési dátum kötelező és múltbeli.
   A szolgáltatót a `spring-boot-starter-validation` adja. Hibás kérés: `400`.
2. **Üzleti szabály:** a service ellenőrzi az e-mail és a felhasználónév
   egyediségét. Módosításkor a saját rekordot kizárja az ellenőrzésből.
   Foglalt adat: `FoglaltFelhasznaloAdatException`, HTTP `409`.
3. **Adatbázis:** névvel ellátott UNIQUE kényszerek védenek akkor is, ha két
   párhuzamos kérés egyszerre átjut az előzetes ellenőrzésen.
   A `DataIntegrityViolationException` egységes, belső SQL-részletek nélküli `409` választ ad.

Az egyediség jelenleg a tárolt értékre vonatkozik; nincs külön e-mail-normalizálás.
Az adatbázis oszlophosszai és a kérés validációs korlátai összhangban vannak.

Órai feladat: küldjünk hibás e-mailt, majd kétszer azonos e-mailt különböző
felhasználónévvel. Hasonlítsuk össze a `400` és `409` válaszokat.

## Egységes hibakezelés

Az `ApiExceptionHandler` `@RestControllerAdvice` és `@ExceptionHandler`
segítségével központilag kezeli a saját kivételeket.
A `ResponseEntityExceptionHandler` kiterjesztése a Spring beépített hibáit
(például hibás JSON, rossz paramétertípus, nem támogatott HTTP-metódus) is lefedi.
A válasz `application/problem+json` típusú, RFC 9457 szerinti `ProblemDetail`.
Az `errors` kiterjesztés mezőnév–hibaüzenet párokat tartalmaz, egyébként üres lista.
A visszautasított mezőértékeket nem küldjük vissza, így a jelszó sem jelenik meg bennük.

```json
{
  "type": "urn:kiadaskezelo:problem:validacios-hiba",
  "title": "Érvénytelen kérés",
  "status": 400,
  "detail": "Egy vagy több mező értéke érvénytelen.",
  "instance": "/api/felhasznalo",
  "errors": [
    {"field": "email", "message": "must be a well-formed email address"}
  ]
}
```

A validációs üzenet nyelve a futási környezettől függhet.
A nem várt hibák `500` választ kapnak általános üzenettel, a részlet szerveroldali naplóba kerül.

## Tranzakció és módosítás

A service osztály alapértelmezetten `@Transactional(readOnly = true)`.
Az író metódusok saját `@Transactional` annotációval felülírják ezt.
A tranzakció a teljes üzleti műveletet fogja körbe: keresés, egyediségellenőrzés,
módosítás, adatbázis-írás és válaszkészítés.

Módosításkor az entitást az URL azonosítója alapján töltjük be. A MapStruct
`@MappingTarget` a már betöltött, managed példányt frissíti.
Az `id`, `jog` és `jelszo` célmezők explicit `ignore = true` beállítást kaptak.
A JPA dirty checking menti a változásokat; nincs új entitás és nincs `save()` hívás.
A `flush()` még a metóduson belül érvényesíti az adatbázis-kényszereket.
Ha ez hibát dob, a teljes tranzakció visszagördül.

A repository egyes metódusainak saját tranzakciója önmagában nem fogna össze
egy több repository-hívásból álló üzleti műveletet. A Spring a service proxyján
át érkező hívásra nyit tranzakciót; egy osztályon belüli saját metódushívás
nem hoz létre új tranzakciót az annotáció miatt.

## Lapozás és rendezés

```http
GET http://localhost:9090/api/felhasznalo?nev=anna&page=0&size=20&sort=nev,asc
```

A `nev` kis- és nagybetűtől független részszöveges keresés.
A lapszámozás nullától indul, az alapméret 20, a maximum 100.
Nagyobb méret kérésekor a méret 100-ra korlátozódik.
A Spring lapozási paraméterfeloldója a negatív lapszámot nullára, a hibás
számalakot vagy nem pozitív méretet az alapértékre igazítja.
Rendezhető mezők: `id`, `nev`, `szuletesiDatum`, `felhasznalonev`, `email`, `nem`.
Más mezőre rendezés `400` választ ad. A service az ID-t másodlagos rendezésként
hozzáadja, ha nincs már megadva, így azonos nevű elemek sorrendje is stabil.

A service és a controller `Page<FelhasznaloResponse>` típussal dolgozik.
A `WebConfig` `VIA_DTO` beállítása stabil JSON-választ készít:

```json
{
  "content": [],
  "page": {"size": 20, "number": 0, "totalElements": 0, "totalPages": 0}
}
```

## LAZY, N+1 és EntityGraph

A `FelhasznaloEntity.jog` kapcsolat `LAZY`. A DTO-ban megjelenő `jogNev`
eléréséhez azonban szükség van a jogosultságra. A lapozott, szűrt és ID szerinti
repository-lekérdezés `@EntityGraph(attributePaths = "jog")` segítségével tölti be.
Mivel to-one kapcsolatról van szó, a JOIN nem sokszorozza meg a lapozott felhasználósorokat.
A `spring.jpa.open-in-view=false` miatt a controller nem támaszkodhat egy
nyitva hagyott perzisztenciakontextusra; a DTO a service tranzakcióján belül készül el.

A `FelhasznaloFetchTests` három külön jogosultságú felhasználóval méri az SQL-ek számát:

| Lekérés | SQL-ek száma |
| --- | --- |
| Egyszerű JPQL, majd mindhárom LAZY jogosultság neve | 1 + 3 = 4 |
| Két felhasználós oldal EntityGraph-fal, összesen három találatból | 1 adatlekérdezés + 1 COUNT = 2 |

A különböző jogosultságok és a perzisztenciakontextus ürítése szükséges ahhoz,
hogy az első szintű cache ne fedje el az N+1 jelenséget.
Részleges vagy üres oldalnál a Spring Data bizonyos esetekben kihagyhatja a COUNT-ot.
A Hibernate statisztika csak a betöltési tesztekben aktív; órán a `show-sql`
segítségével a lekérdezések szövege is megfigyelhető.

## Mit ellenőriznek a tesztek?

- A létrehozás `201` válaszát és használható `Location` fejlécét.
- A hibás kérés `400`, a hiányzó felhasználó `404`, a foglalt adatok `409` státuszát.
- A hibák formátumát, mezőszintű üzeneteit és a beépített MVC-hibák kezelését.
- A jelszó hiányát a válaszokból, a tiltott bemeneti mezők elutasítását.
- Az ID-alapú névváltoztatást és a jelszó, valamint a jogosultság megőrzését.
- A késői adatbázis-ütközésnél az egész módosítás visszagörgetését.
- Két párhuzamos, azonos e-mailes létrehozásnál egy `201` és egy `409` választ.
- A lapozást, rendezést, névszűrést, üres találatot és oldalméretkorlátot.
- A `204` törlést és az N+1 megszüntetését a valós adatbázis-lekérdezések számával.

## Pénzügyi domain: táblák, entitások, repositoryk

A fotón megtervezett nyolc tábla megmaradt. Az ismétlődésekhez és a mentett
kalkulátortervekhez két kiegészítő tábla került be.

| Tábla | Entitás | Repository |
| --- | --- | --- |
| `FELHASZNALO` | `FelhasznaloEntity` | `FelhasznaloRepository` |
| `JOGOSULTSAG` | `JogEntity` | `JogRepository` |
| `FELH_KATEGORIA` | `FelhKategoriaEntity` | `FelhKategoriaRepository` |
| `KATEGORIA` | `KategoriaEntity` | `KategoriaRepository` |
| `SZAMLA` | `SzamlaEntity` | `SzamlaRepository` |
| `KARTYA` | `KartyaEntity` | `KartyaRepository` |
| `PENZMOZGAS` | `PenzmozgasEntity` | `PenzmozgasRepository` |
| `UTALAS` | `UtalasEntity` | `UtalasRepository` |
| `ISMETLODO_PENZMOZGAS` | `IsmetlodoPenzmozgasEntity` | `IsmetlodoPenzmozgasRepository` |
| `MEGTAKARITASI_TERV` | `MegtakaritasiTervEntity` | `MegtakaritasiTervRepository` |

Az `AuditEntity` közös `@MappedSuperclass`, nem külön tábla. A `@PrePersist`
és `@PreUpdate` automatikusan kezeli a `letrehozva` és `modositva` időbélyegeket.
A `FelhKategoriaId` `@Embeddable` kulcs, a kapcsolótábla `@EmbeddedId` és
`@MapsId` segítségével hivatkozik a felhasználóra és a kategóriára.

A kapcsolatok a hivatkozást birtokló oldalon szerepelnek, `LAZY` betöltéssel.
Nincs automatikus törlési kaszkád, amely egy felhasználó vagy számla törlésekor
észrevétlenül eltávolítaná a pénzügyi előzményeket. A hivatkozott rekord törlését
az idegen kulcs megakadályozza; archiváláshoz a meglévő `aktiv` mezők használhatók.

A típusok és állapotok `@Enumerated(EnumType.STRING)` mezők. A pénzösszegek
`BigDecimal` típusúak, `DECIMAL(19,4)` oszlopban. A pozitív összegeket,
az ismétlési intervallumot, a kártya lejárati hónapját és a célösszeg szükségességét
Bean Validation, illetve JPA `@CheckConstraint` adatbázis-kényszerek is védik.
Az `UTALAS.penzmozg_id` egyedi; egy pénzmozgáshoz legfeljebb egy utalási rekord tartozik.
Az `(ismetles_id, esedekesseg)` egyedi kulcs megakadályozza egy ismétlődés duplázását.

A `SZAMLA` tárolja a nyitóegyenleget és a pénznemet. A kártya ehhez kapcsolódik,
nem tart fenn külön egyenleget. Saját átvezetéskor a `PENZMOZGAS` adja a
forrásszámlát és összeget, az `UTALAS` a célszámlát és annak pénznemében a célösszeget.
Külső utalásnál saját célszámla helyett partneradatok szerepelnek.

A repositoryk tulajdonos szerinti, lapozott és időszakos lekérdezési példákat is
tartalmaznak. A `PenzmozgasRepository.findInPeriod` kezdő időpontja inkluzív,
végpontja exkluzív: `[tol, ig)`. A pénzmozgás listáihoz és az esedékes ütemezésekhez
`@EntityGraph` biztosítja a szükséges kapcsolatok betöltését.

Az entitásokhoz service-ek és kontrollerek tartoznak. A service kezeli a
tulajdonosi és üzleti ellenőrzéseket, a tranzakciókat és a DTO-képzést. A kontrollerek
validált kérésobjektumokat fogadnak, entitásokat nem küldenek vissza.

## Automatikus mintaadat-feltöltés

A `bootstrap/DemoAdatFeltolto.java` egy `CommandLineRunner`: az alkalmazás
elindulásakor, a Hibernate sémalétrehozása után automatikusan lefut.
A repositorykat használja, ezért a kapcsolatokat valódi generált azonosítókkal hozza létre.
A teljes feltöltés egy tranzakció; hiba esetén minden létrehozott adat visszagördül.

Alapértelmezésben engedélyezett:

```properties
app.demo-data.enabled=true
```

Kikapcsolható `app.demo-data.enabled=false` beállítással, az
`APP_DEMO_DATA_ENABLED=false` környezeti változóval, vagy futtatási paraméterrel:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.demo-data.enabled=false"
```

Ha már van felhasználó az adatbázisban, a teljes feltöltés kimarad. A feltöltő
nem töröl, nem ír felül felhasználói adatot, és ismételt meghívása nem dupláz.
A jelenlegi `create-drop` és memóriabeli H2 miatt rendes újraindításkor az adatbázis
újra létrejön, így a bemutatóadatok is újra elkészülnek.

| Adat | Darabszám |
| --- | ---: |
| Jogosultság | 2 |
| Felhasználó | 4 |
| Kategória | 8 |
| Felhasználó–kategória kapcsolat | 24 |
| Számla | 9 |
| Kártya | 4 |
| Pénzmozgás | 30 |
| Utalás / saját átvezetés | 9 |
| Ismétlődő ütemezés | 3 |
| Megtakarítási terv | 2 |

Felhasználók: `demo_admin`, `demo_anna`, `demo_bela`, `demo_csilla`.
A három normál felhasználónak fejenként készpénzes, HUF és EUR bankszámlája van.
Annának két kártyája kapcsolódik ugyanahhoz a forintszámlához.
Az e-mail-címek `example.test` végződésűek; az IBAN-ok szándékosan kitaláltak.
A kártyákból kizárólag azonosító metaadatokat tárolunk, teljes kártyaszám, CVV és PIN nélkül.
A demo felhasználók előre rögzített bcrypt-hasht kapnak; ez nem vezet be bejelentkezési funkciót.

A minták között szerepel munkabér, kártyás és készpénzes vásárlás, készpénzfelvétel,
forint–euró átvezetés rögzített bemutatóarány mellett, külső utalás, tervezett és
sztornózott kiadás. A havi albérlethez, előfizetéshez és ösztöndíjhoz két-két korábbi
előfordulás és egy következő esedékesség tartozik. A dátumok az indítás napjához
igazodnak, `Europe/Budapest` időzónával; az adatbázis időpontjai `Instant` értékek.
A feltöltő mintarekordokat hoz létre. Az ismétlődéseket egy külön, konfigurálható
feldolgozó kezeli; a banki utalások helyi nyilvántartási műveletek.

Például a H2 konzolban:

```sql
SELECT f.felhasznalonev, s.nev, s.penznem, s.nyito_egyenleg
FROM SZAMLA s
JOIN FELHASZNALO f ON f.id = s.felh_id
ORDER BY f.felhasznalonev, s.id;

SELECT p.id, s.nev AS szamla, p.tipus, p.osszeg, s.penznem,
       p.megnevezes, p.allapot, k.nev AS kategoria
FROM PENZMOZGAS p
JOIN SZAMLA s ON s.id = p.szamla_id
LEFT JOIN KATEGORIA k ON k.id = p.kat_id
ORDER BY p.datum, p.id;
```

A `test` profilban a feltöltés ki van kapcsolva, hogy a meglévő API-tesztek tiszta
adatbázissal dolgozhassanak. A `DomainPersistenceTests` külön visszakapcsolja,
és ellenőrzi az induláskori feltöltést, az ismételt futtatást, a kapcsolatok
helyességét, a repository-lekérdezéseket és a fontos adatbázis-kényszereket.

## Funkcionális REST API

A pénzügyi végpontok közös előtagja: `/api/felhasznalok/{felhasznaloId}`.
A korábbi felhasználó-CRUD cím `/api/felhasznalo` maradt.
Az összes kérés és válasz megtekinthető és kipróbálható a Swagger UI-ban.
Az `api/kiadaskezelo.http` fájl további, egymás után kipróbálható példákat tartalmaz.

**Ebben a változatban nincs hitelesítés vagy hozzáférés-védelem.** A kliens az URL-ben
adja meg a felhasználót. A service ellenőrzi, hogy a megadott számla, kártya,
pénzmozgás, ismétlődés és megtakarítási terv valóban ehhez a felhasználóhoz tartozik;
ez adatintegritási ellenőrzés, nem bejelentkezésen alapuló jogosultságkezelés.
A security bevezetésekor az URL-ből kapott tulajdonosazonosítót hitelesített
felhasználóval kell összekötni. A jogosultságok végpontjai jelenleg törzsadatkezelést végeznek.

Az alábbi címek a közös előtaghoz képest relatívak:

| Végpont | Műveletek |
| --- | --- |
| `/szamlak` | GET lapozott lista aktuális egyenlegekkel; POST létrehozás |
| `/szamlak/{id}` | GET részletek és egyenleg; PUT metaadatok; DELETE archiválás |
| `/szamlak/{id}/aktiv` | PATCH `{"aktiv": true/false}` |
| `/kartyak` | GET lapozott lista; POST kártya létrehozása |
| `/kartyak/{id}` | GET, PUT; DELETE kikapcsolás |
| `/kartyak/{id}/aktiv` | PATCH aktiválás/kikapcsolás |
| `/kategoriak` | GET saját kategória-hozzárendelések |
| `/kategoriak/{id}` | PUT `{"sajatElnevezes": "Bevásárlás", "aktiv": true}` |
| `/penzmozgasok` | GET szűrt, lapozott lista; POST kiadás vagy bevétel |
| `/penzmozgasok/{id}` | GET; PUT önálló tervezett tétel módosítása |
| `/penzmozgasok/{id}/konyveles` | POST könyvelés |
| `/penzmozgasok/{id}/sztorno` | POST sztornózás |
| `/penzmozgasok/{id}/kategoria` | PATCH `{"kategoriaId": 1}` |
| `/atvezetesek` | POST saját számlák közötti átvezetés |
| `/utalasok` | GET lapozott lista; POST külső utalás rögzítése |
| `/utalasok/{id}` | GET utalás és kapcsolódó pénzmozgás |
| `/ismetlesek` | GET lapozott lista; POST ütemezés |
| `/ismetlesek/{id}` | GET, PUT; DELETE leállítás |
| `/ismetlesek/{id}/aktiv` | PATCH indítás/leállítás |
| `/ismetlesek/{id}/feldolgozas` | POST esedékes tervezett tételek létrehozása |
| `/megtakaritasok` | GET lapozott lista; POST terv mentése |
| `/megtakaritasok/{id}` | GET, PUT, DELETE |
| `/megtakaritasok/{id}/kalkulacio` | GET mentett terv számítása |
| `/statisztika?tol=2026-10-01&ig=2026-10-31` | GET összesítés kategóriánként és pénznemenként |

Globális címek:

- `/api/kategoriak`: GET lapozott lista, opcionális `tipus=KIADAS/BEVETEL`; POST létrehozás.
- `/api/kategoriak/{id}`: GET, PUT, DELETE; használt kategória nem törölhető, típusa nem írható át.
- `/api/jogosultsagok` és `/api/jogosultsagok/{id}`: törzsadatok lapozott listázása és CRUD.
- `/api/megtakaritas-kalkulator`: POST számítás mentés nélkül.
- `/api/felhasznalo/{id}/aktiv`: PATCH fiók archiválása/visszaállítása.

A POST létrehozások `201` és használható `Location` fejlécet adnak. Hiányzó vagy más
tulajdonoshoz tartozó rekord `404`; hibás mező, kategóriatípus vagy dátum `400`;
elégtelen fedezet, inaktív erőforrás és tiltott állapotváltás `409`.
A DELETE műveletek `204` választ adnak. A hibák a már bemutatott `ProblemDetail` formát követik.

## Pénzmozgások üzleti szabályai

- A számla nyitóegyenlege nemnegatív. A pénznem, számlatípus, tulajdonos, nyitási dátum
  és nyitóegyenleg létrehozás után nem írható át; a PUT csak megnevezést és banki metaadatokat módosít.
- Készpénzes számlához nincs banknév vagy IBAN. Kártya csak bankszámlához kapcsolható,
  nem helyezhető át másik számlára, lejárt kártya nem hozható létre és nem aktiválható.
- A kiadás vagy bevétel kategóriájának aktívan a felhasználóhoz kell tartoznia, és
  típusának egyeznie kell a pénzmozgással. Kártya az érintett számlán kiadáshoz használható.
- Új tétel `TERVEZETT` vagy `KONYVELT` lehet. Jövőbeli könyvelés és számlanyitás előtti tétel hibás.
  Tervezett tétel nem befolyásolja az egyenleget és a statisztikát.
- Könyveléskor a kiadásnak és saját átvezetésnek van fedezetvizsgálata; nincs folyószámlahitel.
  A rendszer az aktuális egyenleget ellenőrzi, nem számol újra minden korábbi napi egyenleget.
- A már könyvelt tétel összege nem módosítható. A javítás sztornózással és új tétellel történik.
  A kategória önállóan módosítható, ha a tétel nem átvezetés és nincs sztornózva.
- A könyvelés és a sztornó ismételt hívása nem terheli újra a számlát. Sztornózott
  tétel nem könyvelhető vissza. Bevétel és átvezetés sztornózásánál a visszavont összegnek
  még rendelkezésre kell állnia az érintett számlán.
- Saját átvezetés két különböző, aktív saját számlát érint; azonos pénznemnél a két
  összeg megegyezik, különböző pénznemnél a kliens explicit célösszeget ad.
  Nincs automatikus árfolyamletöltés. Az átvezetés a kiadás/bevétel statisztikából kimarad.
- A külső utalás csak bankszámlán, kiadásként vagy bevételként, partneradatokkal rögzíthető.
  A megadott IBAN helyi referencia; a végpont nem indít banki műveletet.
- Számla nulla egyenleggel és aktív ismétlődés nélkül archiválható, ilyenkor kártyái
  is kikapcsolódnak. Archivált számlán nem könyvelhető tétel; a korábbi adatok olvashatók.
  A fiók kikapcsolása letiltja a pénzügyi adatok írását, a visszaolvasást megőrzi.

Az aktuális egyenleg = nyitóegyenleg + könyvelt bevételek − könyvelt kiadások
− innen induló átvezetések + ide érkező célösszegek. A számlalista két aggregált
lekérdezésből kapja a pénzmozgásváltozásokat, nem számlánként tölti le az összes tételt.
A könyvelő műveletek `@Transactional` határon belül futnak. A pénzmozgás állapotváltásakor
a rekord, pénzösszeg-változáskor az érintett számlák `PESSIMISTIC_WRITE` zárat kapnak.
A számlákat ID szerint növekvő sorrendben zároljuk az ellenirányú átvezetésekhez is.
A fedezetellenőrzés zárolás után történik. A tesztek két párhuzamos kiadást,
ugyanazon terv dupla könyvelését és ellenirányú átvezetést is ellenőriznek.

A pénzmozgáslista szűrői: `szamlaId`, `kategoriaId`, `tipus`, `allapot`, `tol`, `ig`.
Az időpontszűrők ISO-8601 `Instant` értékek, a tartomány `[tol, ig)`.
A statisztika dátumai ISO dátumok, és a megadott utolsó napot is tartalmazzák
az `app.time-zone` időzónában. Az összegek pénznemenként külön szerepelnek.
A lapozott végpontok alapmérete 20, maximuma 100; a rendezhető mezőket a service ellenőrzi.

## Ismétlődések feldolgozása

A sorozat naponta, hetente, havonta vagy évente, 1–120 egységes intervallummal futhat.
A kezdőnap nem előzheti meg a számlanyitást; a zárónap nem előzheti meg a kezdőnapot.
Az eredeti kezdőnap a naptári számítás horgonya: január 31. után február vége,
majd március 31. következik. Az éves február 29. szökőévben visszaáll.

A feldolgozás kizárólag az esedékes napokra készít **tervezett** pénzmozgást,
ezek könyvelése a pénzmozgás végpontján külön történik. A már létrehozott előfordulást
nem duplázza, a sztornózott előfordulást sem állítja vissza. A sablon módosítása nem
írja át a korábbi tételeket; használt sorozat ritmusához új sorozat indítható.
Sorozatonként legfeljebb 120 esedékességet dolgoz fel egy hívás; a következő hívás folytatja.
Zárónap után az ütemezés automatikusan leáll. Aktív sorozat kategóriája nem kapcsolható ki.

```properties
app.time-zone=Europe/Budapest
app.recurring.enabled=true
app.recurring.delay-ms=60000
app.recurring.initial-delay-ms=30000
```

Az automatikus feldolgozó alapból 30 másodperc után indul, majd az előző futás
befejezésétől számított percenként dolgozik. Futásonként legfeljebb 100 aktív,
aktív fiókhoz és számlához tartozó sorozatot választ ki. Minden sorozat külön
tranzakcióban fut, egy hibás sorozat nem gördíti vissza a többit.
A sorozatzár és az adatbázis egyedi kulcsa a párhuzamos feldolgozást is védi.
A `test` profilban az automatikus időzítés ki van kapcsolva; a kézi végpont ugyanazt a service-t használja.

## Megtakarítási kalkuláció

A kalkulátor nominális éves kamatot kap százalékban, és annak tizenkettedével,
havi tőkésítéssel számol. A havi befizetés a hónap végén érkezik:

```text
új egyenleg = korábbi egyenleg × (1 + éves kamat / 1200) + havi befizetés
```

A számítás `BigDecimal` és `DECIMAL128` pontosságú, a válasz pénzösszegei négy tizedesre,
`HALF_EVEN` szabállyal kerekítettek. A nulla kamat is működik; a válasz havi bontást,
összes befizetést, kamatot, végeredményt és opcionálisan a célhoz hiányzó összeget ad.
Futamidő: 1–1200 hónap; éves kamat: 0–100%; a befizetések nemnegatívak.
A számítás nem terhel számlát. A mentett terv ugyanilyen bemeneti adatokat tárol,
a számított eredményt minden lekéréskor újra előállítja.
