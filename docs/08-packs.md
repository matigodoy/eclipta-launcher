# Packs de vehículo

Un solo APK para todos: lo que cambia por auto vive en una carpeta. Se puede sumar un modelo sin tocar Java.

El APK trae un solo pack, `generico`: un auto dibujado sin marca ([`scripts/make_generic_car.py`](../scripts/make_generic_car.py)).
**Los modelos reales, sus logos y sus animaciones no van en el APK ni en el repo**: las fotos y las marcas tienen dueño, y así la app
se puede publicar en tiendas. Cada uno los carga desde su pendrive.

```
EcliptaLauncher/packs/<id>/      en el pendrive (en el APK, solo assets/packs/generico)
  pack.json    marca, nombre, carrocerías, motores, colores, llantas, ubicación de ruedas y patente
  base.webp    foto sin fondo, 640 px de ancho
  paint.webp   máscara: blanco = pintura
  logo.webp    opcional: logo de la marca
  sting.webp   opcional: animación al despertar
EcliptaLauncher/brands/<key>/    opcional: logo y animación que comparten todos los packs de esa marca
  logo.webp
  sting.webp
```

## Marca, logo y animación

`pack.json` lleva `brand`, el nombre de la marca para mostrar (`[A-Za-z0-9][A-Za-z0-9 -]{0,23}`); vacío es "sin marca", como el
auto genérico. `scripts/validate_packs.py` exige la clave.

El launcher busca el logo y la animación en este orden:

1. La carpeta del pack importado: `logo.webp` y `sting.webp`.
2. La marca importada: `brands/<key>/logo.webp` y `sting.webp`, donde `<key>` es `brand` en minúsculas y con guiones
   ("Alfa Romeo" es `alfa-romeo`). Sirve para no repetir la animación (hasta 2 MB) en cada pack de la misma marca.
3. Si no hay, la marca Eclipta y sin animación.

`validate_packs.py` rechaza `logo.webp` y `sting.webp` en `assets/packs`, y `ResourcesTest` falla si vuelve a aparecer
`assets/brands` ([CONTRIBUTING](../CONTRIBUTING.md), regla 5).

Una foto propia con nombre de auto (`title`) saca el logo, la animación y los datos por defecto del subtítulo del pack: así sirve cualquier auto.

## Nombres en otros idiomas

`title` y el `name` de cada color y llanta pueden traer una versión por idioma: `title_en`, `title_pt`, `name_en`, `name_pt`.
Sin ella se usa la clave sin sufijo. Si `bodies` o `engines` vienen vacíos, Mi auto los pide como texto libre.

## Crear uno

1. **Foto propia**, de 3/4 de frente, entera y con buena luz. Mejor un auto claro: se tiñe mucho mejor que uno negro.
2. Sacale el fondo (en Mac, Vision o "Quitar fondo"; si no, remove.bg).
3. Copiá `scripts/packs/ejemplo.json` como `scripts/packs/<id>.json` y marcá los polígonos, medidos en `ref_width` píxeles:
   `exclude` (vidrios, ópticas, parrilla, llantas), `blank` (**siempre** la patente real), `darken` (vidrios con reflejos de otras cosas).
4. `python3 scripts/make_pack.py foto.png scripts/packs/<id>.json <pendrive>/EcliptaLauncher/packs/<id>` (necesita Pillow y numpy).
5. Escribí `pack.json` tomando otro de ejemplo, con su `brand`. `wheel_centers` son centro, radios y giro de cada llanta; `plate`, las cuatro esquinas de la patente.
   `paint.gain`, `gamma` y `spec` ajustan el tinte: un auto negro necesita `gain` cercano a 2, uno blanco cercano a 1.
6. `python3 scripts/validate_packs.py <pendrive>/EcliptaLauncher/packs` e importalo.

`scripts/packs/*.json` es solo el formato de las máscaras: no subas fotos ni packs de modelos reales al repo.

## Para probarlo sin compilar

Copiá la carpeta a `EcliptaLauncher/packs/<id>/` (y la marca, si la tenés, a `EcliptaLauncher/brands/<key>/`) en un pendrive y en el
estéreo: lápiz de la tarjeta del auto, Sistema, Importar packs. El aviso cuenta packs y marcas.
La carpeta vieja `A3Launcher` se sigue leyendo.
Los packs importados no pueden traer código ni llantas nuevas, solo estos archivos:

| Archivo | Límite al importar |
|---|---|
| `pack.json`, `base.webp`, `paint.webp` | Hasta 5 MB cada uno |
| `logo.webp` (pack o marca) | Blanco sobre transparente (el launcher lo tiñe). Hasta 100 KB y 184 px de ancho (y de alto) |
| `sting.webp` (pack o marca) | Hasta 2 MB y 1024x600. Menos de 4 s: a los 4 s se corta |

Un `logo.webp` o `sting.webp` que se pasa de tamaño no se copia y el pack se importa igual, sin ese archivo.

## Perfil (`perfil.json`)

Lo exporta el estéreo y lo genera el [configurador](configurador/index.html). No incluye fotos ni rutas. Claves:

| Clave | Valores |
|---|---|
| `greeting`, `pack`, `body`, `engine`, `year`, `paint`, `wheels`, `lang`, `splash`, `speedLimit`, `rail` | Como en los ajustes |
| `bigRail` | Botones grandes en la barra lateral (`true` por defecto) |
| `title` | Nombre del auto; vacío usa el del pack |
| `plate` | `{text, show}` |
| `layout` | `glass`, `mmi` (en pantalla se llama **Clásico**; el id interno sigue siendo `mmi`) |
| `accent` | `red`, `blue`, `gray` |
| `night` | `off`, `on`, `sun`, `lights` |
| `accApp` | App que se abre al dar contacto, como componente aplanado (`paquete/clase`); vacío = ninguna |
| `remindHome`, `remindAlert` | Aviso en el inicio y alerta al dar contacto |
| `remindLead` | Días de anticipación del aviso en el inicio: 7, 15, 30 o 60 |
| `reminders` | Lista de `{id, name, due, every, notify, note, doneAt}` |

En `reminders`, `id` es uno de `vtv`, `insurance`, `service`, `oil`, `belt`, `brakes`, `tires`, `battery`, `extinguisher`, o `c<número>` para
uno propio; `name` va solo en los propios; `due` y `doneAt` son fechas `yyyy-MM-dd`; `every` son meses, y `notify`, si avisa.

Se siguen aceptando al importar: `carplayOnAcc` (reemplazada por `accApp`) y `dueVtv`, `dueInsurance`, `dueService` (reemplazadas por `reminders`).
El configurador sigue escribiendo `carplayOnAcc`.
