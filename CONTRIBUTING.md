# HealthTracker REST API

The HealthTracker REST API Contributing Guidelines and Local Development Setup

## Branching Conventions

This project follows the [gitflow workflow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow) that was first developed by [Vincent Driessen](https://nvie.com/posts/a-successful-git-branching-model/).

### Branch Names

| Gitflow Branches | Source Branch | Target Branch | Purpose |
|---|---|---|---|
| feature/* | develop | develop | short lived branch used for development of a single feature |
| bugfix/* | develop, release | develop, release | short lived branch used for fixing of bugs |
| develop | n/a | n/a | used for aggregating development changes before cutting a release |
| release/* | develop | master | release for current features |
| hotfix/* | master | master | short-lived branch used for debugging of production critical issues |
| master | n/a | n/a | represents what is currently deployed to production |

### JIRA

Before contributing to this project please first create a issue within JIRA so that you can get credit for your good deeds!

| JIRA Issue Type | Branch Name |
|---|---|
| Story, Task | feature/{issue-id} |
| Bug | bugfix/{issue-id}, hotfix/{issue-id} |

By including the issue-id in the branch name JIRA is automagically able to track it on the associated issue.

### Pull Requests

For this project all pull requests must meet the following criteria:

- Passes all build & test pipelines
- At least 1 reviewer has approved
- No unresolved questions/comments
- Documentation is updated as needed

These following gates are not currenlty implemented but will be in the future, so it might help to keep them in mind:

- For a feature at least 1 happy path & 1 unhappy path test for a new feature
- For a bug at least 1 regression test
- Code Coverage must not fall below a set limit (50% to start then increasing in 5% blocks till 85%)

## Development

### Code Formatting
This project follows [Google's Java style](https://google.github.io/styleguide/javaguide.html), and includes a pre-commit hook to automatically format code. To force format the whole project, run `make format`.

### Auth0

This project uses Auth0 for it's authentication.

Currently only the environments (stagin, prod) that contain PHI have Auth0 enabled.

To make a call against the API you will need to obtain an `Bearer Token` from Auth0 and include it in the `Authorization` header.

```bash
curl --request POST \
  --url https://XXXX.auth0.com/oauth/token \
  --header 'content-type: application/json' \
  --data '{"client_id":"XXXX","client_secret":"XXXX","audience":"https://XXXX/","grant_type":"client_credentials"}'
```

Sample response:

```bash
{
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9UZzNNME5HUWpRNU56SkRSRVZETkRkRFFqZzJNalV3UXpVME5ERXpRakZETWtSR09ERTJNUSJ9.eyJpc3MiOiJodHRwczovL25hdmlnYXRpbmdjYXJlLWRldi5hdXRoMC5jb20vIiwic3ViIjoiZmRlSTBHRW9yVjJjZHF4aHp2WjdrQkg1Q1VKSDRka2JAY2xpZW50cyIsImF1ZCI6Imh0dHBzOi8vZWtzLXRlc3QubmMtYWNjZXB0YW5jZS5jb20vaHQtYXBpLyIsImlhdCI6MTU2NjkyODk4NSwiZXhwIjoxNTY3MDE1Mzg1LCJhenAiOiJmZGVJMEdFb3JWMmNkcXhoenZaN2tCSDVDVUpINGRrYiIsInNjb3BlIjoicmVhZDplbnJvbGxtZW50cy1odCB1cGRhdGU6ZW5yb2xsbWVudHMtaHQiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.IlrUGm06eleckdlSj1FDakAy4vJzaPRWRZIZCTrwSzt6JW8EhK89T1RZYVr-X-8bFN9ZUvGXIv6OgI9sWo0NgXJD92EOK6LLVxsbKvpliiShDlaHgvglPUehIcZ_cwIAX071xAtuz1IFnewoqIn9rNWfMcelJ3qPocTrH53YFTuP-oujz7q6co4KFvRUsyFt_XSaPfhjOfAfpQqK1rcQkvSq7DhpQv10uwkuGh3Sc9uVJ1lpK0DGNNQQBWdsNJNUrhSnZOsLzLgeQwcinMBsrYz0NfL3Tt_E4Fb0OVBQ7yHUpMaTaqbAJy_Whe2KerYMo8Rea-IBYt9Uz1-ngmIvsQ",
  "token_type": "Bearer"
}
```
