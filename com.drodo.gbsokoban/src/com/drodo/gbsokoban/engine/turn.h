#ifndef TURN_H
#define TURN_H

#include <stdint.h>

// Turns taken in the level being played.
extern uint16_t move_count;

void turn_reset(void);
void turn_update(void);

#endif // TURN_H
