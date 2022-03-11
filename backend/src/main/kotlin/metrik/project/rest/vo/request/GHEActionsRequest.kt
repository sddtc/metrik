package metrik.project.rest.vo.request

import metrik.project.domain.model.PipelineConfiguration
import metrik.project.domain.model.PipelineType
import java.net.URL
import javax.validation.constraints.NotBlank

class GHEActionsPipelineRequest(
    @field:NotBlank(message = "GHE base url cannot be empty") val ghe: String,
    @field:NotBlank(message = "Name cannot be empty") val name: String,
    @field:NotBlank(message = "Credential cannot be empty") val credential: String,
    url: String
) : PipelineRequest(url, PipelineType.GITHUB_ENTERPRISE_ACTIONS.toString()) {
    override fun toPipeline(projectId: String, pipelineId: String) = PipelineConfiguration(
        id = pipelineId,
        projectId = projectId,
        name = name,
        username = null,
        baseUrl = ghe,
        credential = credential,
        url = toGithubActionsUrl(ghe, url),
        type = PipelineType.valueOf(type)
    )

    private fun toGithubActionsUrl(ghe: String, url: String) =
        URL(url).path.split("/").let { "$ghe/${it[it.size - ownerIndex]}/${it.last()}" }

    private companion object {
        const val ownerIndex = 2
    }
}

class GHEActionsVerificationRequest(
    @field:NotBlank(message = "GHE base url cannot be empty") val ghe: String,
    @field:NotBlank(message = "Credential cannot be null or empty") val credential: String,
    url: String
) : PipelineVerificationRequest(url, PipelineType.GITHUB_ENTERPRISE_ACTIONS.toString()) {
    override fun toPipeline() = PipelineConfiguration(
        credential = credential,
        baseUrl = ghe,
        url = url,
        type = PipelineType.valueOf(type)
    )
}
