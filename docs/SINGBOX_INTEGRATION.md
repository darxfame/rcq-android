# SingBox-интеграция (Android) — как включить реальный обход

Статус кода: **готов к работе**. Нужен бинарный `libbox.aar` в `app/libs/`.
Как только файл там — обход работает без правок кода
(`build.gradle.kts` подхватывает `app/libs/*.aar` через `fileTree`).

## Архитектура

- Режим: **локальный SOCKS5** на `127.0.0.1:1089` (`mixed` inbound). VPN/TUN НЕ используется —
  проксируется только трафик приложения RCQ через `OkHttp` + `RcqProxySelector`.
- Полный паритет с iOS (`reference/ios/.../SingBoxTransport.swift`): тот же формат
  `relay-config.json`, тот же sing-box JSON, тот же порт 1089.
- Android дополнительно накладывает локальные priority relays поверх signed remote/cache
  config. Сейчас первым идёт `relay-usa-amd-xhttp` — VLESS + Reality + xhttp
  (`amd.com`, `/telemetry`). Для него нужен `libbox`/sing-box build с поддержкой
  V2Ray transport `xhttp`.
- `SingBoxTransport.kt` запускает ядро через **reflection** (без статической зависимости),
  поэтому модуль компилируется и без `.aar`. Поддерживаются два варианта API libbox:
  1. `BoxService(config).start()` / `BoxService().start(config)` (iOS-style конструктор);
  2. `Libbox.newService(config, PlatformInterface).start()` (SagerNet-style) — с no-op
     `PlatformInterface` (в режиме SOCKS методы туннелирования не вызываются).
- Честная индикация: `isEngineAvailable` (проверка класса на classpath) → UI показывает
  «движок не установлен» вместо ложного «sing-box активен».

## Вариант A — готовый `libbox.aar`

Источник: `xinggaoya/sing-box-windows-android`, путь `app/libs/libbox.aar`,
~64.3 МБ, пакет классов `io.nekohasekai.libbox`.

```bash
mkdir -p app/libs
gh api "repos/xinggaoya/sing-box-windows-android/contents/app/libs/libbox.aar" \
  -H "Accept: application/vnd.github.raw" > app/libs/libbox.aar
ls -la app/libs/libbox.aar            # ~64290001 байт
head -c 2 app/libs/libbox.aar | xxd   # 504b (PK = валидный ZIP/AAR)
```

## Вариант B — собрать из форка Lantern (доверенно)

Источник: `getlantern/sing-box-libbox` (ветка `dev-next`). Требуется Go 1.21+, NDK, `ANDROID_HOME`.

```bash
git clone -b dev-next https://github.com/getlantern/sing-box-libbox.git
cd sing-box-libbox
make lib_install     # gomobile + gobind @v0.1.4
make lib_android     # → libbox.aar, скопировать в <проект>/app/libs/
```

## Сборка и проверка

```bash
export ANDROID_HOME=/opt/android-sdk
./gradlew :app:assembleDebug
adb logcat | grep "SingBoxTransport"   # ожидаем "sing-box started on 127.0.0.1:1089"
```

## Если API ядра отличается

Единственное место правки — `SingBoxTransport.startNativeEngine()`. Проверить API:
```bash
unzip -p app/libs/libbox.aar classes.jar > /tmp/libbox.jar
javap -classpath /tmp/libbox.jar io.nekohasekai.libbox.Libbox | grep -i service
javap -classpath /tmp/libbox.jar io.nekohasekai.libbox.BoxService | head
```
