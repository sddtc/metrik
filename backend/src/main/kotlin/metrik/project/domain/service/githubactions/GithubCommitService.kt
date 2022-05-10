package metrik.project.domain.service.githubactions

import feign.FeignException
import feign.codec.DecodeException
import metrik.infrastructure.utlils.toLocalDateTime
import metrik.infrastructure.utlils.toTimestamp
import metrik.project.domain.model.Commit
import metrik.project.domain.model.PipelineConfiguration
import metrik.project.infrastructure.github.feign.GithubFeignClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URL
import java.net.URI

@Service
class GithubCommitService(
    private val githubFeignClient: GithubFeignClient,
) {
    private var logger = LoggerFactory.getLogger(javaClass.name)
    private val githubBaseUrl = "https://api.github.com/repos"
    private val defaultMaxPerPage = 100

    fun getCommitsBetweenTimePeriod(
        startTimeStamp: Long,
        endTimeStamp: Long,
        branch: String? = null,
        pipeline: PipelineConfiguration
    ): List<Commit> {
        logger.info("Started sync for Github Actions commits [${pipeline.url}]/[$branch]")

        var keepRetrieving = true
        var pageIndex = 1
        val allCommits = mutableSetOf<GithubCommit>()
        while (keepRetrieving) {
            val commitsFromGithub =
                retrieveCommits(pipeline.credential, pipeline.baseUrl, pipeline.url, startTimeStamp, endTimeStamp, branch, pageIndex)

            allCommits.addAll(commitsFromGithub)

            keepRetrieving = commitsFromGithub.isNotEmpty()
            pageIndex++
        }

        return allCommits.map {
            Commit(
                commitId = it.id,
                timestamp = it.timestamp.toTimestamp(),
                date = it.timestamp.toString(),
                pipelineId = pipeline.id
            )
        }
    }

    private fun retrieveCommits(
        credential: String,
        baseUrl: String? = null,
        url: String,
        startTimeStamp: Long,
        endTimeStamp: Long,
        branch: String? = null,
        pageIndex: Int? = null
    ): List<GithubCommit> {
        logger.info(
            "Get Github Commits - " +
                    "Sending request to Github Feign Client with owner: $url, " +
                    "since: ${startTimeStamp.toLocalDateTime()}, until: ${endTimeStamp.toLocalDateTime()}, " +
                    "branch: $branch, pageIndex: $pageIndex"
        )
        val commits = with(githubFeignClient) {
            getOwnerRepoFromUrl(url).let { (owner, repo) ->
                try {
                    retrieveCommits(
                        URI(baseUrl ?: githubBaseUrl),
                        credential,
                        owner,
                        repo,
                        if (startTimeStamp == 0L) null else startTimeStamp.toString(),
                        endTimeStamp.toString(),
                        branch,
                        defaultMaxPerPage,
                        pageIndex
                    )
                } catch (ex: DecodeException) {
                    println(ex.status())
                    if (ex.status() == 404) null else throw ex
                }
            }
        }
        return commits?.map { it.toGithubCommit() } ?: listOf()
    }

    private fun getOwnerRepoFromUrl(url: String): Pair<String, String> {
        val ownerIndex = 2
        val components = URL(url).path.split("/")
        val owner = components[components.size - ownerIndex]
        val repo = components.last()
        return Pair(owner, repo)
    }
}
