# Introducción

GBSokoban es un lenguaje específico de dominio para crear juegos del género Sokoban para la Game Boy. En este editor web puedes escribir tu juego en la parte de la izquierda, compilarlo con el botón **Compilar** de la barra superior y jugarlo al momento en el emulador que se encuentra a la derecha.

<img srcset="shots/editor.png 2x">

Para facilitar la programación en este lenguaje, el editor ofrece algunas ayudas a la edición. Las principales son el content assist y los quick fixes, que se explican a continuación.

<div class="box assist-box">
<p class="box-title">Content assist</p>

Pulsa `Ctrl + Espacio` en cualquier lugar del editor y este te sugerirá lo que puedes escribir a continuación. En cada bloque de la gramática se mencionan algunas sugerencias especiales, que no vienen dadas por la propia estructura de la gramática.

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
