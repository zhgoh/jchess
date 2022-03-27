ifdef OS
	RM = del /Q
	FixPath = $(subst /,\,$1)
	CLS = cls
else
   ifeq ($(shell uname), Linux)
		RM = rm -f
		FixPath = $1
		CLS = clear
   endif
endif

all: game
	$(CLS)
	java -cp bin Game

game: Game.java
	javac Game.java -d bin

load: game
	$(CLS)
	java -cp bin Game board.txt

jar: bin/*.class game
# jar cvf chess.jar *.class
	cd bin && jar cfm chess.jar manifest.txt *.class

clean:
	$(RM) -rf bin/*.class
	$(RM) bin/*.jar
