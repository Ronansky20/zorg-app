# zorg-app — Patient Management System

A small Java console application for recording patients and their care team. You
log in as a care provider, then use a menu to view, add and edit the patients
whose care team you are on. Your profession decides which of these you may do
and which fields you may see and change. Data is stored in local JSON files by
default, or in a Firebase Realtime Database with sign-in and server-side
security rules (see [Running against Firebase](#running-against-firebase)).

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

### Running against Firebase

By default the app uses the local JSON files. Set `ZORG_FIREBASE_URL` to use a
Firebase Realtime Database instead. You then sign in with an email and password
(Firebase Auth) instead of picking an account, and the database's security rules
decide what you may read and write.

| Variable | Used by | Meaning |
| --- | --- | --- |
| `ZORG_FIREBASE_URL` | app, migration | database URL, e.g. `http://127.0.0.1:9000?ns=demo-zorg-app-default-rtdb` for the emulator |
| `FIREBASE_AUTH_EMULATOR_HOST` | app | Auth emulator host, e.g. `127.0.0.1:9099`; when set, sign-in goes to the emulator |
| `ZORG_FIREBASE_API_KEY` | app | the project's Web API key; only needed for a real project |
| `ZORG_FIREBASE_ADMIN_TOKEN` | migration | sent as `Authorization: Bearer ...`, which bypasses the rules; the emulator accepts `owner` |

For development, use the **local emulators** (needs Node.js; the JDK you already
have runs them). [firebase.json](firebase.json) configures the database on port
9000 and Auth on port 9099, and loads [database.rules.json](database.rules.json).
A `demo-` project id keeps them fully offline:

```bash
npx -y firebase-tools@15.33.0 emulators:start --only database,auth --project demo-zorg-app
```

The emulators start empty and forget everything when they stop. In another
terminal, create a login for every account in `users.json`, then load the
accounts and patients into the database:

```bash
./scripts/create-emulator-accounts.sh
ZORG_FIREBASE_URL="http://127.0.0.1:9000?ns=demo-zorg-app-default-rtdb" ZORG_FIREBASE_ADMIN_TOKEN=owner \
    ./gradlew migrateToFirebase -q --console=plain
```

Each login's uid is the account's key in `users.json`, its email is
`<key>@zorg.test` (e.g. `jansen@zorg.test`), and the password is
`zorg-dev-password` (set `ZORG_DEV_PASSWORD` to choose another). Then run the app:

```bash
ZORG_FIREBASE_URL="http://127.0.0.1:9000?ns=demo-zorg-app-default-rtdb" FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 \
    ./gradlew run -q --console=plain
```

(PowerShell: set `$env:ZORG_FIREBASE_URL`, `$env:FIREBASE_AUTH_EMULATOR_HOST` and,
for the migration, `$env:ZORG_FIREBASE_ADMIN_TOKEN` first.)

The migration writes every user and patient by id, and builds the
`/userPatients` index along the way, so running it again gives the same data.
Records that only exist in Firebase are left alone. It prints a warning for any
care team member who isn't in `users.json`. If `patients.json` is missing, only
the users are migrated; if it is in the old format (see [Data files](#data-files)),
the migration stops before writing anything.

#### Security rules

[database.rules.json](database.rules.json) is generated from
`ProfessionPermissions` and `FieldPermissions`, so the server enforces the same
rules as the app. After changing either, regenerate it (a unit test fails until
you do):

```bash
./gradlew generateFirebaseRules
```

In short: you can only read a patient whose care team you are on, and only
through your own `/userPatients` index; you can only change the fields your
profession may edit; only professions that may add patients can create one, with
themselves on the team; the care team can't be changed after creation; `/users`
is readable when signed in and writable by nobody. Values are validated too (for
example, weight must be a number above 0).

#### A real Firebase project

This is medical data (GDPR). Keep **only dummy data** in a real project. The
project is `zorg-app-74d7b` ([.firebaserc](.firebaserc)), with its database at
`https://zorg-app-74d7b-default-rtdb.europe-west1.firebasedatabase.app`
(`europe-west1`, created in locked mode). To set it up from scratch:

1. Log in to the Firebase CLI and deploy the rules before adding any data:

   ```bash
   npx -y firebase-tools@15.33.0 login
   npx -y firebase-tools@15.33.0 deploy --only database
   ```

2. In the Firebase console, enable **Authentication → Sign-in method →
   Email/Password**, and copy the **Web API key** from Project settings → General.
3. Install the [Google Cloud CLI](https://cloud.google.com/sdk/docs/install)
   (`brew install --cask google-cloud-sdk` on macOS) and get an admin token for
   your Google account:

   ```bash
   gcloud auth application-default login --scopes=openid,https://www.googleapis.com/auth/userinfo.email,https://www.googleapis.com/auth/cloud-platform,https://www.googleapis.com/auth/firebase.database
   gcloud auth application-default set-quota-project zorg-app-74d7b
   export ZORG_FIREBASE_ADMIN_TOKEN="$(gcloud auth application-default print-access-token)"
   ```

   The token is valid for an hour; run the `export` again when it expires.
4. Create a login for every account in `users.json`. The uid is the key and the
   email is `<key>@zorg.test`, as in the emulator, but you choose the password (it
   asks). Existing logins are left alone, so it can be run again:

   ```bash
   ./scripts/create-firebase-accounts.sh
   ```

5. Load the users and patients:

   ```bash
   ZORG_FIREBASE_URL="https://zorg-app-74d7b-default-rtdb.europe-west1.firebasedatabase.app" \
       ./gradlew migrateToFirebase -q --console=plain
   ```

6. Run the app. Leave `FIREBASE_AUTH_EMULATOR_HOST` unset, or sign-in goes to
   the emulator:

   ```bash
   ZORG_FIREBASE_URL="https://zorg-app-74d7b-default-rtdb.europe-west1.firebasedatabase.app" \
   ZORG_FIREBASE_API_KEY="<web-api-key>" ./gradlew run -q --console=plain
   ```

After changing the permissions, run `./gradlew generateFirebaseRules` and deploy
again (step 1).

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

With the emulators running (see [Running against Firebase](#running-against-firebase)),
the same test runs through Firebase instead, signing in with email and password.
It loads the current `database.rules.json`, wipes the emulator's data and
accounts first, and refuses any URL that isn't on localhost:

```bash
ZORG_FIREBASE_URL="http://127.0.0.1:9000?ns=demo-zorg-app-default-rtdb" ./scripts/smoke-test.sh
```

The Firebase classes also have unit tests that run against small fake servers,
so `./gradlew test` needs neither the emulators nor a network.

The security rules are tested against the real emulators, signing in as
different users and checking what each of them may read and write. These tests
are tagged `emulator`, so `./gradlew test` skips them; run them with:

```bash
ZORG_FIREBASE_URL="http://127.0.0.1:9000?ns=demo-zorg-app-default-rtdb" FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 \
    ./gradlew emulatorTest
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
3. Daan Mulder (Pharmacist)
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
Date of birth: 1998-03-14
Weight: 89.2
Height: 1.9
Address: Elm Street 58
Medication: Paracetamol 500 mg, 3x daily
Care team: Dr. Max Jansen (Doctor), Pieter de Vries (Pharmacist), Sanne Bakker (Physiotherapist), Lotte Visser (Dentist)
----------
0. Quit
...
```

What happens, in order:

1. **Choose your account.** The accounts come from [users.json](users.json). Pick
   a number from the list; anything outside the range is rejected and you are
   asked again. With the JSON files there is no password. On Firebase you sign
   in with email and password instead (three attempts), and your login's uid
   picks the account. Your account's profession decides which menu options and
   fields you get.
2. **Your patients are loaded**, from `patients.json` or from Firebase: only the
   patients whose care team you are on. If `patients.json` is missing the app
   starts with an empty list; if it exists but cannot be read (or is in the old
   format, see [Data files](#data-files)), a warning is printed and the app
   continues with an empty list.
3. **Main menu.** The menu lists only the actions your profession is allowed to
   perform, numbered from 1, plus `0. Quit`. After each action you return to the
   menu, so you can view, add and edit as often as you like in one session.
   Every patient you add or edit is **saved straight away**.
4. **Quit (`0`).** Ends the session. There is nothing left to save.

### Care teams

Every patient has a **care team**: the user accounts that treat them. You only
see, and can only edit, the patients whose care team you are on. Your profession
still decides *which* fields you may see or edit.

When a doctor adds a patient, they pick one doctor, pharmacist, physiotherapist
and dentist for the team (or `0` for none). The doctor who adds the patient is
always on the team. The care team can't be changed from the app yet. With the
JSON files you can edit `careTeam` in `patients.json` by hand; on Firebase the
security rules fix the team once the patient is created, so only an admin can
change it.

### Permissions

Your profession decides which menu actions you get:

| Profession | View patients | Add patient | Edit patient |
| --- | :---: | :---: | :---: |
| Doctor | ✓ | ✓ | ✓ |
| Physiotherapist | ✓ | — | ✓ |
| Dentist | ✓ | — | ✓ |
| Pharmacist | ✓ | — | — |

and which fields you may see (👁) and edit (✎):

| Field | Doctor | Physiotherapist | Dentist | Pharmacist |
| --- | :---: | :---: | :---: | :---: |
| First name, last name | 👁 ✎ | 👁 ✎ | 👁 ✎ | 👁 |
| Date of birth | 👁 ✎ | 👁 | 👁 | 👁 |
| Weight, height | 👁 ✎ | 👁 ✎ | 👁 | — |
| Address | 👁 ✎ | 👁 ✎ | 👁 ✎ | — |
| Medication | 👁 ✎ | 👁 | 👁 | 👁 |

Actions are defined in
[ProfessionPermissions.java](src/main/java/patients/service/ProfessionPermissions.java)
and fields in [FieldPermissions.java](src/main/java/patients/service/FieldPermissions.java).
Both are enforced in `PatientService` too. If the UI ever offers an action the
profession isn't allowed to perform, the service refuses it and the menu prints
e.g. `Pharmacist is not allowed to Add patient`. On Firebase the same rules are
also enforced by the database (see [Security rules](#security-rules)).

### Menu actions

**View patients** prints each of your patients, one field per line (only the
fields your profession may see), followed by the care team:

```text
First name: John
Last name: Doe
Date of birth: 1998-03-14
Medication: Paracetamol 500 mg, 3x daily
Care team: Dr. Max Jansen (Doctor), Pieter de Vries (Pharmacist), Sanne Bakker (Physiotherapist), Lotte Visser (Dentist)
----------
```

(That is what a pharmacist sees; a doctor also sees weight, height and address.)

**Edit Patient** lists your patients by name; pick one, then pick one of the
fields you may edit (each shown with its current value) and enter the new value.
The change is saved straight away and `Patient Updated` is printed. `0` goes
back at every step.

```text
0. Back
1. John Doe
What patient would you like to edit?
1
0. Back
1. First name: John
2. Last name: Doe
3. Address: Elm Street 58
Which field would you like to edit?
3
New Address:
Oak Lane 1
Patient Updated
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

Everyone can see it, but only a doctor can edit it (see [Permissions](#permissions)).
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
- `Patient` can compute age in completed years
  ([Patient.age()](src/main/java/patients/model/Patient.java#L133-L139)) and BMI,
  `weight / (height * height)` ([Patient.bmi()](src/main/java/patients/model/Patient.java#L129-L131)).
  Neither is stored, and the console doesn't show them yet. Someone born on
  29 February turns a year older on 1 March in non-leap years.

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
├── MigrateToFirebase.java                  copies the JSON files into Firebase (./gradlew migrateToFirebase)
├── FirebaseRules.java                      generates database.rules.json (./gradlew generateFirebaseRules)
├── auth/Authenticator.java                 sign-in interface used by the UI
├── auth/FirebaseAuth.java                  email/password sign-in and token refresh (Identity Toolkit REST)
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
├── repository/FirebaseClient.java          Realtime Database REST client (java.net.http)
├── repository/FirebasePatientRepository.java  patients + /userPatients index in Firebase
├── repository/FirebaseUserRepository.java  reads /users from Firebase
├── service/PatientService.java             cached list, login, permission and care team checks, validation
├── service/UserService.java                cached user list, lookups by id and profession
├── service/ProfessionPermissions.java      which profession may perform which action
└── ui/ConsoleUI.java                       login, menu, prompts, parsing, output formatting
src/test/java/patients/                     JUnit 5 tests, mirroring src/main/java
build.gradle.kts                            Gradle build: dependencies, main class, test setup
gradlew, gradle/wrapper/                    Gradle wrapper (pins the Gradle version)
users.json                                  dummy user accounts (committed)
database.rules.json                         Firebase security rules (generated, committed)
firebase.json                               emulator ports and rules file
scripts/create-emulator-accounts.sh         creates a login per account in the Auth emulator
scripts/create-firebase-accounts.sh         creates a login per account in the real project
.firebaserc                                 links the Firebase CLI to project zorg-app-74d7b
patients.json                               local data file (git-ignored)
```

Layering and the reasoning behind it are described in
[docs/architecture.md](docs/architecture.md).

## Current limitations

Known gaps, so nobody goes looking for features that are not there yet:

- **No search or delete.** Patients can be viewed, added and edited.
- **No authentication on JSON.** With the local files anyone can pick any
  account; only the Firebase mode has passwords and server-side rules.
- **Passwords are echoed.** The sign-in prompt reads the password like any other
  line, so it is visible while you type it.
- **Care teams can't be changed in the app.** They are set when a patient is
  added; after that, only by editing `patients.json`.
- **Pharmacists can only view.** They can't add or edit patients, not even
  medication.
- **No duplicate detection** — three identical "John Doe" records are three
  separate patients, each with its own id.
- **One user at a time on JSON.** Each save rewrites the whole file, so two
  sessions running at once can still overwrite each other's changes. On Firebase
  each save only writes that one patient.
- **The migration only goes one way.** `./gradlew migrateToFirebase` copies the
  JSON files into Firebase, but nothing copies Firebase data back. After the
  migration, data entered in one backend isn't visible in the other.
- **The console UI has no tests.** The model, repository, service and permissions
  are covered under [src/test/java/](src/test/java).
