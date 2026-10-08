# Tarea

En este estudio se evalúa la usabilidad de GBSokoban, un lenguaje específico de dominio para crear juegos del género Sokoban para la Game Boy. No se evalúa lo bien que lo hagas tú, sino lo intuitiva y fácil de usar que es la herramienta. Mientras trabajas, comenta en voz alta lo que piensas, lo que intentas hacer y lo que te resulta confuso.

Partirás del juego de ejemplo que aparece al abrir el editor e irás ampliándolo en cuatro fases. Al terminar cada fase, compila tu juego, comprueba en el emulador que funciona y avisa al examinador para que tome nota del tiempo. Debajo de cada fase tienes una imagen que te ayudará a ver cómo debería quedar el juego al terminarla.

El código del juego se divide en bloques, como `TEXTURES` o `LEVELS`, y cada paso de la tarea indica en qué bloque tienes que trabajar. Si te atascas en alguna fase, puedes consultar la documentación desde el botón **Documentación** de la barra superior del editor o preguntar al examinador.

## Fase 1

En esta fase tendrás que personalizar la pantalla de título y la pantalla de fin, la que aparece al superar el último nivel. Sus textos están en los ajustes, al principio del código.

1. Cambia el título, `TITLE`, y el autor, `AUTHOR`. No uses tu nombre real como autor, para mantener el anonimato del estudio.
2. Cambia el texto de la pantalla de fin, `ENDING`.

Puedes consultar la página [Ajustes](prelude.md).

<img srcset="shots/tarea-fase1.png 2x">

## Fase 2

Ahora mismo, el jugador usa la misma animación para caminar y para empujar un objeto. En esta fase tendrás que darle una animación propia para empujar.

1. En el bloque `TEXTURES`, añade las cuatro texturas de empujar que tienes en el anexo. Son las del jugador empujando hacia la derecha y hacia arriba.
2. En el bloque `PLAYER`, añade la animación de empujar en las cuatro direcciones, igual que ya está la de caminar. El anexo solo trae texturas para la derecha y para arriba.

En la imagen, el juego de la izquierda es el de ahora y el de la derecha muestra cómo debería quedar.

Puedes consultar las páginas [Texturas](textures.md) y [Jugador](player.md).

<img srcset="shots/tarea-fase2.gif 2x">

## Fase 3

En esta fase tendrás que añadir una caja verde con su propia casilla objetivo, también verde, de forma que cada caja tenga que llegar a la casilla objetivo de su color.

1. En el bloque `PALETTES`, añade las paletas `greens` y `purples` que tienes en el anexo.
2. En el bloque `TEXTURES`, cambia `goal_art`, la textura de la casilla objetivo que ya existe, para que su punto sea naranja, con la paleta `oranges`.
3. En el mismo bloque, crea tres texturas nuevas, copias de `crate_art`, `crate_done_art` y `goal_art` con otra paleta. La caja verde y la nueva casilla objetivo usan `greens`, y la caja verde sobre su casilla objetivo usa `purples`.
4. En el bloque `TILES`, crea la baldosa de la nueva casilla objetivo, con su textura.
5. En el bloque `OBJECTS`, crea el objeto de la caja verde, con su textura normal y la que usa sobre su casilla objetivo.
6. En el bloque `LEGEND`, añade un carácter que represente la caja verde sobre el suelo, para poder colocarla en los niveles.
7. En el bloque `WIN`, añade una condición para que la caja verde también tenga que llegar a su casilla objetivo.
8. En el bloque `LEVELS`, cambia en el primer nivel una de las cajas por la caja verde y una de las casillas objetivo por la nueva.

Puedes consultar las páginas [Paletas](palettes.md), [Texturas](textures.md), [Baldosas](tiles.md), [Objetos](objects.md), [Leyenda](legend.md), [Condiciones de victoria](win.md) y [Niveles](levels.md).

<img srcset="shots/tarea-fase3.gif 2x">

## Fase 4

En esta fase tendrás que añadir tres baldosas nuevas y un nivel que las use.

1. En el bloque `TEXTURES`, añade las texturas `crumble_art`, `pit_art` y `exit_art` que tienes en el anexo.
2. En el bloque `TILES`, crea una baldosa para cada textura. La de `crumble_art` se transforma en la de `pit_art` cuando el jugador sale de ella. Si el jugador entra en la de `pit_art`, muere y el nivel se reinicia. La de `exit_art` es la salida y no necesita ningún comportamiento especial.
3. En el bloque `WIN`, añade dos condiciones, que no quede ninguna baldosa de `crumble_art` y que el jugador esté en la salida. Los niveles anteriores seguirán funcionando, porque no usan esas baldosas.
4. En el bloque `LEVELS`, añade un nivel nuevo al final, en el que el jugador tenga que pisar todas las baldosas de `crumble_art` antes de llegar a la salida.

Puedes consultar las páginas [Texturas](textures.md), [Baldosas](tiles.md), [Condiciones de victoria](win.md) y [Niveles](levels.md).

<img srcset="shots/tarea-fase4.gif 2x">

## Cuestionario

Cuando termines las cuatro fases, pulsa el botón **Cuestionario** de la barra superior del editor y rellénalo. Lo primero que se te pedirá es tu número de participante.

¡Muchas gracias por participar!

## Anexo

A continuación tienes las texturas que necesitarás en las fases 2 y 4, así como las paletas de la fase 3.

### Fase 2

```gbsoko
push_right_1 USES blues
"...22.3."
"..2223.."
"..1111.1"
"...22231"
"..233..."
"..222..."
"322.2..."
".3..33.."

push_right_2 USES blues
"......3."
"...223.."
"..2111.1"
"...22231"
"..233..."
"..2222.."
"32..32.."
".33..33."

push_up_1 USES blues
".11..11."
".3....3."
".2.11.2."
".232232."
".232232."
"..2333.."
".32.33.."
".33....."

push_up_2 USES blues
".11..11."
".3....3."
".2.11.2."
".232232."
".232232."
"..3332.."
"..33.23."
".....33."
```

### Fase 3

```gbsoko
greens  RGB_WHITE #8CC63F #3B7A2A RGB_BLACK
purples RGB_WHITE #F5D76E #6E4FA5 RGB_BLACK
```

### Fase 4

```gbsoko
crumble_art USES grays
"01110000"
"10000020"
"00022220"
"00200002"
"00233002"
"10003020"
"01002110"
"00010000"

pit_art USES grays
"33333333"
"33333333"
"33333333"
"33333333"
"33333333"
"33333333"
"33333333"
"33333333"

exit_art USES reds
"22222333"
"21133313"
"23331313"
"23131333"
"23133333"
"23333333"
"23333333"
"22222222"
```
