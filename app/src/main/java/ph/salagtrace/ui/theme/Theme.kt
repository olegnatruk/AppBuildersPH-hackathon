package ph.salagtrace.ui.theme

import android.content.res.Configuration
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import ph.salagtrace.domain.AppLanguage
import java.util.Locale

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.FILIPINO }
val LocalDarkTheme = staticCompositionLocalOf { true }
@Composable
fun SalagTheme(language: AppLanguage, dark: Boolean = true, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val registryOwner = LocalActivityResultRegistryOwner.current ?: (context as? ActivityResultRegistryOwner)
    val configuration = LocalConfiguration.current
    val localized = remember(context, language, configuration) {
        context.createConfigurationContext(Configuration(configuration).apply { setLocale(Locale.forLanguageTag(if (language == AppLanguage.FILIPINO) "tl" else "en")) })
    }
    CompositionLocalProvider(LocalAppLanguage provides language, LocalDarkTheme provides dark, LocalContext provides localized, LocalResources provides localized.resources, LocalActivityResultRegistryOwner provides requireNotNull(registryOwner)) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = SalagTypography, content = content)
    }
}
