package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.util.PasswordValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ConfigController (public endpoints, no authentication)")
class ConfigControllerTest extends AbstractApiControllerTest {

    @Test
    void config_exposesRegistrationFlagAndPasswordPolicy() throws Exception {
        mockMvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationEnabled").value(true))
                .andExpect(jsonPath("$.passwordPolicy.minLength").value(PasswordValidator.MIN_LENGTH))
                .andExpect(jsonPath("$.passwordPolicy.maxLength").value(PasswordValidator.MAX_LENGTH))
                .andExpect(jsonPath("$.passwordPolicy.specialChars").value(PasswordValidator.SPECIAL_CHARS));
    }

    @Test
    void countries_returnsCodeNamePairs() throws Exception {
        mockMvc.perform(get("/api/countries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(100)))
                .andExpect(jsonPath("$[?(@.code=='DE')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.code=='DE')].name").isNotEmpty());
    }

    @Test
    void subdivisions_ofGermany_containBavaria() throws Exception {
        mockMvc.perform(get("/api/countries/DE/subdivisions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(16))
                .andExpect(jsonPath("$[?(@.code=='DE-BY')]").isNotEmpty());
    }

    @Test
    void subdivisions_ofUnknownCountry_areEmpty() throws Exception {
        mockMvc.perform(get("/api/countries/ZZ/subdivisions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
