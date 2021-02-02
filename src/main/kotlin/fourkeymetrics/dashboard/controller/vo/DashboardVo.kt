package fourkeymetrics.dashboard.controller.vo

import fourkeymetrics.dashboard.model.Dashboard1
import org.apache.logging.log4j.util.Strings


class DashboardVo(
    val id: String = Strings.EMPTY,
    var name: String = Strings.EMPTY,
    val synchronizationTimestamp: Long? = null,
) {
    companion object {
        fun buildFrom(dashboard: Dashboard1): DashboardVo {
            return DashboardVo(dashboard.id, dashboard.name, dashboard.synchronizationTimestamp)
        }
    }
}
