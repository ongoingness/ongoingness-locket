# Contributing

## Committing
Git commits should be made using [Gitmoji](https://gitmoji.carloscuesta.me/)
where commits are prefixed with an emoji, related to the content of the
commit for improved visual grepping of commit type.

## Updating into Master
Before merging from dev into master, you should create a new branch called
`rc-x.x.x`, where `x.x.x` is the new version number. This branch should be
properly tested, and all instances of the version number should be updated
and documentation should be updated. A pull request can be made to merge
this branch into master. All tests on the `rc` branch should pass before
a request is made.

Merges into master should be made via a pull request. This should make it
easier to identify when and who introduced and approved new features or
changes. Pull requests also ensure code is inline with the rest of the
project when reviewed before approval. On a successful merge the changelog
should be updated with a description of the change (see below).

After the `rc-x.x.x` branch has been merged, the master branch should be
tagged with the new version number in the format `v-x.x.x`. The `rc-x.x.x`
should then be merged into `dev` and then the `rc-x.x.x` branch should
be deleted.

## Hotfixing
Hot fixes should be made by branching `master`, and creating a new branch
called `hf-x.x.x+1` where the fix version is updated. Changes should be
made here, and when completed a pull request into `master` should be made.
Once merged into `master`, `master` should be tagged with the new version
number. `hf-x.x.x+1` should then be merged into `dev`, and the hotfix
branch then deleted. The contributing guide should be updated (see below).

## Changelog
When a pull request into master has been approved, the changelog should
be updated in the following format:
```
* Month, Year
    * Internal changes (After merging rc branches)
        * Refactoring something (<pull request id>)
    * New features (After merging rc branches)
        * A new feature (<pull request id>)
    * Bug fix (After merging 'hf' branches)
        * Fix for ... (<pull request id>)
```
