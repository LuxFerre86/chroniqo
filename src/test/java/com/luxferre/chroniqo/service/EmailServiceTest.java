package com.luxferre.chroniqo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    // =========================================================================
    // anonymize
    // =========================================================================

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "john.doe@example.com,    joh***@example.com",
            "ab@example.com,          ab***@example.com",
            "a@example.com,           a***@example.com",
            "test@gmail.com,          tes***@gmail.com",
            "user@sub.domain.org,     use***@sub.domain.org",
    })
    void anonymize_masksLocalPartAndPreservesDomain(String input, String expected) {
        assertThat(emailService.anonymize(input.strip())).isEqualTo(expected.strip());
    }

    @Test
    void anonymize_nullEmail_returnsPlaceholder() {
        assertThat(emailService.anonymize(null)).isEqualTo("[null]");
    }

    @Test
    void anonymize_noAtSign_returnsInvalidPlaceholder() {
        assertThat(emailService.anonymize("notanemail")).isEqualTo("[invalid]");
    }

    @Test
    void anonymize_atSignAtStart_returnsInvalidPlaceholder() {
        assertThat(emailService.anonymize("@example.com")).isEqualTo("[invalid]");
    }

    @Test
    void anonymize_doesNotRevealFullLocalPart() {
        String result = emailService.anonymize("verylongemail@example.com");
        assertThat(result).startsWith("ver***");
        assertThat(result).doesNotContain("verylongemail");
    }
}