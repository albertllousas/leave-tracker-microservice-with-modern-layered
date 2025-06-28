package leavetracker.infrastructure.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import leavetracker.business.domain.AnnualLeave
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.Year
import java.util.UUID

@Repository
class AnnualLeaveRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
) {

    fun findBy(employeeId: UUID, year: Year): AnnualLeave? = try {
        jdbcTemplate.queryForObject(
            """ SELECT * FROM annual_leave WHERE employee_id = '$employeeId' AND year = ${year.value}""",
            mapToDomain
        )
    } catch (_: EmptyResultDataAccessException) {
        null
    }

    @Transactional
    fun save(annualLeave: AnnualLeave): Unit = with(annualLeave) {
        if (annualLeave.version == 0L)
            jdbcTemplate.update(
                """
                    INSERT INTO annual_leave(id, employee_id, year, leaves, version) VALUES (?,?,?,?::jsonb,?)
                """, id, employeeId, year.value, objectMapper.writeValueAsString(leaves), 1
            )
        else
            jdbcTemplate.queryForObject(
                """
                UPDATE annual_leave SET leaves = ?::jsonb, version = version + 1 WHERE id = ? RETURNING version
                """, Long::class.java, objectMapper.writeValueAsString(leaves), id
            ).also { if (it != version + 1) throw OptimisticLockingException(id) }
    }

    private val mapToDomain: (ResultSet, rowNum: Int) -> AnnualLeave? = { rs, _ ->
        AnnualLeave(
            id = rs.getObject("id", UUID::class.java),
            employeeId = rs.getObject("employee_id", UUID::class.java),
            year = rs.getInt("year").let { Year.of(it) },
            leaves = objectMapper.readValue(rs.getString("leaves")),
            version = rs.getLong("version")
        )
    }
}

data class OptimisticLockingException(val id: UUID) : RuntimeException(
    "Failed to update AnnualLeave with id = '$id', possibly due to a concurrent modification"
)
