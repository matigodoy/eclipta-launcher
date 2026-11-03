# Cómo contribuir

¡Gracias por sumarte! Este proyecto documenta estéreos Android chinos (JanCar / AutoChips y
similares) y mantiene **Eclipta Launcher**, una pantalla de inicio liviana pensada para no quitarle
recursos a CarPlay / Android Auto.

## Reglas de oro

1. **Seguridad del equipo primero.** Toda investigación se hace en **solo lectura**: nada de
   flashear firmware o MCU, `adb root`, `remount`, `setprop`, `settings put`, `pm uninstall/disable`
   ni tocar la configuración del CAN. Si un aporte requiere escribir en el equipo, decilo claramente
   en el PR y explicá cómo volver atrás.
2. **Rendimiento primero.** El launcher no puede correr nada en segundo plano: sin servicios,
   wakelocks ni polling. Los binds van entre `onStart` y `onStop`. Java puro, sin AndroidX ni
   dependencias externas. Si tu cambio toca el launcher, incluí `dumpsys meminfo` y `top` antes y
   después (ver [docs/05-launcher.md](docs/05-launcher.md)).
3. **Solo datos offline.** Nada que dependa de internet (clima, mapas, etc.).
4. **Sin datos personales.** Antes de subir dumps, logs o capturas, sacá MACs, seriales, IMEI,
   `ro.serialno`, nombres de Wi-Fi/Bluetooth, coordenadas GPS, patentes y metadatos EXIF
   (`exiftool -all= foto.jpg`).
5. **Sin material de terceros.** No subas APKs, firmware ni código descompilado de fabricantes
   (JanCar, AutoChips, Simple Soft, etc.), ni logos o animaciones de marcas. Describí el
   comportamiento con tus palabras y citá clase / método como referencia. Tampoco fotos de autos de
   marca: el APK trae solo el auto genérico dibujado y los modelos reales se cargan por pendrive
   ([packs](docs/08-packs.md)). `scripts/validate_packs.py` rechaza `logo.webp` y `sting.webp` dentro
   de `assets/packs`, y `ResourcesTest` falla si vuelve `assets/brands`.

## Aportar datos de otro auto o de otra unidad

Es el aporte más valioso. Creá un archivo en `docs/vehiculos/` (o en `docs/unidades/` si es
un estéreo distinto) siguiendo [la plantilla](docs/vehiculos/_plantilla.md), con:

- Auto (marca, modelo, año, motor) y estéreo (marca, placa `ro.product.model`, SoC,
  `ro.build.display.id`, versión de Android real).
- Decodificador CAN: marca, modelo, versión de software, foto de la etiqueta (sin datos
  personales).
- Props `persist.sys.can_*` y la clase de protocolo que aparece en logcat.
- Qué datos llegan (ACC, luces, reversa, velocidad, puertas, temperatura, teclas del volante…),
  con el byte / bit cuando lo sepas, y **cómo lo verificaste** (ADB, logcat, prueba en vivo).

Marcá cada dato como **verificado** (lo viste por ADB) o **sin verificar** (manual, publicidad,
foro). Esa distinción es lo que hace útil la documentación.

## Código

- Build: ver el [README](README.md#compilar).
- Estilo: el del código existente (Java 8, sin lambdas que requieran desugaring, comentarios
  en inglés cortos, textos de la UI en `strings.xml`).
- Un PR por cambio, con descripción de qué probaste y en qué unidad.
- Si agregás soporte para otra unidad, mantené el comportamiento actual de JanCar intacto y
  aislá lo nuevo detrás de una detección explícita.

## Reportar problemas

Abrí un issue con: unidad (placa y firmware), versión del launcher, qué esperabas, qué pasó y,
si usás una build debug (las release no guardan log), el log de
`/sdcard/Android/data/ar.com.eclipta.launcher/files/launcher-log.txt` (revisalo antes de adjuntarlo:
puede tener coordenadas GPS).

## Licencia de los aportes

Al contribuir aceptás que tu código se publique bajo la licencia del proyecto (ver
[LICENSE](LICENSE)) y tu documentación bajo la licencia de `docs/` (ver
[docs/LICENSE](docs/LICENSE)).
