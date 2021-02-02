package fourkeymetrics.dashboard.repository

import fourkeymetrics.dashboard.exception.PipelineNotFoundException
import fourkeymetrics.dashboard.model.Pipeline
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@SuppressWarnings("TooManyFunctions")
@Component
class PipelineRepository {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    fun findById(pipelineId: String): Pipeline {
        val query = Query().addCriteria(
            Criteria.where("id").isEqualTo(pipelineId)
        )

        return mongoTemplate.findOne(query, Pipeline::class.java) ?: throw PipelineNotFoundException()
    }

    fun deleteById(pipelineId: String) {
        val query = Query().addCriteria(Criteria.where("id").isEqualTo(pipelineId))
        mongoTemplate.remove(query, Pipeline::class.java)
    }

    fun save(pipeline: Pipeline): Pipeline {
        return mongoTemplate.save(pipeline)
    }

    fun saveAll(pipelines: List<Pipeline>): List<Pipeline> {
        return mongoTemplate.insert(pipelines.toMutableList(), Pipeline::class.java).toList()
    }

    fun deleteByDashboardId(dashboardId: String) {
        val query = Query().addCriteria(Criteria.where("dashboardId").isEqualTo(dashboardId))
        mongoTemplate.remove(query, Pipeline::class.java)
    }
}
