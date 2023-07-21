# Contributing to AMPCS

This document describes the process of contributing to AMPCS as well
as the standards that will be applied when evaluating contributions.

## Summary

The short version:

1. Write your contribution.
2. Make sure your contribution meets code, test, and commit message
   standards as described below.
3. Submit a pull request from a topic branch back to `master`. Include a check
   list, as described below. (Optionally, assign this to a specific member
   of the AMPCS team for review.)
4. Respond to any discussion. When the reviewer decides it's ready, they
   will merge back `master` and fill out their own check list.

## Contribution Process

AMPCS uses git for software version control, and for branching and
merging. The central repository is at
https://github.com/NASA-AMMOS/ampcs

### Roles

References to roles are made throughout this document. These are not intended
to reflect titles or long-term job assignments; rather, these are used as
descriptors to refer to members of the development team performing tasks in
the check-in process. These roles are:

* _Author_: The individual who has made changes to files in the software
  repository, and wishes to check these in.
* _Reviewer_: The individual who reviews changes to files before they are
  checked in.
* _Integrator_: The individual who performs the task of merging these files.
  Usually the reviewer.

### Branching

Three basic types of branches may be included in the above repository:

1. Master branch.
2. Topic branches.
3. Developer branches.

Branches which do not fit into the above categories may be created and used
during the course of development for various reasons, such as large-scale
refactoring of code or implementation of complex features which may cause
instability. In these exceptional cases it is the responsibility of the
developer who initiates the task which motivated this branching to
communicate to the team the role of these branches and any associated
procedures for the duration of their use.

#### Master Branch

The role of the `master` branches is to represent the latest
"ready for test" version of the software. Source code on the master
branch has undergone peer review, and will undergo regular automated
testing with notification on failure. Master branches may be unstable
(particularly for recent features), but the intent is for the stability of
any features on master branches to be non-decreasing. It is the shared
responsibility of authors, reviewers, and integrators to ensure this.

#### Topic Branches

Topic branches are used by developers to perform and record work on issues.
Issues are tracked at https://jira-int.jpl.nasa.gov/servicedesk/customer/portal/36

Topic branches need not necessarily be stable, even when pushed to the
central repository; in fact, the practice of making incremental commits
while working on an issue and pushing these to the central repository is
encouraged, to avoid lost work and to share work-in-progress. (Small commits
also help isolate changes, which can help in identifying which change
introduced a defect, particularly when that defect went unnoticed for some
time, e.g. using `git bisect`.)

Topic branches should be named according to their corresponding issue
identifiers, all lower case, without hyphens. (e.g. branch AMPCS-123 would refer
to issue #123.)

In some cases, work on an issue may warrant the use of multiple divergent
branches; for instance, when a developer wants to try more than one solution
and compare them, or when a "dead end" is reached and an initial approach to
resolving an issue needs to be abandoned. In these cases, a short suffix
should be added to the additional branches; this may be simply a single
character (e.g. wtd481b) or, where useful, a descriptive term for what
distinguishes the branches (e.g. wtd481verbose). It is the responsibility of
the author to communicate which branch is intended to be merged to both the
reviewer and the integrator.

#### Developer Branches

Developer branches are any branches used for purposes outside of the scope
of the above; e.g. to try things out, or maintain a "my latest stuff"
branch that is not delayed by the review and integration process. These
may be pushed to the central repository, and may follow any naming convention
desired so long as the owner of the branch is identifiable, and so long as
the name chosen could not be mistaken for a topic or master branch.

### Merging

When development is complete on an issue, the first step toward merging it
back into the master branch is to file a Pull Request. The contributions
should meet code, test, and commit message standards as described below,
and the pull request should include a completed author checklist, also
as described below. Pull requests may be assigned to specific AMPCS team
members when appropriate (e.g. to draw to a specific person's attention.)

Code review should take place using discussion features within the pull
request. When the reviewer is satisfied, they should add a comment to
the pull request containing the reviewer checklist (from below) and complete
the merge back to the master branch.

## Standards

Contributions to AMPCS are expected to meet the following standards. In
addition, reviewers should use general discretion before accepting changes.

### Code Standards


#### Code Guidelines

Java sources in AMPCS should:

* Use four spaces for indentation. Tabs should not be used.
* Use Javadoc style documentation for public members, methods, and classes, see https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html
* Define one public class per file.
* Avoid deep nesting
* Follow Java naming conventions. These include:
    * Classes should use camel case, first letter capitalized (e.g. SomeClassName.)
    * Methods, variables, fields, and function names should use camel case,
      first letter lower-case (e.g. someVariableName.) 
    * Constants (variables or fields which are meant to be declared and initialized
      statically, and never changed) should use only capital letters, with
      underscores between words (e.g. SOME_CONSTANT.)
    * File name should be the name of the class, plus a .java extension (e.g. SomeClassName.java)

Deviations from AMPCS code style guidelines require two-party agreement,
typically from the author of the change and its reviewer.


### Test Standards

Automated testing shall occur whenever changes are merged into the main
development branch and must be confirmed alongside any pull request.

Automated tests are typically unit tests which exercise individual software
components. Tests are subject to code review along with the actual
implementation, to ensure that tests are applicable and useful.

Examples of useful tests:
* Tests which replicate bugs (or their root causes) to verify their
  resolution.
* Tests which reflect details from software specifications.
* Tests which exercise edge or corner cases among inputs.
* Tests which verify expected interactions with other components in the
  system.

During automated testing, code coverage metrics will be reported. Line
coverage must remain at or above 80%.

### Commit Message Standards

Commit messages should:

* Contain a one-line subject, followed by one line of white space,
  followed by one or more descriptive paragraphs, each separated by one
  ￼￼￼￼￼  line of white space.
* Contain a short (usually one word) reference to the feature or subsystem
  the commit effects, in square brackets, at the start of the subject line
  (e.g. `[Documentation] Draft of check-in process`)
* Contain a reference to a relevant issue number in the body of the commit.
    * This is important for traceability; while branch names also provide this,
      you cannot tell from looking at a commit what branch it was authored on.
    * This may be omitted if the relevant issue is otherwise obvious from the
      commit history (that is, if using `git log` from the relevant commit
      directly leads to a similar issue reference) to minimize clutter.
* Describe the change that was made, and any useful rationale therefore.
    * Comments in code should explain what things do, commit messages describe
      how they came to be done that way.
* Provide sufficient information for a reviewer to understand the changes
  made and their relationship to previous code.


See [Contributing to a Project](http://git-scm.com/book/ch5-2.html) from
Pro Git by Shawn Chacon and Ben Straub for a bit of the rationale behind
these standards.

## Issue Reporting

Issues are tracked at https://jira-int.jpl.nasa.gov/servicedesk/customer/portal/36

Issues should include:

* A short description of the issue encountered.
* A longer-form description of the issue encountered. When possible, steps to
  reproduce the issue.
* When possible, a description of the impact of the issue. What use case does
  it impede?
* An assessment of the severity of the issue.

Issue severity is categorized as follows (in ascending order):

* _Trivial_: Minimal impact on the usefulness and functionality of the
  software; a "nice-to-have."
* _Major_: Major loss of functionality or impairment of use.
* _Critical_: Large-scale loss of functionality or impairment of use,
  such that remaining utility becomes marginal.
* _Blocker_: Harmful or otherwise unacceptable behavior. Must fix.

## Check Lists

The following check lists should be completed and attached to pull requests
when they are filed (author checklist) and when they are merged (reviewer
checklist.)

### Author Checklist

1. Changes address original issue?
2. Unit tests included and/or updated with changes?
3. Command line build passes?
4. Changes have been smoke-tested?

### Reviewer Checklist

1. Changes appear to address issue?
2. Appropriate unit tests included?
3. Code style and in-line documentation are appropriate?
4. Commit messages meet standards?
