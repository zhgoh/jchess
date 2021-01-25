all: game
	clear
	java Game

game: Game.java
	javac Game.java
