---
inclusion: always
name: test-execution-rule
description: Mandatory rule for test execution and validation in the Warranty Tracker API project
---

# Test Execution Rule for Warranty Tracker API

## Status: MANDATORY
This rule is project-wide and applies to all tasks, implementations, and code modifications.

---

## Core Rule: No Automatic Test Execution

**CRITICAL**: Kiro MUST NEVER run unit tests automatically without explicit user request.

### What This Means

- Do NOT run ./gradlew test
- Do NOT run individual test classes
- Do NOT run any test command, even if code is modified or new code is added
- Do NOT assume tests pass
- Do NOT assume tests fail
- Do NOT attempt to diagnose test failures without seeing the actual output

### When Kiro Implements Code

1. **After implementing or modifying code**, Kiro MUST:
   - Stop and ask the user to run the relevant tests
   - Provide clear instructions on which tests to run (e.g., "Please run: ./gradlew test --tests WarrantyCalculatorTest")
   - Wait for the user to provide test output

2. **After receiving test output**, Kiro MUST:
   - Analyze the provided test results
   - Identify any test failures
   - Fix failures in the code if necessary
   - Do NOT run tests again automatically

3. **After making a fix**, Kiro MUST:
   - Stop and ask the user to run tests again
   - Wait for the user to provide new test output
   - Repeat the process until tests pass

### No Assumptions

- Kiro MUST NOT assume that tests pass based on code inspection
- Kiro MUST NOT assume that tests fail based on intuition
- Kiro MUST ONLY proceed when the user explicitly provides test results
- The user is the source of truth for test status

---

## Workflow

`
Kiro Implements Code
        ↓
Kiro Asks User to Run Tests
        ↓
User Runs Tests
        ↓
User Provides Test Output to Kiro
        ↓
Kiro Analyzes Results
        ↓
      Passes?
      ↙   ↘
    YES    NO
     ↓      ↓
  Move   Fix Code
  Next   ↓
  Task   Kiro Asks User
         to Run Tests Again
         ↓
      (Loop)
`

---

## Application Scope

This rule applies to ALL unit tests in the project:

- **Service Tests**: WarrantyCalculatorTest.java, CategoryServiceTest.java, ProductServiceTest.java
- **Controller Tests**: CategoryControllerTest.java, ProductControllerTest.java
- **Any Other Tests**: All unit test files created during implementation

This rule does NOT apply to:

- Manual API testing (Phase 6 manual verification is separate)
- Integration testing (project uses unit tests only)
- Build verification (build system can compile)

---

## User Responsibility

The user is responsible for:

1. Running tests when Kiro requests
2. Providing complete test output to Kiro
3. Confirming when tests pass
4. Confirming when tests fail

Example: User runs and provides output like:
`
$ ./gradlew test --tests WarrantyCalculatorTest
...
BUILD SUCCESSFUL
12 tests passed
`

Or:

`
$ ./gradlew test --tests ProductServiceTest
...
FAILED - ProductServiceTest::testCreateProductValidatesName
Expected ValidationException but got nothing
...
BUILD FAILED
`

---

## Implementation Notes for Kiro

When working on this project:

1. **Before Each Task**: Check if this steering file is active
2. **After Writing Code**: ALWAYS ask user to run tests (do not run them yourself)
3. **Never Assume**: Wait for user-provided test results before proceeding
4. **Analyze Failure**: Use provided output to identify and fix issues
5. **Iterate**: Repeat test/fix cycle until user confirms tests pass
6. **Move Forward**: Only proceed to next task after user confirms tests pass

---

## Examples of Correct Behavior

### ✅ CORRECT: Asking for Test Execution
`
I've implemented WarrantyCalculator.java. 

Please run the tests to verify:
./gradlew test --tests WarrantyCalculatorTest

Once you provide the test output, I'll review it and fix any failures.
`

### ✅ CORRECT: Analyzing User-Provided Output
`
User provides:
> FAILED - testCalculateStatusExpiredWhenTodayAfterWarrantyEndDate
> Expected: EXPIRED, Got: ACTIVE

I can see the issue. The boundary condition in the comparison is wrong.
I'll fix it now...

[fixes code]

Please run the test again and let me know the result.
`

### ❌ INCORRECT: Running Tests Automatically
`
I've implemented the code. Let me run the tests to verify...
[runs ./gradlew test]
All tests pass!
`

### ❌ INCORRECT: Assuming Test Results
`
I've reviewed the code and it looks correct, so tests should pass.
Let's move to the next task.
`

---

## Duration

This rule is active for the entire Warranty Tracker API MVP project and remains in effect for:
- All implementation tasks (Tasks 1-19)
- All code modifications during implementation
- Bug fixes and iterations

The rule is lifted only when:
- User explicitly disables it
- Project implementation is complete and all tests have been verified to pass

---

## Build and Compilation Execution

**CRITICAL:** Kiro MUST NOT run Java compilation or build commands automatically.

* Do NOT run `./gradlew build`
* Do NOT run `./gradlew compileJava`
* Do NOT run `./gradlew compileTestJava`
* Do NOT run any other Gradle build or Java compilation command to verify the implementation.
* Do NOT run build/compile commands just to check whether the code compiles.
* Do NOT automatically execute commands that perform compilation as part of validation.

When implementation is complete, Kiro should provide the user with the relevant command if compilation or build verification is needed, but **the user must execute the command and provide the result**.

For example:

> Please run:
> `./gradlew compileJava`
>
> Please provide the output so I can review it.

If the user provides a compilation or build error:

1. Analyze the provided output.
2. Identify the root cause.
3. Make the necessary code changes.
4. Do NOT run the compilation/build command again automatically.
5. Ask the user to run the command again and provide the new result.

The user is the source of truth for compilation, build, and test status.

This rule applies throughout the project unless the user explicitly asks Kiro to execute a build or compilation command.
