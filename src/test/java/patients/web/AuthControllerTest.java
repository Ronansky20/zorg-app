package patients.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import patients.web.session.ClientHeaderInterceptor;
import patients.web.session.LoginThrottle;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static patients.web.WebTestSupport.json;
import static patients.web.WebTestSupport.login;

@WebMvcTest
@Import(TestWebConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void configDescribesTheModeAndLabels() throws Exception {
        mvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("json"))
                .andExpect(jsonPath("$.professions", hasSize(4)))
                .andExpect(jsonPath("$.fields[0].id").value("firstName"))
                .andExpect(jsonPath("$.fields[0].label").value("First name"))
                .andExpect(jsonPath("$.medicationTypes[*].id", contains("PILL", "INJECTION", "OINTMENT")))
                .andExpect(jsonPath("$.medications", hasSize(24)))
                .andExpect(jsonPath("$.medications[0].name").value("Paracetamol"))
                .andExpect(jsonPath("$.medications[0].type").value("PILL"));
    }

    @Test
    void accountsAreSortedByProfessionThenName() throws Exception {
        mvc.perform(get("/api/auth/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains("jansen", "hendriks", "devries", "bakker", "visser")));
    }

    @Test
    void loginReturnsTheUserAndTheirPermissions() throws Exception {
        mvc.perform(json(post("/api/auth/login"), "{\"userId\": \"devries\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("Pieter de Vries"))
                .andExpect(jsonPath("$.user.profession").value("PHARMACIST"))
                .andExpect(jsonPath("$.actions", contains("VIEW_LIST")))
                .andExpect(jsonPath("$.viewableFields", contains("firstName", "lastName", "birthDate", "medications")))
                .andExpect(jsonPath("$.editableFields", hasSize(0)));

        mvc.perform(json(post("/api/auth/login"), "{\"userId\": \"jansen\"}"))
                .andExpect(jsonPath("$.actions", contains("VIEW_LIST", "ADD_PATIENT", "EDIT_PATIENT")))
                .andExpect(jsonPath("$.editableFields", hasSize(7)));
    }

    @Test
    void anUnknownAccountIsUnauthorized() throws Exception {
        mvc.perform(json(post("/api/auth/login"), "{\"userId\": \"nobody\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Unknown account"));
    }

    @Test
    void repeatedFailuresAreThrottled() throws Exception {
        for (int i = 0; i < LoginThrottle.MAX_PER_ACCOUNT; i++) {
            mvc.perform(json(post("/api/auth/login"), "{\"userId\": \"throttled\"}"))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(json(post("/api/auth/login"), "{\"userId\": \"throttled\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.detail").value("Too many failed attempts, try again in 15 minutes"));
    }

    @Test
    void loginTellsTheSessionTimeout() throws Exception {
        mvc.perform(json(post("/api/auth/login"), "{\"userId\": \"jansen\"}"))
                .andExpect(jsonPath("$.sessionTimeoutSeconds").isNumber());
    }

    @Test
    void loginNeedsTheClientHeader() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"userId\": \"jansen\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Requests that change data need the header "
                        + ClientHeaderInterceptor.HEADER + ": " + ClientHeaderInterceptor.VALUE));
    }

    @Test
    void meNeedsASession() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/me").session(login(mvc, "visser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value("visser"));
    }

    @Test
    void loginChangesTheSessionId() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String before = session.getId();

        mvc.perform(json(post("/api/auth/login"), "{\"userId\": \"jansen\"}").session(session))
                .andExpect(status().isOk());

        assertNotEquals(before, session.getId());
    }

    @Test
    void logoutInvalidatesTheSession() throws Exception {
        MockHttpSession session = login(mvc, "jansen");

        mvc.perform(json(post("/api/auth/logout"), "").session(session))
                .andExpect(status().isNoContent());

        assertTrue(session.isInvalid());
    }

    @Test
    void usersNeedASession() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/users").session(login(mvc, "jansen")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    void usersCanBeFilteredByProfession() throws Exception {
        MockHttpSession jansen = login(mvc, "jansen");

        mvc.perform(get("/api/users").param("profession", "DOCTOR").session(jansen))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains("jansen", "hendriks")));

        mvc.perform(get("/api/users").param("profession", "NURSE").session(jansen))
                .andExpect(status().isBadRequest());
    }
}
