# Packs de vehículo

Un solo APK para todos: lo que cambia por auto vive en una carpeta. Se puede aportar un modelo sin tocar Java.

```
launcher/app/src/main/assets/packs/<id>/   (o EcliptaLauncher/packs/<id>/ en un pendrive)
  pack.json    marca, nombre, carrocerías, motores, colores, llantas, ubicación de ruedas y patente
  base.webp    foto sin fondo, 640 px de ancho
  paint.webp   máscara: blanco = pintura
  logo.webp    opcional, solo en packs importados: logo de la marca
  sting.webp   opcional, solo en packs importados: animación al despertar
```

## Marca, logo y animación

`pack.json` lleva `brand`, el nombre de la marca para mostrar (`[A-Za-z0-9][A-Za-z0-9 -]{0,23}`). `scripts/validate_packs.py` lo exige.

El launcher busca el logo y la animación en este orden:

1. La carpeta del pack importado: `logo.webp` y `sting.webp`.
2. `assets/brands/<key>/logo.webp` y `sting.webp`, donde `<key>` es `brand` en minúsculas y con guiones ("Alfa Romeo" es `alfa-romeo`).
3. Si no hay, la marca Eclipta y sin animación.

**Logos y animaciones de terceros no se aceptan en el repo**: `validate_packs.py` rechaza `logo.webp` y `sting.webp` dentro de
`assets/packs` ([CONTRIBUTING](../CONTRIBUTING.md), regla 5). Cada uno puede cargar los suyos desde su pendrive.

Una foto propia con nombre de auto (`title`) saca el logo, la animación y los datos por defecto del subtítulo del pack: así sirve cualquier auto.

## Crear uno

1. **Foto propia**, de 3/4 de frente, entera y con buena luz. Mejor un auto claro: se tiñe mucho mejor que uno negro.
2. Sacale el fondo (en Mac, Vision o "Quitar fondo"; si no, remove.bg).
3. Copiá `scripts/packs/audi-s3-8p.json` como `scripts/packs/<id>.json` y marcá los polígonos, medidos en `ref_width` píxeles:
   `exclude` (vidrios, ópticas, parrilla, llantas), `blank` (**siempre** la patente real), `darken` (vidrios con reflejos de otras cosas).
4. `python3 scripts/make_pack.py foto.png scripts/packs/<id>.json launcher/app/src/main/assets/packs/<id>` (necesita Pillow y numpy).
5. Escribí `pack.json` tomando otro de ejemplo, con su `brand`. `wheel_centers` son centro, radios y giro de cada llanta; `plate`, las cuatro esquinas de la patente.
   `paint.gain`, `gamma` y `spec` ajustan el tinte: un auto negro necesita `gain` cercano a 2, uno blanco cercano a 1.
6. `python3 scripts/validate_packs.py launcher/app/src/main/assets/packs` y abrí un PR. El mismo chequeo corre en GitHub.

Si el modelo se suma, agregalo también a `PACKS` en `docs/configurador/index.html` y copiá sus capas a `docs/configurador/img/`.

## Para probarlo sin compilar

Copiá la carpeta a `EcliptaLauncher/packs/<id>/` en un pendrive y en el estéreo: lápiz de la tarjeta del auto, Sistema, Importar packs.
La carpeta vieja `A3Launcher` se sigue leyendo.
Los packs importados no pueden traer código ni llantas nuevas, solo estos archivos:

| Archivo | Límite al importar |
|---|---|
| `pack.json`, `base.webp`, `paint.webp` | Hasta 5 MB cada uno |
| `logo.webp` | Blanco sobre transparente (el launcher lo tiñe). Hasta 100 KB y 184 px de ancho (y de alto) |
| `sting.webp` | Hasta 2 MB y 1024x600. Menos de 4 s: a los 4 s se corta |

Un `logo.webp` o `sting.webp` que se pasa de tamaño no se copia y el pack se importa igual, sin ese archivo.

## Perfil (`perfil.json`)

Lo exporta el estéreo y lo genera el [configurador](configurador/index.html). No incluye fotos ni rutas. Claves:

| Clave | Valores |
|---|---|
| `greeting`, `pack`, `body`, `engine`, `year`, `paint`, `wheels`, `lang`, `splash`, `speedLimit`, `rail` | Como en los ajustes |
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
