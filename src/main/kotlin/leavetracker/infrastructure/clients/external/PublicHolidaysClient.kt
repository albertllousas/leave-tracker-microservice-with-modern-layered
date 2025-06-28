package leavetracker.infrastructure.clients.external

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import leavetracker.infrastructure.clients.HttpCallNonSucceededException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.MonthDay
import java.time.Year

@Component
class PublicHolidaysClient(
    private val okHttpClient: OkHttpClient,
    private val objectMapper: ObjectMapper,
    @Value("\${clients.external.nager.baseUrl: https://date.nager.at/api/v3}") private val nagerBaseUrl: String,
) {
    fun get(year: Year, countryCode: String): List<MonthDay> =
        okHttpClient.newCall(
            Request.Builder().url("$nagerBaseUrl/PublicHolidays/$year/$countryCode").build()
        ).execute().use { response ->
            if (response.isSuccessful)
                objectMapper.readValue(
                    response.body!!.string(),
                    object : TypeReference<List<PublicHolidayNagerHttpDto>>() {}
                ).map {
                    MonthDay.from(it.date)
                }
            else throw HttpCallNonSucceededException("PublicHolidaysClient", response.body!!.string(), response.code)
        }
}

data class PublicHolidayNagerHttpDto(val date: LocalDate, val localName: String, val name: String, val countryCode: String)
