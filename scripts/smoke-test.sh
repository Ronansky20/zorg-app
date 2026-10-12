#!/usr/bin/env bash
# End-to-end smoke test: builds the app, then drives the real console UI with scripted
# input and checks what it prints and what it writes to patients.json.
# Covers the wiring the unit tests skip: App -> ConsoleUI -> PatientService/UserService -> Json*Repository.
#
# Usage: ./scripts/smoke-test.sh   (from anywhere; needs a JDK on PATH / JAVA_HOME)
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
"$root/gradlew" -p "$root" installDist -q --console=plain
app="$root/build/install/zorg-app/bin/zorg-app"

# the app resolves patients.json against the working directory, so run it in a scratch dir
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
cp "$root/users.json" "$work/"
cd "$work"

failures=0
expect() {
    if grep -qF -- "$2" "$1"; then
        echo "  ok:   contains '$2'"
    else
        echo "  FAIL: expected '$2' in output"
        failures=$((failures + 1))
    fi
}
reject() {
    if grep -qF -- "$2" "$1"; then
        echo "  FAIL: did not expect '$2' in output"
        failures=$((failures + 1))
    else
        echo "  ok:   does not contain '$2'"
    fi
}
run_app() { # $1 = output file, stdin = scripted answers
    if ! "$app" > "$1" 2>&1; then
        echo "  FAIL: app exited with an error"
        cat "$1"
        failures=$((failures + 1))
    fi
}

echo "Doctor adds a patient with medication, adds more medication and sees every field"
# account 1 = Dr. Max Jansen; menu 2 = Add patient (care team: no other doctor, pharmacist 1 = de Vries,
# physio 1 = Bakker, dentist 1 = Visser; one medication, then n),
# 3 = Edit Patient -> patient 1 -> field 7 Medication -> 1 Add medication,
# 1 = View patients, 0 = Quit
run_app doctor.out <<'EOF'
1
2
Jane
Doe
15-06-1995
70
1.8
Elm Street 58
0
1
1
1
y
Paracetamol
500 mg
3x daily
n
3
1
7
1
Ibuprofen
400 mg
as needed
1
0
EOF
expect doctor.out "Add patient"
expect doctor.out "First name: Jane"
expect doctor.out "Date of birth: 1995-06-15"
expect doctor.out "Weight: 70.0"
expect doctor.out "Address: Elm Street 58"
expect doctor.out "Patient Updated"
expect doctor.out "Medication: Paracetamol 500 mg, 3x daily; Ibuprofen 400 mg, as needed"
expect doctor.out "Pieter de Vries (Pharmacist)"
expect doctor.out "Sanne Bakker (Physiotherapist)"
expect doctor.out "Lotte Visser (Dentist)"
expect doctor.out "Dr. Max Jansen (Doctor)"

echo "Patient is saved to patients.json"
if [[ -f patients.json ]]; then
    expect patients.json '"patients": {'
    expect patients.json '"jansen": true'
    expect patients.json '"devries": true'
    expect patients.json '"firstName": "Jane"'
    expect patients.json '"birthDate": "1995-06-15"'
    expect patients.json '"name": "Paracetamol"'
    expect patients.json '"name": "Ibuprofen"'
else
    echo "  FAIL: patients.json was not written"
    failures=$((failures + 1))
fi

echo "Pharmacist on the care team reloads the saved patient but only sees permitted fields"
# account 3 = Pieter de Vries (Pharmacist); menu 1 = View patients, 0 = Quit
run_app pharmacist.out <<'EOF'
3
1
0
EOF
expect pharmacist.out "First name: Jane"
expect pharmacist.out "Last name: Doe"
expect pharmacist.out "Date of birth: 1995-06-15"
expect pharmacist.out "Medication: Paracetamol 500 mg, 3x daily; Ibuprofen 400 mg, as needed"
reject pharmacist.out "Weight:"
reject pharmacist.out "Height:"
reject pharmacist.out "Address:"
reject pharmacist.out "Add patient"

echo "Users who are not on the care team don't see the patient"
# account 2 = Dr. Noor Hendriks, 4 = Daan Mulder (Pharmacist); menu 1 = View patients, 0 = Quit
run_app other-doctor.out <<'EOF'
2
1
0
EOF
expect other-doctor.out "Welcome Dr. Noor Hendriks (Doctor)"
reject other-doctor.out "First name: Jane"
run_app other-pharmacist.out <<'EOF'
4
1
0
EOF
expect other-pharmacist.out "Welcome Daan Mulder (Pharmacist)"
reject other-pharmacist.out "First name: Jane"

echo "Dentist can't pick medication when editing a patient"
# account 7 = Lotte Visser (Dentist); menu 2 = Edit Patient -> patient 1 -> 0 Back, 0 = Quit
run_app dentist.out <<'EOF'
7
2
1
0
0
EOF
expect dentist.out "Which field would you like to edit?"
expect dentist.out "Address: Elm Street 58"
reject dentist.out "Medication"

if (( failures > 0 )); then
    echo "Smoke test failed: $failures check(s) failed"
    exit 1
fi
echo "Smoke test passed"
