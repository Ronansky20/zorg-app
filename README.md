# zorg-app — Patient Management System

A small Java console application for recording patients and their care team. You
enter patients one after another, the app prints an overview with each patient's
BMI, and everything is stored in a local JSON file so the data is still there the
next time you run it.

## Requirements

- **JDK 17 or newer** (the code uses `record`, added in 16); developed and
  tested against Temurin 25
- **Gson 2.14.0** — already vendored in [lib/gson-2.14.0.jar](lib/gson-2.14.0.jar)

No Maven/Gradle build: the project is compiled directly with `javac`, and VS Code
picks up the layout from [.vscode/settings.json](.vscode/settings.json).

## Build & run

From the project root:

```bash
# compile everything into bin/
javac -cp lib/gson-2.14.0.jar -d bin $(find src -name '*.java')

# run
java -cp "bin:lib/gson-2.14.0.jar" patients.App
```

On Windows, use `;` instead of `:` as the classpath separator.

In VS Code, opening [src/patients/App.java](src/patients/App.java) and pressing
**Run** does the same thing — the Java extension compiles to `bin/` and uses
`lib/*.jar` as the classpath.

> The app resolves `patients.json` relative to the **current working directory**,
> so start it from the project root or your data will end up somewhere else.

## Using the app

```
Welcome to the patient management system!
Enter the new patient's first name:
Enter the patient's last name:
Enter the new patient's age:
Enter the new patient's weight (kg):
Enter the new patient's height (m):
Enter the patient's address:
Enter the patient's doctor:
Enter the patient's apothecary:
Enter the patient's physician:
Enter the patient's dentist:
Add another patient? (y/n)
```

What happens, in order:

1. Existing patients are loaded from `patients.json`. If the file is missing the
   app starts with an empty list; if it exists but cannot be read, a warning is
   printed and the app continues with an empty list.
2. You are prompted for one patient at a time, and asked whether to add another.
   Anything other than `y`/`Y` ends the entry loop.
3. All patients — the ones loaded from disk plus the ones you just typed — are
   printed with their computed BMI:
   ```
   John Doe, age: 28, weight: 89.2 kg, address: Elm Street 58, BMI: 24.7
   ```
4. The full list is written back to `patients.json`, replacing the file.

Input handling details:

- Age must be a whole number; weight and height accept decimals with either a
  comma or a dot (`70,5` and `70.5` are both fine). Invalid input is re-prompted.
- All text input is trimmed. Empty answers are accepted and stored as empty
  strings — there is no validation of names, addresses or specialists.
- BMI is `weight / (height * height)` and is computed on the fly
  ([Patient.bmi()](src/patients/model/Patient.java#L12-L14)); it is never stored
  in the JSON file.

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
      "age": 28,
      "weight": 89.2,
      "height": 1.9,
      "address": "Elm Street 58",
      "specialists": {
        "doctor": "Anon",
        "pharmacist": "Anon",
        "physician": "Anon",
        "dentist": "Anon"
      }
    }
  ]
}
```

- The key is read from the file on load and reused on save, so it stays stable
  for a given data file. It is only generated (via `SecureRandom`) when saving a
  file that has no key yet.
- Only the **first** top-level entry is read. The format leaves room for multiple
  groups, but the current code ignores everything after the first one.
- Missing JSON fields are left at their Java defaults by Gson — an absent
  `pharmacist` becomes `null`, an absent `age` becomes `0`. Nothing in the app
  currently rejects such a record.

## Project structure

```
src/patients/
├── App.java                              entry point; wires the layers together
├── model/Patient.java                    Patient + nested Specialists records
├── repository/PatientRepository.java     storage interface (loadAll/saveAll)
├── repository/JsonPatientRepository.java Gson-backed JSON file implementation
├── service/PatientService.java           in-memory patient list + load/save
└── ui/ConsoleUI.java                     prompts, parsing, output formatting
lib/gson-2.14.0.jar                       JSON dependency
bin/                                      compiled .class output (currently committed)
patients.json                             local data file (git-ignored)
```

Layering and the reasoning behind it are described in
[docs/architecture.md](docs/architecture.md).

## Current limitations

Known gaps, so nobody goes looking for features that are not there yet:

- **Add-only.** There is no search, edit or delete, and no way to skip the entry
  loop — every run requires at least one new patient before the overview is shown.
- **Save happens once**, at the very end. If the app is interrupted, the entered
  patients are lost.
- **No duplicate detection and no patient IDs** — three identical "John Doe"
  records are three separate patients.
- **Height is not guarded against zero**, so a patient with height `0` yields an
  infinite BMI.
- The specialist prompt still says *"apothecary"* while the model field is named
  `pharmacist`.
- **No automated tests.**
