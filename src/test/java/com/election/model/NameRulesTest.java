package com.election.model;

import com.election.exception.InvalidNameException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameRulesTest {

    // --- happy paths

    @ParameterizedTest
    @ValueSource(strings = {"Amara Mensah", "Jean-Luc Picard", "Conan O'Brien", "Dr. Who", "Agent 47", "Inès Moreau"})
    void lettersDigitsSpacesHyphensApostrophesAndFullStopsAreValid(String name) {
        assertTrue(NameRules.isValidName(name));
        assertDoesNotThrow(() -> NameRules.requireValidName(name, "Candidate name"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Łukasz Nowak", "Zoë Müller", "Søren Ørsted", "Ngozi Okonjo-Iweala"})
    void lettersFromAnyLanguageAreValid(String name) {
        assertTrue(NameRules.isValidName(name));
    }

    // --- boundaries

    @ParameterizedTest
    @ValueSource(strings = {"A", "7", "007", "Ana  Ben"})
    void aSingleCharacterDigitsOnlyAndDoubleSpacesAreStillValid(String name) {
        assertTrue(NameRules.isValidName(name));
    }

    // --- errors

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void missingOrBlankNamesAreInvalid(String name) {
        assertFalse(NameRules.isValidName(name));
        assertThrows(InvalidNameException.class, () -> NameRules.requireValidName(name, "Candidate name"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Smith, John", "Zoe@Home", "Zoe#1", "Ana!", "Ana/Ben", "Ana_Ben", "Ana & Ben", "50%"})
    void commasAndOtherSymbolsAreInvalid(String name) {
        assertFalse(NameRules.isValidName(name));
        assertThrows(InvalidNameException.class, () -> NameRules.requireValidName(name, "Candidate name"));
    }

    @Test
    void theMessageNamesWhatWasWrong() {
        InvalidNameException e = assertThrows(InvalidNameException.class,
                () -> NameRules.requireValidName("Zoe#1", "Candidate name"));
        assertTrue(e.getMessage().startsWith("Candidate name 'Zoe#1' may only contain"));
    }
}
