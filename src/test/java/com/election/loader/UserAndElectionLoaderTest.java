package com.election.loader;

import com.election.exception.DataLoadException;
import com.election.model.Administrator;
import com.election.model.Deadlines;
import com.election.model.Election;
import com.election.model.ElectionStatus;
import com.election.model.User;
import com.election.model.Voter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The two loaders that need nothing else to be loaded first. */
class UserAndElectionLoaderTest {

    @TempDir
    Path folder;

    @Nested
    class Users {

        @Test
        void theRoleDecidesWhetherAnAdministratorOrAVoterIsBuilt() throws Exception {
            Path file = CsvFiles.write(folder, "users.csv", "id,role", "A1,ADMIN", "V001,VOTER");

            List<User> users = new UserCsvLoader().load(file);

            assertInstanceOf(Administrator.class, users.get(0));
            assertInstanceOf(Voter.class, users.get(1));
            assertEquals("V001", users.get(1).getId());
        }

        @Test
        void anUnknownRoleIsRefusedWithItsLine() throws Exception {
            Path file = CsvFiles.write(folder, "users.csv", "id,role", "A1,ADMIN", "V001,voter");

            DataLoadException e = assertThrows(DataLoadException.class, () -> new UserCsvLoader().load(file));
            assertEquals("users.csv line 3: 'voter' is not one of [ADMIN, VOTER]", e.getMessage());
        }

        @Test
        void aBlankIdIsRefusedWithItsLine() throws Exception {
            Path file = CsvFiles.write(folder, "users.csv", "id,role", ",VOTER");

            DataLoadException e = assertThrows(DataLoadException.class, () -> new UserCsvLoader().load(file));
            assertEquals("users.csv line 2: User id must not be blank", e.getMessage());
        }
    }

    @Nested
    class Elections {

        @Test
        void aDeadlineMayHaveATimeOrNot() throws Exception {
            Path file = CsvFiles.write(folder, "elections.csv", "id,name,deadline,status,createdBy",
                    "E1,Student Council 2026,2026-11-10,OPEN,A1",
                    "E2,Evening Vote,2026-11-10 18:30,DRAFT,A2");

            List<Election> elections = new ElectionCsvLoader().load(file);

            assertEquals(Deadlines.parse("2026-11-10"), elections.get(0).getDeadline());
            assertEquals(Deadlines.parse("2026-11-10 18:30"), elections.get(1).getDeadline());
            assertEquals(ElectionStatus.DRAFT, elections.get(1).getStatus());
            assertEquals("A2", elections.get(1).getCreatedBy());
        }

        @Test
        void aBadDeadlineIsRefusedWithItsLine() throws Exception {
            Path file = CsvFiles.write(folder, "elections.csv", "id,name,deadline,status,createdBy",
                    "E1,Bad,2026-02-30,DRAFT,A1");

            DataLoadException e = assertThrows(DataLoadException.class, () -> new ElectionCsvLoader().load(file));
            assertEquals("elections.csv line 2: '2026-02-30' is not a valid deadline; use " + Deadlines.FORMAT_HINT,
                    e.getMessage());
        }

        @Test
        void anUnknownStatusIsRefusedWithItsLine() throws Exception {
            Path file = CsvFiles.write(folder, "elections.csv", "id,name,deadline,status,createdBy",
                    "E1,Bad,2026-11-10,PENDING,A1");

            DataLoadException e = assertThrows(DataLoadException.class, () -> new ElectionCsvLoader().load(file));
            assertEquals("elections.csv line 2: 'PENDING' is not one of [DRAFT, OPEN, CLOSED]", e.getMessage());
        }

        @Test
        void aBlankNameIsRefusedWithItsLine() throws Exception {
            Path file = CsvFiles.write(folder, "elections.csv", "id,name,deadline,status,createdBy",
                    "E1, ,2026-11-10,DRAFT,A1");

            DataLoadException e = assertThrows(DataLoadException.class, () -> new ElectionCsvLoader().load(file));
            assertEquals("elections.csv line 2: Election name must not be blank", e.getMessage());
        }
    }
}
