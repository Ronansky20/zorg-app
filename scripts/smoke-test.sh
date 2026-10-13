#!/usr/bin/env bash
# End-to-end smoke test: builds the app, then drives the real console UI with scripted
# input and checks what it prints and what it writes to patients.json.
# Covers the wiring the unit tests skip: App -> ConsoleUI -> PatientService/UserService -> repositories.
#
# Usage: ./scripts/smoke-test.sh   (from anywhere; needs a JDK on PATH / JAVA_HOME)
# With ZORG_FIREBASE_URL set to a local emulator, the same run goes through Firebase instead;
# the emulator's database is wiped first.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
"$root/gradlew" -p "$root" installDist -q --console=plain
app="$root/build/install/zorg-app/bin/zorg-app"

# the app resolves patients.json against the working directory, so run it in a scratch dir
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
cp "$root/users.json" "$work/"
cd "$work"

firebase="${ZORG_FIREBASE_URL:-}"
password="${ZORG_DEV_PASSWORD:-zorg-dev-password}"
if [[ -n "$firebase" ]]; then
    if [[ ! "$firebase" =~ ^http://(127\.0\.0\.1|localhost)[:/] ]]; then
        echo "Refusing to wipe $firebase: the Firebase smoke test only runs against a local emulator"
        exit 1
    fi
    export FIREBASE_AUTH_EMULATOR_HOST="${FIREBASE_AUTH_EMULATOR_HOST:-127.0.0.1:9099}"
    export ZORG_FIREBASE_ADMIN_TOKEN=owner
    db_base="${firebase%%\?*}"
    db_query=""
    [[ "$firebase" == *\?* ]] && db_query="${firebase#*\?}"
    db_url() { echo "${db_base%/}/$1.json?$db_query"; }
    admin_curl() { curl -sf -H "Authorization: Bearer owner" "$@"; }
    admin_curl -X PUT --data-binary @"$root/database.rules.json" "$(db_url .settings/rules)" > /dev/null
    admin_curl -X PUT -d '{}' "$(db_url "")" > /dev/null
    "$root/scripts/create-emulator-accounts.sh" > /dev/null
    echo "Running against the Firebase emulator at $firebase"
fi

login() {
    if [[ -n "$firebase" ]]; then
        printf '%s@zorg.test\n%s\n' "$1" "$password"
    else
        printf '%s\n' "$2"
    fi
}

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

if [[ -n "$firebase" ]]; then
    echo "Migration loads the accounts into Firebase"
    java="${JAVA_HOME:+$JAVA_HOME/bin/}java"
    if ! "$java" -cp "$root/build/install/zorg-app/lib/*" patients.MigrateToFirebase > migrate.out 2>&1; then
        echo "  FAIL: migration exited with an error"
        cat migrate.out
        exit 1
    fi
    expect migrate.out "Migrated 8 users and 0 patients"
fi

echo "Doctor adds a patient with medication, adds more medication and sees every field"
# Accounts are listed by profession, then name:
# 1 Jansen, 2 Hendriks (doctors), 3 Mulder, 4 de Vries (pharmacists),
# 5 de Jong, 6 Bakker (physios), 7 Peters, 8 Visser (dentists).
# account 1 = Dr. Max Jansen; menu 2 = Add patient (care team: no other doctor, pharmacist 2 = de Vries,
# physio 2 = Bakker, dentist 2 = Visser; one medication, then n),
# 3 = Edit Patient -> patient 1 -> field 7 Medication -> 1 Add medication,
# 1 = View patients, 0 = Quit
run_app doctor.out < <(login jansen 1; cat <<'EOF'
2
Jane
Doe
15-06-1995
70
1.8
Elm Street 58
0
2
2
2
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
)
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

if [[ -n "$firebase" ]]; then
    echo "Patient and index are saved to Firebase"
    admin_curl "$(db_url patients)" > stored.json || echo "{}" > stored.json
    admin_curl "$(db_url userPatients)" > index.json || echo "{}" > index.json
    expect stored.json '"firstName":"Jane"'
    expect stored.json '"birthDate":"1995-06-15"'
    expect stored.json '"jansen":true'
    expect stored.json '"devries":true'
    expect stored.json '"name":"Paracetamol"'
    expect stored.json '"name":"Ibuprofen"'
    expect index.json '"devries":{'
    reject index.json '"mulder"'
    [[ -f patients.json ]] && { echo "  FAIL: patients.json was written in Firebase mode"; failures=$((failures + 1)); }
elif [[ -f patients.json ]]; then
    echo "Patient is saved to patients.json"
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
# account 4 = Pieter de Vries (Pharmacist); menu 1 = View patients, 0 = Quit
run_app pharmacist.out < <(login devries 4; cat <<'EOF'
1
0
EOF
)
expect pharmacist.out "First name: Jane"
expect pharmacist.out "Last name: Doe"
expect pharmacist.out "Date of birth: 1995-06-15"
expect pharmacist.out "Medication: Paracetamol 500 mg, 3x daily; Ibuprofen 400 mg, as needed"
reject pharmacist.out "Weight:"
reject pharmacist.out "Height:"
reject pharmacist.out "Address:"
reject pharmacist.out "Add patient"

echo "Users who are not on the care team don't see the patient"
# account 2 = Dr. Noor Hendriks, 3 = Daan Mulder (Pharmacist); menu 1 = View patients, 0 = Quit
run_app other-doctor.out < <(login hendriks 2; cat <<'EOF'
1
0
EOF
)
expect other-doctor.out "Welcome Dr. Noor Hendriks (Doctor)"
reject other-doctor.out "First name: Jane"
run_app other-pharmacist.out < <(login mulder 3; cat <<'EOF'
1
0
EOF
)
expect other-pharmacist.out "Welcome Daan Mulder (Pharmacist)"
reject other-pharmacist.out "First name: Jane"

echo "Dentist can't pick medication when editing a patient"
# account 8 = Lotte Visser (Dentist); menu 2 = Edit Patient -> patient 1 -> 0 Back, 0 = Quit
run_app dentist.out < <(login visser 8; cat <<'EOF'
2
1
0
0
EOF
)
expect dentist.out "Which field would you like to edit?"
expect dentist.out "Address: Elm Street 58"
reject dentist.out "Medication"

echo "Dentist edits the address"
run_app dentist-edit.out < <(login visser 8; cat <<'EOF'
2
1
3
Oak Lane 1
1
0
EOF
)
expect dentist-edit.out "Patient Updated"
expect dentist-edit.out "Address: Oak Lane 1"
reject dentist-edit.out "Permission denied"

if [[ -n "$firebase" ]]; then
    echo "Wrong passwords are rejected"
    run_app wrong-password.out < <(printf 'jansen@zorg.test\nnope\n%.0s' 1 2 3)
    expect wrong-password.out "Wrong email or password"
    expect wrong-password.out "Too many failed attempts"
    reject wrong-password.out "Welcome"
fi

if (( failures > 0 )); then
    echo "Smoke test failed: $failures check(s) failed"
    exit 1
fi
echo "Smoke test passed"
