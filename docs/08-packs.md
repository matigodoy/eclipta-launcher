# Packs de vehículo

Un solo APK para todos: lo que cambia por auto vive en una carpeta. Se puede aportar un modelo sin tocar Java.

```
launcher/app/src/main/assets/packs/<id>/   (o A3Launcher/packs/<id>/ en un pendrive)
  pack.json    nombre, carrocerías, motores, colores, llantas, ubicación de ruedas y patente
  base.webp    foto sin fondo, 640 px de ancho
  paint.webp   máscara: blanco = pintura
```

## Crear uno

1. **Foto propia**, de 3/4 de frente, entera y con buena luz. Mejor un auto claro: se tiñe mucho mejor que uno negro.
2. Sacale el fondo (en Mac, Vision o "Quitar fondo"; si no, remove.bg).
3. Copiá `scripts/packs/audi-s3-8p.json` como `scripts/packs/<id>.json` y marcá los polígonos, medidos en `ref_width` píxeles:
   `exclude` (vidrios, ópticas, parrilla, llantas), `blank` (**siempre** la patente real), `darken` (vidrios con reflejos de otras cosas).
4. `python3 scripts/make_pack.py foto.png scripts/packs/<id>.json launcher/app/src/main/assets/packs/<id>` (necesita Pillow y numpy).
5. Escribí `pack.json` tomando otro de ejemplo. `wheel_centers` son centro, radios y giro de cada llanta; `plate`, las cuatro esquinas de la patente.
   `paint.gain`, `gamma` y `spec` ajustan el tinte: un auto negro necesita `gain` cercano a 2, uno blanco cercano a 1.
6. `python3 scripts/validate_packs.py launcher/app/src/main/assets/packs` y abrí un PR. El mismo chequeo corre en GitHub.

Si el modelo se suma, agregalo también a `PACKS` en `docs/configurador/index.html` y copiá sus capas a `docs/configurador/img/`.

## Para probarlo sin compilar

Copiá la carpeta a `A3Launcher/packs/<id>/` en un pendrive y en el estéreo: Personalizar > Copia y packs > Importar packs.
Los packs importados no pueden traer código ni llantas nuevas: solo `pack.json`, `base.webp` y `paint.webp`, de hasta 5 MB cada uno.

## Perfil (`perfil.json`)

Lo exporta el estéreo y lo genera el [configurador](configurador/index.html). No incluye fotos ni rutas. Claves: `greeting`, `pack`,
`body`, `engine`, `year`, `plate {text, show}`, `paint`, `wheels`, `layout` (glass, mmi), `accent` (red, blue, gray),
`night` (off, on, sun, lights), `lang`, `splash`, `carplayOnAcc`, `speedLimit`, `dueVtv`, `dueInsurance`, `dueService`, `rail`.
