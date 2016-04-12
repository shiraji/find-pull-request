package com.github.shiraji.findpullrequest.helper

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications

class FindPullRequestHelper {
    companion object {
        fun showInfoNotification(message: String)
                = Notifications.Bus.notify(Notification("FindPullRequest", "Find Pull Request", message, NotificationType.INFORMATION))

        fun showErrorNotification(message: String)
                = Notifications.Bus.notify(Notification("FindPullRequest", "Find Pull Request", message, NotificationType.ERROR))
    }
}