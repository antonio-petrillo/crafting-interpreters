#include <stdlib.h>
#include "chunk.h"
#include "memory.h"

void initChunk(Chunk* chunk) {
  chunk->count = 0;
  chunk->capacity = 0;
  chunk->count_lines = 0;
  chunk->capacity_lines = 0;
  chunk->code = NULL;
  chunk->lines = NULL;
  initValueArray(&chunk->constants);
}

void freeChunk(Chunk* chunk) {
  FREE_ARRAY(uint8_t, chunk->code, chunk->capacity);
  FREE_ARRAY(int, chunk->lines, chunk->capacity);
  freeValueArray(&chunk->constants);
  initChunk(chunk);
}

void writeChunk(Chunk* chunk, uint8_t byte, int line) {
  if (chunk->capacity < chunk->count + 1) {
	int oldCapacity = chunk->capacity;
	chunk->capacity = GROW_CAPACITY(oldCapacity);
	chunk->code = GROW_ARRAY(uint8_t, chunk->code, oldCapacity, chunk->capacity);
	/* if ((line & 0x 00FFFFFF)  == chuck->lines[chuck] ) */
	/* chunk->lines = GROW_ARRAY(int, chunk->lines, oldCapacity, chunk->capacity); */
  }

  chunk->code[chunk->count] = byte;
  /* chunk->lines[chunk->count] = line; */
  chunk->count++;
  if (chunk->capacity_lines < chunk->count_lines + 1) {
	int oldCapacity = chunk->capacity_lines;
	chunk->capacity_lines = GROW_CAPACITY(oldCapacity);
	chunk->lines = GROW_ARRAY(int, chunk->lines, oldCapacity, chunk->capacity_lines);
  }

  // assume first 24 bit for lines and the remaining 8 for count the rle
  // no check on the lines passed in
  if ((chunk->lines[chunk->count_lines] & 0x00FFFFFF) == (line & 0x00FFFFFF)) {
	// same line, increase the count
	int count = (((chunk->lines[chunk->count_lines] & 0xFF000000) >> 24) + 1) << 24;
	chunk->lines[chunk->count_lines] | count;
  } else {
	// new line, add entry
	chunk->linex[chunk->count_lines++] = line;
  }
}

int addConstant(Chunk* chunk, Value value) {
  writeValueArray(&chunk->constants, value);
  return chunk->constants.count - 1;
}
