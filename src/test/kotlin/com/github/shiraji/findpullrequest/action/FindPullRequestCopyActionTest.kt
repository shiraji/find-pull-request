package com.github.shiraji.findpullrequest.action

import com.github.shiraji.findpullrequest.model.FindPullRequestHostingServices
import com.github.shiraji.findpullrequest.model.getHosting
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.command.impl.DummyProject
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class FindPullRequestCopyActionTest {
    private val action = FindPullRequestCopyAction()

    @Suppress("unused")
    enum class MenuTextTest(
        val config: PropertiesComponent?,
        val useShortName: Boolean,
        val hosting: String,
        val expected: String?,
        private val testName: String,
    ) {
        C1(null, true, "GitHub", null, "Should return null when PropertiesComponent is null"),
        C2(mockk(), true, "GitHub", "Copy Link to ${FindPullRequestHostingServices.GitHub.pullRequestName}", "Should return short name if useShortName is true"),
        C3(mockk(), true, "Bitbucket", "Copy Link to ${FindPullRequestHostingServices.Bitbucket.pullRequestName}", "Should return short name if useShortName is true for Bitbucket"),
        C4(mockk(), true, "GitLab", "Copy Link to ${FindPullRequestHostingServices.GitLab.pullRequestName}", "Should return short name if useShortName is true for GitLab"),
        C5(mockk(), true, "", "Copy Link to ${FindPullRequestHostingServices.GitHub.pullRequestName}", "Should return GitHub's short name if hosting is invalid value"),
        C6(mockk(), false, "GitHub", "Copy Link to ${FindPullRequestHostingServices.GitHub.pullRequestName} URL", "Should return long name if useShortName is false"),
        C7(mockk(), false, "Bitbucket", "Copy Link to ${FindPullRequestHostingServices.Bitbucket.pullRequestName} URL", "Should return short name if useShortName is false for Bitbucket"),
        C8(mockk(), false, "GitLab", "Copy Link to ${FindPullRequestHostingServices.GitLab.pullRequestName} URL", "Should return short name if useShortName is false for GitLab"),
        C9(mockk(), false, "", "Copy Link to ${FindPullRequestHostingServices.GitHub.pullRequestName} URL", "Should return GitHub's short name if hosting is invalid value"),

        ;

        override fun toString(): String {
            return testName
        }
    }

    @Nested
    inner class menuText {

        @BeforeEach
        fun setUp() {
            mockkStatic(PropertiesComponent::class)
        }

        @ParameterizedTest
        @EnumSource(MenuTextTest::class)
        fun `Test menuText`(test: MenuTextTest) {
            every { PropertiesComponent.getInstance(any()) } returns test.config
            if (test.config != null) {
                every { test.config.getHosting() } returns test.hosting
            }
            val result = action.menuText(DummyProject.getInstance(), test.useShortName)
            if (test.expected == null) {
                Assertions.assertNull(result)
            } else {
                assert(result == test.expected)
            }
        }
    }
}