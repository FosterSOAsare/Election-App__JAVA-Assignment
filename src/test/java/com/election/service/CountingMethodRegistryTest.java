package com.election.service;

import com.election.model.BallotType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CountingMethodRegistryTest {

    private final CountingMethodRegistry registry = new CountingMethodRegistry();

    @Test
    void candidateTypesAreCountedByMostVotes() {
        assertInstanceOf(MostVotesCount.class, registry.methodFor(BallotType.SELECT_ONE));
        assertInstanceOf(MostVotesCount.class, registry.methodFor(BallotType.SELECT_MULTIPLE));
    }

    @Test
    void yesNoIsCountedAsYesAgainstNo() {
        assertInstanceOf(YesNoCount.class, registry.methodFor(BallotType.YES_NO));
    }

    @Test
    void selectOneAndSelectMultipleShareOneCountingMethod() {
        assertSame(registry.methodFor(BallotType.SELECT_ONE), registry.methodFor(BallotType.SELECT_MULTIPLE));
    }

    @ParameterizedTest
    @EnumSource(BallotType.class)
    void everyBallotTypeHasACountingMethod(BallotType type) {
        assertNotNull(registry.methodFor(type));
    }
}
