# API de JanCar (de las APKs descompiladas)

Fuente: `decompiled/com.jancar.services`, `decompiled/com.android.launcher3` (launcher JanCar),
`decompiled/com.jancar.radio`, `decompiled/com.jancar.music`, `decompiled/com.autochips.carplayapp`.

## Servicios de `com.jancar.services`

Todos son `exported="true"`, **sin permiso**, y no hay chequeos de quién llama (no se usa
`getCallingUid`, `checkCallingPermission`, `enforceCallingPermission` ni `checkSignatures`).
**Cualquier app puede hacer bind.** Bind con
`new Intent("com.jancar.services.action.<x>").setPackage("com.jancar.services")`.

| Acción | Clase | AIDL |
|---|---|---|
| `com.jancar.services.action.main` | MainService | |
| `...action.media` | media.MediaService | `com.jancar.services.media.IMedia` |
| `...action.radio` | radio.RadioService | `com.jancar.services.radio.IRadio` |
| `...action.system` | system.SystemService | `com.jancar.services.system.ISystem` |
| `...action.car` | car.CarService | (canbus / datos del auto) |
| `...action.audio`, `.avin`, `.voice`, `.dab`, `.cluster`, `.navigation` | | |
| `com.jancar.btservice.action.bluetooth` (paquete `com.jancar.btservice`) | | |

### IMedia (descriptor `com.jancar.services.media.IMedia`)

Códigos de transacción que se usan: `registerMediaInfoCallback`=5, `unRegisterMediaInfoCallback`=6,
`requestMediaInfoAndStateEvent`=22 (sin argumentos). La lista completa está en
`decompiled/com.android.launcher3/sources/com/jancar/services/media/IMedia.java`
(open=1, close=2, getActiveMedia=7, setMediaInfo=8, setMediaState=9, play=15, pause=16,
playPause=17, prev=18, next=19, seekTo=20…).

Callback `com.jancar.services.media.IMediaInfoCallback`:
- 1 `onMediaChange(int type, String name, String info, int artW, int artH, byte[] artRGBA, int index, int total, boolean popup)`
- 2 `onPlayStateChange(int type, int state, int position, int duration)`: state 3 = reproduciendo, 2 = pausa; ms.
- 3 `onMediaZoneChanged(int,int)`, 4 `onCurrentShownMediaTypeChanged(int)`

Tipos (`IVIMedia.Type`): MUSIC=0, RADIO=1, VIDEO=2, A2DP=7 (música BT), DAB=11, THIRD_PARTY=20, CARPLAY_MUSIC=25.

**Valores reales vistos en el auto (sesión 2):**
- Música local: `type=0 name="Beautiful_grassland_my_home.mp3" info=null art=0x0` (sin tags: viene el
  nombre del archivo, sin artista ni carátula). Estado: `type=0 state=2 pos=60016 dur=237897`.
- Radio: `type=1 name="FM 87.50"`, que el launcher muestra como 87.50 MHz.
- JanCar **reenvía la misma información cada 2 a 10 s** (hay que ignorar los duplicados).

### ISystem (descriptor `com.jancar.services.system.ISystem`)

`registerGpsLocationInfoListener`=22, `unregisterGpsLocationInfoListener`=23.
Callback `com.jancar.services.system.IGpsCallback`:
- 1 `onGpsLocationInfoChanged(String lon, String lat, float accuracy, double altitude, float speedKmh)`
- 2 `onGpsCountChanged(int,int,int,int)`

Real: `lon="-6x.xx…" lat="-3x.xx…" acc=0.9–2.7 alt≈4xx speed=0.0` (coordenadas ocultadas, auto quieto). Llega
aproximadamente cada 1 s. La velocidad está en km/h.

### ICar (descriptor `com.jancar.services.car.ICar`)

`...action.car`. `getHandbrakeStatus`=11, `getLightStatusMask`=13, `getHeadLightStatus`=14 (sin argumentos,
responde int). El launcher lee el 14 al conectarse y una vez por minuto mientras está visible, sin registrar
callback. **Sin verificar en el auto**: falta confirmar que el canbus lo alimente; por eso figura como experimental.

### IRadio (descriptor `com.jancar.services.radio.IRadio`)

Lectura: `getFreq`, `getBand`, `getPSText(int)`, `isStereo`, …; registro: `open(IRadioCallback, String)`.
**No usar `open()`**: puede tomar el control del sintonizador. El launcher usa la radio vía
`IMedia` (tipo 1), que alcanza. La interfaz `com.jancar.radio.IFmRadioService` de `FmService` es un
Binder local (`getService()`), así que **no sirve** desde otro proceso.
Unidades: frecuencia int en pasos de 10 kHz para FM (9870 = 98.70 MHz). Banda: AM=0, FM=1.
RDS PS: en la sesión 1 la radio no recibía el PS (señal débil, `getPs error -1013`).

## Broadcasts

Todos se mandan con `sendBroadcast` simple, sin permiso, no protegidos.

| Acción | Extras | Notas |
|---|---|---|
| `com.jancar.services.action.acc` | boolean `acc` | ACC on/off. **Verificado**: llega al dar contacto (a veces dos veces). También guarda `Settings.Global accStatus` |
| `autochips.intent.action.QB_POWERON` / `PREQB_POWERON` / `QB_POWEROFF` | — | QuickBoot de AutoChips (encendido y apagado rápido) |
| `com.jancar.services.action.key.event` | int `key_event_id`, int `key_event_type` (0 UP, 1 DOWN, 2 ONCE) | No se usa: el auto no tiene comandos al volante |
| `com.jancar.services.action.brake` | boolean | No se vio en el auto |
| `com.jancar.carplay_connected` / `_disconnect` | `status`, `phoneMode`=`carplay_wireless`/`carplay_wried` | Dirigidos solo a `com.jancar.services` y `com.can.activity`: otras apps **no** los reciben |
| `com.jancar.services.ready` | | Servicio listo |

## MediaSession

`SecondaryJanCarMediaSession` (de `com.jancar.services`) **nunca** llama `setMetadata` ni
`setPlaybackState`: no sirve para leer qué suena. `ThirdPartyMediaSession` hace lo contrario: lee las
MediaSession de apps de terceros y las pasa a JanCar.

## CarPlay

La metadata de CarPlay (título y artista) **no se publica en ningún lado**: ni MediaSession, ni broadcast, ni
provider, ni push con tipo 25. El stack iAP2 está en `com.autochips.carplay.CarplayManager`
(`/system/framework/autochips.jar`, que no se analizó). El launcher muestra "CarPlay /
Reproduciendo desde el iPhone" si llega el tipo 25.
