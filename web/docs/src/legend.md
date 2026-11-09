# Leyenda

El bloque de la leyenda es obligatorio y se declara con la palabra clave `LEGEND`. Las baldosas ya tienen un carácter con el que dibujarlas en los [niveles](levels.md). La leyenda sirve únicamente para dar también un carácter al jugador y a los objetos, indicando la baldosa sobre la que están.

Para definir un nuevo carácter en la leyenda, primero pon entre comillas el carácter que quieres utilizar, seguido de `=`. Después va la palabra clave `PLAYER`, en caso de querer colocar al jugador, o el nombre de un objeto, seguido de la palabra clave `ON` y el nombre de la baldosa sobre la que se colocará.

```gbsoko
=======
LEGEND
=======

"@" = PLAYER ON suelo
"+" = PLAYER ON objetivo
"$" = caja ON suelo
"*" = caja ON objetivo
```

<div class="box assist-box">
<p class="box-title">Content assist</p>

Pulsa `Ctrl + Espacio` en una línea nueva y el editor te sugerirá un carácter que no esté siendo usado.

<img srcset="shots/assist-legend-symbol.png 2x">

</div>
