package com.github.shiraji.findpullrequest.helper

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications

fun showInfoNotification(message: String)
        = Notifications.Bus.notify(Notification("FindPullRequest", "Find Pull Request", message, NotificationType.INFORMATION, NotificationListener.URL_OPENING_LISTENER))

fun showErrorNotification(message: String)
        = Notifications.Bus.notify(Notification("FindPullRequest", "Find Pull Request", message, NotificationType.ERROR))
