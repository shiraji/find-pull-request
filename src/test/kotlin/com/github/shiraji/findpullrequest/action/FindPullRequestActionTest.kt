package com.github.shiraji.findpullrequest.action

import com.github.shiraji.findpullrequest.model.FindPullRequestHostingServices
import com.github.shiraji.findpullrequest.model.getHosting
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.command.impl.DummyProject
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class FindPullRequestActionTest {

    private val action = FindPullRequestAction()

    @Suppress("unused")
    enum class MenuTextTest(
        val config: PropertiesComponent?,
        val useShortName: Boolean,
        val hosting: String,
        val expected: String?,
        private val testName: String,
    ) {
        C1(null, true, "GitHub", null, "Should return null when PropertiesComponent is null"),
        C2(mockk(), true, "GitHub", FindPullRequestHostingServices.GitHub.pullRequestName, "Should return short name if useShortName is true"),
        C3(mockk(), true, "Bitbucket", FindPullRequestHostingServices.Bitbucket.pullRequestName, "Should return short name if useShortName is true for Bitbucket"),
        C4(mockk(), true, "GitLab", FindPullRequestHostingServices.GitLab.pullRequestName, "Should return short name if useShortName is true for GitLab"),
        C5(mockk(), true, "", FindPullRequestHostingServices.GitHub.pullRequestName, "Should return GitHub's short name if hosting is invalid value"),
        C6(mockk(), false, "GitHub", "Go to ${FindPullRequestHostingServices.GitHub.pullRequestName} page", "Should return long name if useShortName is false"),
        C7(mockk(), false, "Bitbucket", "Go to ${FindPullRequestHostingServices.Bitbucket.pullRequestName} page", "Should return short name if useShortName is false for Bitbucket"),
        C8(mockk(), false, "GitLab", "Go to ${FindPullRequestHostingServices.GitLab.pullRequestName} page", "Should return short name if useShortName is false for GitLab"),
        C9(mockk(), false, "", "Go to ${FindPullRequestHostingServices.GitHub.pullRequestName} page", "Should return GitHub's short name if hosting is invalid value"),

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
                assertNull(result)
            } else {
                assert(result == test.expected)
            }
        }
    }
}