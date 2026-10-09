#!/usr/bin/env bash
# End-to-end smoke test: builds the app, then drives the real console UI with scripted
# input and checks what it prints and what it writes to patients.json.
# Covers the wiring the unit tests skip: App -> ConsoleUI -> PatientService -> JsonPatientRepository.
#
# Usage: ./scripts/smoke-test.sh   (from anywhere; needs a JDK on PATH / JAVA_HOME)
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
"$root/gradlew" -p "$root" installDist -q --console=plain
app="$root/build/install/zorg-app/bin/zorg-app"

# the app resolves patients.json against the working directory, so run it in a scratch dir
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
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

echo "Doctor adds a patient and sees every field"
# profession 1 = Doctor; menu 2 = Add patient, 1 = View patients, 0 = Quit
run_app doctor.out <<'EOF'
1
2
Jane
Doe
15-06-1995
70
1.8
Elm Street 58
Dr. House
Pharma Phil
Physio Paula
Dentist Dan
1
0
EOF
expect doctor.out "Add patient"
expect doctor.out "First name: Jane"
expect doctor.out "Date of birth: 1995-06-15"
expect doctor.out "Weight: 70.0"
expect doctor.out "Address: Elm Street 58"

echo "Patient is saved to patients.json"
if [[ -f patients.json ]]; then
    expect patients.json '"firstName": "Jane"'
    expect patients.json '"birthDate": "1995-06-15"'
else
    echo "  FAIL: patients.json was not written"
    failures=$((failures + 1))
fi

echo "Pharmacist reloads the saved patient but only sees permitted fields"
# profession 2 = Pharmacist; menu 1 = View patients, 0 = Quit
run_app pharmacist.out <<'EOF'
2
1
0
EOF
expect pharmacist.out "First name: Jane"
expect pharmacist.out "Last name: Doe"
expect pharmacist.out "Date of birth: 1995-06-15"
reject pharmacist.out "Weight:"
reject pharmacist.out "Height:"
reject pharmacist.out "Address:"
reject pharmacist.out "Add patient"

if (( failures > 0 )); then
    echo "Smoke test failed: $failures check(s) failed"
    exit 1
fi
echo "Smoke test passed"
