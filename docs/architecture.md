# Architecture

The app is deliberately split into layers so that the user interface, the
business rules and the storage format can change independently. There are two
user interfaces on the same services: the console
([App.java](../src/main/java/patients/App.java) → `ConsoleUI`) and a Spring Boot
REST API ([WebApp.java](../src/main/java/patients/web/WebApp.java) → the
controllers in `patients.web.api`).

Both decide on storage the same way, through
[Backend.java](../src/main/java/patients/Backend.java): it reads
`ZORG_FIREBASE_URL`, `FIREBASE_AUTH_EMULATOR_HOST` and `ZORG_FIREBASE_API_KEY`
(from `System.getenv` in the console, from Spring's `Environment` in the web
app) and builds either the JSON repositories or a `FirebaseAuth` plus Firebase
repositories that use its token. The console then wires one set together:

```java
PatientService service = new PatientService(repositories.patients());
UserService userService = new UserService(repositories.users());
ConsoleUI ui = new ConsoleUI(service, userService, auth);
ui.run();
```

`auth` is `null` in JSON mode, which makes the UI show the account list instead
of a sign-in prompt.

```text
ConsoleUI ─────────┐
 (I/O)             ├──►  PatientService  ──►  PatientRepository (interface)
web controllers ───┤    (rules, user per call)    ├──► JsonPatientRepository (patients.json)
 (JSON over HTTP)  │                               └──► FirebasePatientRepository ─┐
   │               └──►  UserService     ──►  UserRepository (interface)           ├─► FirebaseClient (REST)
   │                     (user lookups)        ├──► JsonUserRepository (users.json)        ▲ idToken
   │                                           └──► FirebaseUserRepository ────────┘       │
   ├──────►  SignIn / Authenticator  ──►  FirebaseAuth (Identity Toolkit REST) ────────────┘
   │
   └──────►  ProfessionPermissions / FieldPermissions  (profession → actions / fields)
                       │
                       └──► FirebaseRules ──► database.rules.json (the same rules, on the server)

Patient, User, Medication, MedicationCatalog, Profession, Action, PatientField  (model types, used everywhere)
```

Dependencies point in one direction only: the UIs know the services, the
services know the repository *interfaces*, and only `App`/`Backend` (and the web
app's `BackendConfig`) know which implementation is used. Nothing below the UIs
reads from `System.in`, writes to `System.out` or knows about HTTP.

## The layers

### `model` — [Patient](../src/main/java/patients/model/Patient.java)

An immutable `record` holding an `id`, `firstName`, `lastName`, `birthDate`, `weight`, `height`,
`address`, a `careTeam` and a list of
[Medication](../src/main/java/patients/model/Medication.java) records (`name`,
`dosage`, `frequency`). The `name` is the label of a
[MedicationCatalog](../src/main/java/patients/model/MedicationCatalog.java) entry,
each of which has a `MedicationType` (pill, injection or ointment). The compact constructor turns a missing (`null`) care
team or medication list into an empty one, so older data files still load, and
stores unmodifiable copies. Its behaviour is `age()`, derived from the date of
birth and today's date, `bmi()`, derived from weight and height, and
`hasMember(userId)`. Age and BMI are not stored, so they can never drift out of
sync with the fields they are computed from (a stored age would be wrong after
the next birthday).

The care team is a `Map<String, Boolean>` from user id to `true`, the usual way
to store a set in Firebase. `hasMember` only counts entries that are `true`.
Later, the database security rules can check `careTeam/<uid>` directly.

[User](../src/main/java/patients/model/User.java) is a record of `id`, `name` and
`profession`. The profession decides what a user may do; the care team decides
which patients they may do it to.

Records were chosen for the model because Gson can serialize them directly and
because patients are values: to "change" one you create a new one with the
`withX(...)` methods, which keep the `id`. The id is what identifies a patient
across saves and sessions; it is `null` until the service adds the patient.

Two enums sit next to it, each with a human-readable `label` that the UI prints:

- [Profession](../src/main/java/patients/model/Profession.java): `DOCTOR`, `PHARMACIST`,
  `PHYSIOTHERAPIST`, `DENTIST`. These are the roles a user can log in as.
- [Action](../src/main/java/patients/model/Action.java): `VIEW_LIST`, `ADD_PATIENT`,
  `EDIT_PATIENT`. These are the things a user can do from the main menu.

### `repository` — storage

[PatientRepository](../src/main/java/patients/repository/PatientRepository.java) is a
per-record contract keyed by patient id:

```java
List<Patient> findAll();
Optional<Patient> findById(String id);
void save(Patient patient);   // insert or replace by id
```

Saving one patient at a time (instead of the whole list) is what lets a shared
database work later: a session only writes the patients it actually changed.

[JsonPatientRepository](../src/main/java/patients/repository/JsonPatientRepository.java)
implements it on top of a single JSON file that already has the shape of the
future Firebase Realtime Database tree:

```json
{ "patients": { "<id>": { "firstName": "...", ... } } }
```

- The map key is the patient's id. On load each patient gets its id from the
  key (`withId(key)`); on save the record is written without an `id` field, so
  the id is never stored twice.
- `save()` reads the file, puts the one patient in the map and writes the file
  back. A missing or empty file counts as no patients.
- A file in any other shape (such as the old `{ "<group key>": [ ... ] }`) is
  refused with an `UncheckedIOException`. Because `save()` reads first, it
  refuses too, so an old file is never silently overwritten.
- Gson is set up once in [GsonFactory](../src/main/java/patients/repository/GsonFactory.java),
  which registers a `LocalDateAdapter` that writes dates as ISO strings
  (`"1998-03-14"`), because Gson cannot reflect into `java.time` classes on
  modern JDKs. The Firebase repository will reuse the same setup.
- [Ids](../src/main/java/patients/repository/Ids.java)`.newId()` generates ids from
  `SecureRandom` (9 random bytes, URL-safe Base64, no padding → 12 characters).
  They contain none of `. $ # [ ] /`, so they are valid Firebase keys.
- I/O failures are wrapped in `UncheckedIOException` with the file path in the
  message, so the interface stays free of checked exceptions and the UI can
  decide how to report them.

[UserRepository](../src/main/java/patients/repository/UserRepository.java) is the
read-only counterpart for accounts (`findAll()`, `findById(id)`).
[JsonUserRepository](../src/main/java/patients/repository/JsonUserRepository.java)
reads the committed `users.json`, shaped `{ "users": { "<id>": { "name": ..., "profession": ... } } }`.
Both JSON repositories read their section through the package-private
[JsonFiles](../src/main/java/patients/repository/JsonFiles.java)`.readSection(...)`, so
the format checks above apply to both files.

`PatientRepository` also has `findAllForUser(userId)`. By default it filters
`findAll()` by care team membership, which is what the JSON repository uses.

#### Firebase Realtime Database

[FirebaseClient](../src/main/java/patients/repository/FirebaseClient.java) talks to
the database's REST API with the JDK's `java.net.http.HttpClient`, so there is no
Firebase SDK dependency. `get`, `put` and `patch` map to `GET`, `PUT` and `PATCH`
on `<database url>/<path>.json`. Any query in the configured URL (such as the
emulator's `?ns=...`) is kept, and the user's ID token is added as `auth=...`.
The token comes from a `Supplier` (in the app, `FirebaseAuth::idToken`), so it
can change after login and after a refresh. `FirebaseClient.admin(url, token)`
instead sends `Authorization: Bearer <token>`, which bypasses the security
rules; only the migration and the emulator tests use it. A missing value
comes back as `JsonNull`. A non-2xx response or a network error becomes an
`UncheckedIOException` carrying the response body, so the UI reports it like any
other storage error.

The tree looks like this:

```text
/users/<userId>                 { name, profession }
/patients/<patientId>           { firstName, ..., careTeam: { <userId>: true }, medications: [...] }
/userPatients/<userId>/<patientId>: true
```

[FirebasePatientRepository](../src/main/java/patients/repository/FirebasePatientRepository.java):

- `save()` first reads the stored patient, then sends one multi-path `PATCH`
  on the root. A new patient is written whole to `/patients/<id>`; for an
  existing one only the fields that changed are written, as
  `/patients/<id>/<field>`. That is what lets the security rules check each
  field against your profession: a dentist editing the address never writes
  `weight`. The same request writes an index entry
  `/userPatients/<member>/<id>` for every care team member, and deletes
  (`null`) the entries of members who were removed, so the patient and its
  index are updated together.
- `findAllForUser()` reads `/userPatients/<userId>` and then fetches each patient.
  It skips entries whose patient is gone or no longer lists the user. Reading
  the whole of `/patients` is not an option once there are security rules,
  because rules don't filter results: a read fails as soon as one patient in it
  is off-limits. `PatientService` passes the fields the user may view. If that is
  not every field (a pharmacist), the whole patient can't be read, so it fetches
  the care team and then each of those fields separately.
- `findAll()` still reads all of `/patients`. It is meant for admin tasks such
  as the migration, not for the app.

[FirebaseUserRepository](../src/main/java/patients/repository/FirebaseUserRepository.java)
reads `/users`. Firebase returns keys in alphabetical order, so `UserService` and
`PatientService` sort what they load. That keeps the numbered lists in the UI
the same on both backends. Its `save()` (a `PUT` on `/users/<id>`) is only used
by the migration. It isn't on the `UserRepository` interface, because the app
never writes users.

[MigrateToFirebase](../src/main/java/patients/MigrateToFirebase.java) is a second
`main`, run with `./gradlew migrateToFirebase`. It reads `users.json` and
`patients.json` through the JSON repositories and saves every record through the
Firebase ones, so the `/userPatients` index is built by the same `save()` the app
uses. Because every write is by id, running it again gives the same data. Before
writing anything it collects a warning for each care team member who isn't a
known user.

#### Firebase Auth

[FirebaseAuth](../src/main/java/patients/auth/FirebaseAuth.java) signs in with
the Identity Toolkit REST endpoint `accounts:signInWithPassword` and returns the
login's uid (`localId`), which is also the user's key in `/users`: a login's uid
*is* the account id that care teams refer to. It keeps the ID token (valid for an
hour) and the refresh token. `idToken()` refreshes through
`securetoken.googleapis.com` once the token is within a minute of expiring.
Wrong credentials become a `SecurityException` ("Wrong email or password"), as
does Firebase locking an account after too many failed attempts; anything else
becomes an `UncheckedIOException`. `FirebaseSignIn` replaces that exception's
message with a generic one, so the raw Firebase response only reaches the log. `forEmulator(host)` points it at
the Auth emulator, `forProject(apiKey)` at Google.

The UI only sees the `Authenticator` interface (`signIn(email, password)`
returns a user id). Because `/users` is only readable when signed in, the UI
signs in first and loads the accounts afterwards.

#### Security rules

[FirebaseRules](../src/main/java/patients/FirebaseRules.java) generates
[database.rules.json](../database.rules.json) from `ProfessionPermissions` and
`FieldPermissions`, so the database enforces the same permissions as
`PatientService`. Run it with `./gradlew generateFirebaseRules`;
`FirebaseRulesTest` fails while the committed file is out of date. The rules:

- `/patients/<id>`: readable as a whole by care team members whose profession
  may view every field (and readable while it doesn't exist, so `save()` can
  check whether a patient is new). Each field also has its own `.read` rule:
  on the team and allowed to view that field (`FieldPermissions.canView`). The
  care team is readable by its members. So a pharmacist reads the name, date of
  birth, medication and care team one by one, and is denied the address, weight
  and height even when querying the database directly. Creating one
  needs the `ADD_PATIENT` action and yourself on its team. After that, each
  field has its own `.write` rule: you must be on the team and your profession
  must have `EDIT_PATIENT` and be allowed to edit that field. The care team has
  no write rule, so it is fixed once created. Each field is validated (types,
  names not empty, weight and height above 0, ISO dates), and unknown fields are
  rejected.
- `/userPatients/<userId>`: readable only by that user. An entry can only be
  written by a member of the patient's team, and must match that team.
- `/users`: readable by anyone signed in, writable by nobody (only the admin
  token used by the migration).

`FirebaseRulesEmulatorTest` (tag `emulator`, run with `./gradlew emulatorTest`)
loads the generated rules into the emulator and checks them as real signed-in
users.

Swapping storage means writing another `PatientRepository` / `UserRepository` (a
database, a CSV file, an in-memory fake for tests) and choosing it in `App`.

### `service` — [PatientService](../src/main/java/patients/service/PatientService.java) and [ProfessionPermissions](../src/main/java/patients/service/ProfessionPermissions.java)

`PatientService` is **stateless**: it holds only its repository and a `Clock`,
and every method takes the `User` who is acting. That is what lets one instance
serve many web requests from different users at the same time. Checking a
password is the UI's job (via `Authenticator` or `SignIn`) before it passes a
user in. The user's profession selects which permissions apply and their id
selects which patients.

- `getAll(user)` reads the user's patients from the repository
  (`findAllForUser(...)`), keeps only those whose care team includes them, and
  sorts them by last name, then first name. The sort keeps the numbered lists in
  the console stable whatever order the storage returns. Nothing is cached, so
  changes made by someone else show up on the next call.
- `get(user, id)` returns one of those patients, or throws
  `PatientNotFoundException` (an `IllegalArgumentException`). It goes through
  `getAll` on purpose: on Firebase, reading a patient you aren't on the team of
  is refused by the rules, while going through your own index simply doesn't
  find it.
- `add(user, patient)` validates the patient, adds the user to its care team,
  gives it a new id if it has none, saves it straight away and returns the
  saved patient.
- `update(user, patient)` saves a changed version of an existing patient. It
  looks up the stored version by id (`PatientNotFoundException` if there is
  none) and refuses with `SecurityException` if the user is not on its care
  team. It then compares every `PatientField` (via `PatientField.valueOf`). If
  any changed field is not editable by the user's profession
  (`FieldPermissions.canEdit`), or the care team changed, it throws
  `SecurityException` and saves nothing. The UIs only offer editable fields, but
  the service checks every field regardless.

Every method is guarded by `requirePermission(user, action)`, which throws
`IllegalStateException` if the user is `null` and `SecurityException` if the
profession may not perform that action. `add` and `update` also validate the
patient: first and last name not blank, a date of birth that is not in the
future (by the injected `Clock`, so tests can fix "today"), weight and height
above 0, and every medication named after a `MedicationCatalog` entry (`update`
only checks this when the medication list changed). Otherwise they throw
`IllegalArgumentException`; this also keeps `bmi()` finite.

[UserService](../src/main/java/patients/service/UserService.java) reads the
accounts sorted by profession and then name: `getAll()` for the login list,
`getByProfession(...)` for picking a care team, and `findById(...)`. It doesn't
cache either; the console keeps the list it loaded at sign-in.

`ProfessionPermissions` is a static `EnumMap<Profession, Set<Action>>`, the single
source of truth for who may do what. `isAllowed(profession, action)` answers one
question; `getAllowedActions(profession)` returns the whole set, which the UI uses
to build the menu. The pharmacist can only view the list.

Checking permissions in the service as well as filtering the menu in the UI is
deliberate. The menu hides what you can't do, and the service enforces it even if
a future UI (or a bug) offers the action anyway.

Every change is persisted the moment it is made, so nothing is lost if the app
is interrupted, and there is no end-of-session save that could overwrite what
another session wrote in the meantime.

### `ui` — [ConsoleUI](../src/main/java/patients/ui/ConsoleUI.java)

All console interaction and all parsing. `run()` is the whole session:

1. Without an `Authenticator`, `chooseAccount()` loads the accounts (with none,
   the app says so and stops) and `askUser()` re-prompts until a valid number is
   chosen. With one, `signIn()` asks for email and password (three attempts),
   then loads the accounts and looks up the signed-in uid. Either way the UI
   keeps the user and the account list in its own fields, and passes the user to
   every service call.
2. `mainMenu(profession)` loops over a numbered menu built from
   `ProfessionPermissions.getAllowedActions(...)`, with `0` to quit. The chosen
   `Action` is dispatched in a `switch`. A `SecurityException` or
   `IllegalArgumentException` from the service is printed, and so is an
   `UncheckedIOException` from a save; either way the loop continues.

Storage problems therefore degrade to a message instead of a stack trace.

The private `readLine` / `readInt` / `readDouble` helpers keep the retry logic in
one place: the numeric ones loop until they parse successfully, and `readDouble`
normalizes a comma to a dot so both decimal conventions work. `printPatients()`
is the only place that formats output.

### `web` — the Spring Boot API

[WebApp](../src/main/java/patients/web/WebApp.java) is a `@SpringBootApplication`
in `patients.web`, so component scanning only picks up the web classes; the
layers above stay plain Java. `./gradlew bootRun` starts it, and `bootJar`
builds one executable jar (both have their main class set in
`build.gradle.kts`, because the `application` plugin's main class is the
console).

- **Wiring.** [BackendConfig](../src/main/java/patients/web/BackendConfig.java)
  defines two beans: a `Backend` read from Spring's `Environment` (so the same
  environment variables work, and tests can override them as properties), and a
  `SignIn` for the chosen storage. In Firebase mode it refuses to start without
  sign-in settings. `zorg.data-dir` (`ZorgProperties`) says where the JSON files
  are.
- **Sign-in and sessions.** [SignIn](../src/main/java/patients/web/session/SignIn.java)
  turns a login request into a `Workspace` record: the user plus the
  `PatientService` and `UserService` to use for them. `LocalSignIn` (JSON) looks
  the account up by id and shares one pair of services, since they are
  stateless. `FirebaseSignIn` signs in with email and password and builds a new
  `FirebaseAuth`, `FirebaseClient` and services *per session*, so every database
  request carries that user's ID token and the security rules apply per user.
  `AuthController` stores the workspace in the `HttpSession`, changing the
  session id on login against session fixation. `/api/me` and the login response
  include `sessionTimeoutSeconds`; the frontend checks `/api/me` once that much
  time has passed without a request, and returns to the login page on any 401.
- **Login throttling.** [LoginThrottle](../src/main/java/patients/web/session/LoginThrottle.java)
  wraps every sign-in. Within a 15-minute window it allows 5 failed attempts per
  account (email or user id) and 50 per client address; after that login
  answers 429 with `Retry-After`, without contacting Firebase. Attempts are
  counted before signing in, so parallel requests can't slip past the limit; a
  success clears the account and doesn't count for the address, and errors that
  aren't wrong credentials (Firebase unreachable) don't count. Counts are kept in
  memory, so a restart clears them. Behind a reverse proxy every request has the
  proxy's address, so set `server.forward-headers-strategy=native` there.
- **Workspace parameter.** [WorkspaceArgumentResolver](../src/main/java/patients/web/session/WorkspaceArgumentResolver.java)
  lets a controller method declare a `Workspace` parameter; without a signed-in
  session it throws `NotSignedInException` (401), so no controller has to check.
- **CSRF.** The session cookie is `SameSite=Strict`, and
  [ClientHeaderInterceptor](../src/main/java/patients/web/session/ClientHeaderInterceptor.java)
  rejects `POST`/`PATCH` under `/api` without `X-Zorg-Client: web`. A form on
  another site can't set that header, and a cross-site `fetch` with it needs a
  CORS preflight that the app never allows. This avoids pulling in Spring
  Security for one check.
- **Controllers and DTOs** (`patients.web.api`). Controllers only translate
  between JSON and service calls. Request and response bodies are records, kept
  separate from the model:
  - `PatientResponse.of(...)` leaves out the fields the user's profession may not
    view (`FieldPermissions.getViewableFields`). In Firebase mode the database
    rules already keep those fields from being read; in JSON mode this is the
    only place they are hidden. The care team is returned with names.
  - `PatientChanges` is the `PATCH` body: a field that is absent (`null`) stays
    as stored, so a client only sends what it changes and never needs fields it
    can't see. The merged patient then goes through `update`, which checks every
    changed field.
  - `NewPatientRequest` takes the care team as a list of user ids, which must
    exist.
  - `MeResponse` and `ConfigResponse` tell a frontend which actions and fields to
    offer, and their labels.
- **Errors.** [ApiExceptionHandler](../src/main/java/patients/web/ApiExceptionHandler.java)
  extends Spring's `ResponseEntityExceptionHandler`, so framework errors (bad
  JSON, unknown URL) and the app's exceptions all come back as problem details:

  | Exception | Status |
  | --- | --- |
  | `NotSignedInException`, `SignInFailedException` | 401 |
  | `SecurityException` | 403 |
  | `PatientNotFoundException` | 404 |
  | `IllegalArgumentException`, unreadable JSON | 400 |
  | `UncheckedIOException` (storage) | 502 |

Jackson writes the API's JSON (records and ISO dates out of the box); Gson stays
inside the repositories for the storage format.

## Where to add things

| Change | Where it goes |
| --- | --- |
| New patient field | `Patient` record (and its `withX` methods) + the prompts in `readPatient()`; if users should see or edit it, a `PatientField` constant (label, JSON key, `valueOf` case) and an entry in `FieldPermissions`; for the web API, the field in `PatientResponse`, `NewPatientRequest` and `PatientChanges`. Old JSON records simply leave it at its default |
| New menu action (search, edit, delete) | a new `Action` constant; grant it to professions in `ProfessionPermissions`; a guarded method on `PatientService` (`requirePermission(user, ...)`); a `case` in `ConsoleUI.mainMenu()` and an endpoint in a controller under `patients.web.api` |
| New endpoint | a method on a `@RestController` in `patients.web.api`; take a `Workspace` parameter if it needs a signed-in user, and throw the usual exceptions (`ApiExceptionHandler` maps them); a `@WebMvcTest` in `src/test/java/patients/web` |
| New profession | a new `Profession` constant, its entries in `ProfessionPermissions` and `FieldPermissions`, and accounts with it in `users.json`. The add-patient flow asks for one team member per profession automatically |
| New user account | an entry in `users.json`; on Firebase also a login whose uid is that key (`scripts/create-emulator-accounts.sh` for the emulator) |
| Change who may do what | `ProfessionPermissions` / `FieldPermissions`. The menu and the service checks read from them; then run `./gradlew generateFirebaseRules` so the database rules follow |
| Different storage (DB, CSV) | new `PatientRepository` / `UserRepository` implementations (reuse `GsonFactory` for JSON); choose them in `Backend` (and a `SignIn` for them in `BackendConfig`) |
| New data in Firebase | the record type (Gson maps it); if users need to find it by something other than id, an index like `/userPatients` written in the same multi-path `PATCH` |
| Validation | `PatientService.validate()` if it is a rule about patients (both UIs get it); `ConsoleUI` if it is about re-prompting input |
| Tests | under `src/test/java`, mirroring `src/main/java`. [InMemoryPatientRepository](../src/test/java/patients/testing/InMemoryPatientRepository.java) and `InMemoryUserRepository` let the services and the web controllers be tested without touching the filesystem; pass a user who is on the patient's care team. The web tests import `TestWebConfig`, which signs in against them. `./scripts/web-smoke-test.sh` drives the real web server with `curl`. `FakeRealtimeDatabase` (a JDK `HttpServer` that mimics the REST API) tests the Firebase classes offline; `ZORG_FIREBASE_URL=<emulator> ./scripts/smoke-test.sh` runs the end-to-end test against the real emulator |
