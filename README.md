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
Hello, please choose your account
1. Dr. Max Jansen (Doctor)
2. Dr. Noor Hendriks (Doctor)
3. Pieter de Vries (Pharmacist)
...
Please choose your account 1-8
1
Welcome Dr. Max Jansen (Doctor)
Welcome to the patient management system!
0. Quit
1. View patients
2. Add patient
3. Edit Patient
Please choose an option 0-3
1
First name: John
Last name: Doe
Date of birth: 2003-02-07
Weight: 67.0
Height: 1.82
Address: Exeter
Medication: none
Care team: Dr. Max Jansen (Doctor), Pieter de Vries (Pharmacist), Sanne Bakker (Physiotherapist), Lotte Visser (Dentist)
----------
0. Quit
...
```

What happens, in order:

1. **Choose your account.** The accounts come from [users.json](users.json). Pick
   a number from the list; anything outside the range is rejected and you are
   asked again. There is no password yet. Your account's profession decides
   which menu options and fields you get.
2. **Patients are loaded** from `patients.json`. If the file is missing the app
   starts with an empty list; if it exists but cannot be read (or is in the old
   format, see [Data files](#data-files)), a warning is printed and the app
   continues with an empty list.
3. **Main menu.** The menu lists only the actions your profession is allowed to
   perform, numbered from 1, plus `0. Quit`. After each action you return to the
   menu, so you can view and add as often as you like in one session. Every
   patient you add or edit is **saved to `patients.json` straight away**.
4. **Quit (`0`).** Ends the session. There is nothing left to save.

### Care teams

Every patient has a **care team**: the user accounts that treat them. You only
see, and can only edit, the patients whose care team you are on. Your profession
still decides *which* fields you may see or edit.

When a doctor adds a patient, they pick one doctor, pharmacist, physiotherapist
and dentist for the team (or `0` for none). The doctor who adds the patient is
always on the team. The care team can't be changed from the app yet; to change
it, edit `careTeam` in `patients.json`.

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
0. None
1. Dr. Max Jansen
2. Dr. Noor Hendriks
Choose the patient's doctor (0 = none)
... (the same for pharmacist, physiotherapist and dentist)
Add medication? (y/n)
Enter the medication name:
Enter the dosage (e.g. 500 mg):
Enter the frequency (e.g. 3x daily):
Add medication? (y/n)
```

The medication questions repeat until you answer `n`; answer `n` straight away
to add a patient without medication.

### Medication

Each patient has a list of medications, each with a name, a dosage and a
frequency. It is shown as one line when viewing patients:

```text
Medication: Paracetamol 500 mg, 3x daily; Ibuprofen 400 mg, as needed
```

| Profession | View medication | Edit medication |
| --- | :---: | :---: |
| Doctor | ✓ | ✓ |
| Pharmacist | ✓ | — |
| Physiotherapist | ✓ | — |
| Dentist | ✓ | — |

A doctor changes medication via **Edit Patient** → choose the patient →
**Medication**, which opens a sub-menu to **Add**, **Change** (re-enter an entry)
or **Remove** a medication. Who may see or edit it is defined in
[FieldPermissions.java](src/main/java/patients/service/FieldPermissions.java) and
enforced by `PatientService.update`, which compares every field with the stored
patient and refuses the whole edit if any field you may not edit has changed.

Input handling details:

- Menu and account choices must be whole numbers; anything else is
  re-prompted.
- The date of birth must be a real date in `dd-mm-yyyy` form (e.g. `15-06-1995`)
  and not in the future. Weight and height accept decimals with either a comma
  or a dot (`70,5` and `70.5` are both fine). Invalid input is re-prompted.
- All text input is trimmed. First and last name must not be empty, and weight
  and height must be greater than 0. A patient (or an edit) that breaks one of
  these rules is refused with a message and nothing is saved. The address may be
  left empty.
- Age (in completed years) and BMI (`weight / (height * height)`) are computed
  on the fly ([Patient.age()](src/main/java/patients/model/Patient.java#L19-L25),
  [Patient.bmi()](src/main/java/patients/model/Patient.java#L15-L17)); neither is stored
  in the JSON file. Someone born on 29 February turns a year older on 1 March
  in non-leap years.

## Data files

`patients.json` lives in the project root and is **git-ignored** — it holds local
data and is not shared through the repository. `users.json` holds the dummy
accounts and **is** committed, so everyone has the same accounts to log in with:

```json
{
  "users": {
    "jansen": { "name": "Dr. Max Jansen", "profession": "DOCTOR" },
    "devries": { "name": "Pieter de Vries", "profession": "PHARMACIST" }
  }
}
```

The key is the user id that care teams refer to. `profession` is one of `DOCTOR`,
`PHARMACIST`, `PHYSIOTHERAPIST` or `DENTIST`. The app only reads this file; to add
an account, add an entry by hand.

`patients.json` has the same shape as the Firebase Realtime Database tree the app
will move to: a `patients` object whose keys are the patient ids (random,
URL-safe, 12 characters) and whose values are the patients:

```json
{
  "patients": {
    "yo3xnW_q7Nnm": {
      "firstName": "John",
      "lastName": "Doe",
      "birthDate": "1998-03-14",
      "weight": 89.2,
      "height": 1.9,
      "address": "Elm Street 58",
      "careTeam": {
        "jansen": true,
        "devries": true
      },
      "medications": [
        {
          "name": "Paracetamol",
          "dosage": "500 mg",
          "frequency": "3x daily"
        }
      ]
    }
  }
}
```

- The id is only stored as the key; it is not repeated inside the patient.
  A new patient gets its id (via `SecureRandom`) when it is added.
- Each add or edit reads the file, replaces that one patient and writes the file
  back. Other patients in the file are left as they are.
- `careTeam` maps user ids from `users.json` to `true`. A map instead of a list
  is how Firebase prefers it, and it lets the database rules check membership
  directly later on.
- The date of birth is stored as an ISO date string (`yyyy-MM-dd`).
- Missing JSON fields are left at their Java defaults by Gson — an absent
  `birthDate` becomes `null`, and an absent `careTeam` or `medications` becomes
  empty. A patient without a care team is visible to nobody.

### Upgrading an old `patients.json`

Files written before patients had ids look like `{ "<group key>": [ ...patients... ] }`.
The app does **not** convert them: it prints a warning, starts with an empty list
and refuses to save, so your old file is never overwritten. Either delete the
file to start empty, or rewrite it by hand into the shape above. For each patient:

1. Give it a key under `"patients"`: any unique string without `. $ # [ ] /`.
2. Replace an old `age` with a `birthDate` (`yyyy-MM-dd`).
3. Replace `specialists` (free-text names) with a `careTeam` of user ids from
   `users.json`, e.g. `"careTeam": { "jansen": true, "devries": true }`. Without
   it, nobody can see the patient. Files from the first id-based version have
   `specialists` too and need the same change.
4. Make sure first and last name are filled in and weight and height are above 0,
   otherwise the patient can't be edited.

## Project structure

```text
src/main/java/patients/
├── App.java                                entry point; wires the layers together
├── model/Patient.java                      patient record, including its care team
├── model/User.java                         user account: id, name, profession
├── model/Medication.java                   one medication: name, dosage, frequency
├── model/Profession.java                   professions a user can have
├── model/Action.java                       menu actions (view, add, edit)
├── repository/PatientRepository.java       storage interface (findAll/findById/save)
├── repository/JsonPatientRepository.java   Gson-backed JSON file implementation
├── repository/UserRepository.java          read-only user storage interface (findAll/findById)
├── repository/JsonUserRepository.java      reads users.json
├── repository/JsonFiles.java               shared "read one section of a JSON file" helper
├── repository/GsonFactory.java             shared Gson setup (ISO dates)
├── repository/Ids.java                     generates patient ids
├── service/PatientService.java             cached list, login, permission and care team checks, validation
├── service/UserService.java                cached user list, lookups by id and profession
├── service/ProfessionPermissions.java      which profession may perform which action
└── ui/ConsoleUI.java                       login, menu, prompts, parsing, output formatting
src/test/java/patients/                     JUnit 5 tests, mirroring src/main/java
build.gradle.kts                            Gradle build: dependencies, main class, test setup
gradlew, gradle/wrapper/                    Gradle wrapper (pins the Gradle version)
users.json                                  dummy user accounts (committed)
patients.json                               local data file (git-ignored)
```

Layering and the reasoning behind it are described in
[docs/architecture.md](docs/architecture.md).

## Current limitations

Known gaps, so nobody goes looking for features that are not there yet:

- **View and add only.** There is no search, edit or delete.
- **No real authentication.** Anyone can pick any account; there are no
  passwords. Firebase Auth replaces this later.
- **Care teams can't be changed in the app.** They are set when a patient is
  added; after that, only by editing `patients.json`.
- **Pharmacists can't do anything** besides quit. They have no permitted actions
  yet.
- **No duplicate detection** — three identical "John Doe" records are three
  separate patients, each with its own id.
- **One user at a time.** Each save rewrites the whole file, so two sessions
  running at once can still overwrite each other's changes. That goes away with
  the move to Firebase.
- **The console UI has no tests.** The model, repository, service and permissions
  are covered under [src/test/java/](src/test/java).
