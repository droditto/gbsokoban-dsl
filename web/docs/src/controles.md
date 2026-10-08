# Controles y menús

Para probar el juego, primero pulsa el botón **Compilar** de la barra superior. Una vez compilado, haz clic en el emulador para poder moverte por el juego, y haz clic de nuevo en el editor para seguir editando. Los controles son los siguientes.

- **Cruceta** (flechas del teclado) para moverte por el nivel y por los menús.
- **A** (X en el teclado) para confirmar.
- **B** (Z en el teclado) para volver atrás en los menús o, si está habilitado `PLAYER_CAN_PULL`, para tirar de los objetos.
- **Start** (Enter en el teclado) para empezar el juego desde la pantalla de título y para abrir y cerrar el menú de pausa.

Además, cualquier juego generado trae por defecto los siguientes menús, que no hay que especificar en el código.

## Menú de pausa

Dentro de un nivel, pulsa Start para que aparezca el menú de pausa, desde el que podrás reiniciar el nivel en caso de que te hayas quedado atascado, o ir al selector de niveles si no lo has deshabilitado con `NO_LEVEL_SELECT`.

<img srcset="shots/menu-pause.png 2x">

## Selector de niveles

Tras elegir SELECT en el menú de pausa o al superar un nivel, estarás en el selector de niveles. Sabrás que estás en él porque, en vez de MOVES, aparecerá BEST junto a tu mejor puntuación en ese nivel. Muévete con la cruceta para cambiar de nivel, que se indica con STAGE seguido de su número, y pulsa A para jugarlo.

<img srcset="shots/menu-select.png 2x">

## Nivel superado

Cuando superes un nivel, aparecerá un menú en el que podrás elegir NEXT para continuar con el siguiente nivel o SELECT para ir al selector de niveles. Además de la puntuación con la que has terminado el nivel, se mostrará también tu mejor puntuación.

<img srcset="shots/menu-clear.png 2x">
