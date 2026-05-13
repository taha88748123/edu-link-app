package ma.naja7i.profile.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.naja7i.profile.application.AdminProfService;
import ma.naja7i.profile.application.FileStorageService;
import ma.naja7i.profile.application.ProfileService;
import ma.naja7i.profile.domain.Profile;
import ma.naja7i.profile.infrastructure.web.dto.UpsertProfProfileRequest;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.ProfStatus;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.dto.SchoolLevel;
import ma.naja7i.shared.security.JwtClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.minio.MinioClient;
import ma.naja7i.profile.application.ProfileService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test against a real Postgres (Testcontainers).
 * Mocks Kafka template and MinIO clients so the test is hermetic.
 *
 * <p>Flow:
 * 1. Simulate auth.user.registered → profile row created.
 * 2. PROF upserts profile (subjects, modalities, location).
 * 3. ADMIN validates the prof.
 * 4. Public catalog now returns the prof.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ProfileControllerIT {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("profile_db")
            .withUsername("profile_user")
            .withPassword("profile_pwd");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean
    MinioClient minioClient;

    @MockBean
    FileStorageService fileStorageService;

    @TestConfiguration
    static class TestKafkaConfig {
        @Bean
        @SuppressWarnings({"unchecked", "rawtypes"})
        public org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate() {
            return (org.springframework.kafka.core.KafkaTemplate)
                    org.mockito.Mockito.mock(org.springframework.kafka.core.KafkaTemplate.class);
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ProfileService profileService;

    @Autowired
    AdminProfService adminProfService;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void prof_onboarding_then_admin_validate_then_public_catalog() throws Exception {
        UUID profId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        // ---- 1) Simulate auth.user.registered: create empty profile ----
        Profile profile = profileService.ensureProfileExists(profId, Role.PROF, "fr");
        assertThat(profile.getRole()).isEqualTo(Role.PROF);

        // ---- 2) PROF upserts profile via HTTP (with gateway headers) ----
        UpsertProfProfileRequest req = new UpsertProfProfileRequest(
                5, null,
                List.of(new UpsertProfProfileRequest.SubjectEntry("MATH", SchoolLevel.LYCEE)),
                List.of(Modality.ONLINE),
                List.of(new UpsertProfProfileRequest.LocationEntry("Casablanca", "Casablanca-Settat")));

        mvc.perform(put("/api/v1/profile/me/prof")
                        .header(JwtClaims.HEADER_USER_ID, profId.toString())
                        .header(JwtClaims.HEADER_USER_ROLE, "PROF")
                        .header(JwtClaims.HEADER_LOCALE, "fr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.data.yearsExperience").value(5));

        // ---- 3) ADMIN validates (call the service directly to stay decoupled from controller) ----
        adminProfService.validate(profId, adminId, "ok");

        assertThat(profileService.findProfProfile(profId).orElseThrow().getStatus())
                .isEqualTo(ProfStatus.VALIDE);

        // ---- 4) Public catalog returns the validated prof ----
        mvc.perform(get("/api/v1/profiles/profs")
                        .header(JwtClaims.HEADER_USER_ID, UUID.randomUUID().toString())
                        .header(JwtClaims.HEADER_USER_ROLE, "STUDENT")
                        .param("subject", "MATH")
                        .param("level", "LYCEE")
                        .param("modality", "ONLINE")
                        .param("city", "Casablanca"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(profId.toString()));

        // ---- 5) Single prof endpoint ----
        mvc.perform(get("/api/v1/profiles/profs/" + profId)
                        .header(JwtClaims.HEADER_USER_ID, UUID.randomUUID().toString())
                        .header(JwtClaims.HEADER_USER_ROLE, "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(profId.toString()))
                .andExpect(jsonPath("$.data.yearsExperience").value(5));
    }
}
