package com.kov.techuserservice.observability;

import com.kov.techuserservice.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P2 Observability: actuator/health+info открыты, metrics/prometheus за аутентификацией,
 * MdcLoggingFilter проставляет X-Request-Id, OpenAPI-группы доступны.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ObservabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void health_ShouldBeOpen() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists(MdcLoggingFilter.REQUEST_ID_HEADER))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void info_ShouldBeOpen() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(header().exists(MdcLoggingFilter.REQUEST_ID_HEADER));
    }

    @Test
    void metricsAndPrometheus_ShouldRequireAuth() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestId_ShouldBeEchoed() throws Exception {
        mockMvc.perform(get("/actuator/health").header(MdcLoggingFilter.REQUEST_ID_HEADER, "test-trace-1"))
                .andExpect(status().isOk())
                .andExpect(header().string(MdcLoggingFilter.REQUEST_ID_HEADER, "test-trace-1"));
    }

    @Test
    void openApiGroups_ShouldBeAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs/users"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs/auth"))
                .andExpect(status().isOk());
    }
}
