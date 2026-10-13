package patients.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static patients.web.WebTestSupport.addJane;
import static patients.web.WebTestSupport.json;
import static patients.web.WebTestSupport.login;

@SpringBootTest
@AutoConfigureMockMvc
class WebAppIntegrationTest {

    private static Path dataDir;

    @Autowired
    private MockMvc mvc;

    @DynamicPropertySource
    static void useATemporaryDataDir(DynamicPropertyRegistry registry) throws IOException {
        dataDir = Files.createTempDirectory("zorg-web-test");
        Files.copy(Path.of("users.json"), dataDir.resolve("users.json"));
        registry.add("zorg.data-dir", dataDir::toString);
        registry.add("ZORG_FIREBASE_URL", () -> "");
    }

    @Test
    void patientsAreSavedToTheJsonFile() throws Exception {
        String id = addJane(mvc);
        MockHttpSession jansen = login(mvc, "jansen");

        mvc.perform(json(patch("/api/patients/" + id), "{\"weight\": 72.5}").session(jansen))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weight").value(72.5));

        mvc.perform(get("/api/patients").session(login(mvc, "bakker")))
                .andExpect(jsonPath("$", hasSize(0)));

        String file = Files.readString(dataDir.resolve("patients.json"));
        assertTrue(file.contains("\"" + id + "\""), file);
        assertTrue(file.contains("\"birthDate\": \"1995-06-15\""), file);
        assertTrue(file.contains("\"weight\": 72.5"), file);
    }
}
