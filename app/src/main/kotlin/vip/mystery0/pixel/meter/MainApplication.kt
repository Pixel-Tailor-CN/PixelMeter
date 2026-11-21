package vip.mystery0.pixel.meter

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import vip.mystery0.pixel.meter.di.appModule
import vip.mystery0.pixel.meter.service.NotificationHelper
import vip.mystery0.pixel.meter.util.AppLocaleManager
import java.util.Collections
import java.util.WeakHashMap

class MainApplication : Application() {
    private val activeActivities = Collections.newSetFromMap(WeakHashMap<Activity, Boolean>())

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocaleManager.wrapBaseContext(base))
    }

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    activeActivities.add(activity)
                }

                override fun onActivityDestroyed(activity: Activity) {
                    activeActivities.remove(activity)
                }

                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            },
        )

        AppLocaleManager.setOnLegacyLanguageChangedListener { localized ->
            NotificationHelper.createNotificationChannel(localized)
            activeActivities.toList().forEach { activity ->
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.recreate()
                }
            }
        }

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule)
        }

        // Initialize Notification Channel immediately
        NotificationHelper.createNotificationChannel(AppLocaleManager.localizedContext(this))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        NotificationHelper.createNotificationChannel(AppLocaleManager.localizedContext(this))
    }
}
