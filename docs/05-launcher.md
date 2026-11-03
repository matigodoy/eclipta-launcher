# Eclipta Launcher

App de inicio (HOME) propia, id `ar.com.eclipta.launcher`, etiqueta "Eclipta Launcher". Antes se llamaba
A3 Launcher (`com.mati.a3launcher`): el motivo está en la [bitácora](bitacora.md). Las filas viejas de abajo quedan como están.
Proyecto: `launcher/`. APKs: en [Releases](../../../releases). Instalación: ver la [guía](instalar.md).

## Versiones

| Versión | versionCode | Cambios | Estado |
|---|---|---|---|
| 0.1 | 1 | Primera versión (foto recortada a mano) | Reemplazada |
| 0.1.1 | 2 | Foto del auto sin fondo, de remove.bg | **Instalada en el estéreo** |
| 0.1.2 | 3 | La animación también se dispara con `QB_POWERON`; el log solo guarda cambios de media; el nombre de archivo se limpia ("a_b.mp3" pasa a "a b") | Publicada |
| 0.2.0 | 4 | Personalizar (saludo, patente, modelo A3/S3 8P, carrocería, motor, año, foto propia), CarPlay al contacto, animación opcional, barra lateral configurable. Release sin log ni permiso de internet y con R8 | Incluida en 0.4.0 |
| 0.3.0 | 5 | Color y llantas por capas, patente sobre la foto, sombra; temas glass y MMI clásico, acentos, modo noche (siempre, atardecer, luces); aviso de velocidad; recordatorios; exportar e importar perfil y packs por pendrive; configurador web | Incluida en 0.4.0 |
| 0.4.0 | 6 | Se adapta a otras pantallas apaisadas; funciona sin servicios JanCar (oculta la radio, GPS estándar, elegís las apps); inglés y portugués; exportar diagnóstico; menos memoria: sin doble GPS en JanCar, reloj interno más lento en reposo, libera bitmaps con CarPlay adelante | No se publicó; incluida en 0.5.0 |
| 0.5.0 | 7 | Nombre e id nuevos (instalación nueva, ajustes de cero, APK `EcliptaLauncher-vX.Y.Z.apk`, carpeta `EcliptaLauncher` en el pendrive, log `launcher-log.txt`); packs con `brand`, logo y animación afuera del código; foto propia con nombre para cualquier auto; ajustes por secciones; mantenimiento con aviso al arrancar; la app de contacto puede ser cualquiera (`accApp`); Acerca de con dos QR; Sistema > Desinstalar (abre el desinstalador de Android, porque los Ajustes vienen escondidos) | Publicada. **Se cierra al abrir en Android 9**: usar 0.5.1 |
| 0.5.1 | 8 | Arregla el cierre al abrir en Android 9: el brillo detrás del auto era un gradiente radial con color del tema (`?attr`), que Android 9 no sabe aplicar; ahora se arma en código (`Theming.carGlow`). Test `ResourcesTest` para que no vuelva. Guía con los carteles de Android en inglés | Publicada. Mati la usó en el auto: anda; de ahí salieron los cambios de 0.6.0 |
| 0.6.0 | 9 | El aviso de Importar packs sale en el idioma de la app (antes, en el del sistema); Mi auto ordena los modelos: primero los de la app y después los importados, por nombre. Botones grandes en la barra lateral (70dp, sin Inicio; opción en Barra lateral, activada por defecto; perfil `bigRail`); barra de velocidad de 0 a 130 y sin el rótulo "GPS" | Publicada. Probada en emulador Android 9 con 13 packs importados, 60 y 117 km/h simulados y la animación al contacto; en el auto, solo la base (0.5.1) |
| 0.7.0 | 10 | **APK sin marcas**, para poder publicarlo en tiendas: salen el A3, el S3, los aros y la animación (`assets/brands`, `assets/packs/audi-*`). Trae un auto dibujado sin marca (`generico`, `scripts/make_generic_car.py`) con 9 colores y las 6 llantas. Logo y animación solo desde packs importados, o desde una marca importada (`EcliptaLauncher/brands/<key>/`, compartida por sus packs). Títulos y nombres de colores y llantas por idioma (`title_en`, `name_pt`, etc.). Sin listas de carrocería o motor, Mi auto las pide como texto. Al importar packs se rehace el auto; al actualizar la app también (la foto horneada podía ser de un pack que ya no está). El teclado se cierra al terminar de escribir: antes quedaba abierto sobre Personalizar (`stateAlwaysHidden`). APK de 300 KB | Sin publicar. Probada en emulador Android 9: actualización desde 0.6.0, instalación limpia, inglés, importar el pendrive Audi (15 packs + marca: aros y animación vuelven), cerrar los diálogos de texto con Aceptar, Cancelar, ✓ y Atrás |

## Build

- Gradle 8.13 (wrapper), AGP 8.12.0, **JDK 21**, compileSdk 36, **minSdk 28, targetSdk 25**
  (a propósito: con target < 26 los receivers del manifest siguen recibiendo los broadcasts
  implícitos de JanCar; la app se instala a mano, no va a Play Store).
- Java puro, **sin AndroidX ni librerías externas**. Firma debug (v2).
- Tamaño de la APK: unos 1,2 MB (la animación WebP ocupa 967 KB; cada pack, unos 40 KB).
- **Probar siempre en Android 9 antes de publicar**: AVD con `system-images;android-28;default;arm64-v8a`, 1024x600 y
  densidad 160. Un emulador nuevo (API 36) no reproduce fallas de Android 9: la v0.5.0 andaba en API 36 y se cerraba
  al abrir en el estéreo. Gradientes radiales: nunca con colores `?attr` en XML (lo controla `ResourcesTest`).

## Código (`launcher/app/src/main/java/ar/com/eclipta/launcher/`)

| Archivo | Qué hace |
|---|---|
| `MainActivity` | HOME. Header (reloj, saludo, fecha, ciudad), barra lateral, tarjeta del auto + velocidad, "Ahora suena", radio. Todo se engancha en `onStart` y se suelta en `onStop` |
| `JanCar` | Clientes Binder escritos a mano para IMedia/IMediaInfoCallback e ISystem/IGpsCallback (mismos descriptores y códigos que el SDK) |
| `AppsActivity` | Cajón de apps, 6 columnas, carga en otro hilo |
| `SplashActivity` | Animación del despertar, la de la marca (`Brand.sting`): una sola vez, se corta a los 4 s, se cierra sola, la salteás tocando, `taskAffinity=""` |
| `AccReceiver` | Receiver del manifest para `com.jancar.services.action.acc` (y `QB_POWERON` desde la v0.1.2); antirrebote de 20 s; le pasa el despertar a `Startup` |
| `Startup` | Cadena del despertar: animación, aviso de mantenimiento y app elegida (ver [abajo](#cadena-del-despertar)) |
| `MaintenanceAlertActivity` | Aviso al arrancar con lo vencido; se cierra sola a los 30 s |
| `Profile` | Preferencias del usuario (SharedPreferences) |
| `Pack` | Pack de vehículo: `pack.json` + capas, en `assets/packs/` (solo `generico`) o `files/packs/`. Se lee solo cuando hace falta, con los nombres en el idioma de la app |
| `Brand` | Logo y animación del pack: carpeta del pack importado, si no la marca importada (`files/brands/<key>/`), si no la marca Eclipta y sin animación. Ni el código ni el APK traen marcas |
| `CarImage` | Hornea las capas en un solo PNG (`files/car_baked.png`) y carga lo que muestra el inicio |
| `CarBaker` | Rehornea el auto en un hilo que existe solo mientras hay trabajo; junta pedidos y relee el perfil en cada pasada |
| `SettingsActivity` | Índice de secciones (Personalizar), cada una es una página de esta activity; solo vive mientras está abierta. No se abre con el auto en movimiento |
| `SettingsSection`, `SettingsForm`, `FlowLayout` | Base de una página; filas que explican cada opción, con el texto en un diálogo; chips que bajan de línea |
| `CarSection`, `LookSection`, `RailSection` | Mi auto, Apariencia y Barra lateral: el auto al lado de las opciones |
| `MaintenanceSection`, `DriveSection`, `SystemSection`, `AboutSection` | Mantenimiento, Contacto y manejo, Sistema (con Desinstalar) y Acerca de: pantalla completa |
| `BaseActivity`, `Theming`, `Sun` | Paleta del usuario antes de crear vistas; modo noche con cálculo solar offline |
| `Reminders` | Fechas de mantenimiento: catálogo, repetición, JSON y guardado aparte del perfil (con tests en `app/src/test`) |
| `PaintMath`, `CityName` | Cuentas puras: tinte de la pintura; qué muestra la pastilla de ciudad (saltea códigos postales y el departamento "Capital") |
| `Launch`, `Files`, `PlateFormat` | Abrir apps y resolver accesos; buscar archivos en pendrive; formato de patente |
| `GlowBackground` | Fondo estático: #060708 + 3 óvalos con degradado radial |
| `DebugLog` | Log a `/sdcard/Android/data/ar.com.eclipta.launcher/files/launcher-log.txt`, como máximo 200 líneas por proceso; solo en builds debug (`ENABLED = BuildConfig.DEBUG`) |

Layouts de los ajustes: `settings_hub` (índice), `settings_split` (auto al lado), `settings_full` y `settings_about`.

## Cadena del despertar

`AccReceiver` avisa una vez por contacto y `Startup` hace el resto: animación, aviso de mantenimiento y, al final, la app
elegida (`accApp`). Corre desde el broadcast o con el inicio visible; no queda nada en segundo plano.

Se diseñó alrededor de lo que hace la unidad de referencia:

- **Inicio se reinicia** cerca del apagado y unos 190 ms antes del broadcast de despertar. Por eso `MainActivity` espera 3 s
  visible antes de mirar el aviso, y `Startup` no lo repite dentro de los 2 minutos.
- **En un arranque real el reloj empieza en la fecha de compilación del firmware** y salta segundos después. Mientras la fecha no
  supere por un día a la del firmware, el aviso queda pendiente, para no calcular vencimientos con un reloj falso.
- **CarPlay puede arrancar solo** cuando se conecta el teléfono, y dejaría el aviso como un parpadeo imposible de contestar. Por eso
  vuelve una vez por encima; si lo vuelven a tapar, se cierra. La tecla Inicio también lo hace volver una vez, porque Android
  informa lo mismo para cualquier app que arranca y no se pueden distinguir.

> **Nada de este flujo se probó todavía en el estéreo real, solo en el emulador.**

## Mantenimiento

Nueve ítems de catálogo (VTV/ITV, seguro, service, aceite y filtros, correa de distribución, frenos, neumáticos, batería, matafuego)
y hasta 12 propios. **Solo fechas**, porque el auto no informa los kilómetros. Cada ítem tiene repetición en meses, nota y si avisa
o no. La fecha más cercana se guarda aparte, así el caso común ni lee la lista. Sin alarmas: se mira al abrir el inicio o al despertar.

El aviso lista lo que vence hoy o ya venció y vuelve en cada contacto hasta que lo marques como hecho:

- **Ahora no**, Atrás o 30 s sin tocar nada lo cierran hasta el próximo arranque.
- **Ya lo hice** calcula la próxima fecha desde hoy (o ninguna, si no se repite) y se puede deshacer.
- Si se evalúa desde el inicio con el auto en movimiento, queda pendiente.
- Al cerrarse sigue con la app elegida, si venía de la cadena del despertar y no hay otra app adelante.

## Diseño ("liquid glass")

![Home](../design/preview-home.png)

- Fondo `#060708` con brillos rojos `#E3263A` y gris `#8A93A3`. Paneles de vidrio: relleno `#0DFFFFFF`, borde
  1dp `#1FFFFFFF`, radio 24dp y línea de brillo arriba `#29FFFFFF`. Acento `#E3263A`. Texto `#F1F2F4` / `#A3A9B1`.
- Fuentes del sistema `sans-serif-light` / `sans-serif-medium` (Inter quedó afuera porque supera los 300 KB).
- Medidas en dp a 1024x600: padding 14/16, header de 56, barra lateral de 56, tarjeta del auto de 520, columna derecha con
  el resto; separaciones de 12.
- Header: logo de marca (del pack, ver `Brand`), hora `HH:mm` 34sp, "Buen día/Buenas tardes/Buenas noches" + nombre opcional (`greeting_name`),
  fecha en español, pastilla con la ciudad (Geocoder, cacheada, se oculta si no hay).
- Barra lateral: Inicio (rojo), CarPlay y Android Auto con **íconos reales** de las apps, Radio, Música; abajo
  Apps y Ajustes.
- Tarjeta del auto: título del modelo ("Mi auto" en el genérico), "Carrocería · motor · año", foto 440dp con brillo rojo y línea de piso,
  panel de velocidad 60sp + barra 0–130 + "Sin señal GPS" si no hay fix por más de 10 s.
- "Ahora suena": carátula de 84dp (o un degradado con nota musical), título, artista, progreso,
  anterior / play-pausa / siguiente (`AudioManager.dispatchMediaKeyEvent`).
- Radio: frecuencia 44sp + MHz/kHz + banda; se guarda la última; los botones de búsqueda mandan
  next/prev si la radio es la fuente actual y, si no, abren la app de radio.

## Reglas de rendimiento (pedido de Mati: no afectar a CarPlay)

1. Nada en segundo plano: sin Service, WakeLock, alarmas ni jobs. Lo único fijo es el receiver del
   manifest (antirrebote + `startActivity`).
2. Bind de JanCar, GPS, reloj (`ACTION_TIME_TICK`), ticker de 1 s, receiver dinámico y Geocoder (cada
   10 min como mínimo): **solo entre `onStart` y `onStop`**.
3. Sin AndroidX ni librerías externas.
4. Visuales estáticos, sin blur ni animaciones; el fondo es el de la ventana; la foto está pre-escalada a 460 px;
   la carátula se escala a 84 px y la anterior se libera con recycle.
5. La animación se decodifica solo al abrir el splash y se libera en `onDestroy`; el splash se cierra en `onStop`.
6. Log en un hilo, con tope de líneas.
7. Los callbacks del Binder publican lo mínimo en el hilo principal y no redibujan si el valor no cambió.

## Mediciones en emulador (v0.4.0, API 36, 1024x600, sin JanCar)

Con `scripts/measure.sh`. **Orientativas**: falta repetirlas en el estéreo y con CarPlay real. La v0.1.2 medida era la build
debug guardada, así que parte de la mejora viene de pasar a release con R8.

| | v0.1.2 | v0.4.0 |
|---|---|---|
| PSS visible | 40 MB | 20 MB |
| PSS con otra app adelante | 46 MB | 27 MB |
| CPU en reposo (visible y en segundo plano) | 0,0 % | 0,0 % |
| Conexiones a JanCar en segundo plano | 0 | 0 |
| APK | 1,16 MB | 1,2 MB |

En el emulador Android 9 (v0.5.1, Clásico, noche, foto propia) `dumpsys meminfo` da 40–55 MB de PSS total, casi todo
heap nativo. No se compara con la tabla de arriba: cambian el sistema y el render. Vale la medición en el estéreo.

## Sin JanCar

`JanCar.available()` decide: si no está, no hay radio ni "qué suena" (hace falta acceso a notificaciones, que no se pide), se usa
el GPS estándar y un acceso cuya app falta abre el selector. Sin señal de contacto no hay animación ni app automática; el aviso de
mantenimiento sale igual, porque `Startup.noteBoot` detecta los arranques reales. Las pantallas verticales no están soportadas.

## Resultados en el estéreo (sesión 2, v0.1.1)

- Instalación OK (`versionCode=2 minSdk=28 targetSdk=25`). Mati eligió "A3 Launcher" como Inicio, "Siempre".
- Arranque: `Displayed … +1s450ms`. Sin crashes.
- `bind media -> true`, `bind system -> true`. Música, radio (87.50 FM), GPS y ciudad **funcionan**.
- Reversa: la cámara se abre por encima del launcher y al sacarla vuelve sin problemas.
- ACC (21:07): al dar contacto llegaron `acc=true` (×2) y `QB_POWERON`; **la animación se mostró** y se cerró sola.
- Memoria con el launcher **visible**: PSS total 43,4 MB (Java 7,5 MB, Native 17,6 MB, gráficos 10,5 MB), CPU 0,0 %.
- **Pendiente**: medir con CarPlay o la radio adelante (que los bindings bajen a 0 y la CPU quede en 0).

## Assets

- Auto: solo `assets/packs/generico`, dibujado por `scripts/make_generic_car.py` (vista lateral, 640x195, máscara de pintura
  y `pack.json` con nombres en tres idiomas). Los modelos reales, sus logos y animaciones llegan en packs ([packs](08-packs.md)).
  `ResourcesTest.noBrandsInTheApk` falla si vuelve `assets/brands` o un logo o animación en `assets/packs`.
- Una animación de marca para un pack: WebP animado de hasta 1024x600 y 2 MB, por ejemplo con `gif2webp -q 80 -m 4 -mixed`.
- QR de Acerca de: `scripts/make_qr.py` (necesita `segno`) genera `res/drawable/qr_portfolio.xml` (https://www.matigodoy.com.ar/)
  y `qr_github.xml` (el repo). Se generan acá, nunca en el estéreo.
- Log de diagnóstico: `/sdcard/Android/data/ar.com.eclipta.launcher/files/launcher-log.txt`, solo en builds debug.

## Pendientes conocidos

- Instalar la última versión: id nuevo y firma de release, así que hay que desinstalar a mano el A3 Launcher v0.1.1, y los ajustes arrancan de cero.
- Probar en el estéreo la cadena del despertar y el aviso de mantenimiento.
- El selector de fecha muestra los meses en inglés: usa el idioma del sistema (en-US en estas unidades), no el de la app.
- Medir con CarPlay adelante (**todavía no se midió en segundo plano**).
- Repetir las mediciones de arriba: la tabla es anterior a estos cambios.
- ~~Decidir si sacar los aros y la animación del público~~: hecho en 0.7.0, el APK no trae marcas.
- ~~Packs genéricos sin foto~~: hecho, el auto dibujado.
- Nombre de la emisora (RDS) y presets vía IRadio, sin usar `open()`. No se hizo: no se puede probar sin el auto y tocar la radio a ciegas es riesgoso.
- Confirmar en el auto si `getHeadLightStatus` cambia con las luces (modo noche experimental).
- Packs de A3 8V y A4 B8: faltan fotos propias (son para pendrive, no van en el APK).
- La temperatura exterior **no se puede** obtener con este protocolo CAN.
- ~~Nombre neutro antes de sumar más modelos~~: hecho, Eclipta Launcher.

## Ideas para próximas versiones

- Sombra suave debajo del auto (la foto de remove.bg no la tiene).
- Presets de radio, nombre RDS (`IRadioCallback.onRdsPsChanged`, sin `open()`), clima.
- Datos del canbus (freno de mano, luces) si se encuentra cómo leerlos.
- Comprobar si con CarPlay llega `onMediaChange` tipo 25.
