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

## 31/10/2026: v0.2 (Mac, emulador)

- Pedido de Mati: desarrollar las próximas versiones sin preguntar. Estéreo no conectado: todo probado en un
  emulador API 36 a 1024x600, **sin** servicios JanCar. Por eso las releases salen como prerelease hasta probarlas en el auto.
- Foto del S3 recortada con Vision de macOS, patente real tapada, parabrisas oscurecido (reflejaba un cartel).

## Correcciones

- "El auto no tiene caja CAN": **falso**, tiene un Simple Soft RP5-AD-001.
- "Con el QuickBoot no se manda el ACC": **falso**. `com.jancar.services.action.acc` llega (acc=false/true
  al apagar y prender; acc=true dos veces al dar contacto). Solo que la conexión ADB ya se había cortado.
- "Android 12/13": falso, es Android 9 (SDK 28).
- "8 núcleos": falso, son 4 Cortex-A53.
- `com.jancar.radio.IFmRadioService`: parecía una API, pero es un Binder local que no sirve desde otra app.
