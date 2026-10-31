# El estéreo: hardware y firmware

Estéreo Android chino de 7" específico para el Audi A3 8P (marca **Podofo**, Amazon **B0DL5NSBY6**), con
botones físicos NAVI / MUTE / BT / MENU / MODE / MUSIC / BAND / APS y dos perillas. El firmware es de **JanCar**
sobre el SoC AutoChips. Foto del equipo y los cables: [estereo-y-cables.jpg](../design/estereo-y-cables.jpg).

Fuente de cada dato: **ADB** = verificado leyendo el equipo; **conversación** = lo informó Mati o salió de
fotos y pantallas "About" del equipo (*sin verificar por ADB*).

## Ficha

| Dato | Valor | Fuente |
|---|---|---|
| Placa | `UJC201_64` (`ro.product.model`) | ADB |
| SoC | MediaTek/AutoChips **AC8257**, **4 núcleos** Cortex-A53 (no 8, como dice la publicidad) | ADB (`/proc/cpuinfo`, procesadores 0–3, part 0xd03) |
| Android | **9 real (SDK 28)**. La interfaz dice 12/13: es falso | ADB |
| Versión del sistema | `UJC201-V1.1.04R1-240907_0428` (`ro.build.display.id`; "CC-C0-CYA" sin verificar) | ADB |
| RAM / almacenamiento | 4 GB / 64 GB | conversación (sin verificar; `ro.vendor.mtk_config_max_dram_size=0x180000000` = 6 GB máx. soportado, no lo instalado) |
| Pantalla | 1024x600, densidad 160; panel MIPI RS070WS163 | ADB (`wm size/density`); panel: conversación |
| MCU | `JCST_Q8_8T7-2024.08.09_12:59` | conversación (sin verificar) |
| DSP | "37534" | conversación (sin verificar) |
| GPS | GPS/AGPS/GLONASS/BeiDou; `gnss_service` corriendo, fix real | ADB (fix) / conversación (constelaciones) |
| Bluetooth | Bluedroid, A2DP sink + HFP client; versión 5.1 | ADB (perfiles) / conversación (5.1) |

## Hardware y sistema (de `getprop`, sesión 1)

| Prop | Valor |
|---|---|
| Modelo | `ro.product.model=UJC201_64`, fabricante `alps`, device `ac8257_demo` |
| SoC | MediaTek/AutoChips **AC8257** (`ro.board.platform=ac8257`, `Hardware: AC8257V/WAB`), 4× Cortex-A53 |
| Android **real** | **9 (Pie), SDK 28**. Dice "12" (`ro.build.version.release=12`) pero es falso |
| Fingerprint | Falso (`alps/full_evb3561sv_w_65_m0/...:6.0/MRA58K/...`); el boot dice `ac8257_demo:9/PPR1.180610.011` |
| Build | `user`, `ro.debuggable=1`, `ro.secure=1`, sin root |
| Arranque | `flash.locked=1`, `vbmeta.device_state=unlocked`, `verifiedbootstate=green`, `veritymode=disabled` |
| Pantalla | 1024x600, densidad 160 (1 dp = 1 px) |
| ADB | `service.adb.tcp.port=5555` (ADB por red siempre activo) |
| Opciones de desarrollador | "USB debugging" ON. No existe "Wireless debugging" (es de Android 11+) |

Volcados locales (no publicados, tienen datos del equipo) en `adb-dump/`: `getprop.txt`, `packages_all.txt`, `launcher_activities.txt`,
`home.txt`, `media_session.txt`, `bluetooth_manager.txt`, etc.

## Apps del sistema relevantes

| Paquete | Para qué | Activity de lanzamiento |
|---|---|---|
| `com.android.launcher3` (código `com.jancar.launcher`) | Launcher original JanCar, `/vendor/app/ivi-launcher` | `com.jancar.launcher.act.ActLauncher` |
| `com.jancar.services` | Servicio central (audio, media, radio, sistema, car, ACC…) | — |
| `com.autochips.carplayapp` | CarPlay con cable e inalámbrico | `.CarPlayActivity` |
| `com.google.android.projection.sink` | Android Auto | `.ui.AndroidAutoActivity` |
| `net.easyconn` | EasyConnection (espejado) | `.ui.Jac04MainActivity` |
| `com.jancar.radio` | Radio FM/AM | `.activity.RadioMainActivity` |
| `com.jancar.music` | Música local + BT | `.activity.MainActivity`, `.activity.BTMusicActivity` |
| `com.jancar.bluetooth` / `com.jancar.btservice` | Teléfono/BT | `.ui.MainActivity` |
| `com.jancar.settings` | Ajustes del auto | `.ui.activity.MainActivity` |
| `com.can.activity` | Canbus (ver [04-canbus.md](04-canbus.md)) | `com.can.ui.CanActivity` |
| `com.jancar.hd2cam` | Cámara de reversa | se abre sola con la reversa |
| `com.ex.dabplayer.pad`, `com.txz.radio` | DAB, radio por internet | |
| `com.txznet.*` | Asistente de voz, clima | |

Apps de terceros instaladas por Mati: `de.szalkowski.activitylauncher`, `org.fdroid.fdroid`
y `com.mati.a3launcher`, el id viejo de este launcher (ahora `ar.com.eclipta.launcher`).

## Menús del fabricante y Ajustes de Android ocultos

- En Ajustes del vendor solo están "About car device" y "Factory setup". Factory setup → PIN **1234** →
  Function Settings. Según la conversación, Mati activó **DAB, ALCOHOL, FAN y FM TRANSMIT** y se le
  recomendó volver a desactivarlos. **No se sabe si lo hizo** (sin verificar).
- Los Ajustes de Android están ocultos. Se llega con **Activity Launcher** (instalado desde F-Droid):
  Opciones de desarrollador activadas y **USB debugging ON**. No existe "Wireless debugging" porque es
  Android 9. Fotos de la pantalla; captura por ADB en
  [estereo-ajustes-original.png](../design/estereo-ajustes-original.png).

## Logo y animación de arranque

Detalle en [07-logo-arranque.md](07-logo-arranque.md).

No hay `bootanimation.zip`. El logo lo maneja el HAL de AutoChips:
`/system/bin/bootanimation`, `/system/bin/boot_logo_updater`,
`/vendor/bin/hw/vendor.autochips.hardware.metalogo@1.0-service` (`init.svc.metalogo_daemon` corriendo),
`libatclogo.so`, `libshowlogo.so`, `liblogo_jni.so`. Probablemente esté en una partición de logo.
No se tocó.

## Encendido y apagado (ACC)

- Al cortar el contacto, el estéreo hace **QuickBoot power off** (`QuickBootPowerOff done`, apaga el WiFi):
  ADB se corta.
- Al dar contacto, `com.jancar.services` manda `com.jancar.services.action.acc` (extra `acc=true`), a veces dos veces.
  También llegan `autochips.intent.action.PREQB_POWERON` y `QB_POWERON`.
  `settings get global accStatus` devuelve 1 o 0.
- En la sesión 2 el estéreo hizo un **reinicio completo** (cambiaron los PIDs) y el ACC igual llegó.
- Reconectar ADB tarda unos segundos después de que el estéreo vuelve al hotspot. Si dice `offline`:
  `adb disconnect` + `adb connect` de nuevo.

## Bluetooth

Teléfono vinculado: un iPhone. Nombre del estéreo: "Android Bluedroid". El BT se
conecta y desconecta solo con frecuencia (autolink).
