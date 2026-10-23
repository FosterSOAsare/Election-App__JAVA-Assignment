# Election App — Design Plan

## Overview

A console election app with two kinds of user. Administrators create elections, set up their categories and candidates, and open and close voting. Voters cast one ballot per category while an election is open. Everything starts from CSV files, invalid actions are rejected, and each category gets its own result.

| Role | Can do | Cannot do |
| --- | --- | --- |
| Administrator | create an election; for elections they created: add categories and candidates while it is a draft, open voting, close voting; view results | vote; manage another administrator's election |
| Voter | vote in open elections, view results of closed elections | create, edit, open or close elections |

Every election moves one way through three states: **DRAFT** (admin sets it up), **OPEN** (voters vote, no more edits), **CLOSED** (no more votes, results available).

The sample election, Student Council 2026, has three categories:

| Category | Ballot type | What voters do | Who wins |
| --- | --- | --- | --- |
| President | SELECT_ONE | pick 1 candidate | most votes |
| Committee members | SELECT_MULTIPLE | pick up to 2 candidates | top 2 by votes |
| Raise the club fee to 10 euros? | YES_NO | answer yes or no | passes if yes beats no |

| Brief requirement | How the app meets it |
| --- | --- |
| Classes and encapsulation | Domain classes (`Election`, `Category`, `Candidate`, `User`, `Ballot`) with private fields and validated constructors |
| Inheritance | Abstract `User` with `Administrator` and `Voter`; abstract `Ballot` with `SingleChoiceBallot`, `MultipleChoiceBallot` and `YesNoBallot`; abstract `CategoryResult` with `CandidateResult` and `YesNoResult` |
| Polymorphism | `user.canManageElections()` and `user.canVote()` answer differently per role; each ballot validates itself; each category is counted through `CountingMethod`; each result prints through `summary()` |
| Abstract class / interface | `User`, `Ballot` and `CategoryResult` are abstract (shared state + behaviour); `ManagementPermission`, `VotingPermission`, `CountingMethod`, `DataLoader<T>` and `Menu` are interfaces |
| Business logic | Role permissions, election lifecycle, setup checks before opening, registration checks, one vote per voter per category, ballot validation per type, multi-seat counting, yes/no outcome, tie handling |
| File loading | `users.csv`, `elections.csv`, `categories.csv`, `candidates.csv`, `ballots.csv` loaded at startup |
| Exceptions | Checked `ElectionException` hierarchy plus `DataLoadException`; bad rows reported and skipped |
| Unit tests | JUnit tests on permissions, lifecycle, counting, voting rules, ballot validation and loaders |
| SOLID | Loading, election management, voting, counting and display live in separate classes; a new ballot type plugs in without changing existing counting code |
| Collections and Java features | `Map` of elections and categories, `Map<String, Set<String>>` of who voted where, `List` of ballots; enums, `EnumMap`, `LocalDate` for election dates, streams, `Optional`, generics |

Scope rule: get SELECT_ONE voting working end to end first, then the admin actions, then YES_NO, then SELECT_MULTIPLE.

## Class list

Six packages under `com.election`, each with one job. Repositories store objects and find them; services hold the business rules and use the repositories. `Application.java` only wires the pieces together, asks the user to log in by id, and starts the right menu. "0. Log out" in either menu returns to the login prompt, with the data kept in memory, so several users can take turns in one run; typing `exit` at the login prompt quits.

| Package | Class | Kind | Responsibility |
| --- | --- | --- | --- |
| model | `ManagementPermission` | interface | `boolean canManageElections()` |
| model | `VotingPermission` | interface | `boolean canVote()` |
| model | `User` | abstract class | id; implements `ManagementPermission` and `VotingPermission`, leaving both methods to its subclasses |
| model | `Administrator` | extends User | manages elections; cannot vote |
| model | `Voter` | extends User | votes; cannot manage elections |
| model | `Role` | enum | ADMIN, VOTER (read from users.csv) |
| model | `Election` | class | id, name, date, status, createdBy, `Map<String, Category>`, ballots; `open()`, `close()`, `ballotsFor(categoryId)` |
| model | `ElectionStatus` | enum | DRAFT, OPEN, CLOSED |
| model | `Deadlines` | final class | parses and prints an election deadline (`LocalDateTime`): `YYYY-MM-DD` or `YYYY-MM-DD HH:MM[:SS]`, 00:00:00 when no time is given; shared by the CSV loader and the console |
| model | `Category` | class | id, electionId, name, ballot type, seats, its candidates |
| model | `BallotType` | enum | SELECT_ONE, SELECT_MULTIPLE, YES_NO |
| model | `Candidate` | class | id, name, electionId, categoryId |
| model | `Ballot` | abstract class | voterId, electionId, categoryId; abstract `selections()` returns the chosen option ids; abstract `validate(Category)` |
| model | `SingleChoiceBallot` | extends Ballot | exactly 1 candidate from this category |
| model | `MultipleChoiceBallot` | extends Ballot | 1 to `seats` distinct candidates from this category |
| model | `YesNoBallot` | extends Ballot | a yes or no answer; `selections()` returns `YES` or `NO` |
| model | `CategoryResult` | abstract class | category, `Map<String, Integer>` tallies; abstract `summary()` |
| model | `CandidateResult` | extends CategoryResult | `List<Candidate>` winners, candidates tied for the last seat |
| model | `YesNoResult` | extends CategoryResult | yes count, no count, passed |
| repository | `UserRepository` | class | `Map<String, User>`; `findById` ignores case; rejects duplicate ids |
| repository | `ElectionRepository` | class | `LinkedHashMap<String, Election>` in file order; `findAll`, `findById`, `findByStatus`, `add`, `delete`, `nextId` |
| service | `ElectionManagementService` | class | admin actions: `createElection`, `addCategory`, `addCandidate`, `openElection`, `closeElection`; checks the actor's role, that the actor created the election, and the election's state |
| service | `CountingMethod` | interface | `CategoryResult count(Category, List<Ballot>)` |
| service | `MostVotesCount` | implements CountingMethod | tallies every selection; the top `seats` candidates win (1 for SELECT_ONE) |
| service | `YesNoCount` | implements CountingMethod | passes when yes votes outnumber no votes |
| service | `CountingMethodRegistry` | class | `EnumMap<BallotType, CountingMethod>`: picks the counting method for a category |
| service | `BallotFactory` | class | builds the right `Ballot` subclass from a category and the raw choices text; used by the loader and the console |
| service | `VotingService` | class | `prepareBallot(voter, election, categoryId, choices)` checks one answer without storing it: user can vote, election is OPEN, its deadline has not passed, voter has not voted in this election yet, category exists, choices fit the ballot type (candidate ids are matched ignoring case). `submitBallots` re-checks every prepared ballot and stores them all together, or none. `hasVoted(voter, election)`: a voter submits once per election, so any ballot of theirs in it counts |
| service | `TallyService` | class | `closedElections()` and `findClosedElection(id)` for the results menu; counts every category of a CLOSED election; returns `Map<Category, CategoryResult>` in file order |
| loader | `DataLoader<T>` | interface | `List<T> load(Path)` throws `DataLoadException` |
| loader | `CsvReader` | class | skips header, splits lines, reports line numbers |
| loader | `UserCsvLoader` | implements DataLoader | builds an `Administrator` or `Voter` from the role column |
| loader | `ElectionCsvLoader`, `CategoryCsvLoader`, `CandidateCsvLoader` | implement DataLoader | one per file |
| loader | `BallotCsvLoader` | implements DataLoader | reads raw ballot rows and passes them to `BallotFactory` |
| exception | `ElectionException` | checked base | parent of the business exceptions |
| exception | `InvalidNameException` | extends ElectionException | a candidate name is empty or uses characters `NameRules` does not allow |
| model | `NameRules` | final class | the one place that defines a valid name: letters (any language), digits, spaces, hyphens, apostrophes and full stops; never commas, which separate CSV columns and console lists. `Candidate`'s constructor enforces it |
| exception | `UnauthorizedActionException` | extends ElectionException | a voter tries an admin action, or an admin manages another admin's election |
| exception | `ElectionStateException` | extends ElectionException | action not allowed in the current state: editing an open election, voting in a draft or closed one, results before closing, opening a category with no candidates |
| exception | `IneligibleVoterException` | extends ElectionException | user is unknown, or is an administrator |
| exception | `DuplicateVoteException` | extends ElectionException | voter already voted in this category |
| exception | `InvalidBallotException` | extends ElectionException | unknown category, candidate from another category, too many or too few choices, repeated choice, answer not YES or NO |
| exception | `DataLoadException` | checked | wraps `IOException`, `DateTimeParseException`, malformed rows, unknown role or ballot type, bad seat count, unknown election or category id |
| ui | `Menu` | interface | `void run()` |
| ui | `AdminMenu` | implements Menu | create election, view all elections, manage elections, view results |
| ui | `ManageElectionsMenu` | implements Menu | shows only the admin's own elections. Manage a draft (election details, categories, candidates): only DRAFT elections can be managed. Close an election: pick one of your OPEN elections, confirm, and `closeElection` checks you are an administrator and its creator before `Election.close()` (OPEN to CLOSED only). Manage a draft → Open voting: confirm, and `openElection` checks the same permissions plus the setup rules (at least 1 category, candidates where needed, more candidates than seats), reporting every problem at once, before `Election.open()` (DRAFT to OPEN only). Manage a draft → Edit election details opens `EditDraftMenu`; Delete this draft removes the whole draft after confirmation (only drafts can be deleted, since an open or closed election may have ballots) |
| ui | `EditDraftMenu` | implements Menu | edits one draft: Election details (name, deadline); Edit categories → pick one → Edit category details (rename: not empty, no comma, not another category's name; then optionally change the type and seats: Yes / No to Select one or multiple asks for candidates, Select one to multiple asks for seats and extra candidates if needed, and anything to Yes / No removes the candidates after confirmation) or Edit candidates; Remove this category (with its candidates, after confirmation); Add categories asks the same questions as Create Election (shared `CategoryForm`), refuses names already used in the election straight away, and numbers the new ones after the existing ids (CAT3, C8 ...). Edit candidates lists the category's candidates; the admin picks one by number, then Rename (Enter keeps the name) or Remove (after confirmation); Add candidates adds comma-separated names (same name rules, none already standing there) with the next free Cn ids, and `renameCandidate` checks the creator, that it is still a DRAFT, the name rules and that no other candidate in the category has that name. Yes / No categories have no candidates. Election details changes the name and/or deadline (Enter keeps either): the name cannot be empty or contain a comma, the new deadline must not have passed, and `updateElectionDetails` checks everything before changing anything |
| ui | `VoterMenu` | implements Menu | list open elections, then pick one to see its categories, types and candidates (never the ballots); vote: pick an open election; a voter who has already voted in it is told so and sees no categories; otherwise answer each category (Enter skips one, an invalid answer is asked again), then review a summary of every category (skipped ones included) and choose Submit, Change an answer (re-asks one category, then shows the summary again) or Cancel; ballots are saved only on Submit; view results of closed elections |
| ui | `MenuFactory` | class | picks `AdminMenu` or `VoterMenu` from `user.canManageElections()` |
| ui | `ConsoleInput` | class | wraps the one `Scanner`; every keyboard read (line, required text, date, number, numbered choice, comma-separated names) re-asks until the answer has the right shape. `pick` shows a numbered list ending in `0. Back`: every election, category and candidate is chosen by its number, never by typing its id (the two exceptions are logging in by user id and voting by candidate ids) |
| ui | `ElectionView` | class | prints elections: the padded election table, an election's categories with their type and candidates, and a newly created election |
| ui | `ElectionPicker` | class | shows elections as a numbered table (`ElectionView.electionTable`) ending in `0. Back`; used by every screen that picks an election |
| ui | `ResultView` | class | prints an election's results: each category's header, ballot count and `summary()`, whatever the result type |
| ui | `ResultsScreen` | implements Menu | "View election results", shared by both menus: list closed elections, pick one, show `TallyService.count` through `ResultView` |

The ballot type lives on the category, not on each ballot. `BallotFactory` reads it to build the right subclass, so file votes and console votes go through the same rules.

Rules `ElectionManagementService` enforces before `openElection` succeeds:

- the election has at least 1 category
- every SELECT_ONE and SELECT_MULTIPLE category has at least 1 candidate
- every SELECT_MULTIPLE category has more candidates than seats
- the deadline has not passed yet

Console prompts by ballot type:

- **SELECT_ONE:** `President - choose 1: C1 Amara Mensah, C2 Kofi Boateng, C3 Lena Dubois >`
- **SELECT_MULTIPLE:** `Committee members - choose up to 2, separated by | >`
- **YES_NO:** `Raise the club fee to 10 euros? (YES/NO) >`

## CSV formats

Five files in `src/main/resources/`, each with a header row, loaded in this order because each file refers to ids from the ones before it. Multiple choices use `|` as the separator because commas already split the columns.

**users.csv** (users are identified by id only; `role` decides whether the loader builds an `Administrator` or a `Voter`)

```csv
id,role
A1,ADMIN
A2,ADMIN
V001,VOTER
V002,VOTER
V003,VOTER
V004,VOTER
V005,VOTER
V006,VOTER
V007,VOTER
V008,VOTER
V009,VOTER
V010,VOTER
```

**elections.csv** (`createdBy` must be an administrator; `deadline` is the moment voting ends, written `YYYY-MM-DD` or `YYYY-MM-DD HH:MM[:SS]`, and a missing time means 00:00:00, the start of that day. Votes are accepted before it and refused from it on, and an election cannot be opened once it has passed. `Deadlines` parses and prints it the same way for the file and the console)

```csv
id,name,deadline,status,createdBy
E1,Student Council 2026,2026-11-10,OPEN,A1
E2,Sports Club Captain 2026,2026-12-01,DRAFT,A2
E3,Student Council 2025,2025-11-12,CLOSED,A1
E4,Library Committee 2026,2026-04-22,CLOSED,A2
E5,Graduation Speaker 2026,2026-10-30,OPEN,A2
E6,Debate Club President 2027,2027-01-15,DRAFT,A1
```

E3 to E6 cover every status for both administrators, so the menus have something to show. They have no categories, candidates or ballots yet.

**categories.csv** (`seats` is the number of winners; always 1 for SELECT_ONE and YES_NO)

```csv
id,electionId,name,ballotType,seats
CAT1,E1,President,SELECT_ONE,1
CAT2,E1,Committee members,SELECT_MULTIPLE,2
CAT3,E1,Raise the club fee to 10 euros?,YES_NO,1
CAT1,E2,Club captain,SELECT_ONE,1
CAT1,E3,President,SELECT_ONE,1
CAT2,E3,Treasurer,SELECT_ONE,1
CAT1,E4,Library committee,SELECT_MULTIPLE,3
CAT2,E4,Extend opening hours to 22:00?,YES_NO,1
CAT1,E5,Graduation speaker,SELECT_ONE,1
CAT1,E6,Debate club president,SELECT_ONE,1
CAT2,E6,Introduce a 5 euro membership fee?,YES_NO,1
```

Category ids are numbered inside their election (`CAT1`, `CAT2`, ...), so the same id can appear in several elections. A category is identified by the pair electionId + categoryId, which is why candidates.csv and ballots.csv carry both columns. `CategoryCsvLoader` adds each category to its election and rejects an unknown election id, or an id or name already used in that election. `Category`'s constructor rejects an unknown ballot type, a SELECT_ONE or YES_NO category without exactly 1 seat, and a SELECT_MULTIPLE category with fewer than 2 seats.

**candidates.csv** (YES_NO categories have no candidates)

```csv
id,name,electionId,categoryId
C1,Amara Mensah,E1,CAT1
C2,Kofi Boateng,E1,CAT1
C3,Lena Dubois,E1,CAT1
C4,Yaw Darko,E1,CAT2
C5,Marie Laurent,E1,CAT2
C6,Kwesi Appiah,E1,CAT2
C7,Julia Weber,E1,CAT2
C8,Nana Acheampong,E2,CAT1
C9,Tom Fischer,E2,CAT1
C1,Daniel Osei,E3,CAT1
C2,Emma Schulz,E3,CAT1
C3,Farid Benali,E3,CAT1
C4,Grace Mensah,E3,CAT2
C5,Hans Becker,E3,CAT2
C1,Isaac Boateng,E4,CAT1
C2,Julia Novak,E4,CAT1
C3,Kwame Asante,E4,CAT1
C4,Lea Martin,E4,CAT1
C5,Musa Diallo,E4,CAT1
C1,Nora Haddad,E5,CAT1
C2,Omar Farouk,E5,CAT1
C3,Priya Nair,E5,CAT1
C1,Quentin Moreau,E6,CAT1
C2,Rosa Alvarez,E6,CAT1
```

`CandidateCsvLoader` finds the election, then the category inside it (`election.findCategory`), and adds the candidate there. It rejects an unknown election, a category that is not in that election, a candidate id already used in that election, and a name already standing in that category. Candidate ids only need to be unique inside their election.

**ballots.csv** (one row per voter per category; the format of `choices` depends on the category's ballot type)

```csv
voterId,electionId,categoryId,choices
V001,E1,CAT1,C1
V002,E1,CAT1,C1
V003,E1,CAT1,C1
V004,E1,CAT1,C1
V005,E1,CAT1,C2
V006,E1,CAT1,C2
V007,E1,CAT1,C2
V008,E1,CAT1,C3
V009,E1,CAT1,C3
V001,E1,CAT2,C4|C5
V002,E1,CAT2,C4|C6
V003,E1,CAT2,C5
V004,E1,CAT2,C4|C7
V005,E1,CAT2,C5|C6
V006,E1,CAT2,C4|C5
V007,E1,CAT2,C6|C7
V008,E1,CAT2,C4
V010,E1,CAT2,C5|C7
V001,E1,CAT3,YES
V002,E1,CAT3,NO
V003,E1,CAT3,YES
V004,E1,CAT3,YES
V005,E1,CAT3,NO
V006,E1,CAT3,YES
V007,E1,CAT3,NO
V008,E1,CAT3,YES
V010,E1,CAT3,YES
V001,E3,CAT1,C1
V002,E3,CAT1,C1
V003,E3,CAT1,C1
V004,E3,CAT1,C1
V005,E3,CAT1,C1
V006,E3,CAT1,C2
V007,E3,CAT1,C2
V008,E3,CAT1,C2
V009,E3,CAT1,C3
V010,E3,CAT1,C3
V001,E3,CAT2,C4
V002,E3,CAT2,C4
V003,E3,CAT2,C4
V004,E3,CAT2,C4
V005,E3,CAT2,C5
V006,E3,CAT2,C5
V007,E3,CAT2,C5
V008,E3,CAT2,C5
V001,E4,CAT1,C1|C2|C3
V002,E4,CAT1,C1|C2|C4
V003,E4,CAT1,C1|C3|C5
V004,E4,CAT1,C1|C2
V005,E4,CAT1,C2|C3|C4
V006,E4,CAT1,C1|C4|C5
V007,E4,CAT1,C2|C3
V008,E4,CAT1,C1|C5
V009,E4,CAT1,C3|C4
V010,E4,CAT1,C1|C2
V001,E4,CAT2,YES
V002,E4,CAT2,YES
V003,E4,CAT2,YES
V004,E4,CAT2,YES
V005,E4,CAT2,YES
V006,E4,CAT2,YES
V007,E4,CAT2,YES
V008,E4,CAT2,NO
V009,E4,CAT2,NO
V010,E4,CAT2,NO
V001,E5,CAT1,C1
V002,E5,CAT1,C2
V003,E5,CAT1,C1
V004,E5,CAT1,C3
```

Every seeded ballot is valid, and drafts (E2, E6) have no ballots at all, since voting only starts once an election is opened. `BallotCsvLoader` only checks the column count and that the election exists, and stores every row in its election as a plain `Ballot`; the voting rules are enforced by `VotingService` for votes cast in the console, and are tested there. Ballots are secret: View all elections only shows how many people have voted, never who chose what.

V010 skips the President vote and V009 votes only for President. Both are allowed: voting in a category is optional.

Changes an administrator makes in the console should eventually be saved back to the CSV files. For now they are kept in memory only, so they last until the program exits. A new election gets the next free id (E7 after E6), starts as DRAFT, and belongs to the administrator who created it. Its categories are entered on one line, separated by commas (`President, Treasurer`); at least one is required, the same name cannot appear twice, and each is numbered inside the new election (`CAT1`, `CAT2`, ...). Then, for each category in turn, the admin picks its type from a numbered list (Select one, Select multiple, Yes / No); a Select multiple category also asks how many winners (2 or more). Unless it is Yes / No, the admin then enters its candidates the same way (`Ama Owusu, Kofi Mensah`); at least one is required and a name cannot appear twice in the same category, though the same person may stand in two categories. Candidates are numbered across the whole election (`C1`, `C2`, ...). A Select multiple category needs more candidates than winners, and a Yes / No category never has candidates.

## Expected results

Once administrator A1 closes E1, Amara Mensah wins President, Yaw Darko and Marie Laurent win the two committee seats, and the fee rise passes 6 to 3. Asking for results while E1 is still open throws `ElectionStateException`.

| Category | Valid ballots | Tally | Result |
| --- | --- | --- | --- |
| President | 9 | C1 4, C2 3, C3 2 | C1 Amara Mensah elected |
| Committee members (2 seats) | 9 | C4 5, C5 5, C6 3, C7 3 | C4 Yaw Darko and C5 Marie Laurent elected |
| Raise the club fee to 10 euros? | 9 | YES 6, NO 3 | Passed (66.7% yes) |

C4 and C5 tie on 5 votes, but both win because there are 2 seats. A tie only matters at the last seat. E2 stays a draft with no ballots until A2 opens it. These numbers double as the expected values in the end-to-end test.

E3 and E4 are already CLOSED, so their results show straight away:

| Election | Category | Valid ballots | Tally | Result |
| --- | --- | --- | --- | --- |
| E3 | President | 10 | C1 5, C2 3, C3 2 | C1 Daniel Osei elected |
| E3 | Treasurer | 8 | C4 4, C5 4 | Tie: C4 Grace Mensah and C5 Hans Becker, seat undecided |
| E4 | Library committee (3 seats) | 10 | C1 7, C2 6, C3 5, C4 4, C5 3 | C1 Isaac Boateng, C2 Julia Novak and C3 Kwame Asante elected |
| E4 | Extend opening hours to 22:00? | 10 | YES 7, NO 3 | Passed (70.0% yes) |

E5 is OPEN with 4 ballots so far (C1 2, C2 1, C3 1), and E6 is a DRAFT with candidates but no ballots.

## Design decisions

Five decisions to settle before coding; the README asks for 2 to 5, so these cover it.

1. **Roles are subclasses, permissions are methods.** `User` implements the `ManagementPermission` and `VotingPermission` interfaces, and `Administrator` and `Voter` each override `canManageElections()` and `canVote()`, so services ask the user instead of checking its type with `instanceof`. Only the administrator who created an election can edit, open or close it; anyone else gets `UnauthorizedActionException`.
2. **A one-way lifecycle.** DRAFT to OPEN to CLOSED, never backwards. Setup is frozen once voting starts, so no one can add a candidate mid-vote, and results exist only after closing. The election deadline (a date with an optional time, 00:00:00 if left out) is the moment voting ends: an election cannot be opened after it and votes are refused from it on. Closing before the deadline is allowed on purpose so the app can be tested without waiting, and the admin is told so when they close early; a real election would only allow closing once the deadline has passed.
3. **The ballot type belongs to the category.** A ballot row only carries voter, category and choices, so a file cannot pair a yes/no answer with the President category. `BallotFactory` reads the type and builds the matching subclass.
4. **Ties.** If candidates tie for the last winning seat, that seat stays undecided and the result lists the tied candidates. A yes/no vote with equal yes and no is rejected, because a change needs more support than opposition.
5. **Checked exceptions.** `ElectionException` is checked, so every caller must decide what to do with a business error instead of letting it crash the app.

How SOLID shows up:

- **Single responsibility:** loading, storage (repositories), election management, ballot creation, voting, counting and display are separate classes. Menus only read input and print; they ask a service for anything that involves a rule.
- **Open/closed:** adding a ranked ballot later means a new `Ballot` subclass, a new `CountingMethod`, an enum value and one registry entry; `TallyService`, `VotingService` and the existing counting methods stay untouched. A new role, such as an observer who can only view results, is a new `User` subclass.
- **Liskov substitution:** any `User` works wherever the services expect one; `MostVotesCount` reads any ballot through `selections()`, so single and multiple choice ballots are interchangeable to it.
- **Interface segregation:** `ManagementPermission`, `VotingPermission`, `CountingMethod`, `DataLoader<T>` and `Menu` each have one method, so a class only depends on the one it needs.
- **Dependency inversion:** `TallyService` depends on the `CountingMethod` interface through `CountingMethodRegistry`, never on `MostVotesCount` or `YesNoCount` directly; `Application` only talks to the `Menu` interface.

## Test plan

JUnit tests target the business logic, grouped by the class under test. Tick each off as it passes.

**ElectionManagementServiceTest**

- [ ] Administrator creates an election; it starts as DRAFT with them as creator
- [ ] Voter trying to create an election throws `UnauthorizedActionException`
- [ ] Administrator editing another administrator's election throws `UnauthorizedActionException`
- [ ] Adding a category or candidate to an OPEN election throws `ElectionStateException`
- [ ] Opening an election with no categories throws `ElectionStateException`
- [ ] Opening with a SELECT_ONE category that has no candidates throws `ElectionStateException`
- [ ] Opening with a SELECT_MULTIPLE category that has no more candidates than seats throws `ElectionStateException`
- [ ] Closing a DRAFT election, or opening a CLOSED one, throws `ElectionStateException`

**MostVotesCountTest**

- [ ] One seat: sample President ballots elect C1
- [ ] Two seats: sample committee ballots elect C4 and C5, even though they tie with each other
- [ ] Tie for the last seat leaves it undecided and lists the tied candidates
- [ ] Zero ballots elects no one

**YesNoCountTest**

- [ ] 6 yes, 3 no passes
- [ ] Equal yes and no is rejected
- [ ] Zero ballots is rejected

**VotingServiceTest**

- [ ] Valid vote is recorded
- [ ] Same voter can vote in two different categories
- [ ] Second vote in the same category throws `DuplicateVoteException`
- [ ] Unknown user throws `IneligibleVoterException`
- [ ] Administrator trying to vote throws `IneligibleVoterException`
- [ ] Vote in a DRAFT or CLOSED election throws `ElectionStateException`

**TallyServiceTest**

- [ ] Results for an OPEN election throw `ElectionStateException`
- [ ] Results for a CLOSED election return one result per category, in file order

**BallotFactoryTest and ballot validation**

- [ ] SELECT_ONE with two choices throws `InvalidBallotException`
- [ ] Candidate from another category throws `InvalidBallotException`
- [ ] SELECT_MULTIPLE with more choices than seats throws `InvalidBallotException`
- [ ] SELECT_MULTIPLE with a repeated candidate throws `InvalidBallotException`
- [ ] SELECT_MULTIPLE with exactly `seats` choices is accepted (edge case)
- [ ] YES_NO answer other than YES or NO throws `InvalidBallotException`
- [ ] Unknown category throws `InvalidBallotException`

**NameRules / Candidate**

- [ ] `Amara Mensah`, `Jean-Luc Picard`, `Conan O'Brien`, `Dr. Who`, `Agent 47` and `Inès Moreau` are accepted
- [ ] An empty name, a comma, or a symbol such as `@` or `#` throws `InvalidNameException`
- [ ] A candidates.csv row with an invalid name is rejected with its line number

**Loader tests**

- [ ] Unknown role in users.csv throws `DataLoadException` with the line number
- [ ] Election whose `createdBy` is a voter throws `DataLoadException`
- [ ] Unknown ballot type in categories.csv throws `DataLoadException`
- [ ] SELECT_ONE category with 2 seats throws `DataLoadException`
- [ ] Category or candidate pointing to an unknown id throws `DataLoadException`
- [ ] Missing column throws `DataLoadException`

**End-to-end**

- [ ] Loading the sample files accepts every ballot; after A1 closes E1, the results match Expected results

Run with `mvn test`.
