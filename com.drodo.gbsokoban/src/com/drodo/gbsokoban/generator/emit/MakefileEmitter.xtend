package com.drodo.gbsokoban.generator.emit

import com.drodo.gbsokoban.generator.plan.GamePlan
import java.util.List
import java.util.Locale

/**
 * Emits the project Makefile. -Wm-y* goes into the cartridge header: -yn title, -yc Game Boy
 * Color byte, -yt MBC, -yo ROM size, -ya RAM banks.
 */
class MakefileEmitter {

	/** The title field runs 0x134..0x143, but 0x143 is the Game Boy Color flag. */
	static val TITLE_BYTES = 15

	def String makefile(GamePlan plan, String project, List<String> sources) {
		val tab = "\t"
		'''
		GBDK_SEARCH = \
		    $(HOME)/gbdk \
		    $(HOME)/Development/gbdk \
		    $(HOME)/Documents/gbdk \
		    $(HOME)/Downloads/gbdk \
		    /opt/gbdk \
		    /usr/local/gbdk

		GBDK_HOME ?= $(patsubst %/bin/lcc,%,$(firstword $(wildcard $(addsuffix /bin/lcc,$(GBDK_SEARCH)))))

		ifeq ($(wildcard $(GBDK_HOME)/bin/lcc),)
		$(error Could not find GBDK. Export GBDK_HOME=/path/to/gbdk, or install it in one of: $(GBDK_SEARCH))
		endif

		LCC = $(GBDK_HOME)/bin/lcc

		ROM = «project»

		TITLE = «cartridgeTitle(plan, project)»

		SRCDIR = src

		«IF plan.palette.hasColor»
		BIN = $(ROM).gbc
		«ELSE»
		BIN = $(ROM).gb
		«ENDIF»
		SRCS = $(addprefix $(SRCDIR)/,«sources.sort.join(" ")»)

		«IF plan.palette.hasColor»
		«ENDIF»
		CART = -Wm-yn$(TITLE)«IF plan.palette.hasColor» -Wm-yc«ENDIF» -Wm-yt«IF plan.needsSave»0x1B -Wm-ya1«ELSE»0x00«ENDIF» -Wm-yoA

		LCCFLAGS = -I$(SRCDIR) $(CART) -Wf--max-allocs-per-node50000

		.PHONY: all clean FORCE

		all: $(BIN)

		ADDRESSABLE = 32768

		$(BIN): $(SRCS) FORCE
		«tab»$(LCC) $(LCCFLAGS) -o $@ $(SRCS)
		«tab»@size=`wc -c < $@`; \
		«tab»if [ $$size -gt $(ADDRESSABLE) ]; then \
		«tab»«tab»rm -f $@; \
		«tab»«tab»echo "$@ came to $$size bytes and only $(ADDRESSABLE) can be reached."; \
		«tab»«tab»echo "Shorten the game: fewer levels, smaller ones, or less art."; \
		«tab»«tab»exit 1; \
		«tab»fi

		FORCE:

		clean:
		«tab»rm -f $(ROM).gb $(ROM).gbc $(ROM).ihx $(ROM).map $(ROM).noi $(ROM).sym $(ROM).lk $(ROM).cdb $(ROM).adb
		«tab»rm -f $(SRCDIR)/*.o $(SRCDIR)/*.rel $(SRCDIR)/*.asm $(SRCDIR)/*.lst $(SRCDIR)/*.sym $(SRCDIR)/*.rst $(SRCDIR)/*.ihx
		'''
	}

	private def String cartridgeTitle(GamePlan plan, String project) {
		val named = if (plan.plainTitle.empty) project else plan.plainTitle
		val upper = named.toUpperCase(Locale.ROOT)
		upper.substring(0, Math.min(TITLE_BYTES, upper.length))
	}
}
