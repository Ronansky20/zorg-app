package patients;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import patients.auth.FirebaseAuth;
import patients.model.Medication;
import patients.model.Patient;
import patients.model.Profession;
import patients.model.User;
import patients.repository.FirebaseClient;
import patients.repository.FirebasePatientRepository;
import patients.repository.FirebaseUserRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("emulator")
class FirebaseRulesEmulatorTest {

    private static final String PROJECT = "demo-zorg-app";
    private static final String PASSWORD = "zorg-dev-password";
    private static final List<User> USERS = List.of(
            new User("jansen", "Dr. Max Jansen", Profession.DOCTOR),
            new User("hendriks", "Dr. Noor Hendriks", Profession.DOCTOR),
            new User("devries", "Pieter de Vries", Profession.PHARMACIST),
            new User("bakker", "Sanne Bakker", Profession.PHYSIOTHERAPIST),
            new User("visser", "Lotte Visser", Profession.DENTIST));

    private static URI databaseUrl;
    private static String authHost;
    private static FirebaseClient admin;

    private final Patient john = new Patient("p1", "John", "Doe", LocalDate.of(1995, 6, 15), 70.0, 1.8,
            "Elm Street 58", Map.of("jansen", true, "devries", true, "bakker", true, "visser", true),
            List.of(new Medication("Paracetamol", "500 mg", "3x daily")));

    @BeforeAll
    static void prepareEmulators() throws IOException, InterruptedException {
        String url = System.getenv("ZORG_FIREBASE_URL");
        authHost = System.getenv("FIREBASE_AUTH_EMULATOR_HOST");
        assumeTrue(url != null && !url.isBlank() && authHost != null && !authHost.isBlank(),
                "set ZORG_FIREBASE_URL and FIREBASE_AUTH_EMULATOR_HOST to run against the emulators");
        assertTrue(url.matches("^http://(127\\.0\\.0\\.1|localhost)[:/].*"),
                "refusing to wipe " + url + ": these tests only run against a local emulator");

        databaseUrl = URI.create(url);
        admin = FirebaseClient.admin(databaseUrl, "owner");
        admin.put(".settings/rules", JsonParser.parseString(FirebaseRules.generate()));

        HttpClient http = HttpClient.newHttpClient();
        String authBase = "http://" + authHost;
        send(http, HttpRequest.newBuilder(URI.create(authBase + "/emulator/v1/projects/" + PROJECT + "/accounts"))
                .DELETE().build());
        for (User user : USERS) {
            JsonObject account = new JsonObject();
            account.addProperty("localId", user.id());
            account.addProperty("email", user.id() + "@zorg.test");
            account.addProperty("password", PASSWORD);
            send(http, HttpRequest.newBuilder(URI.create(authBase
                            + "/identitytoolkit.googleapis.com/v1/projects/" + PROJECT + "/accounts"))
                    .header("Authorization", "Bearer owner")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(account.toString()))
                    .build());
        }
    }

    private static void send(HttpClient http, HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), request.uri() + ": " + response.body());
    }

    @BeforeEach
    void resetDatabase() {
        admin.put("", new JsonObject());
        FirebaseUserRepository users = new FirebaseUserRepository(admin);
        USERS.forEach(users::save);
        new FirebasePatientRepository(admin).save(john);
    }

    private static FirebaseClient clientFor(String userId) {
        FirebaseAuth auth = FirebaseAuth.forEmulator(authHost);
        auth.signIn(userId + "@zorg.test", PASSWORD);
        return new FirebaseClient(databaseUrl, auth::idToken);
    }

    private static FirebasePatientRepository patientsFor(String userId) {
        return new FirebasePatientRepository(clientFor(userId));
    }

    private static void assertDenied(Executable request) {
        UncheckedIOException e = assertThrows(UncheckedIOException.class, request);
        assertTrue(e.getMessage().contains("Permission denied"), e.getMessage());
    }

    private Patient newPatient(String id, String... team) {
        Map<String, Boolean> careTeam = new LinkedHashMap<>();
        for (String member : team) {
            careTeam.put(member, true);
        }
        return john.withId(id).withFirstName("Jane").withCareTeam(careTeam);
    }

    @Test
    void careTeamMemberCanReadThePatient() {
        assertEquals(List.of(john), patientsFor("visser").findAllForUser("visser"));
        assertEquals(john, patientsFor("devries").findById("p1").orElseThrow());
    }

    @Test
    void nonMemberCantReadThePatient() {
        FirebasePatientRepository hendriks = patientsFor("hendriks");

        assertDenied(() -> hendriks.findById("p1"));
        assertEquals(List.of(), hendriks.findAllForUser("hendriks"));
    }

    @Test
    void nobodyCanReadAllPatients() {
        assertDenied(() -> patientsFor("jansen").findAll());
    }

    @Test
    void signedOutClientCantReadAnything() {
        FirebaseClient anonymous = new FirebaseClient(databaseUrl, () -> null);

        assertDenied(() -> anonymous.get("users"));
        assertDenied(() -> anonymous.get("patients/p1"));
        assertDenied(() -> anonymous.get("userPatients/jansen"));
    }

    @Test
    void indexIsOnlyReadableByItsOwner() {
        FirebaseClient visser = clientFor("visser");

        assertTrue(visser.get("userPatients/visser/p1").getAsBoolean());
        assertDenied(() -> visser.get("userPatients/jansen"));
    }

    @Test
    void usersAreReadableButNotWritable() {
        FirebaseClient visser = clientFor("visser");

        assertEquals(USERS.size(), new FirebaseUserRepository(visser).findAll().size());
        assertDenied(() -> visser.put("users/visser/profession", new JsonPrimitive("DOCTOR")));
        assertDenied(() -> new FirebaseUserRepository(visser)
                .save(new User("visser2", "Lotte Visser", Profession.DOCTOR)));
    }

    @Test
    void dentistCanEditTheAddress() {
        patientsFor("visser").save(john.withAddress("Oak Lane 1"));

        assertEquals("Oak Lane 1", admin.get("patients/p1/address").getAsString());
    }

    @Test
    void dentistCantEditWeightOrMedication() {
        FirebasePatientRepository visser = patientsFor("visser");

        assertDenied(() -> visser.save(john.withWeight(80.0)));
        assertDenied(() -> visser.save(john.withMedications(List.of())));
        assertEquals(70.0, admin.get("patients/p1/weight").getAsDouble());
    }

    @Test
    void physiotherapistCanEditWeightButNotTheBirthDate() {
        FirebasePatientRepository bakker = patientsFor("bakker");

        bakker.save(john.withWeight(80.0));

        assertEquals(80.0, admin.get("patients/p1/weight").getAsDouble());
        assertDenied(() -> bakker.save(john.withWeight(80.0).withBirthDate(LocalDate.of(1990, 1, 1))));
    }

    @Test
    void pharmacistCantEditAnything() {
        assertDenied(() -> patientsFor("devries").save(john.withFirstName("Johnny")));
    }

    @Test
    void doctorCanEditEveryField() {
        Patient edited = john.withFirstName("Johnny").withBirthDate(LocalDate.of(1990, 1, 1)).withHeight(1.75)
                .withMedications(List.of(new Medication("Ibuprofen", "400 mg", "as needed")));

        patientsFor("jansen").save(edited);

        assertEquals(edited, new FirebasePatientRepository(admin).findById("p1").orElseThrow());
    }

    @Test
    void nonMemberCantEditEvenWithTheRightProfession() {
        FirebaseClient hendriks = clientFor("hendriks");
        JsonObject update = new JsonObject();
        update.addProperty("patients/p1/address", "Oak Lane 1");

        assertDenied(() -> hendriks.patch("", update));
    }

    @Test
    void careTeamCantBeChangedAfterCreation() {
        Map<String, Boolean> team = new LinkedHashMap<>(john.careTeam());
        team.put("hendriks", true);

        assertDenied(() -> patientsFor("jansen").save(john.withCareTeam(team)));
    }

    @Test
    void indexEntriesCantBeForged() {
        assertDenied(() -> clientFor("hendriks").put("userPatients/hendriks/p1", new JsonPrimitive(true)));
        assertDenied(() -> clientFor("jansen").put("userPatients/hendriks/p1", new JsonPrimitive(true)));
    }

    @Test
    void invalidValuesAndUnknownFieldsAreRejected() {
        FirebaseClient jansen = clientFor("jansen");

        assertDenied(() -> jansen.put("patients/p1/weight", new JsonPrimitive(-5)));
        assertDenied(() -> jansen.put("patients/p1/firstName", new JsonPrimitive("")));
        assertDenied(() -> jansen.put("patients/p1/birthDate", new JsonPrimitive("15-06-1995")));
        assertDenied(() -> jansen.put("patients/p1/ssn", new JsonPrimitive("123")));
    }

    @Test
    void doctorCanCreateAPatientWithThemselvesOnTheTeam() {
        Patient jane = newPatient("p2", "jansen", "visser");

        patientsFor("jansen").save(jane);

        assertEquals(List.of(jane), patientsFor("visser").findAllForUser("visser").stream()
                .filter(p -> p.id().equals("p2")).toList());
    }

    @Test
    void doctorCantCreateAPatientWithoutBeingOnTheTeam() {
        assertDenied(() -> patientsFor("jansen").save(newPatient("p2", "visser")));
    }

    @Test
    void otherProfessionsCantCreatePatients() {
        assertDenied(() -> patientsFor("bakker").save(newPatient("p2", "bakker")));
        assertDenied(() -> patientsFor("visser").save(newPatient("p2", "visser")));
    }

    @Test
    void newPatientMustBeValid() {
        assertDenied(() -> patientsFor("jansen").save(newPatient("p2", "jansen").withWeight(0)));
    }
}
