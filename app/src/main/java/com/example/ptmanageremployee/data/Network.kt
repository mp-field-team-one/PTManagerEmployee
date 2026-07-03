package com.example.ptmanageremployee.data

import com.example.ptmanageremployee.BuildConfig
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Retrofit/OkHttp 클라이언트 구성과 전역 [ApiService] 인스턴스.
 *
 * 안드로이드 에뮬레이터에서 호스트 PC 의 localhost 는 10.0.2.2 로 접근한다.
 * 실제 기기/운영 환경에서는 BASE_URL 을 교체한다.
 */
object Network {

    val BASE_URL: String = BuildConfig.BASE_URL

    /** 저장된 액세스 토큰을 Authorization 헤더로 부착한다. */
    /**
     * 리프레시 토큰마저 만료돼 세션을 복구할 수 없을 때 호출된다.
     * UI(예: MainActivity)에서 로그인 화면으로 보내도록 설정한다.
     */
    @Volatile
    var onSessionExpired: (() -> Unit)? = null

    /** 저장된 액세스 토큰을 Authorization 헤더로 부착한다. */
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = TokenStore.accessToken
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        chain.proceed(request)
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * 401 응답 시 리프레시 토큰으로 액세스 토큰을 갱신하고 원 요청을 재시도한다.
     * 갱신 호출은 [refreshApi](authenticator 미적용 클라이언트)로 수행해 재귀를 막는다.
     */
    private val refreshLock = Any()

    /** 리프레시 재시도 횟수/간격. 서버 일시 장애 시 바로 로그아웃하지 않도록 몇 차례 재시도한다. */
    private const val REFRESH_MAX_ATTEMPTS = 3
    private const val REFRESH_RETRY_DELAY_MS = 600L

    private val tokenAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            // 이미 갱신 후 재시도한 요청까지 401 이면 포기(무한 루프 방지).
            if (responseCount(response) >= 2) return null

            val refreshToken = TokenStore.refreshToken
            if (refreshToken.isNullOrBlank()) return null

            synchronized(refreshLock) {
                val currentToken = TokenStore.accessToken
                val failedToken = response.request.header("Authorization")
                    ?.removePrefix("Bearer ")

                // 다른 스레드가 이미 갱신했으면 새 토큰으로 곧바로 재시도.
                if (!currentToken.isNullOrBlank() && currentToken != failedToken) {
                    return retryWith(response.request, currentToken)
                }

                // 리프레시를 여러 번 재시도한다. 서버 과부하로 인한 '일시적' 실패(네트워크·5xx·빈 401)면
                // 세션을 지우지 않고 이번 요청만 실패시켜, 사용자가 로그아웃되지 않게 한다.
                var definitiveInvalid = false
                for (attempt in 0 until REFRESH_MAX_ATTEMPTS) {
                    val renewed = runCatching {
                        refreshApi.refresh(RefreshRequest(refreshToken)).execute()
                    }.getOrNull()
                    val body = renewed?.body()
                    if (renewed != null && renewed.isSuccessful && body != null) {
                        TokenStore.saveSession(body)
                        return retryWith(response.request, body.accessToken)
                    }
                    // 리프레시 토큰이 '확실히' 무효(본문 있는 400/401)일 때만 로그아웃 확정.
                    if (renewed != null && (renewed.code() == 401 || renewed.code() == 400)) {
                        val err = runCatching { renewed.errorBody()?.string() }.getOrNull()
                        if (!err.isNullOrBlank()) {
                            definitiveInvalid = true
                            break
                        }
                    }
                    // 그 외(네트워크·5xx·빈 응답)는 일시적 → 잠깐 대기 후 재시도.
                    if (attempt < REFRESH_MAX_ATTEMPTS - 1) {
                        runCatching { Thread.sleep(REFRESH_RETRY_DELAY_MS) }
                    }
                }

                if (definitiveInvalid) {
                    // 리프레시 토큰 만료/무효 → 세션 종료.
                    TokenStore.clear()
                    onSessionExpired?.invoke()
                }
                // 일시적 실패면 세션 유지(다음 요청에서 회복). 이번 요청만 401로 실패.
                return null
            }
        }
    }

    private fun retryWith(request: Request, accessToken: String): Request =
        request.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

    /** 이 응답까지 이어진 응답 체인 길이(재시도 횟수 추적). */
    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .authenticator(tokenAuthenticator)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /** 토큰 갱신 전용 클라이언트: authInterceptor·authenticator 를 적용하지 않는다. */
    private val refreshClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val refreshApi: RefreshApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(refreshClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RefreshApi::class.java)
    }

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

/** 토큰 갱신만 담당하는 최소 API(동기 호출). */
private interface RefreshApi {
    @POST("api/auth/refresh")
    fun refresh(@Body body: RefreshRequest): retrofit2.Call<TokenResponse>
}

/** 백엔드 공통 에러 응답(ApiError) 구조. */
data class ApiError(
    val status: Int? = null,
    val error: String? = null,
    val code: String? = null,
    val message: String? = null,
    val path: String? = null,
)

private val errorGson = Gson()

/**
 * 예외에서 사용자에게 보여줄 메시지를 추출한다.
 * 백엔드 ApiError.message 가 있으면 그대로, 없으면 상태코드/기본 문구로 대체한다.
 */
fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> {
        val body = response()?.errorBody()?.string()
        val parsed = body?.let {
            runCatching { errorGson.fromJson(it, ApiError::class.java) }.getOrNull()
        }
        parsed?.message?.takeIf { it.isNotBlank() }
            ?: "요청을 처리하지 못했습니다. (${code()})"
    }
    is java.net.ConnectException,
    is java.net.SocketTimeoutException,
    is java.net.UnknownHostException ->
        "서버에 연결할 수 없습니다. 네트워크와 서버 상태를 확인해 주세요."
    else -> message ?: "알 수 없는 오류가 발생했습니다."
}
