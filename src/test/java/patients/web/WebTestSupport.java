package patients.web;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import patients.web.session.ClientHeaderInterceptor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

final class WebTestSupport {

    static final String JANE = """
            {"firstName": "Jane", "lastName": "Doe", "birthDate": "1995-06-15", "weight": 70, "height": 1.8,
             "address": "Elm Street 58", "careTeam": ["devries", "visser"],
             "medications": [{"name": "Paracetamol", "dosage": "500 mg", "frequency": "3x daily"}]}
            """;

    private WebTestSupport() {}

    static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request
                .header(ClientHeaderInterceptor.HEADER, ClientHeaderInterceptor.VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    static MockHttpSession login(MockMvc mvc, String userId) throws Exception {
        return (MockHttpSession) mvc.perform(json(post("/api/auth/login"), "{\"userId\": \"" + userId + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    static String addJane(MockMvc mvc) throws Exception {
        String body = mvc.perform(json(post("/api/patients"), JANE).session(login(mvc, "jansen")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
