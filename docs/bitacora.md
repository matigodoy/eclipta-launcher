# Bitácora

## 24–25/10/2026: inicio

*Sin verificar por ADB.*
- Se relevan el auto y el estéreo (Podofo B0DL5NSBY6). Fotos de "About" (versión, MCU, panel, DSP).
- Se encontraron el Factory setup (PIN 1234) y los Ajustes de Android ocultos (vía Activity Launcher de
  F-Droid). Se activaron las opciones de desarrollador y el USB debugging.
- Diseño: propuesta "Virtual Cockpit" y después "liquid glass" en rojo (aprobado). Documento de factibilidad.

## 29/10/2026: sesión 1 en el auto (ADB por WiFi)

- Mac 172.20.10.5 y estéreo 172.20.10.2 en el hotspot. El 5555 estaba abierto y autorizó sin preguntar.
- Volcado de solo lectura en `adb-dump/`. **Android 9 real**, aunque diga 12.
- Radio: la frecuencia y el RDS no aparecen en las media sessions; con la señal débil falla el PS (`getPs -1013`).
- Se copiaron y descompilaron 5 APKs; análisis completo de la API de JanCar → [03-apis-jancar.md](03-apis-jancar.md).

## 29/10/2026: launcher v0.1 y v0.1.1 (Mac)

- Construido el A3 Launcher con las reglas de rendimiento. Probado en el emulador a 1024x600.
- La foto de remove.bg dio la v0.1.1.
- El auto **no tiene comandos al volante**; **sí tiene canbus** (foto: Simple Soft RP5-AD-001).

## 29–30/10/2026: sesión 2 en el auto

- Canbus: `com.can.activity` copiada y descompilada; protocolo `SpAudiProtocol` → [04-canbus.md](04-canbus.md).
  Pruebas: puerta (nada), freno de mano (nada en el log), reversa (cámara por cable y CAN 0,9 s después).
- **v0.1.1 instalada**. Funcionan música, radio 87.50, GPS, ciudad y la animación al dar contacto
  (21:07). Visible: 43 MB y 0 % de CPU, arranque en 1,45 s.
- El auto se apagó mientras se instalaba la v0.1.2: **quedó sin instalar**. Falta medir con CarPlay adelante.

## 31/10/2026: v0.2 a v0.4 (Mac, emulador)

- Pedido de Mati: desarrollar las próximas versiones sin preguntar. Estéreo no conectado: todo probado en un
  emulador API 36 a 1024x600, **sin** servicios JanCar. Por eso las releases salen como prerelease hasta probarlas en el auto.
- Foto del S3 recortada con Vision de macOS, patente real tapada, parabrisas oscurecido (reflejaba un cartel).

- v0.3: capas de pintura y llantas, temas y perfil. v0.4: otras pantallas, sin JanCar, idiomas. PSS visible de 40 a 20 MB en emulador.

## 31/10/2026: de A3 Launcher a Eclipta Launcher (Mac, emulador)

- **Cambio de nombre** a Eclipta Launcher, por dos motivos: bajar el riesgo de marca y preparar el proyecto para varios modelos.
  Id `ar.com.eclipta.launcher` (antes `com.mati.a3launcher`), repo `matigodoy/eclipta-launcher`, carpeta `EcliptaLauncher` en el pendrive
  (la vieja `A3Launcher` se sigue leyendo). El id nuevo es una instalación nueva: el A3 Launcher se desinstala a mano y los ajustes arrancan de cero.
- **Varios modelos**: los packs nombran su `brand`; el logo y la animación salen del pack o de `assets/brands/<key>/`, y sin ellos se ve la marca
  Eclipta. Una foto propia con nombre sirve para cualquier auto.
- **Ajustes** por secciones. **Mantenimiento** con nueve ítems y hasta 12 propios, solo por fecha, con aviso al arrancar.
  Acerca de con QR. El diseño "MMI clásico" pasa a mostrarse como "Clásico".
- `versionName` sigue en 0.4.0 y no se publicó nada. Probado solo en emulador: la cadena del despertar falta probarla en el auto ([05-launcher.md](05-launcher.md#cadena-del-despertar)).

## 02/11/2026: v0.5.0 publicada

- Mati probó la APK de prueba en el auto y dio el OK para publicar. Sale como **v0.5.0** (versionCode 7), release normal, no prerelease.
  La 0.4.0 nunca se publicó: todo lo de 0.2 a 0.4 llega al público con esta versión.
- Nuevo antes de publicar: **Sistema > Desinstalar**, que abre el desinstalador de Android. En estas unidades los Ajustes de
  Android vienen escondidos y no había otra forma fácil de volver al inicio original. Probado en emulador (abre el diálogo; Cancelar no toca nada).
- [Guía de instalación](instalar.md) para cualquiera, en castellano rioplatense, con cómo buscar y preparar la foto del auto.

## 02/11/2026: la v0.5.0 se cierra en Android 9; v0.5.1

- **Corrección**: la v0.5.0 se publicó creyendo que la APK de prueba se había probado en el auto. No era así: en el estéreo
  se abría y se cerraba. Todo se había probado en un emulador API 36.
- Con un emulador **Android 9 (API 28)** a 1024x600 se reprodujo al instante: `InflateException` en el layout del inicio,
  causada por `drawable/car_glow` (`<gradient> tag requires 'gradientRadius' attribute with radial type`). El radio estaba,
  pero Android 9 lo pierde al aplicar el tema cuando el gradiente radial usa un color `?attr`. Android 10 o más no tiene el problema.
- v0.5.1: el brillo se arma en código, `ResourcesTest` frena gradientes radiales con colores del tema, y desde ahora cada
  versión se prueba en el AVD Android 9 antes de publicar. En ese emulador pasaron todas las pantallas, diálogos, la animación,
  la alerta, la foto propia, exportar y el diálogo de desinstalar. Falta el auto (JanCar, CarPlay).
- En Android 9 los carteles del sistema salen en inglés ("Select a Home app", "Always"): la guía ahora da los dos nombres.

## 02/11/2026: v0.6.0, después de usar la v0.5.1 en el auto

- Mati usó la v0.5.1 en el auto. En la pantalla de 7", los botones de la barra lateral (50) eran chicos para abrir CarPlay
  manejando, y la barra de velocidad (0 a 200) casi no se movía a 60 km/h.
- v0.6.0: botones grandes por defecto (70, sin Inicio), barra de 0 a 130 sin el rótulo "GPS", el aviso de Importar packs en el
  idioma de la app y los modelos de Mi auto ordenados. Probada en el emulador Android 9 con 13 packs importados.
- "¿Sigue funcionando la animación?": sí, en el emulador. Una foto propia **con nombre** la saca a propósito (se toma como otro
  auto); sin nombre, la foto propia conserva el logo y la animación del modelo elegido.

## 03/11/2026: v0.7.0, APK sin marcas (Mac, emulador)

- Pedido de Mati: que todo lo de marca llegue por pack, para poder subir la app a Play Store sin sanciones.
- Salen del APK y del repo el A3, el S3, los aros, la animación, las capturas que los mostraban y las capas del configurador.
  Quedan en `private/` de Mati, con un pendrive listo (`private/pendrive-audi/EcliptaLauncher`: 15 packs + `brands/audi`).
  La historia de git todavía los tiene.
- El APK trae un auto dibujado sin marca (`scripts/make_generic_car.py`). La marca se puede importar aparte
  (`EcliptaLauncher/brands/audi/`) y la usan todos los packs de Audi. El APK bajó de 1,3 MB a 300 KB.
- Probada en emulador Android 9: encima de la 0.6.0 se ve el auto genérico sin aros; al importar el pendrive vuelven el A3 con
  los aros y la animación al contacto. Si el perfil no tenía modelo guardado, hay que elegirlo en Mi auto.

## 03/11/2026: v0.7.0 en el auto

- El estéreo tenía la 0.4.0 de Eclipta y la v0.1.1 como Inicio. La 0.7.0 se instaló encima de la 0.4.0 y Eclipta pasó a ser el Inicio.
- Anduvieron el auto genérico, el pendrive (15 packs + marca Audi), el teclado de Personalizar y la animación al dar contacto.
- 23,5 MB visible, 0 % de CPU atrás. Con la pantalla de espera de CarPlay (sin iPhone) el launcher no se detiene y sigue
  conectado a JanCar: queda medir con CarPlay conectado. Publicada como versión estable.

## Correcciones

- "El auto no tiene caja CAN": **falso**, tiene un Simple Soft RP5-AD-001.
- "Con el QuickBoot no se manda el ACC": **falso**. `com.jancar.services.action.acc` llega (acc=false/true
  al apagar y prender; acc=true dos veces al dar contacto). Solo que la conexión ADB ya se había cortado.
- "Android 12/13": falso, es Android 9 (SDK 28).
- "8 núcleos": falso, son 4 Cortex-A53.
- `com.jancar.radio.IFmRadioService`: parecía una API, pero es un Binder local que no sirve desde otra app.
