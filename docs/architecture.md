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

```
ConsoleUI  ──►  PatientService  ──►  PatientRepository (interface)
 (I/O)           (list + rules)            │
                                           └──► JsonPatientRepository (Gson + file)
                        │
                        └──► Patient / Patient.Specialists  (records, used everywhere)
```

Dependencies point in one direction only: the UI knows the service, the service
knows the repository *interface*, and only `App` knows which implementation is
used. Nothing below the UI reads from `System.in` or writes to `System.out`.

## The layers

### `model` — [Patient](../src/patients/model/Patient.java)

An immutable `record` holding `firstName`, `lastName`, `age`, `weight`, `height`,
`address` and a nested `Specialists` record (`doctor`, `pharmacist`, `physician`,
`dentist`). The only behaviour is `bmi()`, which derives BMI from weight and
height instead of storing it — so the value can never drift out of sync with the
fields it is computed from.

Records were chosen for the model because Gson can serialize them directly and
because patients are values: to "change" one you create a new one.

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

### `service` — [PatientService](../src/patients/service/PatientService.java)

Owns the in-memory list of patients that the rest of the run works against:

- `load()` clears the list and refills it from the repository.
- `add(Patient)` appends to the list only — it does **not** touch the file.
- `getAll()` returns `List.copyOf(...)`, an unmodifiable snapshot, so callers
  cannot reach in and mutate the service's state.
- `save()` hands the whole list to the repository.

The load/save split is what makes the current session-based behaviour explicit:
the file is read once at the start and written once at the end.

### `ui` — [ConsoleUI](../src/patients/ui/ConsoleUI.java)

All console interaction and all parsing. `run()` is the whole session — load,
entry loop, print, save — and catches `UncheckedIOException` at both ends so a
storage problem degrades to a message instead of a stack trace.

The private `readLine` / `readInt` / `readDouble` / `askYesNo` helpers keep the
retry logic in one place: the numeric ones loop until they parse successfully,
and `readDouble` normalizes a comma to a dot so both decimal conventions work.
`printPatients()` is the only place that formats output.

## Where to add things

| Change | Where it goes |
| --- | --- |
| New patient field | `Patient` record + the prompts in `readPatient()`; old JSON records simply leave it at its default |
| New menu action (search, edit, delete) | a method on `PatientService` for the rule, plus a prompt in `ConsoleUI` |
| Different storage (DB, CSV) | new `PatientRepository` implementation; swap the one line in `App` |
| Validation (non-empty names, height > 0) | `PatientService` if it is a rule about patients; `ConsoleUI` if it is about re-prompting input |
| Tests | a fake `PatientRepository` lets `PatientService` be tested without touching the filesystem |
