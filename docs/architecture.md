# Architecture

The app is deliberately split into four layers so that the console, the business
rules and the storage format can change independently. Everything is wired
together once, in [App.java](../src/main/java/patients/App.java):

```java
PatientRepository repository = new JsonPatientRepository(Paths.get("patients.json"));
PatientService service = new PatientService(repository);
ConsoleUI ui = new ConsoleUI(service);
ui.run();
```

```text
ConsoleUI  ──►  PatientService  ──►  PatientRepository (interface)
 (I/O)           (list + rules)            │
   │                  │                    └──► JsonPatientRepository (Gson + file)
   │                  │
   └──────────────────┴──► ProfessionPermissions  (profession → allowed actions)

Patient / Patient.Specialists, Medication, Profession, Action, PatientField  (model types, used everywhere)
```

Dependencies point in one direction only: the UI knows the service, the service
knows the repository *interface*, and only `App` knows which implementation is
used. Nothing below the UI reads from `System.in` or writes to `System.out`.

## The layers

### `model` — [Patient](../src/main/java/patients/model/Patient.java)

An immutable `record` holding an `id`, `firstName`, `lastName`, `birthDate`, `weight`, `height`,
`address`, a nested `Specialists` record (`doctor`, `pharmacist`,
`physiotherapist`, `dentist`) and a list of
[Medication](../src/main/java/patients/model/Medication.java) records (`name`,
`dosage`, `frequency`). The compact constructor turns a missing (`null`)
medication list into an empty one, so older data files still load, and stores
an unmodifiable copy. Its only behaviour is `age()`, derived from the date of birth and today's
date, and `bmi()`, derived from weight and height. Neither is stored, so they can
never drift out of sync with the fields they are computed from (a stored age
would be wrong after the next birthday).

Records were chosen for the model because Gson can serialize them directly and
because patients are values: to "change" one you create a new one with the
`withX(...)` methods, which keep the `id`. The id is what identifies a patient
across saves and sessions; it is `null` until the service adds the patient.

Two enums sit next to it, each with a human-readable `label` that the UI prints:

- [Profession](../src/main/java/patients/model/Profession.java): `DOCTOR`, `PHARMACIST`,
  `PHYSIOTHERAPIST`, `DENTIST`. These are the roles a user can log in as.
- [Action](../src/main/java/patients/model/Action.java): `VIEW_LIST`, `ADD_PATIENT`. These
  are the things a user can do from the main menu.

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

Swapping storage means writing another `PatientRepository` (a database, a CSV
file, an in-memory fake for tests) and changing the one line in `App`.

### `service` — [PatientService](../src/main/java/patients/service/PatientService.java) and [ProfessionPermissions](../src/main/java/patients/service/ProfessionPermissions.java)

`PatientService` keeps a cached list of patients for the UI to show, and the
profession of the user currently logged in:

- `login(Profession)` sets the current profession. There is no password check;
  it only selects which permissions apply.
- `load()` clears the cache and refills it from the repository, sorted by last
  name, then first name. The sort keeps the numbered lists in the UI stable
  whatever order the storage returns.
- `add(Patient)` validates the patient, gives it a new id if it has none, saves
  it through the repository straight away and appends it to the cache.
- `update(Patient)` saves a changed version of an existing patient. It looks up
  the stored version by id and compares every `PatientField` (via
  `PatientField.valueOf`). If any changed field is not editable by the current
  profession (`FieldPermissions.canEdit`), or the specialists changed, it throws
  `SecurityException` and saves nothing. The UI only offers editable fields, but
  the service checks every field regardless.
- `getAll()` returns `List.copyOf(...)`, an unmodifiable snapshot, so callers
  cannot reach in and mutate the service's state.

`add`, `update` and `getAll` are guarded by `requirePermission(Action)`, which
throws `IllegalStateException` if nobody is logged in and `SecurityException`
if the current profession may not perform that action. `add` and `update` also
validate the patient (first and last name not blank, weight and height above 0)
and throw `IllegalArgumentException` otherwise; this also keeps `bmi()` finite.
`load` is not guarded.

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

1. `askProfession()` lists the professions and re-prompts until a valid number is
   chosen, then calls `service.login(...)`.
2. `loadPatients()` loads the file, printing a warning on `UncheckedIOException`.
3. `mainMenu(profession)` loops over a numbered menu built from
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
| New profession | a new `Profession` constant, its entry in `ProfessionPermissions`, and a case in `ConsoleUI.professionSwitch()` |
| Change who may do what | `ProfessionPermissions` only. The menu and the service checks both read from it |
| Different storage (DB, CSV) | new `PatientRepository` implementation (reuse `GsonFactory` for JSON); swap the one line in `App` |
| Validation | `PatientService.validate()` if it is a rule about patients; `ConsoleUI` if it is about re-prompting input |
| Tests | under `src/test/java`, mirroring `src/main/java`. [InMemoryPatientRepository](../src/test/java/patients/service/InMemoryPatientRepository.java) lets `PatientService` be tested without touching the filesystem; remember to `login(...)` first |
