package com.election.service;

import com.election.model.BallotType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Picks the counting method for a ballot type. A new ballot type only needs a new CountingMethod
 * and one line here; TallyService and the existing counting methods stay unchanged.
 */
public class CountingMethodRegistry {

    // EnumMap is a Map whose keys are the values of one enum; it is small and keeps them in enum order.
    private final Map<BallotType, CountingMethod> methods = new EnumMap<>(BallotType.class);

    public CountingMethodRegistry() {
        CountingMethod mostVotes = new MostVotesCount();
        methods.put(BallotType.SELECT_ONE, mostVotes);
        methods.put(BallotType.SELECT_MULTIPLE, mostVotes);
        methods.put(BallotType.YES_NO, new YesNoCount());
    }

    public CountingMethod methodFor(BallotType ballotType) {
        CountingMethod method = methods.get(ballotType);
        if (method == null) {
            throw new IllegalStateException("No counting method for " + ballotType);
        }
        return method;
    }
}
