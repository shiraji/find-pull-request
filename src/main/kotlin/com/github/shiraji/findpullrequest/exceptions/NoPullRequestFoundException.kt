package com.github.shiraji.findpullrequest.exceptions

class NoPullRequestFoundException(val detailMessage: String) : Exception(detailMessage)