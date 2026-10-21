package com.election.service;

import com.election.model.Ballot;
import com.election.model.Category;
import com.election.model.CategoryResult;
import com.election.model.YesNoResult;

import java.util.List;

/** Yes / No: counts the YES and NO answers; YesNoResult decides whether it passed. */
public class YesNoCount implements CountingMethod {

    @Override
    public CategoryResult count(Category category, List<Ballot> ballots) {
        int yes = 0;
        int no = 0;
        for (Ballot ballot : ballots) {
            List<String> choices = ballot.getChoices();
            // Anything other than exactly one YES or NO is not counted.
            if (choices.size() == 1 && choices.get(0).equalsIgnoreCase(YesNoResult.YES)) {
                yes++;
            } else if (choices.size() == 1 && choices.get(0).equalsIgnoreCase(YesNoResult.NO)) {
                no++;
            }
        }
        return new YesNoResult(category, ballots.size(), yes, no);
    }
}
