package com.avinal.memos

import com.avinal.memos.api.ApiResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ApiResultTest {

    @Test
    fun successHoldsData() {
        val result: ApiResult<String> = ApiResult.Success("hello")
        assertIs<ApiResult.Success<String>>(result)
        assertEquals("hello", result.data)
    }

    @Test
    fun errorHoldsCodeAndMessage() {
        val result: ApiResult<String> = ApiResult.Error(404, "not found")
        assertIs<ApiResult.Error>(result)
        assertEquals(404, result.code)
        assertEquals("not found", result.message)
    }

    @Test
    fun networkErrorHoldsException() {
        val ex = RuntimeException("timeout")
        val result: ApiResult<String> = ApiResult.NetworkError(ex)
        assertIs<ApiResult.NetworkError>(result)
        assertEquals("timeout", result.exception.message)
    }
}
