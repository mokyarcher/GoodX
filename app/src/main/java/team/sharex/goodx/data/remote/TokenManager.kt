package team.sharex.goodx.data.remote

import android.content.Context
import android.content.SharedPreferences

object TokenManager {
    private const val PREFS_NAME = "goodx_prefs"
    private const val KEY_TOKEN = "token"
    private const val KEY_THEME = "theme"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_AVATAR = "avatar"
    private const val KEY_USERNAME = "username"
    private const val KEY_USER_ID = "userId"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun isLoggedIn(): Boolean = getToken() != null

    // 主题切换
    fun saveTheme(theme: Int) {
        prefs.edit().putInt(KEY_THEME, theme).apply()
    }

    fun getTheme(): Int = prefs.getInt(KEY_THEME, 1) // 默认方案1

    // 用户信息缓存
    fun saveUserInfo(id: String, username: String, nickname: String?, avatar: String?) {
        prefs.edit()
            .putString(KEY_USER_ID, id)
            .putString(KEY_USERNAME, username)
            .putString(KEY_NICKNAME, nickname)
            .putString(KEY_AVATAR, avatar)
            .apply()
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getNickname(): String? = prefs.getString(KEY_NICKNAME, null)
    fun getAvatar(): String? = prefs.getString(KEY_AVATAR, null)

    fun updateNickname(nickname: String) {
        prefs.edit().putString(KEY_NICKNAME, nickname).apply()
    }

    fun updateAvatar(avatar: String?) {
        prefs.edit().putString(KEY_AVATAR, avatar).apply()
    }
}
