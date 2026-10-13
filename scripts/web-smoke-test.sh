#!/usr/bin/env bash
# End-to-end smoke test of the web API: starts the real Spring Boot app and drives it with curl.
# Usage: ./scripts/web-smoke-test.sh   With ZORG_FIREBASE_URL set to a local emulator it runs through Firebase.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
"$root/gradlew" -p "$root" installDist -q --console=plain
java="${JAVA_HOME:+$JAVA_HOME/bin/}java"
classpath="$root/build/install/zorg-app/lib/*"

work="$(mktemp -d)"
server_pid=""
cleanup() {
    [[ -n "$server_pid" ]] && kill "$server_pid" 2> /dev/null
    rm -rf "$work"
}
trap cleanup EXIT
cp "$root/users.json" "$work/"
cd "$work"

failures=0
expect() {
    if grep -qF -- "$2" "$1"; then
        echo "  ok:   contains '$2'"
    else
        echo "  FAIL: expected '$2' in $1:"
        cat "$1"; echo
        failures=$((failures + 1))
    fi
}
reject() {
    if grep -qF -- "$2" "$1"; then
        echo "  FAIL: did not expect '$2' in $1"
        failures=$((failures + 1))
    else
        echo "  ok:   does not contain '$2'"
    fi
}
status_is() {
    if [[ "$2" == "$1" ]]; then
        echo "  ok:   $3 (HTTP $2)"
    else
        echo "  FAIL: $3: expected HTTP $1, got $2"
        failures=$((failures + 1))
    fi
}

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
    "$java" -cp "$classpath" patients.MigrateToFirebase > migrate.out 2>&1 || { cat migrate.out; exit 1; }
    echo "Running against the Firebase emulator at $firebase"
fi

"$java" -cp "$classpath" patients.web.WebApp --server.port=0 --zorg.data-dir="$work" > server.log 2>&1 &
server_pid=$!
port=""
for _ in $(seq 1 60); do
    port="$(sed -n 's/.*Tomcat started on port \([0-9]*\).*/\1/p' server.log)"
    [[ -n "$port" ]] && break
    kill -0 "$server_pid" 2> /dev/null || { echo "The server exited:"; cat server.log; exit 1; }
    sleep 0.5
done
[[ -n "$port" ]] || { echo "The server did not start:"; cat server.log; exit 1; }
api="http://127.0.0.1:$port/api"

call() { # $1 = user (cookie jar and $1.out), $2 = method, $3 = path, $4 = JSON body; prints the status
    local args=(-s -o "$1.out" -w '%{http_code}' -b "$1.jar" -c "$1.jar" -X "$2" -H 'X-Zorg-Client: web')
    [[ $# -ge 4 ]] && args+=(-H 'Content-Type: application/json' --data "$4")
    curl "${args[@]}" "$api$3"
}
login() {
    : > "$1.jar"
    local body="{\"userId\": \"$1\"}"
    [[ -n "$firebase" ]] && body="{\"email\": \"$1@zorg.test\", \"password\": \"$password\"}"
    status_is 200 "$(call "$1" POST /auth/login "$body")" "$1 signs in"
}

jane='{"firstName": "Jane", "lastName": "Doe", "birthDate": "1995-06-15", "weight": 70, "height": 1.8,
       "address": "Elm Street 58", "careTeam": ["devries", "bakker", "visser"],
       "medications": [{"name": "Paracetamol", "dosage": "500 mg", "frequency": "3x daily"}]}'
both='[{"name": "Paracetamol", "dosage": "500 mg", "frequency": "3x daily"},
       {"name": "Ibuprofen", "dosage": "400 mg", "frequency": "as needed"}]'

echo "Without a session or the client header requests are rejected"
: > anonymous.jar
status_is 401 "$(call anonymous GET /patients)" "listing patients without signing in"
status_is 403 "$(curl -s -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' \
    --data '{"userId": "jansen"}' "$api/auth/login")" "signing in without the X-Zorg-Client header"
status_is 200 "$(call anonymous GET /config)" "reading the config"
expect anonymous.out "\"mode\":\"$([[ -n "$firebase" ]] && echo firebase || echo json)\""

if [[ -n "$firebase" ]]; then
    echo "A wrong password is rejected"
    : > intruder.jar
    status_is 401 "$(call intruder POST /auth/login '{"email": "jansen@zorg.test", "password": "wrong"}')" \
        "signing in with a wrong password"
fi

echo "Doctor adds a patient with medication, adds more medication and sees every field"
login jansen
status_is 201 "$(call jansen POST /patients "$jane")" "adding Jane"
id="$(grep -o '"id":"[^"]*"' jansen.out | head -1 | cut -d'"' -f4)"
status_is 200 "$(call jansen PATCH "/patients/$id" "{\"medications\": $both}")" "adding Ibuprofen"
status_is 200 "$(call jansen GET /patients)" "listing patients"
expect jansen.out '"firstName":"Jane"'
expect jansen.out '"birthDate":"1995-06-15"'
expect jansen.out '"weight":70.0'
expect jansen.out '"address":"Elm Street 58"'
expect jansen.out '"name":"Ibuprofen"'
expect jansen.out '"name":"Pieter de Vries"'
expect jansen.out '"name":"Dr. Max Jansen"'
status_is 400 "$(call jansen POST /patients "${jane/1995-06-15/2999-01-01}")" "adding a patient born in the future"

if [[ -n "$firebase" ]]; then
    echo "Patient and index are saved to Firebase"
    admin_curl "$(db_url patients)" > stored.json || echo "{}" > stored.json
    admin_curl "$(db_url userPatients)" > index.json || echo "{}" > index.json
    expect stored.json '"firstName":"Jane"'
    expect stored.json '"name":"Ibuprofen"'
    expect index.json '"devries":{'
    reject index.json '"mulder"'
else
    echo "Patient is saved to patients.json"
    touch patients.json
    expect patients.json '"firstName": "Jane"'
    expect patients.json '"jansen": true'
    expect patients.json '"name": "Ibuprofen"'
fi

echo "Pharmacist on the care team only sees permitted fields and can't edit"
login devries
status_is 200 "$(call devries GET /patients)" "listing patients"
expect devries.out '"firstName":"Jane"'
expect devries.out '"name":"Paracetamol"'
reject devries.out '"weight"'
reject devries.out '"height"'
reject devries.out '"address"'
status_is 403 "$(call devries PATCH "/patients/$id" '{"address": "Oak Lane 1"}')" "editing the address"

echo "Users who are not on the care team don't see the patient"
for user in hendriks mulder; do
    login "$user"
    status_is 200 "$(call "$user" GET /patients)" "$user lists patients"
    expect "$user.out" '[]'
    status_is 404 "$(call "$user" GET "/patients/$id")" "$user opens Jane"
done

echo "Dentist edits the address but not the medication or date of birth"
login visser
status_is 200 "$(call visser PATCH "/patients/$id" '{"address": "Oak Lane 1"}')" "editing the address"
expect visser.out '"address":"Oak Lane 1"'
status_is 403 "$(call visser PATCH "/patients/$id" '{"medications": []}')" "removing the medication"
status_is 403 "$(call visser PATCH "/patients/$id" '{"birthDate": "1990-01-01"}')" "editing the date of birth"

echo "Physiotherapist edits the weight"
login bakker
status_is 200 "$(call bakker PATCH "/patients/$id" '{"weight": 72.5}')" "editing the weight"
expect bakker.out '"weight":72.5'

echo "Signing out ends the session"
status_is 204 "$(call jansen POST /auth/logout)" "signing out"
status_is 401 "$(call jansen GET /me)" "reading /me after signing out"

if [[ $failures -gt 0 ]]; then
    echo "$failures check(s) failed, server log:"
    cat server.log
    exit 1
fi
echo "Web smoke test passed"
