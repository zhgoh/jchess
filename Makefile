ifdef OS
	RM = del /Q
	FixPath = $(subst /,\,$1)
	CLS = cls
	SET_UNICODE = chcp 65001
else
   ifeq ($(shell uname), Linux)
		RM = rm -f
		FixPath = $1
		CLS = clear
		SET_UNICODE =
   endif
endif

all: game
	$(SET_UNICODE)
	$(CLS)
	java -Dfile.encoding=UTF8 -cp bin Game

game: Game.java
	javac -encoding utf8 Game.java -d bin

load: game
	$(CLS)
	java -cp bin Game board.txt

jar: bin/*.class game
# jar cvf chess.jar *.class
	cd bin && jar cfm chess.jar manifest.txt *.class

clean:
	$(RM) $(call FixPath, bin/*.class)
	$(RM) $(call FixPath, bin/*.jar)
