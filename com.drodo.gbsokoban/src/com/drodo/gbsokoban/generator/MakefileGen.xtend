package com.drodo.gbsokoban.generator

import com.drodo.gbsokoban.gBSokoban.Game
import com.drodo.gbsokoban.util.Feature
import java.util.Set

/**
 * Emits the project Makefile. GBDK_HOME comes from the DSL `gbdk` field
 * when present, otherwise from the environment. LCCFLAGS sets MBC5+RAM
 * +battery (-Wm-yt0x1B), 10 ROM banks (-Wm-yoA), 1 RAM bank (-Wm-ya1) and
 * GBDK's recommended -Wf--max-allocs-per-node50000.
 */
class MakefileGen {

	def String makefile(Game game, Set<Feature> features) {
		val tab = "\t"
		val hasTitleScreen = features.contains(Feature.TITLE_SCREEN)
		val hasEndScreen = features.contains(Feature.END_SCREEN)
        '''
        «IF game.gbdk !== null»
        GBDK_HOME = «game.gbdk»
        «ELSE»
        # GBDK_HOME set from environment
        ifndef GBDK_HOME
        $(error GBDK_HOME is not set. Define `gbdk "/path/to/gbdk"` in the DSL or export GBDK_HOME in your shell)
        endif
        «ENDIF»
        LCC = $(GBDK_HOME)/bin/lcc
        PNG2ASSET = $(GBDK_HOME)/bin/png2asset

        ROM = «game.name»

        GENDIR = gen
        RESDIR = res
        SRCDIR = src

        BIN = $(ROM).gb
        SRCS = $(wildcard $(SRCDIR)/*.c) $(GENDIR)/background.c $(GENDIR)/sprites.c«IF hasTitleScreen» $(GENDIR)/title_screen.c«ENDIF»«IF hasEndScreen» $(GENDIR)/ending_screen.c«ENDIF»

        LCCFLAGS = -I$(SRCDIR) -I$(GENDIR) -Wm-yt0x1B -Wm-yoA -Wm-ya1 -Wf--max-allocs-per-node50000

        .PHONY: all assets clean

        all: assets $(BIN)

        assets:
        «tab»mkdir -p $(GENDIR)
        «tab»$(PNG2ASSET) $(RESDIR)/background.png -c $(GENDIR)/background.c -sw 16 -sh 16 -map -noflip -keep_palette_order
        «tab»$(PNG2ASSET) $(RESDIR)/sprites.png -c $(GENDIR)/sprites.c -sw 16 -sh 16 -spr8x16 -keep_palette_order
        «IF hasTitleScreen»
        «tab»$(PNG2ASSET) $(RESDIR)/title_screen.png -c $(GENDIR)/title_screen.c -map -noflip -keep_palette_order
        «ENDIF»
        «IF hasEndScreen»
        «tab»$(PNG2ASSET) $(RESDIR)/ending_screen.png -c $(GENDIR)/ending_screen.c -map -noflip -keep_palette_order
        «ENDIF»

        $(BIN): $(SRCS)
        «tab»$(LCC) $(LCCFLAGS) -o $@ $^

        clean:
        «tab»rm -f $(BIN)
        «tab»rm -f $(SRCDIR)/*.o
        «tab»rm -rf $(GENDIR)
        '''
	}
}
