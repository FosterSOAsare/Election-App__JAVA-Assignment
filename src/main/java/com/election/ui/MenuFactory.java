package com.election.ui;

import com.election.model.User;
import com.election.service.ElectionManagementService;
import com.election.service.TallyService;
import com.election.service.VotingService;

/**
 * Picks the menu for whoever logged in. The user answers for its own role (canManageElections),
 * so there is no instanceof here; a new role would only need a new case in this one place.
 */
public class MenuFactory {

    private final ElectionManagementService managementService;
    private final VotingService votingService;
    private final TallyService tallyService;
    private final ConsoleInput input;

    public MenuFactory(ElectionManagementService managementService, VotingService votingService,
                       TallyService tallyService, ConsoleInput input) {
        this.managementService = managementService;
        this.votingService = votingService;
        this.tallyService = tallyService;
        this.input = input;
    }

    public Menu menuFor(User user) {
        if (user.canManageElections()) {
            return new AdminMenu(user, managementService, tallyService, input);
        }
        return new VoterMenu(user, votingService, tallyService, input);
    }
}
