# Texturas

Para definir las baldosas con las que se construye cada nivel de nuestro juego necesitamos texturas, por eso este bloque es obligatorio. Se declara con la palabra clave `TEXTURES`.

Una textura viene definida por un nombre, seguido de la palabra clave `USES` y, por último, el nombre de la [paleta](palettes.md) que quieres utilizar para esa textura. Debajo se pinta la textura con cadenas de caracteres, donde cada carácter debe ser `0`, `1`, `2` o `3`, refiriéndonos a la posición del color dentro de la paleta, siendo `0` el primer color y `3` el último. Opcionalmente también se puede utilizar un punto para referirnos al color `0`, ya que en ciertos casos el color `0` no es visible y es transparente, como se explicará más adelante.

<img srcset="shots/texture.png 2x">

Todas las texturas de un mismo juego deben ser del mismo tamaño, es decir, o todas de 8×8 píxeles o todas de 16×16. Si decides utilizar texturas de 8×8, podrás ver hasta 20×17 baldosas en pantalla a la vez y tu nivel podrá medir como máximo 32×32, mientras que si eliges texturas de 16×16 tan solo podrás ver 10×8 baldosas en pantalla y un nivel podrá medir como máximo 16×16. En los niveles que son más grandes que la pantalla de la consola, la cámara seguirá al jugador a lo largo del nivel.

<img srcset="shots/solve-8px-16px.gif 2x">

<div class="box assist-box">
<p class="box-title">Content assist</p>

Para no tener que pintar la textura desde cero, pulsa `Ctrl + Espacio` en la línea siguiente a `nombre USES paleta` y el editor te sugerirá añadir una cuadrícula de puntos a partir de la cual empezar a pintar tu textura.

<img srcset="shots/assist-grid.png 2x">

</div>

El hardware de la Game Boy dibuja las texturas en tres capas, de las cuales nos interesan dos, la capa de fondo y la capa de sprites. La peculiaridad es que en la capa de sprites el color `0` de la paleta se vuelve transparente, por lo que en nuestro juego habrá tres tipos de texturas, que se explican a continuación.

## Baldosas

Las baldosas forman parte siempre de la capa de fondo, por lo que puedes utilizar sin ninguna preocupación el color `0` de su paleta. Por ejemplo, el muro de 16×16 píxeles usa sus cuatro colores y todos se ven en pantalla en todo momento.

<img srcset="shots/wall.png 2x">

## Objetos

Debido a las limitaciones del hardware de la consola, solo se pueden mostrar 40 sprites a la vez en pantalla, y como mucho 10 por línea. Por eso, para optimizar nuestros juegos, los objetos únicamente forman parte de la capa de sprites cuando están en movimiento, y se funden con el fondo cuando están quietos. Es por ello que debes tener cuidado cuando utilices el color `0` en las texturas de los objetos, ya que se volverá transparente mientras el objeto se mueve.

Es buena idea hacer que el color `0` de la paleta de tus objetos coincida con el color de las baldosas por las que se mueven. En el ejemplo, la paleta de la caja empieza por `RGB_WHITE`, igual que el suelo.

<img srcset="shots/crate.png 2x">

## Jugador

El jugador, al estar siempre en la capa de sprites, nunca muestra el color `0` de su paleta. Se recomienda utilizar puntos en vez de ceros al pintar el arte del jugador, para más claridad visual.

<img srcset="shots/holes.png 2x">
