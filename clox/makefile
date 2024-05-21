CC=gcc
CFLAGS=-I ./includes/ -Wall

compile: ./src/*.c ./includes/*.h
	$(CC) $(CFLAGS) ./src/*.c -o ./out/clox

run: ./out/clox
	./out/clox

clean:
	rm -rfv out/*
