package com.github.shiraji.findpullrequest.service

import com.github.shiraji.findpullrequest.domain.HostingService
import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
import com.github.shiraji.findpullrequest.exceptions.NoRevisionFoundException
import com.github.shiraji.findpullrequest.exceptions.UnsupportedHostingServiceException
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import git4idea.GitCommit
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class FindPrServiceTest {
    private val config: PropertiesComponent = mockk()
    private val gitHistoryService: GitHistoryService = mockk()
    private val service = FindPrInfoService(config, gitHistoryService)
    private val revisionNumber: VcsRevisionNumber = mockk()
    private val mergeCommit: GitCommit? = mockk()
    private val squashCommit: GitCommit? = mockk()
    private val hostingService: HostingService = mockk()
    private val otherHostingService: HostingService = mockk()

    @Nested
    inner class findPr {

        @AfterEach
        fun tearDown() {
            unmockkObject(HostingService)
        }

        @Nested
        inner class mergeCommit {
            @BeforeEach
            fun setUp() {
                every { gitHistoryService.isRevisionMergedAtMergeCommit(any(), any()) } returns true
                every { mergeCommit?.fullMessage } returns "full message"
            }

            @Test
            fun `Should find PrInfo if PR number found from merge commit`() {
                every { hostingService.getPrNumberFromMergeCommit(any()) } returns 1

                val result = service.findPrInfo(revisionNumber, mergeCommit, hostingService)

                assertEquals(result.hostingService, hostingService)
                assertEquals(result.prNumber, 1)
            }

            @Test
            fun `Should find PrInfo if PR number is null and other hosting service found PR number`() {
                every { hostingService.getPrNumberFromMergeCommit(any()) } returns null
                mockkObject(HostingService)
                every { HostingService.findFromMergeCommitMessage(any()) } returns otherHostingService
                every { otherHostingService.getPrNumberFromMergeCommit(any()) } returns 2

                val result = service.findPrInfo(revisionNumber, mergeCommit, hostingService)

                assertEquals(result.hostingService, otherHostingService)
                assertEquals(result.prNumber, 2)
            }

            @Test
            fun `Should throw Exception if PR number is null and no other hosting service found`() {
                every { hostingService.getPrNumberFromMergeCommit(any()) } returns null
                mockkObject(HostingService)
                every { HostingService.findFromMergeCommitMessage(any()) } returns null

                assertThrows<UnsupportedHostingServiceException> {
                    service.findPrInfo(
                        revisionNumber,
                        mergeCommit,
                        hostingService
                    )
                }
            }

            @Test
            fun `Should throw Exception if PR number is null and other hosting does not return pr number`() {
                every { hostingService.getPrNumberFromMergeCommit(any()) } returns null
                mockkObject(HostingService)
                every { HostingService.findFromMergeCommitMessage(any()) } returns otherHostingService
                every { otherHostingService.getPrNumberFromMergeCommit(any()) } returns null

                assertThrows<NoPullRequestFoundException> {
                    service.findPrInfo(
                        revisionNumber,
                        mergeCommit,
                        hostingService
                    )
                }
            }
        }

        @Nested
        inner class squashCommit {
            @BeforeEach
            fun setUp() {
                every { gitHistoryService.isRevisionMergedAtMergeCommit(any(), any()) } returns false
                every { squashCommit?.fullMessage } returns "full message"
            }

            @Test
            fun `Should find PrInfo if PR number found from merge commit`() {
                every { gitHistoryService.toGitCommit(revisionNumber) } returns squashCommit
                every { hostingService.getPrNumberFromSquashCommit(any()) } returns 1

                val result = service.findPrInfo(revisionNumber, mergeCommit, hostingService)

                assertEquals(result.hostingService, hostingService)
                assertEquals(result.prNumber, 1)
            }

            @Test
            fun `Should find PrInfo if PR number is null and other hosting service found PR number`() {
                every { gitHistoryService.toGitCommit(revisionNumber) } returns squashCommit
                every { hostingService.getPrNumberFromSquashCommit(any()) } returns null
                mockkObject(HostingService)
                every { HostingService.findFromSquashCommitMessage(any()) } returns otherHostingService
                every { otherHostingService.getPrNumberFromSquashCommit(any()) } returns 2

                val result = service.findPrInfo(revisionNumber, mergeCommit, hostingService)

                assertEquals(result.hostingService, otherHostingService)
                assertEquals(result.prNumber, 2)
            }

            @Test
            fun `Should throw Exception if no squash commit found`() {
                every { gitHistoryService.toGitCommit(revisionNumber) } returns null

                assertThrows<NoRevisionFoundException> {
                    service.findPrInfo(
                        revisionNumber,
                        mergeCommit,
                        hostingService
                    )
                }
            }

            @Test
            fun `Should throw Exception if PR number is null and no other hosting service found`() {
                every { gitHistoryService.toGitCommit(revisionNumber) } returns squashCommit
                every { hostingService.getPrNumberFromSquashCommit(any()) } returns null
                mockkObject(HostingService)
                every { HostingService.findFromSquashCommitMessage(any()) } returns null

                assertThrows<UnsupportedHostingServiceException> {
                    service.findPrInfo(
                        revisionNumber,
                        mergeCommit,
                        hostingService
                    )
                }
            }

            @Test
            fun `Should throw Exception if PR number is null and other hosting does not return pr number`() {
                every { gitHistoryService.toGitCommit(revisionNumber) } returns squashCommit
                every { hostingService.getPrNumberFromSquashCommit(any()) } returns null
                mockkObject(HostingService)
                every { HostingService.findFromSquashCommitMessage(any()) } returns otherHostingService
                every { otherHostingService.getPrNumberFromSquashCommit(any()) } returns null

                assertThrows<NoPullRequestFoundException> {
                    service.findPrInfo(
                        revisionNumber,
                        mergeCommit,
                        hostingService
                    )
                }
            }
        }
    }
}