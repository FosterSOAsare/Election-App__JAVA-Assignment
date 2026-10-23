package com.election.ui;

import com.election.exception.ElectionStateException;
import com.election.model.Election;
import com.election.service.TallyService;

import java.util.List;
import java.util.Optional;

/** "View election results", shared by the admin and voter menus: pick a closed election, see its results. */
public class ResultsScreen implements Menu {

    private final TallyService tallyService;
    private final ElectionPicker picker;
    private final ResultView resultView = new ResultView();

    public ResultsScreen(TallyService tallyService, ConsoleInput input) {
        this.tallyService = tallyService;
        this.picker = new ElectionPicker(input);
    }

    @Override
    public void run() {
        List<Election> closed = tallyService.closedElections();
        if (closed.isEmpty()) {
            System.out.println("No elections have been closed yet.");
            return;
        }
        Optional<Election> selected = picker.pick("Closed elections (pick one to see its results):", closed);
        if (selected.isEmpty()) {
            return;
        }

        try {
            resultView.printResults(selected.get(), tallyService.count(selected.get()));
        } catch (ElectionStateException e) {
            // Cannot happen for an election picked from the closed list, but the service still enforces it.
            System.out.println(e.getMessage());
        }
    }
}
