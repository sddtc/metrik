package fourkeymetrics.dashboard.controller

import fourkeymetrics.dashboard.controller.applicationservice.DashboardApplicationService
import fourkeymetrics.dashboard.controller.vo.DashboardDetailVo
import fourkeymetrics.dashboard.controller.vo.DashboardRequest
import fourkeymetrics.dashboard.controller.vo.DashboardVo
import fourkeymetrics.dashboard.controller.vo.PipelineStagesResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class DashboardController {
    @Autowired
    private lateinit var dashboardApplicationService: DashboardApplicationService


    @GetMapping("/dashboard")
    @ResponseStatus(HttpStatus.OK)
    fun getDashboards(): List<DashboardVo> {
        return dashboardApplicationService.getDashboards()
    }

    @GetMapping("/dashboard/{dashboardId}")
    @ResponseStatus(HttpStatus.OK)
    fun getDashboard(@PathVariable dashboardId: String): DashboardVo {
        return dashboardApplicationService.getDashboard(dashboardId)
    }

    @PostMapping("/dashboard")
    fun createDashboardAndPipeline(@RequestBody dashboardRequest: DashboardRequest): DashboardDetailVo {
        return dashboardApplicationService.createDashboardAndPipeline(dashboardRequest)
    }

    @PutMapping("/dashboard/{dashboardId}")
    @ResponseStatus(HttpStatus.OK)
    fun updateDashboardName(@PathVariable dashboardId: String, @RequestBody dashboardName: String): DashboardVo {
        return dashboardApplicationService.updateDashboardName(dashboardId, dashboardName)
    }

    @DeleteMapping("/dashboard/{dashboardId}")
    @ResponseStatus(HttpStatus.OK)
    fun deleteDashboard(@PathVariable dashboardId: String) {
        return dashboardApplicationService.deleteDashboard(dashboardId)
    }

    @GetMapping("/dashboard/{dashboardId}/pipelines-stages")
    fun getPipelineStages(@PathVariable("dashboardId") dashboardId: String): List<PipelineStagesResponse> {
        return dashboardApplicationService.getPipelinesStages(dashboardId)
    }
}