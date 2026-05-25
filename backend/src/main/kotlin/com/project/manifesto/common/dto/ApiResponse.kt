package com.project.manifesto.common.dto

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T? = null
) {
    companion object {
        fun <T> success(data: T? = null): ApiResponse<T> =
            ApiResponse(200, "success", data)

        fun <T> error(code: Int, message: String): ApiResponse<T> =
            ApiResponse(code, message, null)

        fun <T> badRequest(message: String): ApiResponse<T> =
            ApiResponse(400, message, null)

        fun <T> unauthorized(message: String = "Unauthorized"): ApiResponse<T> =
            ApiResponse(401, message, null)

        fun <T> forbidden(message: String = "Forbidden"): ApiResponse<T> =
            ApiResponse(403, message, null)

        fun <T> notFound(message: String): ApiResponse<T> =
            ApiResponse(404, message, null)
    }
}
