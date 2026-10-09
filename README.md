# zorg-app — Patient Management System

A small Java console application for recording patients and their care team. You
pick your profession, then use a menu to view the patient overview (with each
patient's BMI) or add new patients — depending on what your profession is allowed
to do. Everything is stored in a local JSON file so the data is still there the
next time you run it.

## Requirements

- **JDK 17 or newer** (the code uses `record`, added in 16); developed and
  tested against Temurin 25
- **Gson 2.14.0** and **JUnit 5** — declared in
  [build.gradle.kts](build.gradle.kts) and downloaded from Maven Central

The project builds with Gradle. You don't need to install it: the Gradle wrapper
(`./gradlew`) downloads the right version on first use. It runs on whatever JDK is
on your `PATH` / `JAVA_HOME`, and the code is compiled for Java 17.

## Build & run

From the project root:

```bash
# compile and run (interactive; reads from the console)
./gradlew run -q --console=plain

# build a runnable distribution into build/install/zorg-app/
./gradlew installDist
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

In VS Code, the Java extension imports the Gradle project automatically, so
opening [src/main/java/patients/App.java](src/main/java/patients/App.java) and
pressing **Run** also works.

## Tests

Unit tests live under [src/test/java/](src/test/java), mirroring the
`src/main/java` package layout, and use JUnit 5.

```bash
./gradlew test
```

An HTML report is written to `build/reports/tests/test/index.html`.

There is also an end-to-end smoke test that builds the app, drives the console UI
with scripted input (a doctor adds a patient, then a pharmacist views it) and
checks the output and the saved `patients.json`:

```bash
./scripts/smoke-test.sh
```

Both run automatically on every pull request (and on pushes to `main`) via
[.github/workflows/pr-checks.yml](.github/workflows/pr-checks.yml); the unit
tests run on JDK 17 and 25.

> The app resolves `patients.json` relative to the **current working directory**.
> `./gradlew run` always uses the project root; if you start the app another way
> (e.g. from `build/install/`), run it from the project root or your data will
> end up somewhere else.

## Using the app

A session looks like this (logged in as a doctor):

```text
Hello, please choose your profession
1. Doctor
2. Pharmacist
3. Physiotherapist
4. Dentist
Please choose your profession 1-4
1
Welcome Doctor
Welcome to the patient management system!
0. Quit
1. View patients
2. Add patient
Please choose an option 0-2
1
John Doe, age: 28, weight: 89.2 kg, address: Elm Street 58, BMI: 24.7
0. Quit
1. View patients
2. Add patient
Please choose an option 0-2
0
```

What happens, in order:

1. **Choose your profession.** Pick a number from the list. Anything outside the
   range is rejected and you are asked again. There is no password. Picking a
   profession is the "login" and only decides which menu options you get.
2. **Patients are loaded** from `patients.json`. If the file is missing the app
   starts with an empty list; if it exists but cannot be read, a warning is
   printed and the app continues with an empty list.
3. **Main menu.** The menu lists only the actions your profession is allowed to
   perform, numbered from 1, plus `0. Quit`. After each action you return to the
   menu, so you can view and add as often as you like in one session.
4. **Quit (`0`).** The full in-memory list (the patients loaded from disk plus
   any you added) is written back to `patients.json`, replacing the file.

### Permissions

| Profession | View patients | Add patient |
| --- | :---: | :---: |
| Doctor | ✓ | ✓ |
| Physiotherapist | ✓ | ✓ |
| Dentist | ✓ | ✓ |
| Pharmacist | — | — |

A pharmacist currently gets a menu with only `0. Quit`. Permissions are defined in
[ProfessionPermissions.java](src/main/java/patients/service/ProfessionPermissions.java) and
are also enforced in `PatientService`. If the UI ever offers an action the
profession isn't allowed to perform, the service refuses it and the menu prints
e.g. `Pharmacist is not allowed to Add patient`.

### Menu actions

**View patients** prints every patient with their computed age and BMI:

```text
John Doe, age: 28, weight: 89.2 kg, address: Elm Street 58, BMI: 24.7
```

**Add patient** asks for one patient, then returns to the menu:

```text
Enter the new patient's first name:
Enter the patient's last name:
Enter the new patient's date of birth (dd-mm-yyyy):
Enter the new patient's weight (kg):
Enter the new patient's height (m):
Enter the patient's address:
Enter the patient's doctor:
Enter the patient's pharmacist:
Enter the patient's physiotherapist:
Enter the patient's dentist:
```

New patients are only kept in memory until you quit. Nothing is written to disk
until then.

Input handling details:

- Menu and profession choices must be whole numbers; anything else is
  re-prompted.
- The date of birth must be a real date in `dd-mm-yyyy` form (e.g. `15-06-1995`)
  and not in the future. Weight and height accept decimals with either a comma
  or a dot (`70,5` and `70.5` are both fine). Invalid input is re-prompted.
- All text input is trimmed. Empty answers are accepted and stored as empty
  strings — there is no validation of names, addresses or specialists.
- Age (in completed years) and BMI (`weight / (height * height)`) are computed
  on the fly ([Patient.age()](src/main/java/patients/model/Patient.java#L19-L25),
  [Patient.bmi()](src/main/java/patients/model/Patient.java#L15-L17)); neither is stored
  in the JSON file. Someone born on 29 February turns a year older on 1 March
  in non-leap years.

## Data file

`patients.json` lives in the project root and is **git-ignored** — it holds local
data and is not shared through the repository.

The file has one top-level key (a random, URL-safe, 12-character "group key")
whose value is the list of patients:

```json
{
  "yo3xnW_q7Nnm": [
    {
      "firstName": "John",
      "lastName": "Doe",
      "birthDate": "1998-03-14",
      "weight": 89.2,
      "height": 1.9,
      "address": "Elm Street 58",
      "specialists": {
        "doctor": "Anon",
        "pharmacist": "Anon",
        "physiotherapist": "Anon",
        "dentist": "Anon"
      }
    }
  ]
}
```

> The specialist field used to be called `physician`. Data files written before
> the rename still load, but that value is ignored (the physiotherapist comes
> back as `null`) and is dropped on the next save.

- The key is read from the file on load and reused on save, so it stays stable
  for a given data file. It is only generated (via `SecureRandom`) when saving a
  file that has no key yet.
- Only the **first** top-level entry is read. The format leaves room for multiple
  groups, but the current code ignores everything after the first one.
- The date of birth is stored as an ISO date string (`yyyy-MM-dd`).
- Missing JSON fields are left at their Java defaults by Gson — an absent
  `pharmacist` or `birthDate` becomes `null`. Nothing in the app currently
  rejects such a record; a patient without a date of birth is listed with
  `age: unknown`.

> Patients used to store a fixed `age` instead of a `birthDate`. Data files
> written before that change still load, but the old `age` is ignored (shown as
> `unknown`) and is dropped on the next save.

## Project structure

```text
src/main/java/patients/
├── App.java                                entry point; wires the layers together
├── model/Patient.java                      Patient + nested Specialists records
├── model/Profession.java                   professions you can log in as
├── model/Action.java                       menu actions (view, add)
├── repository/PatientRepository.java       storage interface (loadAll/saveAll)
├── repository/JsonPatientRepository.java   Gson-backed JSON file implementation
├── service/PatientService.java             in-memory list, login, permission checks, load/save
├── service/ProfessionPermissions.java      which profession may perform which action
└── ui/ConsoleUI.java                       login, menu, prompts, parsing, output formatting
src/test/java/patients/                     JUnit 5 tests, mirroring src/main/java
build.gradle.kts                            Gradle build: dependencies, main class, test setup
gradlew, gradle/wrapper/                    Gradle wrapper (pins the Gradle version)
patients.json                               local data file (git-ignored)
```

Layering and the reasoning behind it are described in
[docs/architecture.md](docs/architecture.md).

## Current limitations

Known gaps, so nobody goes looking for features that are not there yet:

- **View and add only.** There is no search, edit or delete.
- **No real authentication.** Anyone can pick any profession; there are no user
  accounts or passwords.
- **Pharmacists can't do anything** besides quit. They have no permitted actions
  yet.
- **Save happens once**, when you choose `0. Quit`. If the app is interrupted
  (Ctrl+C, crash), every patient added in that session is lost.
- **No duplicate detection and no patient IDs** — three identical "John Doe"
  records are three separate patients.
- **Height is not guarded against zero**, so a patient with height `0` yields an
  infinite BMI.
- **The console UI has no tests.** The model, repository, service and permissions
  are covered under [src/test/java/](src/test/java).
