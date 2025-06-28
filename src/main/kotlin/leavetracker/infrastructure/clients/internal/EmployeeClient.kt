package leavetracker.infrastructure.clients.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import leavetracker.business.domain.Employee
import leavetracker.infrastructure.clients.HttpCallNonSucceededException
import leavetracker.infrastructure.defaultObjectMapper
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EmployeeClient(
    private val okHttpClient: OkHttpClient,
    private val objectMapper: ObjectMapper,
    @Value("\${clients.internal.employees.baseUrl: https://internal-network.employee.service/}") private val baseUrl: String,
) {
    fun get(employeeId: UUID) : Employee =
        okHttpClient.newCall(
            okhttp3.Request.Builder().url("$baseUrl/employees/$employeeId").build()
        ).execute().use { response ->
            if (response.isSuccessful)
                objectMapper.readValue(response.body!!.string(), EmployeeHttpDto::class.java)
                    .let { Employee(it.id, it.countryCode) }
             else throw HttpCallNonSucceededException("EmployeeClient", response.body!!.string(), response.code)
        }
}

data class EmployeeHttpDto(val id: UUID, val fullName: String, val countryCode: String)
