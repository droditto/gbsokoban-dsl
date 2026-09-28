# Leyenda

El bloque de la leyenda es obligatorio y se declara con la palabra clave `LEGEND`. Es necesario porque, para colocar al jugador o un objeto en los [niveles](levels.md), hay que asignarles un carácter, a diferencia de las baldosas, que ya lo tienen.

Para definir un nuevo carácter en la leyenda, primero pon entre comillas el carácter que quieres utilizar, seguido de `=`. Después va la palabra clave `PLAYER`, en caso de querer colocar al jugador, o el nombre de un objeto, seguido de la palabra clave `ON` y el nombre de la baldosa sobre la que se colocará.

```gbsoko
=======
LEGEND
=======

"@" = PLAYER ON floor
"+" = PLAYER ON goal
"$" = crate  ON floor
"*" = crate  ON goal
```

<div class="box assist-box">
<p class="box-title">Content assist</p>

Pulsa `Ctrl + Espacio` en una línea nueva y el editor te sugerirá un carácter que no esté siendo usado.

<img srcset="shots/assist-legend-symbol.png 2x">

</div>
