package app.rcq.android.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import app.rcq.android.R

/** One selectable launcher icon. [alias] is the SHORT name of an
 *  `<activity-alias>` under the `.icon.` namespace (e.g. "Default", "Mono").
 *  The first entry in [AppIconManager.options] is the default. */
data class AppIconOption(
    val alias: String,
    @StringRes val labelRes: Int,
)

/**
 * Alternate launcher-icon support. Each selectable icon is an
 * `<activity-alias>` that targets MainActivity; exactly one is enabled at a
 * time. Toggling is done with [PackageManager.setComponentEnabledSetting]
 * (DONT_KILL_APP), the standard Android pattern. Until the manifest aliases
 * exist this is a safe no-op and the chooser shows only the default.
 *
 * ── HOW TO ADD AN ICON (founder, once the art lands in assets/iOS_Logos) ──
 *  1. Add the launcher art as mipmaps: ic_launcher_<name>.png (+ _round)
 *     under each res/mipmap density bucket, like the existing ic_launcher.
 *  2. ONE-TIME manifest refactor (do it with the FIRST alternate so the live
 *     launcher is never doubled). In AndroidManifest.xml:
 *       - REMOVE the MAIN/LAUNCHER `<intent-filter>` from `.MainActivity`
 *         (keep the activity + its rcq:// deep-link filter).
 *       - add a DEFAULT alias that carries the launcher and the current icon:
 *           <activity-alias android:name=".icon.Default"
 *               android:targetActivity=".MainActivity"
 *               android:enabled="true"  android:exported="true"
 *               android:icon="@mipmap/ic_launcher"
 *               android:roundIcon="@mipmap/ic_launcher_round"
 *               android:label="@string/app_name">
 *             <intent-filter>
 *               <action android:name="android.intent.action.MAIN"/>
 *               <category android:name="android.intent.category.LAUNCHER"/>
 *             </intent-filter>
 *           </activity-alias>
 *       - one alias per alternate, `android:enabled="false"`,
 *         `android:icon="@mipmap/ic_launcher_<name>"`, same MAIN/LAUNCHER.
 *  3. Append entries to [options] below (alias == the alias short name).
 *  4. Add the label strings (values + values-ru).
 *  Test on a device: switching disables the old alias and enables the new
 *  one; some launchers drop the icon for a beat before it reappears.
 */
object AppIconManager {
    // First entry is the default; append alternates after the manifest
    // refactor above. The "Default" alias is `.icon.Default` once created.
    val options: List<AppIconOption> = listOf(
        AppIconOption("Default", R.string.app_icon_default),
        // AppIconOption("Mono", R.string.app_icon_mono),
        // AppIconOption("Dark", R.string.app_icon_dark),
    )

    private val default: AppIconOption get() = options.first()

    private fun component(ctx: Context, alias: String): ComponentName =
        ComponentName(ctx, "${ctx.packageName}.icon.$alias")

    /** The currently enabled option, or the default when no alias is on
     *  (also the case before the manifest aliases exist). */
    fun current(ctx: Context): AppIconOption {
        val pm = ctx.packageManager
        for (opt in options) {
            if (opt.alias == default.alias) continue
            val enabled = runCatching {
                pm.getComponentEnabledSetting(component(ctx, opt.alias)) ==
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }.getOrDefault(false)
            if (enabled) return opt
        }
        return default
    }

    /** Enable [target]'s alias and disable the rest. No-op while the
     *  registry is default-only (no aliases wired yet). */
    fun set(ctx: Context, target: AppIconOption) {
        if (options.size < 2) return
        val pm = ctx.packageManager
        for (opt in options) {
            val state = if (opt.alias == target.alias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            runCatching {
                pm.setComponentEnabledSetting(
                    component(ctx, opt.alias), state, PackageManager.DONT_KILL_APP,
                )
            }
        }
    }
}
