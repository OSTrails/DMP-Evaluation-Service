# Contributing Guidelines

Thank you for considering contributing to the OSTrails Open Source code.
We welcome contributions from everyone, regardless of your level of experience.
This document outlines the process for contributing to the code.

## Ways of Contributing

- **Reporting Issues**: If you find a bug in the code, please open an issue on the
GitHub repository.
- **Suggesting Enhancements**: If you have an idea for improving the code, please
open an issue on the GitHub repository.

## Issue Reporting

If you find a bug in the code, please
[open an issue](https://github.com/OSTrails/DMP-Evaluation-Service/issues/new) in the
GitHub repository.

## Way of Working

The following part describes the way of working when contributing to the code.
Always also comply to the [Code of Conduct](./CODE_OF_CONDUCT.md) when contributing.

### Commit Messages

Please follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)
specification when writing commit messages.
This will help us to automatically generate the changelog.

For instance, the following commit message:

``` text
- *Added (for new features)*
- *Changed (for changes in existing functionality)*
- *Deprecated (for soon-to-be removed features)*
- *Removed (for now removed features)*
- *Fixed (for any bug fixes)*
- *Security (in case of vulnerabilities)*
```

### Branching and Pull Requests

When contributing to the code, please follow these steps:

1. Fork the repository (if not in the OSTrails organisation).
2. Create a new branch for your changes (base = `nextRelease`).
3. Make your changes and commit them.
4. Push your changes.
5. Open a pull request to the `nextRelease` branch of the main repository.

Both `nextRelease` and `main` branches are protected and require a review before merging.
The `next` branch is used for the next release, while the `main` branch is used
for the current release
(and only release/hotfix PRs will be merged according to
[Gitflow workflow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow)).

### Crediting Contributors

We will credit all contributors. If you would like to be credited,
please add your name and related information to the
[CONTRIBUTORS.md](./CONTRIBUTORS.md) and [CITATION.cff](./CITATION.cff) files.
