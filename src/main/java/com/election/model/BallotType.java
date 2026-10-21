package com.election.model;

/** How voters answer a category. The ballot type belongs to the category, not to each ballot. */
public enum BallotType {
    SELECT_ONE("Select one"),
    SELECT_MULTIPLE("Select multiple"),
    YES_NO("Yes / No");

    private final String label;

    BallotType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
