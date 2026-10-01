# Architecture

The app is deliberately split into four layers so that the console, the business
rules and the storage format can change independently. Everything is wired
together once, in [App.java](../src/patients/App.java):

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

Patient / Patient.Specialists, Profession, Action  (model types, used everywhere)
```

Dependencies point in one direction only: the UI knows the service, the service
knows the repository *interface*, and only `App` knows which implementation is
used. Nothing below the UI reads from `System.in` or writes to `System.out`.

## The layers

### `model` — [Patient](../src/patients/model/Patient.java)

An immutable `record` holding `firstName`, `lastName`, `age`, `weight`, `height`,
`address` and a nested `Specialists` record (`doctor`, `pharmacist`,
`physiotherapist`, `dentist`). The only behaviour is `bmi()`, which derives BMI from weight and
height instead of storing it — so the value can never drift out of sync with the
fields it is computed from.

Records were chosen for the model because Gson can serialize them directly and
because patients are values: to "change" one you create a new one.

Two enums sit next to it, each with a human-readable `label` that the UI prints:

- [Profession](../src/patients/model/Profession.java): `DOCTOR`, `PHARMACIST`,
  `PHYSIOTHERAPIST`, `DENTIST`. These are the roles a user can log in as.
- [Action](../src/patients/model/Action.java): `VIEW_LIST`, `ADD_PATIENT`. These
  are the things a user can do from the main menu.

### `repository` — storage

[PatientRepository](../src/patients/repository/PatientRepository.java) is a
two-method interface:

```java
List<Patient> loadAll();
void saveAll(List<Patient> patients);
```

It is a whole-list contract, not per-record CRUD — the app always reads and
writes the complete collection.

[JsonPatientRepository](../src/patients/repository/JsonPatientRepository.java)
implements it on top of a single JSON file:

- The file maps a **group key** to a list of patients
  (`Map<String, List<Patient>>`, captured with a Gson `TypeToken` because generic
  types are erased at runtime).
- `loadAll()` returns an empty list when the file does not exist, and otherwise
  takes the first map entry, remembering its key in the `groupKey` field.
- `saveAll()` reuses that remembered key, or generates one from `SecureRandom`
  (9 random bytes, URL-safe Base64, no padding → 12 characters) the first time a
  file is written. This is why load-then-save keeps the key stable, while saving
  without a prior load creates a new one.
- I/O failures are wrapped in `UncheckedIOException` with the file path in the
  message, so the interface stays free of checked exceptions and the UI can
  decide how to report them.

Swapping storage means writing another `PatientRepository` (a database, a CSV
file, an in-memory fake for tests) and changing the one line in `App`.

### `service` — [PatientService](../src/patients/service/PatientService.java) and [ProfessionPermissions](../src/patients/service/ProfessionPermissions.java)

`PatientService` owns the in-memory list of patients that the rest of the run
works against, and the profession of the user currently logged in:

- `login(Profession)` sets the current profession. There is no password check;
  it only selects which permissions apply.
- `load()` clears the list and refills it from the repository.
- `add(Patient)` appends to the list only — it does **not** touch the file.
- `getAll()` returns `List.copyOf(...)`, an unmodifiable snapshot, so callers
  cannot reach in and mutate the service's state.
- `save()` hands the whole list to the repository.

`add` and `getAll` are guarded by `requirePermission(Action)`, which throws
`IllegalStateException` if nobody is logged in and `SecurityException` if the
current profession may not perform that action. `load` and `save` are not
guarded. Every session loads at the start and saves at the end, whoever is
logged in.

`ProfessionPermissions` is a static `EnumMap<Profession, Set<Action>>`, the single
source of truth for who may do what. `isAllowed(profession, action)` answers one
question; `getAllowedActions(profession)` returns the whole set, which the UI uses
to build the menu. The pharmacist currently maps to an empty set.

Checking permissions in the service as well as filtering the menu in the UI is
deliberate. The menu hides what you can't do, and the service enforces it even if
a future UI (or a bug) offers the action anyway.

The load/save split is what makes the current session-based behaviour explicit:
the file is read once at the start and written once at the end.

### `ui` — [ConsoleUI](../src/patients/ui/ConsoleUI.java)

All console interaction and all parsing. `run()` is the whole session:

1. `askProfession()` lists the professions and re-prompts until a valid number is
   chosen, then calls `service.login(...)`.
2. `loadPatients()` loads the file, printing a warning on `UncheckedIOException`.
3. `mainMenu(profession)` loops over a numbered menu built from
   `ProfessionPermissions.getAllowedActions(...)`, with `0` to quit. The chosen
   `Action` is dispatched in a `switch`; a `SecurityException` from the service
   is printed and the loop continues.
4. `savePatients()` writes the file, printing an error on `UncheckedIOException`.

Storage problems at either end therefore degrade to a message instead of a stack
trace.

The private `readLine` / `readInt` / `readDouble` helpers keep the retry logic in
one place: the numeric ones loop until they parse successfully, and `readDouble`
normalizes a comma to a dot so both decimal conventions work. `printPatients()`
is the only place that formats output.

## Where to add things

| Change | Where it goes |
| --- | --- |
| New patient field | `Patient` record + the prompts in `readPatient()`; old JSON records simply leave it at its default |
| New menu action (search, edit, delete) | a new `Action` constant; grant it to professions in `ProfessionPermissions`; a guarded method on `PatientService` (`requirePermission(...)`); a `case` in `ConsoleUI.mainMenu()` |
| New profession | a new `Profession` constant, its entry in `ProfessionPermissions`, and a case in `ConsoleUI.professionSwitch()` |
| Change who may do what | `ProfessionPermissions` only. The menu and the service checks both read from it |
| Different storage (DB, CSV) | new `PatientRepository` implementation; swap the one line in `App` |
| Validation (non-empty names, height > 0) | `PatientService` if it is a rule about patients; `ConsoleUI` if it is about re-prompting input |
| Tests | under `test/`, mirroring `src/`. [InMemoryPatientRepository](../test/patients/service/InMemoryPatientRepository.java) lets `PatientService` be tested without touching the filesystem; remember to `login(...)` first |
