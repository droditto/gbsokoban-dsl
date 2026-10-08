# Sonidos

Opcionalmente, puedes añadir sonidos al juego para ciertas acciones. El bloque de sonidos se declara con la palabra clave `SOUNDS`, y estas son las acciones a las que se les puede asignar un sonido.

- `MOVE`, cuando el jugador se mueve.
- `BLOCKED`, cuando el jugador intenta moverse contra algo que no cede.
- `PUSH`, cuando el jugador empuja un objeto.
- `ON_GOAL`, cuando un objeto entra en su casilla objetivo.
- `DESTROY`, cuando desaparece un objeto.
- `COMPLETE`, cuando se supera el nivel.
- `RESTART`, cuando reinicias el nivel desde el menú de pausa.
- `COLLAPSE`, cuando una baldosa con `COLLAPSES INTO` se transforma.
- `MENU_MOVE`, cuando te mueves por los menús.
- `MENU_OK`, cuando eliges una opción de un menú.

Para declarar un sonido, escribe la palabra clave de la acción, seguida del canal por el que quieres que se reproduzca, de los cuatro que tiene el hardware de la Game Boy (`NR1` y `NR2` son de onda cuadrada, `NR3` de onda programable y `NR4` de ruido), y de cinco números que concretan cómo va a sonar.

Es muy difícil adivinar sonidos de la nada y probarlos resultaría engorroso, por lo que se recomienda utilizar la ROM <a href="sound.gb" download>sound.gb</a> en un emulador, o copiar directamente los del ejemplo.

```gbsoko
=======
SOUNDS
=======

MOVE      NR1   0 129  67 115 134
BLOCKED   NR1   0   1 241  48 128
PUSH      NR1   0 193  98  96 135
ON_GOAL   NR2   0 129 115  89 135
DESTROY   NR4   0   0 103 192 128
COMPLETE  NR3 128 192  32  50 199
RESTART   NR2   0 129  81  64 134
COLLAPSE  NR4   0   0  83 160 128
MENU_MOVE NR1   0 129  35   0 134
MENU_OK   NR1   0 129  67   0 134
```

<div class="box assist-box">
<p class="box-title">Content assist</p>

Pulsa `Ctrl + Espacio` en el primer número y el editor te sugerirá el valor que necesita el canal.

<img srcset="shots/assist-sound.png 2x">

</div>
