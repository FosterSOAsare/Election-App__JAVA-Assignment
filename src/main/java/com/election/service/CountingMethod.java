package com.election.service;

import com.election.model.Ballot;
import com.election.model.Category;
import com.election.model.CategoryResult;

import java.util.List;

/** One way of turning a category's ballots into a result. */
public interface CountingMethod {

    CategoryResult count(Category category, List<Ballot> ballots);
}
