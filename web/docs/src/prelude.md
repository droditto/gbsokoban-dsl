# Ajustes

Al principio del código es donde se definen los ajustes del juego. Son opcionales, pero rellenarlos te permite añadir una pantalla de título, una pantalla de fin, cambiar las velocidades, permitir que el jugador tire de los objetos o quitar el selector de niveles.

```gbsoko
TITLE "SOKOBAN"
AUTHOR "TU NOMBRE"
MOVE_SPEED 8
ANIM_SPEED 4
ENDING "YOU WIN\nSEE YOU AGAIN"
PLAYER_CAN_PULL
NO_LEVEL_SELECT
```

## TITLE y AUTHOR

Si defines título, autor o ambos, el juego empezará desde una pantalla de título. De no definir ninguno de los dos, el juego empezará directamente en el primer nivel.

<img srcset="shots/titles.png 2x">

Cada palabra debe tener a lo sumo 20 caracteres, que es el máximo que cabe en una línea. Las frases con varias palabras se reparten de forma automática en varias líneas, y con `\n` puedes saltar de línea tú mismo. Únicamente se mostrarán letras sin tilde, números y espacios, así que ten cuidado con los caracteres especiales.

## MOVE_SPEED

`MOVE_SPEED` es la velocidad a la que se mueve el jugador. Por defecto vale 8 con texturas de 8×8 píxeles y 16 con texturas de 16×16, pero puedes cambiarla a tu gusto.

<div class="box assist-box">
<p class="box-title">Content assist</p>

Pulsa `Ctrl + Espacio` después de `MOVE_SPEED` y el editor te sugerirá el valor por defecto.

<img srcset="shots/assist-speed.png 2x">

</div>

## ANIM_SPEED

`ANIM_SPEED` es la velocidad a la que se alternan los fotogramas del jugador. Por defecto se calcula a partir de `MOVE_SPEED` y del número de fotogramas que hayas definido en el bloque [`PLAYER`](player.md), pero puedes ajustarla si así lo deseas.

<div class="box assist-box">
<p class="box-title">Content assist</p>

Pulsa `Ctrl + Espacio` después de `ANIM_SPEED` y el editor te sugerirá el valor calculado.

<img srcset="shots/assist-anim-speed.png 2x">

</div>

## ENDING

`ENDING` es el texto que aparece al superar el último nivel, siempre y cuando se hayan superado todos los niveles anteriores. De no definir una pantalla de fin con `ENDING`, el juego volverá a la pantalla de título al terminar el último nivel.

<img srcset="shots/ending.png 2x">

Al igual que en la pantalla de título, evita utilizar palabras de más de 20 caracteres y utiliza solo letras sin tilde, números y espacios.

## PLAYER_CAN_PULL

`PLAYER_CAN_PULL` habilita que el jugador pueda tirar de los objetos. Para tirar de un objeto, mantén pulsado el botón B (Z en el teclado) mientras te mueves en la dirección contraria al objeto y lo arrastrarás contigo. Además, te permite añadir las animaciones `PULL` al bloque [`PLAYER`](player.md).

## NO_LEVEL_SELECT

Al pulsar Start (Enter en el teclado) durante la partida se abre un menú de pausa, desde el que puedes ir al selector de niveles y saltar al nivel que quieras. Si prefieres que el jugador no pueda hacerlo, escribe `NO_LEVEL_SELECT` al principio del código y le obligarás a jugarlos en orden.
