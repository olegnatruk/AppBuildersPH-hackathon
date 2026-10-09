package ph.salagtrace.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import ph.salagtrace.domain.Verdict

val TraceAmber = Color(0xFFF5B83D)
val DarkColors = darkColorScheme(
    primary = TraceAmber, onPrimary = Color(0xFF0B1220), background = Color(0xFF0B1220), onBackground = Color(0xFFE8EDF7),
    surface = Color(0xFF121A2B), onSurface = Color(0xFFE8EDF7), surfaceVariant = Color(0xFF1A2438), onSurfaceVariant = Color(0xFF8E9BB3),
    primaryContainer = Color(0xFF1A2438), onPrimaryContainer = TraceAmber,
    secondaryContainer = Color(0xFF1A2438), onSecondaryContainer = Color(0xFFE8EDF7),
    surfaceContainer = Color(0xFF121A2B), surfaceContainerLow = Color(0xFF121A2B), surfaceContainerHigh = Color(0xFF1A2438), surfaceContainerHighest = Color(0xFF1A2438),
    secondary = Color(0xFF2DD4A7), onSecondary = Color(0xFF0B1220), outline = Color(0xFF475569), error = Color(0xFFFF5A5F),
)
val LightColors = lightColorScheme(
    primary = Color(0xFF946019), onPrimary = Color.White, background = Color(0xFFF6F7FB), onBackground = Color(0xFF0F172A),
    surface = Color.White, onSurface = Color(0xFF0F172A), surfaceVariant = Color(0xFFEEF1F7), onSurfaceVariant = Color(0xFF5B6478),
    primaryContainer = Color(0xFFEEF1F7), onPrimaryContainer = Color(0xFF946019),
    secondaryContainer = Color(0xFFEEF1F7), onSecondaryContainer = Color(0xFF0F172A),
    surfaceContainer = Color.White, surfaceContainerLow = Color.White, surfaceContainerHigh = Color(0xFFEEF1F7), surfaceContainerHighest = Color(0xFFEEF1F7),
    secondary = Color(0xFF047857), onSecondary = Color.White, outline = Color(0xFF94A3B8), error = Color(0xFFC81E1E),
)
fun riskColor(verdict: Verdict, dark: Boolean) = when (verdict) {
    Verdict.LIKELY_SCAM -> Color(if (dark) 0xFFFF5A5F else 0xFFC81E1E)
    Verdict.SUSPICIOUS -> Color(if (dark) 0xFFFFA94D else 0xFFA94708)
    Verdict.UNCERTAIN -> Color(if (dark) 0xFF8EA2C0 else 0xFF475569)
    Verdict.LIKELY_SAFE -> Color(if (dark) 0xFF2DD4A7 else 0xFF047857)
}
