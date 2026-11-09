# Texturas

Una textura es solo un dibujo. Para que aparezca en el juego, tiene que usarla una [baldosa](tiles.md), un [objeto](objects.md) o el [jugador](player.md). Este bloque es obligatorio, ya que las baldosas con las que se construye cada nivel necesitan texturas. Se declara con la palabra clave `TEXTURES`.

Una textura viene definida por un nombre, seguido de la palabra clave `USES` y, por último, el nombre de la [paleta](palettes.md) que usará esa textura. Debajo se pinta la textura con cadenas de caracteres, donde cada carácter debe ser `0`, `1`, `2` o `3`, que indica la posición del color dentro de la paleta, siendo `0` el primer color y `3` el último. Opcionalmente también se puede utilizar un punto en lugar del `0`, ya que en ciertos casos el color `0` es transparente, como se explicará más adelante.

<img srcset="shots/texture.png 2x">

Todas las texturas de un mismo juego deben ser del mismo tamaño, es decir, o todas de 8×8 píxeles o todas de 16×16. Con texturas de 8×8 se ven hasta 20×17 baldosas en pantalla a la vez y un nivel puede medir como máximo 32×32, mientras que con texturas de 16×16 solo se ven 10×8 baldosas y un nivel puede medir como máximo 16×16. En los niveles que son más grandes que la pantalla de la consola, la cámara seguirá al jugador a lo largo del nivel.

<img srcset="shots/solve-8px-16px.gif 2x">

<div class="box assist-box">
<p class="box-title">Content assist</p>

Para no tener que pintar la textura desde cero, pulsa `Ctrl + Espacio` en la línea siguiente a `nombre USES paleta` y el editor te sugerirá añadir una cuadrícula de puntos a partir de la cual empezar a pintar tu textura.

<img srcset="shots/assist-grid.png 2x">

</div>

El color `0` de la paleta no se comporta igual en todas las texturas, por lo que hay tres tipos, que se explican a continuación.

## Baldosas

En las baldosas, el color `0` de su paleta se puede utilizar sin ninguna preocupación. Por ejemplo, la pared de 16×16 píxeles usa sus cuatro colores y todos se ven en pantalla en todo momento.

<img srcset="shots/wall.png 2x">

## Objetos

Un objeto quieto se dibuja como una baldosa más, con todos sus colores, pero mientras se mueve el color `0` de su paleta se vuelve transparente. Por eso hay que tener cuidado al utilizar el color `0` en las texturas de los objetos.

Es buena idea que el color `0` de la paleta de los objetos coincida con el color de las baldosas por las que se mueven. En el ejemplo, la paleta de la caja empieza por `RGB_WHITE`, igual que el suelo.

<img srcset="shots/crate.png 2x">

## Jugador

El jugador nunca muestra el color `0` de su paleta. Se recomienda utilizar puntos en vez de ceros al pintar las texturas del jugador, para más claridad visual.

<img srcset="shots/holes.png 2x">
