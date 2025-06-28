package leavetracker.infrastructure.clients

data class HttpCallNonSucceededException(
    val httpClient: String,
    val errorBody: String?,
    val httpStatus: Int
) : RuntimeException("Http call with '$httpClient' failed with status '$httpStatus' and body '$errorBody' ")
