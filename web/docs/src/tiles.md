# Baldosas

Las baldosas son las casillas con las que se construyen los niveles. Este bloque es obligatorio y se declara con la palabra clave `TILES`.

Para definir una baldosa necesitas el nombre que le quieres dar, el carácter con el que la quieres dibujar a la hora de pintar los niveles, la palabra clave `USES` seguida del nombre de la [textura](textures.md) que va a utilizar y, opcionalmente, un modificador que indica cómo interactúan los objetos y el jugador con esa baldosa. Sin un modificador, el jugador y los objetos pueden pasar por la baldosa sin problema.

```gbsoko
======
TILES
======

suelo " " USES textura_suelo
pared "#" USES textura_pared BLOCKS
objetivo "." USES textura_objetivo
```

<div class="box assist-box">
<p class="box-title">Content assist</p>

Pulsa `Ctrl + Espacio` después de haber elegido el nombre de tu baldosa y el editor te sugerirá un carácter que no esté siendo usado.

<img srcset="shots/assist-tile-symbol.png 2x">

</div>

A continuación se explican los distintos modificadores y para qué se puede usar cada uno.

## BLOCKS

El modificador `BLOCKS` hace que ni el jugador ni los objetos puedan atravesar la casilla. Normalmente se utiliza para las paredes que delimitan el nivel.

```gbsoko
pared "#" USES textura_pared BLOCKS
```

## SLIDES

El modificador `SLIDES` hace que tanto el jugador como los objetos resbalen. El jugador o el objeto que entra en la casilla resbala en la misma dirección en la que entra, hasta chocar con algo o salir de la baldosa.

```gbsoko
hielo "~" USES textura_hielo SLIDES
```

<img srcset="shots/tile-slides.gif 2x">

## DESTROYS

Cuando un objeto entra en una casilla con el modificador `DESTROYS`, desaparece. Esto se puede utilizar como condición de victoria o para que, si el jugador destruye un objeto que necesitaba, el nivel se quede sin solución y tenga que reiniciarlo. Si es el jugador quien entra, se interpreta que ha muerto y el nivel se reinicia.

```gbsoko
abismo "!" USES textura_abismo DESTROYS
```

<img srcset="shots/tile-destroys.gif 2x">

## COLLAPSES INTO

El modificador `COLLAPSES INTO` transforma una baldosa en otra cuando el jugador sale de ella. En el ejemplo, la baldosa `fragil` se transforma en `abismo` cuando el jugador sale de ella.

```gbsoko
fragil "%" USES textura_fragil COLLAPSES INTO abismo
```

<img srcset="shots/tile-collapses.gif 2x">

## FILLS INTO

El modificador `FILLS INTO` funciona de forma similar a `DESTROYS`, ya que el objeto que entra en la casilla desaparece. La diferencia es que, además, la casilla se rellena y se transforma en otra baldosa, como ocurre con `COLLAPSES INTO`. Cuidado, porque este modificador también puede matar al jugador y reiniciar el nivel.

```gbsoko
agujero "H" USES textura_agujero FILLS INTO suelo
```

<img srcset="shots/tile-fills.gif 2x">

## CARRIES

El modificador `CARRIES` funciona de forma similar a `SLIDES`, pero en vez de mantener al jugador en la dirección en la que entró, lo empuja en la dirección que especifiques, que puede ser `UP`, `DOWN`, `LEFT` o `RIGHT`. Se pueden encadenar varias baldosas con este modificador, que no afecta a los objetos, únicamente al jugador.

```gbsoko
cinta_derecha ">" USES textura_cinta_derecha CARRIES RIGHT
```

<img srcset="shots/tile-carries.gif 2x">
