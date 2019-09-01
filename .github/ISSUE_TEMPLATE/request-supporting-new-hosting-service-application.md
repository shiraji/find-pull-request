---
name: Request supporting new hosting service/application
about: Add support for new hosting service/application
title: Request supporting new hosting service/application
labels: FeatureRequest
assignees: ''

---

Please fill in following questions. 

Because some hosting service/application might not be able to use by maintainer, keep in mind that the feature may be lack of support. Feel free to volunteer to maintain the supporting the hosting service/application :wink: 

## Name of the git hosting service/application

## default merge commit message
e.g. Merge pull request $PR_NUMBER for GitHub

## default squash commit message (if any)
e.g. .*($PR_NUMBER) for GitHub

## PR url path format 
e.g. https://$DOMAIN/$USER_NAME/$PROJECT_REPO/pull/$PR_NUMBER for GitHub

## commit page url format
e.g. https://$DOMAIN/$USER_NAME/$PROJECT_REPO/commit/$COMMIT_HASH#diff-$FILENAME_MD5 for GitHub

## The name of the pull request
e.g. pull request for GitHub, merge request for GitLab

## the page of icon that I can use (if any)
e.g. https://github.com/logos

---

If you are interested to implement this support, please start adding these in https://github.com/shiraji/find-pull-request/blob/7f0d052f4776dc4d4b4eb8f237851ec5c6779122/src/main/kotlin/com/github/shiraji/findpullrequest/model/FindPullRequestHostingServices.kt
