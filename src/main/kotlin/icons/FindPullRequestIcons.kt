package icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

interface FindPullRequestIcons {
    companion object {
        val gitLabIcon: Icon = IconLoader.getIcon("/icons/gitlab.svg")
        val bitbucketIcon: Icon = IconLoader.getIcon("/icons/bitbucket.svg")
    }
}