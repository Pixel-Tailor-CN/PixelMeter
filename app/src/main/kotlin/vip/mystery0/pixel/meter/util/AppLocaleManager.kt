package vip.mystery0.pixel.meter.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

enum class AppLanguage(val languageTag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    SIMPLIFIED_CHINESE("zh-CN"),
    PORTUGUESE("pt"),
    PORTUGUESE_BRAZIL("pt-BR"),
    RUSSIAN("ru");

    companion object {
        fun fromLocales(locales: LocaleList?): AppLanguage {
            if (locales == null || locales.isEmpty) return SYSTEM
            val primary = locales[0] ?: return SYSTEM
            return when {
                primary.language.equals("zh", ignoreCase = true) -> SIMPLIFIED_CHINESE
                primary.language.equals("pt", ignoreCase = true) &&
                    primary.country.equals("BR", ignoreCase = true) -> PORTUGUESE_BRAZIL
                primary.language.equals("pt", ignoreCase = true) -> PORTUGUESE
                primary.language.equals("ru", ignoreCase = true) -> RUSSIAN
                primary.language.equals("en", ignoreCase = true) -> ENGLISH
                else -> SYSTEM
            }
        }

        fun fromTag(tag: String?): AppLanguage = when {
            tag.isNullOrBlank() -> SYSTEM
            tag.startsWith("zh", ignoreCase = true) -> SIMPLIFIED_CHINESE
            tag.equals("pt-BR", ignoreCase = true) ||
                tag.equals("pt_BR", ignoreCase = true) -> PORTUGUESE_BRAZIL
            tag.startsWith("pt", ignoreCase = true) -> PORTUGUESE
            tag.startsWith("ru", ignoreCase = true) -> RUSSIAN
            tag.startsWith("en", ignoreCase = true) -> ENGLISH
            else -> SYSTEM
        }
    }
}

/**
 * Manages in-app language selection using system Per-App Language (LocaleManager) on Android 13+
 * and a SharedPreferences + wrapped-context fallback on Android 12/12L.
 */
object AppLocaleManager {
    private const val PREFS_NAME = "app_locale_prefs"
    private const val KEY_LANGUAGE_TAG = "app_language_tag"

    @Volatile
    private var legacyChangeListener: ((Context) -> Unit)? = null

    fun setOnLegacyLanguageChangedListener(listener: ((Context) -> Unit)?) {
        legacyChangeListener = listener
    }

    fun currentLanguage(context: Context): AppLanguage {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            return AppLanguage.fromLocales(localeManager?.applicationLocales)
        }
        return readLegacyLanguage(context)
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java) ?: return
            val targetLocales = if (language == AppLanguage.SYSTEM) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(language.languageTag)
            }
            if (localeManager.applicationLocales != targetLocales) {
                localeManager.applicationLocales = targetLocales
            }
            return
        }

        val current = readLegacyLanguage(context)
        if (current == language) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_LANGUAGE_TAG, language.languageTag)
        }
        syncLegacyDefaultLocale(language)
        val localized = localizedContext(context.applicationContext)
        legacyChangeListener?.invoke(localized)
    }

    fun wrapBaseContext(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return base
        }
        val language = readLegacyLanguage(base)
        syncLegacyDefaultLocale(language)
        return createLocaleContext(base, language)
    }

    fun localizedContext(context: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context
        }
        val language = readLegacyLanguage(context)
        syncLegacyDefaultLocale(language)
        return createLocaleContext(context, language)
    }

    private fun readLegacyLanguage(context: Context): AppLanguage {
        val tag = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE_TAG, null)
        return AppLanguage.fromTag(tag)
    }

    private fun resolveTargetLocale(language: AppLanguage): Locale? = when (language) {
        AppLanguage.SYSTEM -> null
        AppLanguage.ENGLISH -> Locale.ENGLISH
        AppLanguage.SIMPLIFIED_CHINESE -> Locale.SIMPLIFIED_CHINESE
        AppLanguage.PORTUGUESE -> Locale.forLanguageTag("pt")
        AppLanguage.PORTUGUESE_BRAZIL -> Locale.forLanguageTag("pt-BR")
        AppLanguage.RUSSIAN -> Locale.forLanguageTag("ru")
    }

    private fun syncLegacyDefaultLocale(language: AppLanguage) {
        val target = resolveTargetLocale(language)
            ?: Resources.getSystem().configuration.locales[0]
            ?: return
        if (Locale.getDefault() != target) {
            Locale.setDefault(target)
        }
    }

    private fun createLocaleContext(context: Context, language: AppLanguage): Context {
        val targetLocale = resolveTargetLocale(language) ?: return context
        val config = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(targetLocale))
        }
        return context.createConfigurationContext(config)
    }
}
