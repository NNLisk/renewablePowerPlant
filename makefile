SCALA = scalac
RUN = scala
SRC = $(shell find src -name "*.scala")
OUT = OUT

compile: 
	mkdir -p $(OUT)
	$(SCALA) -d $(OUT) $(SRC)

run:
	$(RUN) -cp $(OUT) Main

clean:
	rm -rf $(OUT)

all: compile run