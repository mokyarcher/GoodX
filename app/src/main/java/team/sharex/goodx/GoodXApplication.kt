package team.sharex.goodx

import android.app.Application
import team.sharex.goodx.data.remote.TokenManager

class GoodXApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenManager.init(this)
    }
}
