# Baldosas

Las baldosas son las casillas con las que se construyen los niveles. Este bloque es obligatorio y se declara con la palabra clave `TILES`.

Para definir una baldosa necesitas el nombre que le quieres dar, el carácter con el que la quieres dibujar a la hora de pintar los niveles, la palabra clave `USES` seguida del nombre de la [textura](textures.md) que va a utilizar y, opcionalmente, un modificador que indica cómo interactúan los objetos y el jugador con esa baldosa. Sin un modificador, la baldosa se comportará como una casilla sin colisión, por la que pueden pasar el jugador y los objetos sin problema.

```gbsoko
======
TILES
======

floor " " USES floor_art
wall  "#" USES wall_art BLOCKS
goal  "." USES goal_art
```

<div class="box assist-box">
<p class="box-title">Content assist</p>

Pulsa `Ctrl + Espacio` después de haber elegido el nombre de tu baldosa y el editor te sugerirá un carácter que no esté siendo usado.

<img srcset="shots/assist-tile-symbol.png 2x">

</div>

A continuación se explican los distintos modificadores y para qué se puede usar cada uno.

## BLOCKS

El modificador `BLOCKS` le da colisión a la casilla, para que ni el jugador ni los objetos puedan atravesarla. Normalmente se utiliza para las paredes que delimitan el nivel.

```gbsoko
wall "#" USES wall_art BLOCKS
```

## SLIDES

El modificador `SLIDES` hace que tanto el jugador como los objetos resbalen. El jugador o el objeto que entra en la casilla resbala en la misma dirección en la que entra, hasta chocar con algo o salir de la baldosa.

```gbsoko
ice "~" USES ice_art SLIDES
```

<img srcset="shots/tile-slides.gif 2x">

## DESTROYS

Cuando un objeto entra en una casilla con el modificador `DESTROYS`, desaparece. Esto se puede utilizar como condición de victoria o para que, si el jugador destruye un objeto que necesitaba, el nivel se quede sin solución y tenga que reiniciarlo. Si es el jugador quien entra, se interpreta que ha muerto y el nivel se reinicia.

```gbsoko
pit "!" USES pit_art DESTROYS
```

<img srcset="shots/tile-destroys.gif 2x">

## COLLAPSES INTO

El modificador `COLLAPSES INTO` transforma una baldosa en otra cuando el jugador sale de ella. En el ejemplo, la baldosa `crumble` se transforma en `pit` cuando el jugador sale de ella.

```gbsoko
crumble "%" USES crumble_art COLLAPSES INTO pit
```

<img srcset="shots/tile-collapses.gif 2x">

## FILLS INTO

El modificador `FILLS INTO` funciona de forma similar a `DESTROYS`, ya que el objeto que entra en la casilla desaparece. La diferencia es que, además, la casilla se rellena y se transforma en otra baldosa, como ocurre con `COLLAPSES INTO`. Cuidado, porque este modificador también puede matar al jugador y reiniciar el nivel.

```gbsoko
hole "H" USES hole_art FILLS INTO floor
```

<img srcset="shots/tile-fills.gif 2x">

## CARRIES

El modificador `CARRIES` funciona de forma similar a `SLIDES`, pero en vez de mantener al jugador en la dirección en la que entró, lo empuja en la dirección que especifiques, que puede ser `UP`, `DOWN`, `LEFT` o `RIGHT`. Se pueden encadenar varias baldosas con este modificador, que no afecta a los objetos, únicamente al jugador.

```gbsoko
beltRight ">" USES beltRight_art CARRIES RIGHT
```

<img srcset="shots/tile-carries.gif 2x">
