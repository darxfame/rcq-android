# Android Xray Engine Runbook

## Why Xray Is Needed

The bundled sing-box `libbox.aar` cannot parse the priority relay with `type=xhttp`:

```text
decode config: outbounds[1].transport: unknown transport type: xhttp
```

XHTTP is an Xray-core transport. Mainline sing-box remains useful for sing-box-compatible relays, but Android needs Xray for `VLESS + REALITY + XHTTP`.

## Selected Artifact

Source: `XTLS/Xray-core`

Release: `v26.3.27`

Artifact:

```text
https://github.com/XTLS/Xray-core/releases/download/v26.3.27/Xray-android-arm64-v8a.zip
```

Verified checksum:

```text
57149ffd48b629c07bf76938e73ab2729fde5910091497eab3e93d1c190f4c1b
```

The ZIP contains:

```text
xray
geoip.dat
geosite.dat
LICENSE
README.md
```

The `xray` executable is packaged as:

```text
app/src/main/jniLibs/arm64-v8a/libxray_exec.so
```

This avoids gomobile Java runtime conflicts with `app/libs/libbox.aar`.

## Rejected AAR Path

`2dust/AndroidLibXrayLite v26.6.2` provides `libv2ray.aar`, but both it and `libbox.aar` contain gomobile `go/Seq*.class`, `go/Universe*.class`, and `go/error.class`. Their `go.Seq` classes load different native libraries (`gojni` vs `box`), so they cannot simply coexist as Gradle dependencies.

## Runtime Contract

`XrayTransport` starts the executable as a child process:

```text
xray run -config <filesDir>/xray/config.json
```

It exposes a local SOCKS inbound:

```text
127.0.0.1:1089
```

The transport reports active only after `BuildConfig.API_BASE_URL/health` succeeds through that SOCKS proxy.
