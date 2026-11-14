# Niveles

El bloque de niveles es obligatorio y se declara con la palabra clave `LEVELS`. Para definir un nivel escribe la palabra clave `LEVEL`, seguida de las cadenas con los caracteres que se han definido previamente en las [baldosas](tiles.md) o en la [leyenda](legend.md).

Un nivel tiene que tener un único jugador y las cadenas que lo componen pueden ser de distintas longitudes. Todo lo que queda fuera de lo que dibujas es sólido y se pinta con la textura de la primera baldosa que hayas definido, así que conviene que esa primera baldosa sea el suelo o la pared.

<img srcset="shots/levels.gif 2x">

Los niveles se jugarán en el orden en el que los declares. Como ya se mencionaba en [Texturas](textures.md), si tus texturas son de 8×8 píxeles el nivel podrá ser de hasta 32×32 casillas, y si son de 16×16 píxeles, de hasta 16×16. En caso de que el nivel sea más grande que la pantalla, la cámara te seguirá a lo largo del nivel.

A continuación se muestra el código de los dos niveles anteriores.

```gbsoko
=======
LEVELS
=======

LEVEL
"#####"
"#@  #"
"# $$# ###"
"# $ # #.#"
"### ###.#"
" ##    .#"
" #   #  #"
" #   ####"
" #####"

LEVEL
"##########"
"#..@     #"
"#..$  #  #"
"#  #$## ##"
"# $     #"
"##### # #"
"  # $   #"
"  #     #"
"  #######"
```

Los caracteres `#`, el espacio y `.` son los de las baldosas de la pared, el suelo y la casilla objetivo.

```gbsoko
======
TILES
======

suelo " " USES textura_suelo
pared "#" USES textura_pared BLOCKS
objetivo "." USES textura_objetivo
```

Los caracteres `@`, `+`, `$` y `*` son los de la leyenda, que colocan al jugador y a la caja sobre el suelo o sobre la casilla objetivo.

```gbsoko
=======
LEGEND
=======

"@" = PLAYER ON suelo
"+" = PLAYER ON objetivo
"$" = caja ON suelo
"*" = caja ON objetivo
```
