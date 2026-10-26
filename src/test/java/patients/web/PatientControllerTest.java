package patients.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import patients.testing.InMemoryPatientRepository;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static patients.web.WebTestSupport.JANE;
import static patients.web.WebTestSupport.addJane;
import static patients.web.WebTestSupport.json;
import static patients.web.WebTestSupport.login;

@WebMvcTest
@Import(TestWebConfig.class)
class PatientControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private InMemoryPatientRepository repository;

    @BeforeEach
    void emptyRepository() {
        repository.clear();
    }

    @Test
    void listingPatientsNeedsASignIn() throws Exception {
        mvc.perform(get("/api/patients"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Please sign in"));
    }

    @Test
    void aDoctorAddsAPatient() throws Exception {
        MockHttpSession jansen = login(mvc, "jansen");

        mvc.perform(json(post("/api/patients"), JANE).session(jansen))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/patients/")))
                .andExpect(jsonPath("$.birthDate").value("1995-06-15"))
                .andExpect(jsonPath("$.careTeam[*].id", containsInAnyOrder("devries", "visser", "jansen")))
                .andExpect(jsonPath("$.careTeam[?(@.id == 'jansen')].name").value("Dr. Max Jansen"));

        mvc.perform(get("/api/patients").session(jansen))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].weight").value(70.0))
                .andExpect(jsonPath("$[0].bmi").value(21.6))
                .andExpect(jsonPath("$[0].bmiCategory").value("Healthy weight"));
    }

    @Test
    void aPharmacistDoesNotSeeHiddenFields() throws Exception {
        addJane(mvc);

        mvc.perform(get("/api/patients").session(login(mvc, "devries")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Jane"))
                .andExpect(jsonPath("$[0].medications[0].name").value("Paracetamol"))
                .andExpect(jsonPath("$[0].weight").doesNotExist())
                .andExpect(jsonPath("$[0].height").doesNotExist())
                .andExpect(jsonPath("$[0].bmi").doesNotExist())
                .andExpect(jsonPath("$[0].bmiCategory").doesNotExist())
                .andExpect(jsonPath("$[0].address").doesNotExist());
    }

    @Test
    void aDentistCanNotAddPatients() throws Exception {
        mvc.perform(json(post("/api/patients"), JANE).session(login(mvc, "visser")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Dentist is not allowed to Add patient"));
    }

    @Test
    void aDentistCanEditTheAddressButNotTheMedication() throws Exception {
        String id = addJane(mvc);
        MockHttpSession visser = login(mvc, "visser");

        mvc.perform(json(patch("/api/patients/" + id), "{\"address\": \"Oak Lane 1\"}").session(visser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("Oak Lane 1"))
                .andExpect(jsonPath("$.firstName").value("Jane"));

        mvc.perform(json(patch("/api/patients/" + id), "{\"medications\": []}").session(visser))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You do not have the permission to edit Medication"));
    }

    @Test
    void nonMembersGetNotFound() throws Exception {
        String id = addJane(mvc);
        MockHttpSession hendriks = login(mvc, "hendriks");

        mvc.perform(get("/api/patients/" + id).session(hendriks))
                .andExpect(status().isNotFound());
        mvc.perform(json(patch("/api/patients/" + id), "{\"address\": \"Oak Lane 1\"}").session(hendriks))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/patients").session(hendriks))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void invalidInputIsABadRequest() throws Exception {
        MockHttpSession jansen = login(mvc, "jansen");

        mvc.perform(json(post("/api/patients"), JANE.replace("1995-06-15", "2999-01-01")).session(jansen))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A date of birth can't be in the future"));
        mvc.perform(json(post("/api/patients"), JANE.replace("Paracetamol", " ")).session(jansen))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The medication name can't be empty"));
        mvc.perform(json(post("/api/patients"), JANE.replace("Paracetamol", "Aspirin")).session(jansen))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown medication: Aspirin"));
        mvc.perform(json(post("/api/patients"), JANE.replace("visser", "nobody")).session(jansen))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown care team member: nobody"));
        mvc.perform(json(post("/api/patients"), JANE.replace("1995-06-15", "15-06-1995")).session(jansen))
                .andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/patients"), "{").session(jansen))
                .andExpect(status().isBadRequest());
    }

    @Test
    void writesWithoutTheClientHeaderAreForbidden() throws Exception {
        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content(JANE)
                        .session(login(mvc, "jansen")))
                .andExpect(status().isForbidden());
    }

    @Test
    void storageErrorsAreABadGateway() throws Exception {
        MockHttpSession jansen = login(mvc, "jansen");
        repository.broken = true;

        mvc.perform(get("/api/patients").session(jansen))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail", containsString("Repository is broken")));
    }
}
