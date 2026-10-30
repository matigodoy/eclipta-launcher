# Diseño

## Decisiones, en orden

1. Primera propuesta: estilo **"Virtual Cockpit"** de Audi.
2. Mati eligió **"liquid glass"** (referencia: vidrio estilo Tesla) en **rojo `#E3263A`** sobre fondo
   negro, **sin** los paneles de carga ni de aire acondicionado del ejemplo.
3. **Sin clima, mapa ni pastilla de BT**: el auto normalmente no tiene internet. Solo datos offline
   (hora, fecha, GPS, lo que suena, radio). La ciudad se cachea cuando hay red.
4. **Sin botón grande de CarPlay**: CarPlay y Android Auto van en la barra lateral, con los **logos reales**
   de las apps instaladas.
5. **Cajón de apps** propio (6 columnas), porque el launcher reemplaza al de JanCar.
6. **Animación "Audi Logo Sting"** (800x600, unos 3,6 s, fondo `#00010C`) al **despertar** el estéreo (ACC on),
   no en el arranque en frío (ver [07-logo-arranque.md](07-logo-arranque.md)).
7. Sin comandos al volante (el auto no los tiene).

## Medidas (dp = px, 1024x600, densidad 160)

- Padding: 14 arriba, 16 a los costados y abajo. Header de 56. Separación de 12. Cuerpo con el resto.
- Cuerpo: barra lateral de 56 | tarjeta del auto de 520 | columna derecha con el resto (unos 392), separaciones de 12.
- Barra lateral: botones circulares de 50, separados 10 (Inicio, CarPlay, Android Auto, Radio, Música | Apps, Ajustes).
- Header: logo de 4 aros (46x18), hora 34sp light, saludo 15sp medium, fecha 12sp, pastilla de ciudad.
- Tarjeta del auto: título 26sp, subtítulo 13sp, foto de 440 de ancho con brillo rojo ovalado y línea de piso,
  panel de velocidad (radio 18) con número de 60sp light y barra de 6dp de 0 a 200.
- Columna derecha: "AHORA SUENA" (carátula 84, título 19sp, artista 14sp, barra de 4dp, botón play de 56) y
  "RADIO FM" (frecuencia 44sp light), con el mismo peso.
- Toques de 44dp como mínimo.

## Estilo vidrio

| Elemento | Valor |
|---|---|
| Fondo | `#060708` + brillos: rojo 30 % (560x400 en 340,340), rojo 20 % (esquina inferior derecha), gris `#8A93A3` 12 % (arriba) |
| Panel | relleno `#0DFFFFFF`, borde 1dp `#1FFFFFFF`, radio 24, línea de brillo arriba `#29FFFFFF` |
| Acento | `#E3263A` |
| Texto | `#F1F2F4` principal, `#A3A9B1` secundario |
| Fuente | sistema (`sans-serif-light` / `-medium`); Inter se descartó por peso (> 300 KB) |

## Capturas

Ver [design/README.md](../design/README.md).
