# Architecture

The app is deliberately split into four layers so that the console, the business
rules and the storage format can change independently. Everything is wired
together once, in [App.java](../src/main/java/patients/App.java):

it picks the Firebase repositories when `ZORG_FIREBASE_URL` is set and the JSON
ones otherwise, then wires them into the services and the UI:

```java
PatientService service = new PatientService(repository);
UserService userService = new UserService(userRepository);
ConsoleUI ui = new ConsoleUI(service, userService);
ui.run();
```

```text
ConsoleUI  ──►  PatientService  ──►  PatientRepository (interface)
 (I/O)         (list + rules)             ├──► JsonPatientRepository (patients.json)
   │                                      └──► FirebasePatientRepository ─┐
   ├──────►  UserService     ──►  UserRepository (interface)              ├─► FirebaseClient (REST)
   │          (user lookups)              ├──► JsonUserRepository (users.json)
   │                                      └──► FirebaseUserRepository ────┘
   │
   └──────►  ProfessionPermissions / FieldPermissions  (profession → actions / fields)

Patient, User, Medication, Profession, Action, PatientField  (model types, used everywhere)
```

Dependencies point in one direction only: the UI knows the service, the service
knows the repository *interface*, and only `App` knows which implementation is
used. Nothing below the UI reads from `System.in` or writes to `System.out`.

## The layers

### `model` — [Patient](../src/main/java/patients/model/Patient.java)

An immutable `record` holding an `id`, `firstName`, `lastName`, `birthDate`, `weight`, `height`,
`address`, a `careTeam` and a list of
[Medication](../src/main/java/patients/model/Medication.java) records (`name`,
`dosage`, `frequency`). The compact constructor turns a missing (`null`) care
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
emulator's `?ns=...`) is kept, and the auth token is added as `auth=...`. The
token comes from a `Supplier`, so it can change after login. A missing value
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

- `save()` is one multi-path `PATCH` on the root. It writes `/patients/<id>` and
  an index entry `/userPatients/<member>/<id>` for every care team member. It
  also deletes (`null`) the entries of members who were removed. A single
  request means the patient and its index are updated together.
- `findAllForUser()` reads `/userPatients/<userId>` and then fetches each patient.
  It skips entries whose patient is gone or no longer lists the user. Reading
  the whole of `/patients` is not an option once there are security rules,
  because rules don't filter results: a read fails as soon as one patient in it
  is off-limits.
- `findAll()` still reads all of `/patients`. It is meant for admin tasks such
  as the migration, not for the app.

[FirebaseUserRepository](../src/main/java/patients/repository/FirebaseUserRepository.java)
reads `/users`. Firebase returns keys in alphabetical order, so `UserService` and
`PatientService` sort what they load. That keeps the numbered lists in the UI
the same on both backends.

Swapping storage means writing another `PatientRepository` / `UserRepository` (a
database, a CSV file, an in-memory fake for tests) and choosing it in `App`.

### `service` — [PatientService](../src/main/java/patients/service/PatientService.java) and [ProfessionPermissions](../src/main/java/patients/service/ProfessionPermissions.java)

`PatientService` keeps a cached list of patients for the UI to show, and the
user currently logged in:

- `login(User)` sets the current user. There is no password check; the user's
  profession selects which permissions apply and their id selects which
  patients.
- `load()` clears the cache and refills it with the logged-in user's patients
  (`repository.findAllForUser(...)`), sorted by last name, then first name. The
  sort keeps the numbered lists in the UI stable whatever order the storage
  returns. It needs a logged-in user, so it is called after `login`.
- `add(Patient)` validates the patient, adds the current user to its care team,
  gives it a new id if it has none, saves it through the repository straight
  away and appends it to the cache.
- `update(Patient)` saves a changed version of an existing patient. It looks up
  the stored version by id and refuses with `SecurityException` if the current
  user is not on its care team. It then compares every `PatientField` (via
  `PatientField.valueOf`). If any changed field is not editable by the current
  profession (`FieldPermissions.canEdit`), or the care team changed, it throws
  `SecurityException` and saves nothing. The UI only offers editable fields, but
  the service checks every field regardless.
- `getAll()` returns an unmodifiable list of only the patients whose care team
  includes the current user.

`add`, `update` and `getAll` are guarded by `requirePermission(Action)`, which
throws `IllegalStateException` if nobody is logged in and `SecurityException`
if the current profession may not perform that action. `add` and `update` also
validate the patient (first and last name not blank, weight and height above 0)
and throw `IllegalArgumentException` otherwise; this also keeps `bmi()` finite.
`load` is not guarded.

[UserService](../src/main/java/patients/service/UserService.java) caches the
accounts for the UI, sorted by profession and then name: `getAll()` for the
login list, `getByProfession(...)` for picking a care team, and `findById(...)`
to show team members by name.

`ProfessionPermissions` is a static `EnumMap<Profession, Set<Action>>`, the single
source of truth for who may do what. `isAllowed(profession, action)` answers one
question; `getAllowedActions(profession)` returns the whole set, which the UI uses
to build the menu. The pharmacist currently maps to an empty set.

Checking permissions in the service as well as filtering the menu in the UI is
deliberate. The menu hides what you can't do, and the service enforces it even if
a future UI (or a bug) offers the action anyway.

Every change is persisted the moment it is made, so nothing is lost if the app
is interrupted, and there is no end-of-session save that could overwrite what
another session wrote in the meantime.

### `ui` — [ConsoleUI](../src/main/java/patients/ui/ConsoleUI.java)

All console interaction and all parsing. `run()` is the whole session:

1. `loadUsers()` loads the accounts; with none, the app says so and stops.
2. `askUser()` lists the accounts and re-prompts until a valid number is
   chosen, then calls `service.login(...)`.
3. `loadPatients()` loads the file, printing a warning on `UncheckedIOException`.
4. `mainMenu(profession)` loops over a numbered menu built from
   `ProfessionPermissions.getAllowedActions(...)`, with `0` to quit. The chosen
   `Action` is dispatched in a `switch`. A `SecurityException` or
   `IllegalArgumentException` from the service is printed, and so is an
   `UncheckedIOException` from a save; either way the loop continues.

Storage problems therefore degrade to a message instead of a stack trace.

The private `readLine` / `readInt` / `readDouble` helpers keep the retry logic in
one place: the numeric ones loop until they parse successfully, and `readDouble`
normalizes a comma to a dot so both decimal conventions work. `printPatients()`
is the only place that formats output.

## Where to add things

| Change | Where it goes |
| --- | --- |
| New patient field | `Patient` record (and its `withX` methods) + the prompts in `readPatient()`; if users should see or edit it, a `PatientField` constant with its `valueOf` case and an entry in `FieldPermissions`. Old JSON records simply leave it at its default |
| New menu action (search, edit, delete) | a new `Action` constant; grant it to professions in `ProfessionPermissions`; a guarded method on `PatientService` (`requirePermission(...)`); a `case` in `ConsoleUI.mainMenu()` |
| New profession | a new `Profession` constant, its entries in `ProfessionPermissions` and `FieldPermissions`, and accounts with it in `users.json`. The add-patient flow asks for one team member per profession automatically |
| New user account | an entry in `users.json` |
| Change who may do what | `ProfessionPermissions` only. The menu and the service checks both read from it |
| Different storage (DB, CSV) | new `PatientRepository` / `UserRepository` implementations (reuse `GsonFactory` for JSON); choose them in `App` |
| New data in Firebase | the record type (Gson maps it); if users need to find it by something other than id, an index like `/userPatients` written in the same multi-path `PATCH` |
| Validation | `PatientService.validate()` if it is a rule about patients; `ConsoleUI` if it is about re-prompting input |
| Tests | under `src/test/java`, mirroring `src/main/java`. [InMemoryPatientRepository](../src/test/java/patients/service/InMemoryPatientRepository.java) lets `PatientService` be tested without touching the filesystem; remember to `login(...)` first, as a user on the patient's care team. `FakeRealtimeDatabase` (a JDK `HttpServer` that mimics the REST API) tests the Firebase classes offline; `ZORG_FIREBASE_URL=<emulator> ./scripts/smoke-test.sh` runs the end-to-end test against the real emulator |
