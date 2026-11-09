# Introducción

GBSokoban es un lenguaje específico de dominio para programar videojuegos del género Sokoban para la Game Boy. Con este editor web, puedes generar tus propios juegos desarrollándolos en la parte izquierda de la pantalla, compilarlos con el botón **Compilar** de la barra superior y jugarlos al momento con el emulador que se encuentra a la derecha.

<img srcset="shots/editor.png 2x">

[Sokoban](https://es.wikipedia.org/wiki/Sokoban) es un clásico videojuego de puzles japonés de 1982 en el que controlas a un personaje que tiene que empujar cajas por un almacén hasta dejarlas sobre sus casillas objetivo, con cuidado de no atascarlas contra una pared.

Para facilitar la programación en este lenguaje, el editor ofrece algunas ayudas a la edición. Las principales son el content assist y los quick fixes, que se explican a continuación.

<div class="box assist-box">
<p class="box-title">Content assist</p>

Pulsa `Ctrl + Espacio` en cualquier lugar del editor y este te sugerirá lo que puedes escribir a continuación. En cada página se explican las sugerencias más útiles de ese bloque.

</div>

<div class="box fix-box">
<p class="box-title">Quick fix</p>

Mientras escribes, el editor irá subrayando los errores en rojo y los avisos en amarillo. Pasa el ratón por encima para ver cuál es el problema y, en caso de que tenga arreglo, pulsa el botón que aparece para resolverlo con un arreglo rápido.

</div>

Además de estas ayudas, puedes usar líneas de `=====` para separar visualmente cada bloque, aunque son completamente opcionales.

```gbsoko
=========
PALETTES
=========
```

También puedes escribir comentarios dentro del código, de una línea o de varias, como se muestra a continuación.

```gbsoko
// Un comentario de una línea

/* Un comentario
 de varias líneas */
```
