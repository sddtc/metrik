package metrik.project.domain.service.githubactions

import feign.FeignException
import feign.Request
import feign.Response
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import metrik.exception.ApplicationException
import metrik.infrastructure.utlils.toTimestamp
import metrik.project.branch
import metrik.project.executions
import metrik.project.commit
import metrik.project.credential
import metrik.project.currentTimeStamp
import metrik.project.domain.model.Commit
import metrik.project.domain.model.PipelineConfiguration
import metrik.project.domain.model.Status
import metrik.project.domain.repository.BuildRepository
import metrik.project.githubActionsExecution
import metrik.project.githubActionsPipeline
import metrik.project.githubActionsRun
import metrik.project.infrastructure.github.feign.GithubFeignClient
import metrik.project.mockEmitCb
import metrik.project.pipelineId
import metrik.project.previousTimeStamp
import metrik.project.userInputURL
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.ZonedDateTime

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "LargeClass")
@ExtendWith(MockKExtension::class)
internal class GithubActionsPipelineServiceTest {
    @MockK(relaxed = true)
    private lateinit var buildRepository: BuildRepository

    @InjectMockKs
    private lateinit var githubActionsPipelineService: GithubActionsPipelineService

    @MockK(relaxed = true)
    private lateinit var githubCommitService: GithubCommitService

    @MockK(relaxed = true)
    private lateinit var githubRunService: GithubRunService

    @MockK(relaxed = true)
    private lateinit var githubFeignClient: GithubFeignClient

    @SpyK
    private var githubExecutionConverter: GithubExecutionConverter = GithubExecutionConverter()

    private val dummyFeignRequest = Request.create(Request.HttpMethod.POST, "url", mapOf(), null, null, null)

    @Test
    fun `should successfully verify a pipeline given response is 200`() {
        githubActionsPipelineService.verifyPipelineConfiguration(githubActionsPipeline)

        verify {
            githubFeignClient.retrieveMultipleRuns(any(), any(), any(), any())
        }
    }

    @Test
    fun `should throw exception when verify pipeline given response is 500`() {
        every {
            githubFeignClient.retrieveMultipleRuns(any(), any(), any(), any())
        } throws FeignException.errorStatus(
            "GET",
            buildFeignResponse(500)
        )

        assertThrows(ApplicationException::class.java) {
            githubActionsPipelineService.verifyPipelineConfiguration(
                PipelineConfiguration(credential = credential, url = userInputURL)
            )
        }
    }

    @Test
    fun `should throw exception when verify pipeline given response is 404`() {
        every {
            githubFeignClient.retrieveMultipleRuns(any(), any(), any(), any())
        } throws FeignException.errorStatus(
            "GET",
            buildFeignResponse(404)
        )

        assertThrows(ApplicationException::class.java) {
            githubActionsPipelineService.verifyPipelineConfiguration(
                PipelineConfiguration(credential = credential, url = userInputURL)
            )
        }
    }

    @Test
    fun `should return sorted stage name lists when getStagesSortedByName() called`() {
        every { buildRepository.getAllBuilds(pipelineId) } returns (executions)

        val result = githubActionsPipelineService.getStagesSortedByName(pipelineId)

        assertEquals(5, result.size)
        assertEquals("amazing", result[0])
        assertEquals("build", result[1])
        assertEquals("clone", result[2])
        assertEquals("good", result[3])
        assertEquals("zzz", result[4])
    }

    @Test
    fun `should map one run to its commits`() {
        val build = githubActionsExecution.copy(changeSets = listOf(commit.copy(timestamp = previousTimeStamp)))

        every {
            buildRepository.getPreviousBuild(
                pipelineId,
                currentTimeStamp,
                branch
            )
        } returns build

        every {
            githubCommitService.getCommitsBetweenTimePeriod(
                ZonedDateTime.parse("2021-04-23T13:41:01.779Z")!!.toTimestamp(),
                ZonedDateTime.parse("2021-08-17T12:23:25Z")!!.toTimestamp(),
                branch = branch,
                pipeline = githubActionsPipeline
            )
        } returns listOf(commit)

        val map = mapOf(branch to mapOf(githubActionsRun to listOf(commit)))

        assertEquals(
            map,
            githubActionsPipelineService.mapCommitToRun(
                githubActionsPipeline,
                mutableListOf(githubActionsRun)
            )
        )
    }

    @Test
    fun `should map one run to its commit given no previous build and commits are in correct time range`() {
        every {
            buildRepository.getPreviousBuild(
                pipelineId,
                currentTimeStamp,
                branch
            )
        } returns null

        every {
            githubCommitService.getCommitsBetweenTimePeriod(
                startTimeStamp = 0,
                ZonedDateTime.parse("2021-08-17T12:23:25Z")!!.toTimestamp(),
                branch = branch,
                pipeline = githubActionsPipeline
            )
        } returns listOf(commit)

        val map = mapOf(branch to mapOf(githubActionsRun to listOf(commit)))

        assertEquals(
            map,
            githubActionsPipelineService.mapCommitToRun(
                githubActionsPipeline,
                mutableListOf(githubActionsRun)
            )
        )
    }

    @Test
    fun `should map one run to its commit given no previous build and commits are not in correct time range`() {
        every {
            buildRepository.getPreviousBuild(
                pipelineId,
                currentTimeStamp,
                branch
            )
        } returns null

        every {
            githubCommitService.getCommitsBetweenTimePeriod(
                0,
                ZonedDateTime.parse("2021-08-17T12:23:25Z")!!.toTimestamp(),
                branch = branch,
                pipeline = githubActionsPipeline
            )
        } returns listOf(commit.copy(timestamp = 1629856700010))

        val map = mapOf(branch to mapOf(githubActionsRun to emptyList<Commit>()))

        assertEquals(
            map,
            githubActionsPipelineService.mapCommitToRun(
                githubActionsPipeline,
                mutableListOf(githubActionsRun)
            )
        )
    }

    @Test
    fun `should map multiple runs to their commits`() {
        val buildFirst = githubActionsExecution.copy(changeSets = listOf(commit.copy(timestamp = previousTimeStamp)))
        val buildSecond = githubActionsExecution.copy(changeSets = listOf(commit.copy(timestamp = 1619098860779)))
        val buildThird = githubActionsExecution.copy(changeSets = listOf(commit.copy(timestamp = 1618926060779)))

        val githubActionsRunSecond = githubActionsRun.copy(
            commitTimeStamp = ZonedDateTime.parse("2021-04-23T13:41:00.779Z")
        )

        val githubActionsRunThird = githubActionsRun.copy(
            commitTimeStamp = ZonedDateTime.parse("2021-04-22T13:41:00.779Z")
        )

        every {
            buildRepository.getPreviousBuild(
                pipelineId,
                any(),
                branch
            )
        } returns buildThird andThen buildFirst andThen buildSecond andThen buildThird

        val commit1 = commit.copy(timestamp = 1629203005000)
        val commit2 = commit.copy(timestamp = 1619185260779)
        val commit3 = commit.copy(timestamp = 1619098860779)

        every {
            githubCommitService.getCommitsBetweenTimePeriod(
                ZonedDateTime.parse("2021-04-20T13:41:01.779Z")!!.toTimestamp(),
                ZonedDateTime.parse("2021-08-17T12:23:25Z")!!.toTimestamp(),
                branch = branch,
                pipeline = githubActionsPipeline
            )
        } returns listOf(commit1, commit2, commit3)

        val map = mapOf(
            branch to mapOf(
                githubActionsRun to listOf(commit1),
                githubActionsRunSecond to listOf(commit2),
                githubActionsRunThird to listOf(commit3)
            )
        )

        assertEquals(
            map,
            githubActionsPipelineService.mapCommitToRun(
                githubActionsPipeline,
                mutableListOf(githubActionsRun, githubActionsRunSecond, githubActionsRunThird)
            ),
        )
    }

    @Test
    fun `should map first run given previous run does not have commits`() {
        val build = githubActionsExecution.copy(changeSets = emptyList())

        every {
            buildRepository.getPreviousBuild(
                pipelineId,
                any(),
                branch
            )
        } returns build

        every {
            githubCommitService.getCommitsBetweenTimePeriod(
                startTimeStamp = 0,
                endTimeStamp = ZonedDateTime.parse("2021-08-17T12:23:25Z")!!.toTimestamp(),
                branch = branch,
                pipeline = githubActionsPipeline
            )
        } returns listOf(commit)

        val map = mapOf(
            branch to mapOf(
                githubActionsRun to listOf(commit),
            )
        )

        assertEquals(
            map,
            githubActionsPipelineService.mapCommitToRun(
                githubActionsPipeline,
                mutableListOf(githubActionsRun)
            )
        )
    }

    @Test
    fun `should sync and save all in-progress builds to databases`() {
        every { buildRepository.getLatestBuild(pipelineId) } returns null
        every { buildRepository.getInProgressBuilds(pipelineId) } returns emptyList()
        every {
            buildRepository.getPreviousBuild(
                pipelineId,
                any(),
                branch
            )
        } returns githubActionsExecution
        every {
            githubCommitService.getCommitsBetweenTimePeriod(
                any(),
                any(),
                branch = branch,
                pipeline = githubActionsPipeline
            )
        } returns listOf(commit)
        every {
            githubRunService.syncRunsByPage(any(), any(), any())
        } returns mutableListOf(
            GithubActionsRun(
                id = 1111111111,
                name = "CI",
                branch = "master",
                status = "in_progress",
                conclusion = null,
                url = "http://localhost:80/test_project/test_repo/actions/runs/1111111111",
                commitTimeStamp = ZonedDateTime.parse("2021-08-11T01:46:31Z"),
                createdTimestamp = ZonedDateTime.parse("2021-08-11T11:11:01Z"),
                updatedTimestamp = ZonedDateTime.parse("2021-08-11T11:11:17Z")
            ),
            GithubActionsRun(
                id = 1111111112,
                name = "CI",
                branch = "master",
                status = "queued",
                conclusion = null,
                url = "http://localhost:80/test_project/test_repo/actions/runs/1111111111",
                commitTimeStamp = ZonedDateTime.parse("2021-08-11T01:46:31Z"),
                createdTimestamp = ZonedDateTime.parse("2021-08-11T11:11:01Z"),
                updatedTimestamp = ZonedDateTime.parse("2021-08-11T11:11:17Z")
            )
        )

        githubActionsPipelineService.syncBuildsProgressively(githubActionsPipeline, mockEmitCb)

        verify {
            buildRepository.save(
                githubActionsExecution.copy(
                    result = Status.IN_PROGRESS,
                    stages = emptyList(),
                    changeSets = emptyList()
                )
            )
            buildRepository.save(
                githubActionsExecution.copy(
                    result = Status.IN_PROGRESS,
                    stages = emptyList(),
                    number = 1111111112,
                    changeSets = emptyList()
                )
            )
        }
    }

    @Test
    fun `should sync and update all previous in-progress builds`() {
        val build = githubActionsExecution.copy(result = Status.IN_PROGRESS, stages = emptyList())
        every { buildRepository.getLatestBuild(pipelineId) } returns (build)
        every { buildRepository.getInProgressBuilds(pipelineId) } returns (listOf(build))
        every {
            buildRepository.getPreviousBuild(
                pipelineId,
                any(),
                branch
            )
        } returns githubActionsExecution
        every {
            githubCommitService.getCommitsBetweenTimePeriod(
                any(),
                any(),
                branch = branch,
                pipeline = githubActionsPipeline
            )
        } returns listOf(commit)
        every {
            githubRunService.syncRunsByPage(any(), any(), any())
        } returns mutableListOf()
        every {
            githubRunService.syncSingleRun(any(), any())
        } returns GithubActionsRun(
            id = 1111111111,
            name = "CI",
            branch = "master",
            status = "completed",
            conclusion = "success",
            url = "http://localhost:80/test_project/test_repo/actions/runs/1111111111",
            commitTimeStamp = ZonedDateTime.parse("2021-08-11T01:46:31Z"),
            createdTimestamp = ZonedDateTime.parse("2021-08-11T11:11:01Z"),
            updatedTimestamp = ZonedDateTime.parse("2021-08-11T11:11:17Z")
        )

        githubActionsPipelineService.syncBuildsProgressively(githubActionsPipeline, mockEmitCb)

        verify {
            buildRepository.save(githubActionsExecution.copy(changeSets = emptyList()))
        }
    }

    //    @Test
//    fun `should sync new builds, update all previous in-progress builds and emit the progress event`() {
//        val build = githubActionsBuild.copy(result = Status.IN_PROGRESS, stages = emptyList())
//        every { buildRepository.getLatestBuild(pipelineId) } returns build
//        every { buildRepository.getInProgressBuilds(pipelineId) } returns listOf(build)
//        every {
//            buildRepository.getPreviousBuild(
//                pipelineId,
//                any(),
//                branch
//            )
//        } returns githubActionsBuild
//        every {
//            githubCommitService.getCommitsBetweenTimePeriod(
//                any(),
//                any(),
//                branch = branch,
//                pipeline = githubActionsPipeline
//            )
//        } returns listOf(commit)
//        every {
//            githubClient.retrieveMultipleRuns(any(), any(), any(), 1)
//        } returns listOf(
//            GithubActionsRun(
//                id = 1111111111,
//                name = "CI",
//                branch = "master",
//                status = "completed",
//                conclusion = "success",
//                url = "http://localhost:80/test_project/test_repo/actions/runs/1111111111",
//                commitTimeStamp = ZonedDateTime.parse("2021-08-11T01:46:31Z"),
//                createdTimestamp = ZonedDateTime.parse("2021-08-11T11:11:01Z"),
//                updatedTimestamp = ZonedDateTime.parse("2021-08-11T11:11:17Z")
//            ),
//            GithubActionsRun(
//                id = 1111111113,
//                name = "CI",
//                branch = "master",
//                status = "completed",
//                conclusion = "failure",
//                url = "http://localhost:80/test_project/test_repo/actions/runs/1111111111",
//                commitTimeStamp = ZonedDateTime.parse("2021-08-11T01:46:31Z"),
//                createdTimestamp = ZonedDateTime.parse("2021-08-12T11:11:01Z"),
//                updatedTimestamp = ZonedDateTime.parse("2021-08-12T11:11:17Z")
//            ),
//            GithubActionsRun(
//                id = 1111111114,
//                name = "CI",
//                branch = "master",
//                status = "completed",
//                conclusion = "success",
//                url = "http://localhost:80/test_project/test_repo/actions/runs/1111111111",
//                commitTimeStamp = ZonedDateTime.parse("2021-08-11T01:46:31Z"),
//                createdTimestamp = ZonedDateTime.parse("2021-08-12T11:11:01Z"),
//                updatedTimestamp = ZonedDateTime.parse("2021-08-12T11:11:17Z")
//            )
//        )
//        every {
//            githubClient.retrieveSingleRun(any(), any(), "1111111111")
//        } returns GithubActionsRun(
//            id = 1111111111,
//            name = "CI",
//            branch = "master",
//            status = "completed",
//            conclusion = "success",
//            url = "http://localhost:80/test_project/test_repo/actions/runs/1111111111",
//            commitTimeStamp = ZonedDateTime.parse("2021-08-11T01:46:31Z"),
//            createdTimestamp = ZonedDateTime.parse("2021-08-11T11:11:01Z"),
//            updatedTimestamp = ZonedDateTime.parse("2021-08-11T11:11:17Z")
//        )
//
//        githubActionsPipelineService.syncBuildsProgressively(githubActionsPipeline, mockEmitCb)
//
//        val progress = SyncProgress(pipelineId, name, 1, 3)
//
//        verify {
//            buildRepository.save(
//                githubActionsBuild.copy(
//                    number = 1111111113,
//                    result = Status.FAILED,
//                    timestamp = 1628766661000,
//                    stages = listOf(
//                        stage.copy(
//                            status = Status.FAILED,
//                            startTimeMillis = 1628766661000,
//                            completedTimeMillis = 1628766677000
//                        )
//                    ),
//                    changeSets = emptyList()
//                )
//            )
//            buildRepository.save(
//                githubActionsBuild.copy(
//                    number = 1111111114,
//                    timestamp = 1628766661000,
//                    stages = listOf(
//                        stage.copy(
//                            startTimeMillis = 1628766661000,
//                            completedTimeMillis = 1628766677000
//                        )
//                    ),
//                    changeSets = emptyList()
//                )
//            )
//            buildRepository.save(githubActionsBuild.copy(changeSets = emptyList()))
//            mockEmitCb.invoke(progress)
//            mockEmitCb.invoke(progress.copy(progress = 2))
//            mockEmitCb.invoke(progress.copy(progress = 3))
//        }
//    }
//
    @Test
    fun `should sync builds given status is completed and conclusion is non-supported types`() {
        every { buildRepository.getLatestBuild(pipelineId) } returns null
        every { buildRepository.getInProgressBuilds(pipelineId) } returns emptyList()
        every {
            buildRepository.getPreviousBuild(
                pipelineId,
                any(),
                branch
            )
        } returns githubActionsExecution
        every {
            githubCommitService.getCommitsBetweenTimePeriod(
                any(),
                any(),
                branch = branch,
                pipeline = githubActionsPipeline
            )
        } returns listOf(commit)
        every {
            githubRunService.syncRunsByPage(any(), any(), any())
        } returns mutableListOf(
            GithubActionsRun(
                id = 1111111111,
                name = "CI",
                branch = "master",
                status = "completed",
                conclusion = "stale",
                url = "http://localhost:80/test_project/test_repo/actions/runs/1111111111",
                commitTimeStamp = ZonedDateTime.parse("2021-08-11T01:46:31Z"),
                createdTimestamp = ZonedDateTime.parse("2021-08-11T11:11:01Z"),
                updatedTimestamp = ZonedDateTime.parse("2021-08-11T11:11:17Z")
            ),
            GithubActionsRun(
                id = 1111111112,
                name = "CI",
                branch = "master",
                status = "completed",
                conclusion = "stale",
                url = "http://localhost:80/test_project/test_repo/actions/runs/1111111111",
                commitTimeStamp = ZonedDateTime.parse("2021-08-11T01:46:31Z"),
                createdTimestamp = ZonedDateTime.parse("2021-08-11T11:11:01Z"),
                updatedTimestamp = ZonedDateTime.parse("2021-08-11T11:11:17Z")
            )
        )

        githubActionsPipelineService.syncBuildsProgressively(githubActionsPipeline, mockEmitCb)

        verify {
            buildRepository.save(
                githubActionsExecution.copy(
                    result = Status.OTHER,
                    stages = emptyList(),
                    changeSets = emptyList()
                )
            )
            buildRepository.save(
                githubActionsExecution.copy(
                    result = Status.OTHER,
                    stages = emptyList(),
                    number = 1111111112,
                    changeSets = emptyList()
                )
            )
        }
    }

//    @Test
//    fun `should ignore not found in-progress builds and continue with next in-progress build`() {
//        val build = githubActionsBuild.copy(result = Status.IN_PROGRESS, stages = emptyList())
//
//        every { buildRepository.getLatestBuild(pipelineId) } returns (null)
//        every { buildRepository.getInProgressBuilds(pipelineId) } returns
//                listOf(
//                    build,
//                    build.copy(
//                        number = 1111111112,
//                        url = "http://localhost:80/test_project/test_repo/actions/runs/1111111112"
//                    )
//                )
//        every {
//            buildRepository.getPreviousBuild(
//                pipelineId,
//                any(),
//                branch
//            )
//        } returns githubActionsBuild
//        every {
//            githubCommitService.getCommitsBetweenTimePeriod(
//                any(),
//                any(),
//                branch = branch,
//                pipeline = githubActionsPipeline
//            )
//        } returns listOf(commit)
//        every {
//            githubRunService.syncRunsByPage(any(), any(), any())
//        } returns mutableListOf()
//        every {
//            githubRunService.syncSingleRun(any(), "https://test.com/1111111111")
//        } returns null
//        every {
//            githubRunService.syncSingleRun(any(), "https://test.com/1111111112")
//        } returns GithubActionsRun(
//            id = 1111111112,
//            name = "CI",
//            branch = "master",
//            status = "completed",
//            conclusion = "success",
//            url = "http://localhost:80/test_project/test_repo/actions/runs/1111111112",
//            commitTimeStamp = ZonedDateTime.parse("2021-08-11T01:46:31Z"),
//            createdTimestamp = ZonedDateTime.parse("2021-08-11T11:11:01Z"),
//            updatedTimestamp = ZonedDateTime.parse("2021-08-11T11:11:17Z")
//        )
//
//        githubActionsPipelineService.syncBuildsProgressively(githubActionsPipeline, mockEmitCb)
//
//        verify(exactly = 1) {
//            buildRepository.save(
//                githubActionsBuild.copy(
//                    number = 1111111112,
//                    url = "http://localhost:80/test_project/test_repo/actions/runs/1111111112",
//                    changeSets = emptyList()
//                )
//            )
//        }
//    }

    private fun buildFeignResponse(statusCode: Int) =
        Response.builder().status(statusCode).request(dummyFeignRequest).build()
}
