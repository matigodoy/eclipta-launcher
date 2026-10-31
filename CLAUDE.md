# Eclipta Launcher: guía para agentes (Claude Code y similares)

**Eclipta Launcher** (id `ar.com.eclipta.launcher`): launcher HOME liviano y documentación abierta para estéreos Android chinos con firmware **JanCar**
(placa UJC201 / AutoChips AC8257, **Android 9 real** aunque la interfaz diga 12/13). Proyecto
comunitario: ver [README.md](README.md) y [CONTRIBUTING.md](CONTRIBUTING.md). La documentación está en
español; los identificadores de código quedan en inglés.

Si tenés notas personales (IP del estéreo, nombre de tu hotspot, estado de tu auto), ponelas en
`CLAUDE.local.md`, que está en `.gitignore`.

## Reglas fijas

1. **Estéreo en solo lectura** salvo aprobación explícita del dueño para ese cambio puntual.
   Nunca flashear firmware ni MCU, nunca `adb root` ni `remount`, nunca tocar la configuración del CAN ni
   los ajustes de fábrica, nunca `pm uninstall/disable`, `settings put`, `setprop`, `reboot` ni escribir en
   /sdcard. Capturas solo con `adb exec-out screencap -p > archivo.png`.
2. **Rendimiento primero**: el launcher no puede sacarle recursos a CarPlay / Android Auto. Nada de
   servicios en segundo plano, wakelocks ni polling. Bind solo entre `onStart` y `onStop`. Sin AndroidX
   ni dependencias externas, visuales estáticos. Medir cada versión con `dumpsys meminfo` y `top` (docs/05).
3. **Solo datos offline** (sin clima, mapas ni nada que dependa de internet).
4. **Nada personal ni de terceros en git**: sin MACs, seriales, IMEI, nombres de Wi-Fi/BT, coordenadas
   GPS, patentes ni EXIF; sin APKs, firmware ni código descompilado de fabricantes; nunca el keystore.
5. zsh: `$path` es el PATH (no usarlo como variable) y `X="adb -s …"; $X` no separa palabras.
   Usar una función: `A(){ adb -s <ip>:5555 "$@"; }`.

## Mapa

| Ruta | Contenido |
|---|---|
| `docs/01-estereo-hardware.md` | Hardware, firmware, props, apps del sistema y menús ocultos |
| `docs/02-conexion-adb.md` | Conexión ADB por Wi-Fi, comandos útiles e instalación |
| `docs/03-apis-jancar.md` | Servicios, AIDL, códigos de transacción, broadcasts y valores reales |
| `docs/04-canbus.md` | Decodificador Simple Soft, protocolo SpAudiProtocol, pruebas en vivo |
| `docs/05-launcher.md` | El launcher: build, código, versiones, rendimiento y pendientes |
| `docs/06-diseno.md` | Decisiones de diseño, medidas y estilo |
| `docs/07-logo-arranque.md` | Logo de arranque en frío (AutoChips metalogo) |
| `docs/08-packs.md`, `docs/configurador/` | Formato de packs (marca, logo y animación) y perfil; configurador web estático |
| `docs/vehiculos/` | Datos de otros autos / unidades aportados por la comunidad (con plantilla) |
| `docs/bitacora.md` | Línea de tiempo y correcciones |
| `design/` | Capturas, foto del auto y animación (ver `design/README.md`) |
| `launcher/` | Proyecto Android (Gradle). `launcher/private-res/` = overrides locales, no versionados |
| `scripts/release.sh` | Build firmado + GitHub Release (`PRERELEASE=1` si no se probó en el auto) |
| `scripts/measure.sh`, `make_pack.py`, `make_wheels.py`, `validate_packs.py`, `make_qr.py` | Medir consumo; crear y validar packs; generar los QR de Acerca de (necesita `segno`) |
| `apks/`, `decompiled/`, `private/` | Locales, **no versionados** (material propietario o personal) |

## Build

```bash
cd launcher
./gradlew --no-daemon -q assembleDebug   # JDK 21
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Release firmado: `scripts/release.sh X.Y.Z` (necesita `launcher/keystore.properties`, ver el script).

## Regenerar `apks/` y `decompiled/` para investigar (solo local)

Con el estéreo conectado, en solo lectura:

```bash
A(){ adb -s <ip>:5555 "$@"; }
for p in com.jancar.radio com.android.launcher3 com.jancar.services com.jancar.music com.autochips.carplayapp com.can.activity; do
  for ap in $(A shell pm path $p | sed 's/package://' | tr -d '\r'); do mkdir -p apks/$p; A pull "$ap" apks/$p/; done
done
for p in apks/*/; do n=$(basename $p); jadx -j 8 -d decompiled/$n $p*.apk; done   # brew install jadx
```
