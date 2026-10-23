package com.election.ui;

import com.election.model.Election;

import java.util.List;
import java.util.Optional;

/** Shows elections as a numbered table ending in "0. Back"; every screen that picks an election uses this. */
public class ElectionPicker {

    private final ConsoleInput input;
    private final ElectionView view = new ElectionView();

    public ElectionPicker(ConsoleInput input) {
        this.input = input;
    }

    /** Empty when the voter or admin chooses Back. */
    public Optional<Election> pick(String title, List<Election> elections) {
        List<String> table = view.electionTable(elections);
        return input.pick(title, table.get(0), table.subList(1, table.size()))
                .map(elections::get);
    }
}
