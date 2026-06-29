package com.example.ptmanageremployee.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * PTManager 백엔드 REST API (직원 앱이 사용하는 엔드포인트).
 * 모든 호출은 suspend 함수이며, 인증 헤더는 [AuthInterceptor] 가 자동 부착한다.
 */
interface ApiService {

    // ---- Auth ----
    @POST("api/auth/signup")
    suspend fun signup(@Body body: SignupRequest): TokenResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @POST("api/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenResponse

    @POST("api/auth/logout")
    suspend fun logout()

    @GET("api/auth/me")
    suspend fun me(): UserDto

    // ---- Workplace / JoinRequest ----
    @GET("api/workplaces/{id}")
    suspend fun getWorkplace(@Path("id") id: Long): WorkplaceDto

    @GET("api/workplaces/{id}/members")
    suspend fun getMembers(
        @Path("id") id: Long,
        @Query("role") role: String? = null,
    ): List<UserDto>

    @POST("api/join-requests")
    suspend fun createJoinRequest(@Body body: CreateJoinRequest): JoinRequestDto

    // ---- User ----
    @PATCH("api/users/me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): UserDto

    @GET("api/users/me/notification-setting")
    suspend fun getNotificationSetting(): NotificationSettingDto

    @PATCH("api/users/me/notification-setting")
    suspend fun updateNotificationSetting(@Body body: NotificationSettingUpdate): NotificationSettingDto

    // ---- Shift ----
    @GET("api/shifts")
    suspend fun getShifts(
        @Query("workplaceId") workplaceId: Long? = null,
        @Query("employeeId") employeeId: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("status") status: String? = null,
    ): List<ShiftDto>

    @GET("api/shifts/{id}")
    suspend fun getShift(@Path("id") id: Long): ShiftDto

    @POST("api/shifts/{id}/check-in")
    suspend fun checkIn(@Path("id") id: Long, @Body body: CheckInRequest): ShiftDto

    // ---- SwapRequest ----
    @POST("api/swap-requests")
    suspend fun createSwapRequest(@Body body: CreateSwapRequest): SwapRequestDto

    @GET("api/swap-requests")
    suspend fun getSwapRequests(
        @Query("workplaceId") workplaceId: Long,
        @Query("view") view: String,
        @Query("status") status: String? = null,
    ): List<SwapRequestDto>

    @GET("api/swap-requests/{id}")
    suspend fun getSwapRequest(@Path("id") id: Long): SwapRequestDetailDto

    @POST("api/swap-requests/{id}/applications")
    suspend fun applyToSwap(@Path("id") id: Long): SwapApplicationDto

    @GET("api/swap-applications/me")
    suspend fun getMySwapApplications(@Query("status") status: String? = null): List<SwapApplicationDto>

    // ---- Notice ----
    @GET("api/notices")
    suspend fun getNotices(
        @Query("workplaceId") workplaceId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageResponse<NoticeDto>

    @GET("api/notices/{id}")
    suspend fun getNotice(@Path("id") id: Long): NoticeDto

    @GET("api/notices/unread")
    suspend fun getNoticeUnread(@Query("workplaceId") workplaceId: Long): UnreadFlag

    @POST("api/notices/read")
    suspend fun markNoticesRead()

    // ---- Notification ----
    @GET("api/notifications")
    suspend fun getNotifications(
        @Query("isRead") isRead: Boolean? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageResponse<NotificationDto>

    @GET("api/notifications/unread-count")
    suspend fun getNotificationUnreadCount(): UnreadCount

    @PATCH("api/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Long)

    @POST("api/notifications/read-all")
    suspend fun markAllNotificationsRead()

    // ---- Device token (푸시 / FCM) ----
    @POST("api/users/me/device-tokens")
    suspend fun registerDeviceToken(@Body body: RegisterDeviceTokenRequest)

    @DELETE("api/users/me/device-tokens/{token}")
    suspend fun deleteDeviceToken(@Path("token") token: String)
}
