# Election App

Console election app for the Java OOP final project. The full design, with every class and the sample data, is in [PLAN.md](PLAN.md).

## Purpose

The app runs small elections, such as a student council or a club vote. Administrators set elections up and voters vote in them. Then the app counts each category with the rule that fits it: the most votes for one seat, the top N for several seats, or more yes than no for a question.

The business logic is in the rules:

- who may do what
- the one-way life of an election
- the checks before voting opens
- one submission per voter
- validating every ballot
- counting several seats
- handling ties

## How to run

You need JDK 21 or later. Run the app from the project folder, because it reads its data from `src/main/resources`.

- **IntelliJ:** open the folder as a Maven project, then run `com.election.Application`.
- **Maven:** `mvn compile exec:java`

Log in by typing a user ID from `users.csv`:

| ID | Role |
| --- | --- |
| `A1`, `A2` | administrator |
| `V001` to `V010` | voter |

Choose `0. Log out` to go back to the login, and type `exit` there to quit. Changes are kept in memory until the program exits, and they are not written back to the files. Every run starts from the same sample data.

A short tour:

1. Log in as `V005` and vote in *Graduation Speaker 2026* (E5).
2. Log in as `A1`, close *Student Council 2026* (E1), and view its results.
3. *Student Council 2025* (E3) is already closed. It shows a tie for Treasurer.

## Main features

**Administrators**

- Create an election with a name, a deadline, categories and candidates.
- While it is a draft:
  - edit its details, categories and candidates;
  - change a category's ballot type;
  - delete it.
- Open voting. The app first checks that the election is ready and lists every problem at once.
- Close voting and view results.
- An administrator can only manage the elections they created.

**Voters**

- See the open elections and look inside one: its categories, types and candidates.
- Vote once per election:
  - answer each category, or skip it;
  - review a summary;
  - change an answer if needed;
  - submit everything together.
- View the results of closed elections.

**Three ballot types**

| Type | The voter... | Result |
| --- | --- | --- |
| Select one | picks 1 candidate | the most votes wins |
| Select multiple | picks up to N candidates | the top N win |
| Yes / No | answers a question | passes if yes beats no |

Every list is picked by number, and `0` goes back or cancels. A wrong answer is asked again instead of crashing the app.

## Main classes

| Package | Main classes | Job |
| --- | --- | --- |
| `model` | `Election`, `Category`, `Candidate`, `User`, `Ballot`, `CategoryResult` | The domain objects and their own rules: an election's state changes, a category's seats, a ballot checking itself |
| `repository` | `ElectionRepository`, `UserRepository` | Store the loaded objects and find them by id or status |
| `service` | `ElectionManagementService`, `VotingService`, `TallyService`, `BallotFactory`, `CountingMethodRegistry` | The business rules: admin actions, voting, counting |
| `loader` | `CsvReader` and one `DataLoader` per file | Read the CSV files into objects |
| `exception` | `ElectionException` and its subclasses, `DataLoadException` | Business errors and loading errors |
| `ui` | `MenuFactory`, `AdminMenu`, `VoterMenu`, `ConsoleInput`, `ResultView` | Read the keyboard and print. The menus only ask a service; they hold no rules |
| (root) | `Application` | Wiring only: load the files, build the services, log in, start the right menu |

## Technical decisions

1. **Roles are subclasses, and permissions are methods.** `Administrator` and `Voter` each answer `canManageElections()` and `canVote()`. The services ask the user object instead of checking a role string. Adding a new role means adding one class, and no `if` statements change.
2. **An election only moves forward: DRAFT → OPEN → CLOSED.**
   - Setup is frozen once voting starts, so nobody can add a candidate in the middle of a vote.
   - Results only exist after closing.
   - The deadline is a `LocalDateTime`. Votes are refused from that moment on, and the services read the time from an injected `Clock`, so the tests can fix the time.
3. **The ballot type belongs to the category, not to the ballot.** `BallotFactory` reads the category's type and builds the matching `Ballot` subclass. Votes typed in the console and votes loaded from `ballots.csv` go through the same factory, so they follow exactly the same rules.
4. **Ties are not decided by luck.**
   - If candidates tie for the last seat, that seat stays undecided and the result lists the tied candidates.
   - A tie that still fits within the seats is not a tie.
   - A yes / no tie is rejected.
5. **Business errors are checked exceptions.** `ElectionException` is checked, so the compiler makes every caller decide what to do with a refused action. The menus catch it and print the reason, and the user tries again.

## Where inheritance is used

| Parent (abstract) | Children | What they share |
| --- | --- | --- |
| `User` | `Administrator`, `Voter` | The id. Each child answers the two permission questions in its own way |
| `Ballot` | `SingleChoiceBallot`, `MultipleChoiceBallot`, `YesNoBallot` | Voter, election, category and choices, plus helpers for matching candidate ids |
| `CategoryResult` | `CandidateResult`, `YesNoResult` | The category, the tallies and the ballot count |
| `ElectionException` | six business exceptions (see below) | Lets a menu catch every business error in one place |

## Where polymorphism is used

The code works with the parent type, and the behaviour depends on the real object:

- **`MenuFactory`** calls `user.canManageElections()` on a `User` to pick `AdminMenu` or `VoterMenu`. `Application` then just calls `menu.run()`.
- **`BallotFactory`** builds a ballot and calls `ballot.validate(category)` on a `Ballot` reference. Each subclass checks its own rules: exactly one candidate, up to N distinct candidates, or YES / NO.
- **`TallyService`** gets a `CountingMethod` from the registry for each category and calls `count(...)`, without knowing whether it is `MostVotesCount` or `YesNoCount`:

  ```java
  for (Category category : election.getCategories()) {
      CountingMethod method = countingMethods.methodFor(category.getBallotType());
      results.put(category, method.count(category, ballots));
  }
  ```

- **`ResultView`** prints any list of results through `result.summary()`. A candidate result prints its tally and winners, and a yes / no result prints its counts and whether it passed.

## Where the abstract classes and interfaces are used

**Abstract classes** are used where the subclasses share data and code, but the parent alone means nothing. A user is always an administrator or a voter; it is never just "a user".

- `User`
- `Ballot`, whose abstract method is `validate`
- `CategoryResult`, whose abstract method is `summary`

**Interfaces** are used where only a capability is shared, with no state. Each has a single method, so a class depends only on what it uses:

| Interface | Method | Implemented by |
| --- | --- | --- |
| `ManagementPermission` | `canManageElections()` | `User` |
| `VotingPermission` | `canVote()` | `User` |
| `CountingMethod` | `count(category, ballots)` | `MostVotesCount`, `YesNoCount` |
| `DataLoader<T>` | `load(path)` | one loader per CSV file (generic in the type it builds) |
| `Menu` | `run()` | every screen |

## Custom exceptions

| Exception | Kind | Thrown when |
| --- | --- | --- |
| `ElectionException` | checked, the base class | (parent of the six below) |
| `UnauthorizedActionException` | business | a voter tries an admin action, or an admin manages another admin's election |
| `ElectionStateException` | business | the action does not fit the election's state, for example editing an open election, voting in a closed one, results before closing, or opening an election that is not ready |
| `IneligibleVoterException` | business | an administrator tries to vote |
| `DuplicateVoteException` | business | a voter submits a second time in the same election |
| `InvalidBallotException` | business | the choices don't fit the category: unknown candidate, too many choices, a repeated choice, or an answer that is not YES / NO |
| `InvalidNameException` | business | a name is empty or uses characters that are not allowed (commas would break the CSV files) |
| `DataLoadException` | checked | a data file is missing, unreadable or has a bad row. It wraps `IOException` and parsing errors |
| `CancelledException` | unchecked, `ui` | the user types `0` to leave a multi-step form. This is not an error, so callers are not forced to declare it |

Standard exceptions are used too:

- `IllegalArgumentException` for invalid constructor arguments.
- `DateTimeParseException` for a bad date, which the console catches so it can ask again.

## How the application loads its data

At startup `Application` loads five CSV files from `src/main/resources`. They load in this order, because each file refers to ids from the files before it:

| File | Holds | Loaded by |
| --- | --- | --- |
| `users.csv` | id, role | `UserCsvLoader`, which builds an `Administrator` or a `Voter` |
| `elections.csv` | id, name, deadline, status, creator | `ElectionCsvLoader` |
| `categories.csv` | id, election, name, ballot type, seats | `CategoryCsvLoader`, which adds each category to its election |
| `candidates.csv` | id, name, election, category | `CandidateCsvLoader`, which adds each candidate to its category |
| `ballots.csv` | voter, election, category, choices (several joined with `\|`) | `BallotCsvLoader`, which passes each row through `BallotFactory` |

`CsvReader` does the part every file shares:

- reads the file and checks the header;
- skips blank lines;
- splits the columns and checks how many there are.

The loaders then build the objects with the same constructors and rules the console uses. The first bad row stops the app with its file, line and reason, for example:

```
categories.csv line 13: 'RANKED' is not one of [SELECT_ONE, SELECT_MULTIPLE, YES_NO]
```

## How SOLID principles are applied

- **Single responsibility.** Loading, storing (repositories), election management, building ballots, voting, counting and display are all separate classes. `Application` only wires them together, and the menus only read input and print.
- **Open/closed.** Adding a new ballot type, such as a ranked vote, would take:
  - a new `Ballot` subclass;
  - a new `CountingMethod`;
  - an enum value and one registry entry.

  `TallyService`, `VotingService` and the existing counting methods would not change.
- **Liskov substitution.** Any `User` works wherever the services expect one. `MostVotesCount` reads any ballot through `getChoices()`, so single- and multiple-choice ballots can be used in place of each other.
- **Interface segregation.** `ManagementPermission`, `VotingPermission`, `CountingMethod`, `DataLoader<T>` and `Menu` each have one method.
- **Dependency inversion.**
  - `TallyService` depends on the `CountingMethod` interface through `CountingMethodRegistry`, never on a concrete count.
  - The services receive their repository and `Clock` through the constructor instead of creating them, which is what makes them testable.

## How to run the tests

- **IntelliJ:** right-click `src/test/java`, then choose Run.
- **Maven:** `mvn test`

The project has 293 JUnit 5 tests, one test class per class under test. They cover normal cases, edge cases and refused actions:

- each model class's own rules;
- every permission and state check in the services;
- the deadline down to the second, using a fixed `Clock`;
- counting, including ties;
- every kind of bad CSV row, using small files in a `@TempDir`;
- console input, with typed answers given as a string.

Two end-to-end tests run the whole app without the console:

- `SeedDataTest` loads the real data files and checks the results against the expected numbers in PLAN.md.
- `LifecycleTest` runs one election through create → open → vote → close → count.
