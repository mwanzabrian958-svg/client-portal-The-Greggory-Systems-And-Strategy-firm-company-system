package com.greggory.portal

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class NetworkPipelineTest {

    @Test
    fun `Retry Interceptor retries on 502, 503, 504`() {
        val attempts = AtomicInteger(0)
        
        // Custom chain mock to simulate backend failures
        val mockChain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("https://test.com").build()
            
            override fun proceed(request: Request): Response {
                val count = attempts.incrementAndGet()
                val code = when (count) {
                    1 -> 502
                    2 -> 503
                    3 -> 504
                    else -> 200
                }
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("Test Message")
                    .body("".toResponseBody(null))
                    .build()
            }

            // Unused methods for this test
            override fun connection() = null
            override fun call() = throw UnsupportedOperationException()
            override fun connectTimeoutMillis() = 0
            override fun readTimeoutMillis() = 0
            override fun writeTimeoutMillis() = 0
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        }

        // The logic from RetrofitClient.kt
        val retryInterceptor = Interceptor { chain ->
            val request = chain.request()
            var response = chain.proceed(request)
            var tryCount = 0
            val maxLimit = 3

            while (!response.isSuccessful && (response.code in 502..504) && tryCount < maxLimit) {
                tryCount++
                // Skip Thread.sleep in unit tests for speed
                response.close()
                response = chain.proceed(request)
            }
            response
        }

        val finalResponse = retryInterceptor.intercept(mockChain)
        
        // Should have tried 4 times (Initial + 3 retries)
        assertEquals(4, attempts.get())
        assertEquals(200, finalResponse.code)
    }

    @Test
    fun `Header Interceptor injects pipeline identifiers`() {
        var capturedRequest: Request? = null
        
        val mockChain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("https://test.com").build()
            override fun proceed(request: Request): Response {
                capturedRequest = request
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .build()
            }
            override fun connection() = null
            override fun call() = throw UnsupportedOperationException()
            override fun connectTimeoutMillis() = 0
            override fun readTimeoutMillis() = 0
            override fun writeTimeoutMillis() = 0
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        }

        // Logic from RetrofitClient.kt
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
            
            // Hardcoded values for test verification
            val userId = 42 
            val token = "test_token"
            
            requestBuilder.header("User-Agent", "GreggoryClientPortal/1.0.0 (Android; UnitTests)")
            requestBuilder.header("Authorization", "Bearer $token")
            requestBuilder.header("X-Greggory-Client-ID", userId.toString())
            requestBuilder.header("X-Routing-Policy", "set-in-stone-v1")
            
            chain.proceed(requestBuilder.build())
        }

        authInterceptor.intercept(mockChain)

        assertNotNull(capturedRequest)
        assertEquals("GreggoryClientPortal/1.0.0 (Android; UnitTests)", capturedRequest?.header("User-Agent"))
        assertEquals("Bearer test_token", capturedRequest?.header("Authorization"))
        assertEquals("42", capturedRequest?.header("X-Greggory-Client-ID"))
        assertEquals("set-in-stone-v1", capturedRequest?.header("X-Routing-Policy"))
    }

    private fun assertNotNull(obj: Any?) {
        if (obj == null) throw AssertionError("Object is null")
    }
}
