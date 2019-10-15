package com.github.shiraji.findpullrequest.helper

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

fun showInfoNotification(message: String) =
        Notifications.Bus.notify(Notification("FindPullRequest.Info", "Find Pull Request", message, NotificationType.INFORMATION, NotificationListener.URL_OPENING_LISTENER))

fun showErrorNotification(message: String) =
        Notifications.Bus.notify(Notification("FindPullRequest.Error", "Find Pull Request", message, NotificationType.ERROR))

val Project.root: VirtualFile?
        get() {
                val basePath = basePath ?: return null
                return LocalFileSystem.getInstance().findFileByPath(basePath) ?: return null
        }