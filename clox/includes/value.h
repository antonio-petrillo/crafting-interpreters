#ifndef clox_value_h
#define clox_value_h

typedef double Value;

typedef struct {
  int capacity;
  int count;
  Value* values;
} ValueArray;

void initValueArray(ValueArray* array);
void freeValueArray(ValueArray* array);
void printValue(Value value);
void writeValueArray(ValueArray* array, Value value);

#endif
