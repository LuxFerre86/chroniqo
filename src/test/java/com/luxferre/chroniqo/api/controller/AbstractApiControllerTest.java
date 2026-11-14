package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.service.SummaryService;
import com.luxferre.chroniqo.service.TimeTrackingService;
import com.luxferre.chroniqo.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Shared setup for the REST controller tests: the full application context
 * (including the real security configuration and exception handler) with the
 * business services replaced by mocks. Sharing the mock set keeps the number
 * of cached application contexts at one.
 *
 * <p>MockMvc is built manually with {@code springSecurity()} so that
 * {@code @WithMockUser} and {@code csrf()} work without the optional Boot
 * security test module.
 */
@SpringBootTest
abstract class AbstractApiControllerTest {

    @Autowired
    private WebApplicationContext context;

    protected MockMvc mockMvc;

    @MockitoBean
    protected UserService userService;

    @MockitoBean
    protected TimeTrackingService timeTrackingService;

    @MockitoBean
    protected SummaryService summaryService;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
}
