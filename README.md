# find-pull-request

[![Software License](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg)](https://github.com/shiraji/find-pull-request/blob/master/LICENSE)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-find--pull--request-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/3353)
[![Build Status](https://travis-ci.org/shiraji/find-pull-request.svg?branch=master)](https://travis-ci.org/shiraji/find-pull-request)

Intellij plugin that jumps to the pull request page (or GitHub commits page)

## How to use

* Select/Choose a line
* Right click
* Click "Find Pull Request"

![screenshot](website/images/screenshot.gif)

* Open the pull request page if the plugin finds pull request
* Open the commit page if no pull request found for the selected line

## Requirement

* Remote repo should be GitHub/GitHub Enterprise
* Repository must set upstream or origin remote url
 * if upstream is set, then use upstream remote url
* Do not select multiple lines
* Project that has multiple git repository may not work

## How to install

Use the IDE's plugin manager to install the latest version of the plugin.

## License

```
Copyright 2018 Yoshinori Isogai

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
