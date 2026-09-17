# GBSokoban

GBSokoban es un DSL textual para crear juegos Sokoban para Game Boy. A partir de un fichero `.gbsoko` genera un proyecto C de GBDK-2020 que compila a una ROM `.gb` o `.gbc` si el juego declara color. Está construido con Xtext y Xtend sobre Eclipse, e incluye validación, content assist y quick-fixes.

Para usarlo, importa los proyectos en Eclipse, ejecuta `GenerateGBSokoban.mwe2` como MWE2 Workflow y lanza una instancia con Run As > Eclipse Application. Al guardar un `.gbsoko` se genera el proyecto GBDK, que se compila con `make` (requiere GBDK-2020).
