package com.demo.productapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "app.admin.email=" + ProductApiIntegrationTests.ADMIN_EMAIL,
        "app.admin.password=" + ProductApiIntegrationTests.ADMIN_PASSWORD
})
@AutoConfigureMockMvc
class ProductApiIntegrationTests {

    static final String ADMIN_EMAIL = "admin@test.local";
    static final String ADMIN_PASSWORD = "test-admin-password";

    private static final String KEYBOARD = """
            {"name": "Mechanical Keyboard", "description": "Hot-swappable", "price": 79.90, "stock": 25}
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void registerReturnsTokenAndRejectsDuplicateEmail() throws Exception {
        String body = credentials("duplicate@test.local", "password-123");

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void registerRejectsInvalidPayloadWithFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("not-an-email", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(ADMIN_EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void productsRequireAValidToken() throws Exception {
        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mvc.perform(get("/api/v1/products").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void regularUserCanReadButNotWrite() throws Exception {
        String token = register("reader@test.local", "password-123");

        mvc.perform(get("/api/v1/products").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/products").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(KEYBOARD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminCanCreateReadUpdateAndDeleteProducts() throws Exception {
        String token = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        MvcResult created = mvc.perform(post("/api/v1/products").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(KEYBOARD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mechanical Keyboard"))
                .andReturn();
        int id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        String path = "/api/v1/products/" + id;
        assertThat(created.getResponse().getHeader(HttpHeaders.LOCATION)).endsWith(path);

        mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(79.90));

        mvc.perform(get("/api/v1/products").param("q", "keyboard").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(put(path).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Mechanical Keyboard v2", "price": 89.00, "stock": 10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mechanical Keyboard v2"))
                .andExpect(jsonPath("$.stock").value(10));

        mvc.perform(delete(path).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRejectsNegativePrice() throws Exception {
        String token = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mvc.perform(post("/api/v1/products").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Broken", "price": -1, "stock": 1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    private String register(String email, String password) throws Exception {
        return bearerToken("/api/v1/auth/register", email, password);
    }

    private String login(String email, String password) throws Exception {
        return bearerToken("/api/v1/auth/login", email, password);
    }

    private String bearerToken(String path, String email, String password) throws Exception {
        String response = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, password)))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(response, "$.accessToken");
    }

    private static String credentials(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }
}
