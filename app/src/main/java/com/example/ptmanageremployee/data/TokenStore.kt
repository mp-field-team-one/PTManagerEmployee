package com.example.ptmanageremployee.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * JWT 토큰과 로그인한 사용자 기본 정보를 SharedPreferences 에 보관한다.
 * 앱 어디서나 [TokenStore.init] 로 초기화된 싱글턴으로 접근한다.
 */
object TokenStore {
    private const val PREFS = "ptmanager_auth_secure"
    private const val LEGACY_PREFS = "ptmanager_auth"
    private const val KEY_ACCESS = "access_token"
    private const val KEY_REFRESH = "refresh_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_ROLE = "role"
    private const val KEY_WORKPLACE_ID = "workplace_id"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        val appContext = context.applicationContext
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            appContext,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        migrateLegacySession(appContext)
    }

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)
        set(value) = prefs.edit { putString(KEY_ACCESS, value) }

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(value) = prefs.edit { putString(KEY_REFRESH, value) }

    val userId: Long
        get() = prefs.getLong(KEY_USER_ID, -1L)

    val role: String?
        get() = prefs.getString(KEY_ROLE, null)

    /** 소속 매장 ID. 직원이 아직 매장에 가입되지 않았으면 -1. */
    val workplaceId: Long
        get() = prefs.getLong(KEY_WORKPLACE_ID, -1L)

    val name: String?
        get() = prefs.getString(KEY_NAME, null)

    val email: String?
        get() = prefs.getString(KEY_EMAIL, null)

    val isLoggedIn: Boolean
        get() = !accessToken.isNullOrBlank()

    /** 로그인/회원가입 응답으로 토큰과 사용자 정보를 한꺼번에 저장한다. */
    fun saveSession(token: TokenResponse) {
        prefs.edit {
            putString(KEY_ACCESS, token.accessToken)
            putString(KEY_REFRESH, token.refreshToken)
        }
        updateUser(token.user)
    }

    /** /api/auth/me 등으로 최신 사용자 정보를 받아 갱신한다. */
    fun updateUser(user: UserDto) {
        prefs.edit {
            putLong(KEY_USER_ID, user.id)
            putString(KEY_ROLE, user.role)
            putString(KEY_NAME, user.name)
            putString(KEY_EMAIL, user.email)
            val wp = user.workplaceId
            if (wp != null) putLong(KEY_WORKPLACE_ID, wp) else remove(KEY_WORKPLACE_ID)
        }
    }

    fun clear() {
        prefs.edit { clear() }
    }

    /** 평문 저장소에 남아 있던 세션을 암호화 저장소로 한 번만 옮긴다. */
    private fun migrateLegacySession(context: Context) {
        val legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        if (legacy.all.isEmpty()) return
        prefs.edit {
            legacy.all.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Long -> putLong(key, value)
                    else -> Unit // 저장하는 값은 String/Long 뿐이다.
                }
            }
        }
        legacy.edit { clear() }
    }
}
