# Cuadros y Pizarras — mod Fabric para Minecraft 26.2

Guarda tus **capturas de pantalla** como cuadros y **dibuja con el ratón** en una pizarra.
El resultado es un **mapa** de Minecraft que cuelgas en un **marco** (también funciona en
multijugador: el servidor sincroniza el mapa solo).

## Ítems

| Ítem | Receta | Uso (clic derecho) |
|---|---|---|
| **Pizarra** | palos (marco) + lana blanca + tinte negro | Abre el lienzo de 128×128: lápiz, borrador, cubo de pintura, 61 colores × 4 brillos, deshacer (Z), tamaño de pincel (rueda). |
| **Cámara de cuadros** | hierro + panel de cristal + redstone | Abre tus capturas (F2) o imágenes de la carpeta `cuadros/`; elige ajuste (rellenar/entera) y tramado. |

Ambos están también en la pestaña creativa *Herramientas y utilidades*.

**Guardar** crea un mapa lleno con tu imagen. En supervivencia consume **1 mapa vacío** del
inventario (en creativo es gratis). Cuélgalo en un marco para verlo en la pared.

Puedes meter cualquier PNG/JPG/GIF/BMP en `.minecraft/cuadros/` y aparecerá en la cámara.

## Cómo obtener el .jar

### Opción A — sin instalar nada (GitHub Actions)
1. Crea un repositorio en GitHub y sube **todo el contenido de esta carpeta** (incluida `.github/`).
2. Ve a la pestaña **Actions**, abre la última ejecución de *build* y descarga el artefacto **Artifacts**.
3. Dentro está `cuadrospizarras-1.0.0.jar`.

### Opción B — en tu PC
Necesitas **JDK 25**. En esta carpeta:

    ./gradlew build        (Windows: gradlew.bat build)

El jar queda en `build/libs/cuadrospizarras-1.0.0.jar` (usa el que NO termina en `-sources`).

## Instalación del mod
- Minecraft **26.2**, **Fabric Loader ≥ 0.19.5**, **Fabric API** para 26.2 y **Java 25**.
- Copia el `.jar` a `.minecraft/mods/`. Debe estar también en el servidor si juegas en multijugador.

## Estado
El código se escribió contra la API de 26.2 usando como referencia el código fuente de Fabric API
y el proyecto de ejemplo de la documentación de Fabric, pero **no se ha podido compilar ni probar en el juego**
en el entorno donde se generó. Si la compilación falla, el error indica el archivo y la línea exacta
a corregir.

## Estructura
- `src/main` — ítems, red (paquete cliente→servidor), paleta de mapas y creación del mapa en el servidor.
- `src/client` — pantallas de pizarra y cámara, conversión de imágenes (escalado + tramado Floyd–Steinberg).
