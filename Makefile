all: game
	clear
	java -cp bin Game

game: Game.java
	javac Game.java -d bin

jar: bin/*.class game
	# jar cvf chess.jar *.class
	cd bin && jar cfm chess.jar manifest.txt *.class

clean:
	rm -rf bin/*.class
	rm bin/*.jar
