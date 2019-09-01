---
name: Could not find the pull request
about: Reporting the plugin cannot find the pull request
title: Could not find the pull request
labels: Bug
assignees: shiraji

---

This problem highly related to your environment. Please answer following questions. If you ignore the template, the developer won't be able to help you.

Also, please consider using debug mode to submit this issue. You need to change the setting to use debug mode.

## Does your repository squash PR commits and customize the commit message?

## Does the commit merge to master branch?

## Does the PR repository url set to origin or upstream?

## Do you use multiple repositories?

## Could you give me the result of following commands?

```
git log {commit hash}..HEAD --merges --ancestry-path --reverse --oneline --no-abbrev-commit
// find merged commit hash
git log {merged commit hash}^..{merged commit hash} --oneline --no-abbrev-commit
// Don't forget ^
```

For example, if I have problem finding PR for `1a9c5fbfac280f9d81f5125073fe6cf3f1bfeaf7`, I ran the following commands:

```
$ git log 1a9c5fbfac280f9d81f5125073fe6cf3f1bfeaf7..HEAD --merges --ancestry-path --reverse --oneline --no-abbrev-commit 

ba7534eed14cb5a742ea9ccde12bcaf854af6856 Merge pull request #29 from shiraji/fix_rev_null

$ git log ba7534eed14cb5a742ea9ccde12bcaf854af6856\^..ba7534eed14cb5a742ea9ccde12bcaf854af6856 --oneline --no-abbrev-commit 

ba7534eed14cb5a742ea9ccde12bcaf854af6856 Merge pull request #29 from shiraji/fix_rev_null
02f4c56c2a88e397627005720053028d5dacc4d3 (origin/fix_rev_null, fix_rev_null) Update travis settings
1a9c5fbfac280f9d81f5125073fe6cf3f1bfeaf7 Update gradle setting
6046d2f850ff7847e3228573afcf45f97cbbaf71 Update code style settings
271add58827e473d6b5f52e9fca10c5e9ac3d3c9 Refactor a lot
fa4febe58815aa205bee00864ebdd5d29b34db6f Mave FindPullRequestHelper top level methods
32e4cd51981b6043130f216a61ec8f694ef32530 Upgrade intellij-gradle-plugin
aa767458b9bc5a9a795e15bba7cb773247922016 Add file md5 null check
61de5773abcbf4af8d9b7301a70bfd989d2a2735 Make path val
051051b8ba423b75a4d6acda74d4691e301a21bd Change the way to calculate file MD5
```

**You can hide your commit messages when you post the result**
