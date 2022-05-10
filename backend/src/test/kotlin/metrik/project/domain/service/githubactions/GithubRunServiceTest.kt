package metrik.project.domain.service.githubactions

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import metrik.infrastructure.utlils.toTimestamp
import metrik.project.domain.model.PipelineConfiguration
import metrik.project.infrastructure.github.feign.GithubFeignClient
import metrik.project.infrastructure.github.feign.response.MultipleRunResponse
import metrik.project.infrastructure.github.feign.response.SingleRunResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.ZoneId
import java.time.ZonedDateTime

@ExtendWith(MockKExtension::class)
internal class GithubRunServiceTest {
    @InjectMockKs
    private lateinit var githubRunService: GithubRunService

    @MockK
    private lateinit var githubFeignClient: GithubFeignClient

    private val testPipeline =
        PipelineConfiguration(id = "test pipeline", credential = "fake token", url = "https://test.com/test/test")

    @Test
    internal fun `should keep syncing runs until meet timestamp limitation`() {
        every { githubFeignClient.retrieveMultipleRuns(any(), any(), any(), any(), 2, 1) } returns MultipleRunResponse(
            listOf(
                SingleRunResponse(createdAt = ZonedDateTime.of(2021, 1, 10, 0, 0, 0, 0, ZoneId.systemDefault())),
                SingleRunResponse(createdAt = ZonedDateTime.of(2021, 1, 9, 0, 0, 0, 0, ZoneId.systemDefault())),
            )
        )
        every { githubFeignClient.retrieveMultipleRuns(any(), any(), any(), any(), 2, 2) } returns MultipleRunResponse(
            listOf(
                SingleRunResponse(createdAt = ZonedDateTime.of(2021, 1, 8, 0, 0, 0, 0, ZoneId.systemDefault())),
                SingleRunResponse(createdAt = ZonedDateTime.of(2021, 1, 7, 0, 0, 0, 0, ZoneId.systemDefault())),
            )
        )
        every { githubFeignClient.retrieveMultipleRuns(any(), any(), any(), any(), 2, 3) } returns MultipleRunResponse(
            listOf(
                SingleRunResponse(createdAt = ZonedDateTime.of(2021, 1, 6, 0, 0, 0, 0, ZoneId.systemDefault())),
            )
        )

        val syncedRuns = githubRunService.syncRunsByPage(
            testPipeline,
            ZonedDateTime.of(2021, 1, 7, 0, 0, 0, 0, ZoneId.systemDefault()).toTimestamp(),
            2
        )

        assertThat(syncedRuns.size).isEqualTo(3)
        verify(exactly = 1) { githubFeignClient.retrieveMultipleRuns(any(), any(), any(), any(), 2, 1) }
        verify(exactly = 1) { githubFeignClient.retrieveMultipleRuns(any(), any(), any(), any(), 2, 2) }
        verify(exactly = 0) { githubFeignClient.retrieveMultipleRuns(any(), any(), any(), any(), 2, 3) }
    }

    @Test
    internal fun `should sync single run`() {
        every { githubFeignClient.retrieveSingleRun(any(), any(), any(), any(), any()) } returns SingleRunResponse(id = 1)

        val run = githubRunService.syncSingleRun(testPipeline, "https://test.com/123")

        assertThat(run!!.id).isEqualTo(1)
    }
}
