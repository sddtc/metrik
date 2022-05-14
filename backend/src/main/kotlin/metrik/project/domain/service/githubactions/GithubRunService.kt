package metrik.project.domain.service.githubactions

import metrik.infrastructure.utlils.toTimestamp
import metrik.project.domain.model.PipelineConfiguration
import metrik.project.infrastructure.github.feign.GithubFeignClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URL
import java.net.URI

@Service
class GithubRunService(
    private val githubFeignClient: GithubFeignClient,
) {
    private var logger = LoggerFactory.getLogger(javaClass.name)
    private val githubBaseUrl = "https://api.github.com/repos"

    fun syncRunsByPage(
        pipeline: PipelineConfiguration,
        latestTimestamp: Long,
        maxPerPage: Int = defaultMaxPerPage
    ): MutableList<GithubActionsRun> {
        val (owner, repo) = getOwnerRepoFromUrl(pipeline.url)

        val syncedRuns = mutableListOf<GithubActionsRun>()
        var keepRetrieving = true
        var pageIndex = 1
        while (keepRetrieving) {
            if(pageIndex>10) {
                break
            }
            logger.info(
                "Get Github Runs - " +
                    "Sending request to Github Feign Client with url: ${pipeline.url}, pageIndex: $pageIndex"
            )
            val syncedRunsFromCurrentPage = githubFeignClient.retrieveMultipleRuns(
                URI(pipeline.baseUrl ?: githubBaseUrl),
                pipeline.credential,
                owner,
                repo,
                maxPerPage,
                pageIndex
            )?.let { response -> response.workflowRuns.map { it.toGithubActionsRun() } }
                ?.filter { it.createdTimestamp.toTimestamp() > latestTimestamp } ?: listOf()

            keepRetrieving = syncedRunsFromCurrentPage.size == maxPerPage

            syncedRuns.addAll(syncedRunsFromCurrentPage)

            pageIndex++
        }
        return syncedRuns
    }

    fun syncSingleRun(
        pipeline: PipelineConfiguration,
        runUrl: String
    ): GithubActionsRun? {
        val runId = URL(runUrl).path.split("/").last()
        val (owner, repo) = getOwnerRepoFromUrl(pipeline.url)

        logger.info(
            "Get Github Runs - " +
                "Sending request to Github Feign Client with owner: ${pipeline.url}, runId: $runId"
        )

        return githubFeignClient.retrieveSingleRun(URI(pipeline.baseUrl ?: githubBaseUrl), pipeline.credential, owner, repo, runId)?.toGithubActionsRun()
    }

    private fun getOwnerRepoFromUrl(url: String): Pair<String, String> {
        val components = URL(url).path.split("/")
        val owner = components[components.size - ownerIndex]
        val repo = components.last()
        return Pair(owner, repo)
    }

    private fun getToken(token: String) = "$tokenPrefix $token"

    private companion object {
        const val ownerIndex = 2
        const val tokenPrefix = "Bearer"
        const val defaultMaxPerPage = 100
    }
}
