# Logo de arranque en frío

**Estado: sin tocar.** La animación al **despertar** (ACC on, el caso más común porque el estéreo usa
QuickBoot) **ya la resuelve el launcher** (SplashActivity). Este documento es sobre el logo de un
arranque en frío (encendido completo).

## Lo que se encontró (solo lectura, sesión 1)

- **No hay `bootanimation.zip`** en `/system/media`, `/product/media`, `/oem` ni `/vendor/media`
  (`/system/media` solo tiene `audio/`).
- El logo lo maneja el HAL de AutoChips **metalogo**:
  - `/vendor/bin/hw/vendor.autochips.hardware.metalogo@1.0-service` (`init.svc.metalogo_daemon=running`)
  - `/vendor/etc/init/vendor.autochips.hardware.metalogo@1.0-service.rc`
  - `/system/bin/bootanimation`, `/system/etc/init/bootanim.rc`
  - `/system/bin/boot_logo_updater`, `/system/etc/init/bootlogoupdater.rc`
  - Librerías: `libatclogo.so` (vendor), `libshowlogo.so`, `liblogo_jni.so` (system)
- Lo más probable es que el logo esté en una **partición de logo** y no en un archivo.
- En el Factory setup no se encontró ninguna opción de logo (según la conversación).

## Ideas para investigar (solo lectura)

- Ver la app de ajustes de AutoChips `com.android.settings/com.autochips.atcsettings.mainui.MainActivity`
  (descompilar `com.android.settings` y buscar "logo").
- Leer `bootlogoupdater.rc` y `metalogo@1.0-service.rc` (`adb shell cat`) para ver cómo se invoca
  `boot_logo_updater` y desde dónde lee la imagen.
- Buscar en `liblogo_jni.so` qué app lo usa (quizás un selector de logos del vendor).

**No escribir nada** (ni particiones ni archivos) sin el OK explícito del dueño. Flashear un logo mal
puede dejar el estéreo sin arrancar.
