#!/usr/bin/env bash

javac -d ./bin com/craftinginterpreters/lox/*.java
java -cp bin com.craftinginterpreters.lox.Lox
