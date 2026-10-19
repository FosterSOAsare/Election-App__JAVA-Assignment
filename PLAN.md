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
| Abstract class / interface | `User`, `Ballot` and `CategoryResult` are abstract (shared state + behaviour); `CountingMethod`, `DataLoader<T>` and `Menu` are interfaces |
| Business logic | Role permissions, election lifecycle, setup checks before opening, registration checks, one vote per voter per category, ballot validation per type, multi-seat counting, yes/no outcome, tie handling |
| File loading | `users.csv`, `elections.csv`, `categories.csv`, `candidates.csv`, `ballots.csv` loaded at startup |
| Exceptions | Checked `ElectionException` hierarchy plus `DataLoadException`; bad rows reported and skipped |
| Unit tests | JUnit tests on permissions, lifecycle, counting, voting rules, ballot validation and loaders |
| SOLID | Loading, election management, voting, counting and display live in separate classes; a new ballot type plugs in without changing existing counting code |
| Collections and Java features | `Map` of elections and categories, `Map<String, Set<String>>` of who voted where, `List` of ballots; enums, `EnumMap`, `LocalDate` for election dates, streams, `Optional`, generics |

Scope rule: get SELECT_ONE voting working end to end first, then the admin actions, then YES_NO, then SELECT_MULTIPLE.

## Class list

Five packages under `com.election`, each with one job. `Application.java` only wires the pieces together, asks the user to log in by id, and starts the right menu.

| Package | Class | Kind | Responsibility |
| --- | --- | --- | --- |
| model | `User` | abstract class | id; abstract `canManageElections()` and `canVote()` |
| model | `Administrator` | extends User | manages elections; cannot vote |
| model | `Voter` | extends User | votes; cannot manage elections |
| model | `Role` | enum | ADMIN, VOTER (read from users.csv) |
| model | `Election` | class | id, name, date, status, createdBy, `Map<String, Category>`, ballots; `open()`, `close()`, `ballotsFor(categoryId)` |
| model | `ElectionStatus` | enum | DRAFT, OPEN, CLOSED |
| model | `Category` | class | id, electionId, name, ballot type, seats, its candidates |
| model | `BallotType` | enum | SELECT_ONE, SELECT_MULTIPLE, YES_NO |
| model | `Candidate` | class | id, name, party, categoryId |
| model | `Ballot` | abstract class | voterId, categoryId; abstract `selections()` returns the chosen option ids; abstract `validate(Category)` |
| model | `SingleChoiceBallot` | extends Ballot | exactly 1 candidate from this category |
| model | `MultipleChoiceBallot` | extends Ballot | 1 to `seats` distinct candidates from this category |
| model | `YesNoBallot` | extends Ballot | a yes or no answer; `selections()` returns `YES` or `NO` |
| model | `CategoryResult` | abstract class | category, `Map<String, Integer>` tallies; abstract `summary()` |
| model | `CandidateResult` | extends CategoryResult | `List<Candidate>` winners, candidates tied for the last seat |
| model | `YesNoResult` | extends CategoryResult | yes count, no count, passed |
| service | `ElectionRegistry` | class | holds every election and user by id (`Map<String, Election>`, `Map<String, User>`); finds the election a category belongs to |
| service | `ElectionManagementService` | class | admin actions: `createElection`, `addCategory`, `addCandidate`, `openElection`, `closeElection`; checks the actor's role, that the actor created the election, and the election's state |
| service | `CountingMethod` | interface | `CategoryResult count(Category, List<Ballot>)` |
| service | `MostVotesCount` | implements CountingMethod | tallies every selection; the top `seats` candidates win (1 for SELECT_ONE) |
| service | `YesNoCount` | implements CountingMethod | passes when yes votes outnumber no votes |
| service | `CountingMethodRegistry` | class | `EnumMap<BallotType, CountingMethod>`: picks the counting method for a category |
| service | `BallotFactory` | class | builds the right `Ballot` subclass from a category and the raw choices text; used by the loader and the console |
| service | `VotingService` | class | `castVote(voterId, ballot)`: user exists and can vote, category exists, its election is OPEN, not voted in this category, ballot valid; then records it |
| service | `TallyService` | class | counts every category of a CLOSED election; returns `Map<Category, CategoryResult>` in file order |
| loader | `DataLoader<T>` | interface | `List<T> load(Path)` throws `DataLoadException` |
| loader | `CsvReader` | class | skips header, splits lines, reports line numbers |
| loader | `UserCsvLoader` | implements DataLoader | builds an `Administrator` or `Voter` from the role column |
| loader | `ElectionCsvLoader`, `CategoryCsvLoader`, `CandidateCsvLoader` | implement DataLoader | one per file |
| loader | `BallotCsvLoader` | implements DataLoader | reads raw ballot rows and passes them to `BallotFactory` |
| exception | `ElectionException` | checked base | parent of the business exceptions |
| exception | `UnauthorizedActionException` | extends ElectionException | a voter tries an admin action, or an admin manages another admin's election |
| exception | `ElectionStateException` | extends ElectionException | action not allowed in the current state: editing an open election, voting in a draft or closed one, results before closing, opening a category with no candidates |
| exception | `IneligibleVoterException` | extends ElectionException | user is unknown, or is an administrator |
| exception | `DuplicateVoteException` | extends ElectionException | voter already voted in this category |
| exception | `InvalidBallotException` | extends ElectionException | unknown category, candidate from another category, too many or too few choices, repeated choice, answer not YES or NO |
| exception | `DataLoadException` | checked | wraps `IOException`, `DateTimeParseException`, malformed rows, unknown role or ballot type, bad seat count, unknown election or category id |
| ui | `Menu` | interface | `void run()` |
| ui | `AdminMenu` | implements Menu | create election, add category, add candidate, open, close, view results |
| ui | `VoterMenu` | implements Menu | list open elections, vote, view results of closed elections |
| ui | `MenuFactory` | class | picks `AdminMenu` or `VoterMenu` from `user.canManageElections()` |
| ui | `ConsoleView` | class | prints each category's `summary()` and lists rejected ballots |

The ballot type lives on the category, not on each ballot. `BallotFactory` reads it to build the right subclass, so file votes and console votes go through the same rules.

Rules `ElectionManagementService` enforces before `openElection` succeeds:

- the election has at least 1 category
- every SELECT_ONE and SELECT_MULTIPLE category has at least 1 candidate
- every SELECT_MULTIPLE category has more candidates than seats

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

**elections.csv** (`createdBy` must be an administrator)

```csv
id,name,date,status,createdBy
E1,Student Council 2026,2026-11-10,OPEN,A1
E2,Sports Club Captain 2026,2026-12-01,DRAFT,A2
```

**categories.csv** (`seats` is the number of winners; always 1 for SELECT_ONE and YES_NO)

```csv
id,electionId,name,ballotType,seats
PRES,E1,President,SELECT_ONE,1
COMM,E1,Committee members,SELECT_MULTIPLE,2
FEE,E1,Raise the club fee to 10 euros?,YES_NO,1
CAPT,E2,Club captain,SELECT_ONE,1
```

**candidates.csv** (YES_NO categories have no candidates)

```csv
id,name,party,categoryId
C1,Amara Mensah,Unity,PRES
C2,Kofi Boateng,Progress,PRES
C3,Lena Dubois,Green Future,PRES
C4,Yaw Darko,Unity,COMM
C5,Marie Laurent,Progress,COMM
C6,Kwesi Appiah,Green Future,COMM
C7,Julia Weber,Independent,COMM
C8,Nana Acheampong,Independent,CAPT
C9,Tom Fischer,Independent,CAPT
```

**ballots.csv** (one row per voter per category; the format of `choices` depends on the category's ballot type)

```csv
voterId,categoryId,choices
V001,PRES,C1
V002,PRES,C1
V003,PRES,C1
V004,PRES,C1
V005,PRES,C2
V006,PRES,C2
V007,PRES,C2
V008,PRES,C3
V009,PRES,C3
V001,COMM,C4|C5
V002,COMM,C4|C6
V003,COMM,C5
V004,COMM,C4|C7
V005,COMM,C5|C6
V006,COMM,C4|C5
V007,COMM,C6|C7
V008,COMM,C4
V010,COMM,C5|C7
V001,FEE,YES
V002,FEE,NO
V003,FEE,YES
V004,FEE,YES
V005,FEE,NO
V006,FEE,YES
V007,FEE,NO
V008,FEE,YES
V010,FEE,YES
V003,PRES,C2
V005,PRES,C4
V009,COMM,C4|C5|C6
V009,FEE,MAYBE
V099,PRES,C1
V001,VP,C1
A1,PRES,C1
V001,CAPT,C8
```

The last eight ballot rows are invalid on purpose, so startup shows the error handling:

| Row | Problem | Rejected with |
| --- | --- | --- |
| `V003,PRES,C2` | V003 already voted for President | `DuplicateVoteException` |
| `V005,PRES,C4` | C4 is a committee candidate, not a presidential one | `InvalidBallotException` |
| `V009,COMM,C4\|C5\|C6` | 3 choices, but the committee has 2 seats | `InvalidBallotException` |
| `V009,FEE,MAYBE` | answer must be YES or NO | `InvalidBallotException` |
| `V099,PRES,C1` | not a known user | `IneligibleVoterException` |
| `V001,VP,C1` | no category called VP | `InvalidBallotException` |
| `A1,PRES,C1` | administrators cannot vote | `IneligibleVoterException` |
| `V001,CAPT,C8` | election E2 is still a draft | `ElectionStateException` |

V010 skips the President vote and V009 ends up with no valid committee or fee vote. Both are allowed: voting in a category is optional.

Elections, categories and candidates an administrator creates in the console exist only for that run; the files are not rewritten.

## Expected results

Once administrator A1 closes E1, Amara Mensah wins President, Yaw Darko and Marie Laurent win the two committee seats, and the fee rise passes 6 to 3. Asking for results while E1 is still open throws `ElectionStateException`.

| Category | Valid ballots | Tally | Result |
| --- | --- | --- | --- |
| President | 9 | C1 4, C2 3, C3 2 | C1 Amara Mensah elected |
| Committee members (2 seats) | 9 | C4 5, C5 5, C6 3, C7 3 | C4 Yaw Darko and C5 Marie Laurent elected |
| Raise the club fee to 10 euros? | 9 | YES 6, NO 3 | Passed (66.7% yes) |

C4 and C5 tie on 5 votes, but both win because there are 2 seats. A tie only matters at the last seat. E2 stays a draft with no ballots until A2 opens it. These numbers double as the expected values in the end-to-end test.

## Design decisions

Five decisions to settle before coding; the README asks for 2 to 5, so these cover it.

1. **Roles are subclasses, permissions are methods.** `Administrator` and `Voter` override `canManageElections()` and `canVote()`, so services ask the user instead of checking its type with `instanceof`. Only the administrator who created an election can edit, open or close it; anyone else gets `UnauthorizedActionException`.
2. **A one-way lifecycle.** DRAFT to OPEN to CLOSED, never backwards. Setup is frozen once voting starts, so no one can add a candidate mid-vote, and results exist only after closing.
3. **The ballot type belongs to the category.** A ballot row only carries voter, category and choices, so a file cannot pair a yes/no answer with the President category. `BallotFactory` reads the type and builds the matching subclass.
4. **Ties.** If candidates tie for the last winning seat, that seat stays undecided and the result lists the tied candidates. A yes/no vote with equal yes and no is rejected, because a change needs more support than opposition.
5. **Checked exceptions.** `ElectionException` is checked, so every caller must decide what to do with a business error instead of letting it crash the app.

How SOLID shows up:

- **Single responsibility:** loading, election management, ballot creation, voting, counting and display are separate classes.
- **Open/closed:** adding a ranked ballot later means a new `Ballot` subclass, a new `CountingMethod`, an enum value and one registry entry; `TallyService`, `VotingService` and the existing counting methods stay untouched. A new role, such as an observer who can only view results, is a new `User` subclass.
- **Liskov substitution:** any `User` works wherever the services expect one; `MostVotesCount` reads any ballot through `selections()`, so single and multiple choice ballots are interchangeable to it.
- **Interface segregation:** `CountingMethod`, `DataLoader<T>` and `Menu` each have one method.
- **Dependency inversion:** `TallyService` depends on the `CountingMethod` interface through the registry, never on `MostVotesCount` or `YesNoCount` directly; `Application` only talks to the `Menu` interface.

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

**Loader tests**

- [ ] Unknown role in users.csv throws `DataLoadException` with the line number
- [ ] Election whose `createdBy` is a voter throws `DataLoadException`
- [ ] Unknown ballot type in categories.csv throws `DataLoadException`
- [ ] SELECT_ONE category with 2 seats throws `DataLoadException`
- [ ] Category or candidate pointing to an unknown id throws `DataLoadException`
- [ ] Missing column throws `DataLoadException`

**End-to-end**

- [ ] Loading the sample files rejects exactly 8 ballots; after A1 closes E1, the results match Expected results

Run with `mvn test`.
