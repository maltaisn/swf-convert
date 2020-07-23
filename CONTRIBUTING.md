## Contributing

A few guidelines for contributors.

### Issues

- If creating or fixing an issue related to a bad conversion, *always* provide
sample files that can be used to reproduce the issue.
- Look for existing issues.
- Include logs when appropriate. Logs can be found in `~/swfconvert/logs`.
- Include all other relevant information: version, OS version, viewer used, stacktrace, etc.

### Pull request
- Always open an issue before making a non trivial pull request.
- All changes should be commited to the `dev` branch, not `master`.
- Add tests when possible.

**Before creating a pull request**
- Run `gradlew detekt` to check for style issues.
- Run `gradlew test` to make sure all tests pass.
