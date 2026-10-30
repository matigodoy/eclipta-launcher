# Cómo conectarse al estéreo (ADB por WiFi)

## Red

- Mac y estéreo conectados al **hotspot de un iPhone** (subred `172.20.10.0/28`, la que usa siempre el hotspot de iOS).
- Mac: `172.20.10.5` (en0). Estéreo: **`172.20.10.2`**, puerto **5555** (ADB por red siempre activo).
- Si la IP cambia, buscar el puerto en la /28:
  ```bash
  for i in $(seq 1 14); do nc -z -G 1 172.20.10.$i 5555 && echo open $i; done
  ```

> **Seguridad**: el ADB por red está **siempre abierto en el 5555 y autoriza sin preguntar**. Cualquiera
> en la misma red WiFi puede controlar el estéreo. **Nunca conectar el estéreo a un WiFi público.**

> **CarPlay inalámbrico no funciona** mientras el estéreo es cliente del hotspot (CarPlay inalámbrico
> usa el WiFi del estéreo). Durante las sesiones de ADB hay que usar **CarPlay con cable**.

## Conectar

```bash
adb connect 172.20.10.2:5555
adb -s 172.20.10.2:5555 get-state   # device
```

- adb: el de Android SDK platform-tools (probado con v37).
- La Mac quedó **autorizada**: no aparece el cartel "Permitir depuración USB".
- Shell: `uid=2000(shell)`, sin root. `/data/local` da "Permission denied" (es normal).
- zsh: usar una función, no una variable con espacios:
  ```bash
  A(){ adb -s 172.20.10.2:5555 "$@"; }
  A shell dumpsys activity activities | grep mResumedActivity
  ```
- Si queda `offline` (por ejemplo después de un apagado o encendido): `adb disconnect; adb connect 172.20.10.2:5555`.
- Al cortar el contacto, el estéreo apaga el WiFi y la conexión se pierde.

## Comandos útiles (solo lectura)

```bash
A exec-out screencap -p > adb-dump/captura.png                 # captura sin escribir en el equipo
A shell cat /sdcard/Android/data/com.mati.a3launcher/files/a3l-log.txt   # log del launcher
A shell dumpsys meminfo com.mati.a3launcher                     # memoria
A shell top -b -n 1 | grep -E "PID|a3launcher"                  # CPU
A shell dumpsys activity services com.jancar.services | grep -c a3launcher  # bindings activos
A shell dumpsys media_session
A shell dumpsys activity broadcasts history | grep act=
A shell logcat -d | grep JLOG                                   # logs de JanCar
A shell settings get global accStatus
A pull /vendor/app/<app>/<app>.apk apks/<pkg>/                  # copiar APK (lee, no modifica)
```

## Instalar o actualizar el launcher

```bash
A install -r -g A3Launcher-vX.Y.Z.apk   # bajado de Releases o compilado
```

Para desinstalarlo: Ajustes > Apps > A3 Launcher > Desinstalar, o `A uninstall com.mati.a3launcher`.
Para volver al launcher original sin desinstalar: Ajustes > Apps > A3 Launcher >
"Abrir de forma predeterminada" > Borrar valores, y al tocar Inicio elegir el original.

También se puede instalar con un pendrive: copiar la APK a la raíz, abrirla desde el administrador de
archivos del estéreo (la instalación manual funciona). Detalle en el [README](../README.md#instalar).

Volver a una versión anterior: `A install -r -d A3Launcher-v0.1.1.apk` (`-d` permite bajar el versionCode).

## Herramientas

- jadx 1.5.6 (`brew install jadx`). Descompilar con `jadx -j 8 -d decompiled/<pkg> apks/<pkg>/*.apk`
  (no usar `--no-res-deobf`, esa opción no existe).
- JDK 21 en `/Library/Java/JavaVirtualMachines/jdk-21.jdk` (también está Temurin 25, que no se usa).
- `gif2webp` / `img2webp` (brew webp).
- Un AVD (API 36) para probar: abrirlo con `-read-only`, `wm size 1024x600`, `wm density 160`.
  En API 36 aparece el aviso de "app para una versión vieja"; en el estéreo (Android 9) no.
- Python con Pillow en un venv para recortar la foto del auto.
