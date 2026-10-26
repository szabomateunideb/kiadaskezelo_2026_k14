# Kiadáskezelő – backend órai mintaprojekt

Java 25, Spring Boot 4.1.1, Spring MVC, Spring Data JPA, MapStruct és H2.
A jelenlegi példák a felhasználókezelésen mutatják be a backend működését.

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
