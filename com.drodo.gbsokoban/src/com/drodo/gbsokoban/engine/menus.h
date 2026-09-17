#ifndef MENUS_H
#define MENUS_H

#include <stdint.h>
#include "config.h"

// One init/update pair per state.

#ifdef FEAT_TITLE_SCREEN
void title_init(void);
void title_update(void);
#endif

void pause_init(void);
void pause_update(void);

#ifdef FEAT_LEVEL_SELECT
void select_init(void);
void select_update(void);
#endif

void clear_init(void);
void clear_update(void);

#ifdef FEAT_END_SCREEN
void all_levels_complete_init(void);
void all_levels_complete_update(void);
#endif

#endif // MENUS_H
