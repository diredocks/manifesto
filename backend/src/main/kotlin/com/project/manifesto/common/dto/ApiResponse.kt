package com.project.manifesto.common.dto

import org.springframework.http.HttpStatus

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T? = null,
) {
    companion object {
        fun <T> success(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatus.OK.value(), "success", data)

        fun <T> error(
            code: Int,
            message: String,
        ): ApiResponse<T> = ApiResponse(code, message, null)

        fun <T> badRequest(message: String): ApiResponse<T> = ApiResponse(HttpStatus.BAD_REQUEST.value(), message, null)

        fun <T> unauthorized(message: String = "Unauthorized"): ApiResponse<T> = ApiResponse(HttpStatus.UNAUTHORIZED.value(), message, null)

        fun <T> forbidden(message: String = "Forbidden"): ApiResponse<T> = ApiResponse(HttpStatus.FORBIDDEN.value(), message, null)

        fun <T> notFound(message: String): ApiResponse<T> = ApiResponse(HttpStatus.NOT_FOUND.value(), message, null)
    }
}
