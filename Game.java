import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.Objects;
import java.util.ArrayList;

public class Game {
    public static void main(String[] args) {
        Game game = new Game();
        if (args.length == 1) {
            game.load(args[0]);
        }
        game.play();
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private Board board;
    private COLOR turn;

    public Game() {
        start();
    }

    private void start() {
        // Init board
        board = new Board();
        turn = COLOR.WHITE;
    }

    public void play() {
        clearScreen();
        board.draw();

        System.out.println("Type help to show all commands");

        Scanner sc = new Scanner(System.in);
        label:
        while (true) {
            // Wait for user inputs
            switch (turn) {
                case WHITE -> System.out.println("Current turn: White");
                case BLACK -> System.out.println("Current turn: Black");
            }

            System.out.print("Input: ");
            String input = sc.nextLine();

            switch (input) {
                case "help":
                case "h":
                case "?":
                    System.out.println("List of commands: end, help, load, quit, restart, save");
                    System.out.println("input:            a1-b2 (to move from a1-b2)");
                    System.out.println("castle:           0-0 (King side), 0-0-0 (Queen side).");
                    System.out.println("save:             Save the game board");
                    System.out.println("end:              End current turn.");
                    System.out.println("help:             Show this help message.");
                    System.out.println("load:             Load chess game from text file.");
                    System.out.println("restart:          Restart the chess game to default (Not to the loaded state).");
                    System.out.println("quit:             Quit the chess game.");
                    break;
                case "restart":
                case "r":
                    start();
                    break;
                case "quit":
                case "q":
                    break label;
                case "end":
                case "e":
                    endTurn();
                    break;
                case "save":
                case "s": {
                    System.out.print("Filename to save: ");
                    String fileName = sc.nextLine();

                    clearScreen();
                    board.draw();

                    if (!board.save(turn, fileName)) {
                        System.out.println("File save fail");
                    }
                    break;
                }
                case "load":
                case "l": {
                    System.out.print("Filename to load: ");
                    String fileName = sc.nextLine();

                    if (!load(fileName)) {
                        System.out.println("File load fail");
                    }
                    clearScreen();
                    board.draw();
                    break;
                }
                default:
                    // Check if valid input
                    String[] moves = input.split("-");

                    // For checking of castling notation 0-0 or 0-0-0
                    int zero = 0;
                    for (String elem : moves) {
                        if (elem.equals("0")) {
                            ++zero;
                        }
                    }

                    if (zero == 2 || zero == 3) {
                        if (!board.castle(turn, zero == 2 ? CASTLE.KING : CASTLE.QUEEN)) {
                            continue;
                        }
                    } else {
                        if (moves.length < 2) {
                            System.out.println("Invalid input, type help to learn more.");
                            continue;
                        }

                        // Check if valid moves
                        Location from = board.translateMove(moves[0]);
                        Location to = board.translateMove(moves[1]);

                        if (from == null || to == null) {
                            System.out.println("Invalid input, type help to learn more.");
                            continue;
                        }

                        Piece piece = board.getPiece(from);

                        if (piece == null) {
                            // Check if piece we are moving is a blank piece
                            System.out.println("Not a valid piece!");
                            continue;
                        }

                        if (piece.getColor() != turn) {
                            System.out.println("Not your turn yet!");
                            continue;
                        } else {
                            // Move pieces
                            if (board.movePiece(piece, to)) {
                                // Check for Pawn promotion
                                while (true) {
                                    Pawn promoted = board.checkPromotion();

                                    if (promoted == null) {
                                        break;
                                    }

                                    System.out.println("you can choose one of the following");
                                    System.out.print("queen,bishop, knight, rook: ");

                                    input = sc.nextLine();
                                    board.promotePawn(promoted, input);
                                }
                            } else {
                                continue;
                            }
                        }

                        if (board.hasWinner()) {
                            switch (turn) {
                                case WHITE -> {
                                    System.out.println("Congrats, white has win");
                                    return;
                                }
                                case BLACK -> {
                                    System.out.println("Congrats, black has win");
                                    return;
                                }
                            }
                        }
                    }
                    endTurn();
                    break;
            }
        }
    }

    private void endTurn() {
        switch (turn) {
            case WHITE -> turn = COLOR.BLACK;
            case BLACK -> turn = COLOR.WHITE;
        }

        clearScreen();
        // Draw board
        board.draw();
    }

    private boolean load(String fileName) {
        COLOR loadTurn = board.load(fileName);
        if (loadTurn == null)
            return false;

        turn = loadTurn;
        return true;
    }
}

enum PieceType {
    BLACK_PAWN   ('p'),
    WHITE_PAWN   ('P'),
    BLACK_ROOK   ('r'),
    WHITE_ROOK   ('R'),
    BLACK_KNIGHT ('n'),
    WHITE_KNIGHT ('N'),
    BLACK_BISHOP ('b'),
    WHITE_BISHOP ('B'),
    BLACK_QUEEN  ('q'),
    WHITE_QUEEN  ('Q'),
    BLACK_KING   ('k'),
    WHITE_KING   ('K');

    private final char icon;
    PieceType(char icon) {
        this.icon = icon;
    }
    public char getIcon() {
        return this.icon;
    }
}

enum COLOR {
    BLACK,
    WHITE
}

enum CASTLE {
    QUEEN, // 0-0-0
    KING   // 0-0
}

class Board {
    private Piece[] pieces;
    private final char[] boards;

    public Board() {
        boards = new char[64];

        initPieces();
        fillBoards();
        fillPieces();
    }

    public boolean hasWinner() {
        int count = 0;
        for (Piece piece : pieces) {
            if (piece == null) {
                continue;
            }

            if (piece.getType() == PieceType.BLACK_KING || piece.getType() == PieceType.WHITE_KING) {
                ++count;
            }
        }

        return count == 1;
    }

    public Pawn checkPromotion() {
        for (Piece piece : pieces) {
            if (piece == null) {
                continue;
            }

            if (piece.getType() == PieceType.BLACK_PAWN || piece.getType() == PieceType.WHITE_PAWN) {
                if (piece.getLocation().getY() == 8 || piece.getLocation().getY() == 0) {
                    System.out.println("Pawn Promotion");
                    return (Pawn)piece;
                }
            }
        }

        return null;
    }

    public boolean movePiece(Piece piece, Location to) {
        if (piece == null) {
            return false;
        }

        final Location from = piece.getLocation();

        // Check if from and to is the same
        if (from.equals(to)) {
            System.out.println("Cannot move to the same position!");
            return false;
        }

        // Check if within board
        if (!isInsideBoard(from) || !isInsideBoard(to)) {
            System.out.println("Moves must be inside the board!");
            return false;
        }

        // Check if piece can move according to rule
        Location[] locations = piece.getMoves(true);
        if (locations != null) {
            for (Location loc : locations) {
                if (loc.equals(to)) {
                    // If piece is king, check if destination is in check
                    if (piece.getType() == PieceType.BLACK_KING || piece.getType() == PieceType.WHITE_KING) {
                        if (isCheck(piece.getColor(), to)) {
                            System.out.println("King is moving to checked position");
                            return false;
                        }
                    }

                    if (piece.getType() == PieceType.BLACK_PAWN || piece.getType() == PieceType.WHITE_PAWN) {
                        // Check for En Passant
                        // Check if any pawn in between 2 pawns
                        int dir = piece.getType() == PieceType.BLACK_PAWN ? -1 : 1;
                        Location back = new Location(loc.getX(), loc.getY() + (2 * dir));
                        Piece backPiece = getPiece(back);
                        boolean hasBack = backPiece != null && piece.isSameType(backPiece);

                        Location mid = new Location(loc.getX(), loc.getY() + dir);
                        Piece midPiece = getPiece(mid);
                        boolean hasMid = midPiece != null && piece.isEnemy(midPiece);

                        if (hasMid && hasBack) {
                            removePiece(midPiece);
                        }
                    }

                    Piece destination = getPiece(to);
                    if (destination != null) {
                        removePiece(destination);
                    }
                    piece.move(to);
                    return true;
                }
            }
        }

        System.out.println("Not a valid move!");
        return false;
    }

    public Piece getPiece(Location loc) {
        if (isInsideBoard(loc)) {
            for (Piece piece : pieces) {
                if (piece == null) {
                    continue;
                }

                if (piece.getLocation().equals(loc)) {
                    return piece;
                }
            }
        }
        return null;
    }

    public Location translateMove(String input) {
        input = input.toUpperCase();
        if (input.length() == 2) {
            if (Character.isLetter(input.charAt(0))) {
                if (Character.isDigit(input.charAt(1))) {
                    final int x = input.charAt(0) - 'A';
                    final int y = input.charAt(1) - '1';
                    return new Location(x, 7 - y);
                }
            }
        }
        return null;
    }

    public boolean isInsideBoard(Location loc) {
        return loc.getX() >= 0 &&
                loc.getX() < 8 &&
                loc.getY() >= 0 &&
                loc.getY() < 8;
    }

    public void draw() {
        fillBoards();
        fillPieces();

        System.out.println("  a b c d e f g h");
        for (int y = 0; y < 8; ++y) {
            System.out.print(8 - y);
            System.out.print(' ');
            for (int x = 0; x < 8; ++x) {
                System.out.print(boards[y * 8 + x]);
                System.out.print(' ');
            }
            System.out.print(8 - y);
            System.out.println();
        }
        System.out.println("  a b c d e f g h");
    }

    public boolean isEmptySpace(Location at) {
        if (isInsideBoard(at)) {
            char board = boards[at.getY() * 8 + at.getX()];
            return board == '.';
        }
        return false;
    }

    private void initPieces() {
        pieces = new Piece[32];

        int id = 0;
        for (int y = 0; y < 8; ++y) {
            for (int x = 0; x < 8; ++x) {
                Location loc = new Location(x, y);

                if (y == 0) {
                    if (x == 0 || x == 7) {
                        pieces[id++] = new Rook(COLOR.BLACK, this, loc);
                    } else if (x == 1 || x == 6) {
                        pieces[id++] = new Knight(COLOR.BLACK, this, loc);
                    } else if (x == 2 || x == 5) {
                        pieces[id++] = new Bishop(COLOR.BLACK, this, loc);
                    } else if (x == 3) {
                        pieces[id++] = new Queen(COLOR.BLACK, this, loc);
                    } else {
                        pieces[id++] = new King(COLOR.BLACK, this, loc);
                    }
                } else if (y == 1) {
                    pieces[id++] = new Pawn(COLOR.BLACK, this, 1, loc);
                } else if (y == 6) {
                    pieces[id++] = new Pawn(COLOR.WHITE, this, -1, loc);
                } else if (y == 7) {
                    if (x == 0 || x == 7) {
                        pieces[id++] = new Rook(COLOR.WHITE, this, loc);
                    } else if (x == 1 || x == 6) {
                        pieces[id++] = new Knight(COLOR.WHITE, this, loc);
                    } else if (x == 2 || x == 5) {
                        pieces[id++] = new Bishop(COLOR.WHITE, this, loc);
                    } else if (x == 3) {
                        pieces[id++] = new Queen(COLOR.WHITE, this, loc);
                    } else {
                        pieces[id++] = new King(COLOR.WHITE, this, loc);
                    }
                }
            }
        }
    }

    private void fillBoards() {
        for (int y = 0; y < 8; ++y) {
            for (int x = 0; x < 8; ++x) {
                boards[y * 8 + x] = '.';
            }
        }
    }

    private void fillPieces() {
        for (Piece piece : pieces) {
            if (piece != null) {
                Location loc = piece.getLocation();
                if (loc != null) {
                    boards[loc.getY() * 8 + loc.getX()] = piece.getType().getIcon();
                }
            }
        }
    }

    public void promotePawn(Pawn pawn, String input) {
        if (pawn == null)
            return;

        final Location loc = pawn.getLocation();
        final COLOR color = pawn.getColor();

        switch (input) {
            case "q", "queen" -> replacePiece(pawn, new Queen(color, this, loc));
            case "b", "bishop" -> replacePiece(pawn, new Bishop(color, this, loc));
            case "k", "knight" -> replacePiece(pawn, new Knight(color, this, loc));
            case "r", "rook" -> replacePiece(pawn, new Rook(color, this, loc));
            default -> System.out.println("Wrong key detected, type q for queen, b for bishop, k for knight, r for rook");
        }
    }

    private void replacePiece(Piece oldPiece, Piece newPiece) {
        if (oldPiece == null || newPiece == null) {
            return;
        }

        for (int i = 0; i < pieces.length; ++i) {
            if (pieces[i] == null) {
                continue;
            }

            if (pieces[i].equals(oldPiece)) {
                pieces[i] = newPiece;
            }
        }
    }

    private void removePiece(Piece toRemove) {
        for (int i = 0; i < pieces.length; ++i) {
            Piece piece = pieces[i];
            if (piece == null) {
                continue;
            }

            if (piece.equals(toRemove)) {
                pieces[i] = null;
                return;
            }
        }
    }

    public boolean isCheck(COLOR color, Location loc) {
        for (Piece piece : pieces) {
            if (piece == null) {
                continue;
            }

            if (piece.getColor() == color) {
                continue;
            }

            for (Location move : piece.getMoves(false)) {
                if (loc.equals(move)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean castle(COLOR color, CASTLE side) {
        System.out.println("Castle");

        Location kingLoc = null;
        Location rookLoc = null;
        Location newKingSideLoc = null;
        Location newKingLoc = null;
        Location newRookLoc = null;

        // King side
        // Queen side
        // King side
        // Queen side
        switch (color) {
            case BLACK -> {
                kingLoc = new Location(4, 0);
                switch (side) {
                    case KING -> {
                        rookLoc = new Location(7, 0);
                        newKingLoc = new Location(6, 0);
                        newRookLoc = new Location(5, 0);
                        newKingSideLoc = new Location(5, 0);
                    }
                    case QUEEN -> {
                        rookLoc = new Location(0, 0);
                        newKingLoc = new Location(2, 0);
                        newRookLoc = new Location(3, 0);
                        newKingSideLoc = new Location(3, 0);
                    }
                }
            }
            case WHITE -> {
                kingLoc = new Location(4, 7);
                switch (side) {
                    case KING -> {
                        rookLoc = new Location(7, 7);
                        newKingLoc = new Location(6, 7);
                        newRookLoc = new Location(5, 7);
                        newKingSideLoc = new Location(3, 7);
                    }
                    case QUEEN -> {
                        rookLoc = new Location(0, 7);
                        newKingLoc = new Location(2, 7);
                        newRookLoc = new Location(3, 7);
                        newKingSideLoc = new Location(3, 7);
                    }
                }
            }
        }

        if (kingLoc != null && rookLoc != null) {
            if (!isEmptySpace(newKingLoc) || !isEmptySpace(newRookLoc)) {
                System.out.println("King or Rook space is occupied");
                return false;
            }

            Piece king = getPiece(kingLoc);
            Piece rook = getPiece(rookLoc);

            if (king != null && rook != null) {
                // 1. Check if moved before
                if (king.moved() || rook.moved()) {
                    System.out.println("King or Rook have been moved!");
                    return false;
                }

                // 2. Check if king is not in checks
                if (isCheck(king.getColor(), kingLoc)) {
                    System.out.println("King is in checks!");
                    return false;
                }

                // 3. Check if spaces are in checks
                if (isCheck(king.getColor(), newKingLoc) || isCheck(king.getColor(), newKingSideLoc)) {
                    System.out.println("Spaces is in checks for castling!");
                    return false;
                }


                king.move(newKingLoc);
                rook.move(newRookLoc);
                return true;
            }
        }
        return false;
    }

    public boolean save(COLOR turn, String fileName) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))){
            writer.printf("%s\r\n", turn);
            writer.printf("%d\r\n", pieceCount());

            for (Piece piece : pieces) {
                if (piece == null) {
                    continue;
                }
                writer.printf("%s \r\n%d %d\r\n", piece.getType(), piece.getLocation().getX(), piece.getLocation().getY());
            }
            System.out.println("Board saved as " + fileName);
            return true;
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return false;
    }

    public COLOR load(String fileName) {
        PieceFactory factory = new PieceFactory(this);

        COLOR turn = null;
        try (Scanner scanner = new Scanner(new FileReader(fileName))){
            if (scanner.hasNext()) {
                String color = scanner.next();
                turn = COLOR.valueOf(color.toUpperCase());
            }

            int count = 0;
            if (scanner.hasNextInt()) {
                count = scanner.nextInt();
            }

            pieces = new Piece[count];

            for (int i = 0; i < count; ++i) {
                if (scanner.hasNext()) {
                    String piece = scanner.next();
                    PieceType type = PieceType.valueOf(piece.toUpperCase());

                    int x = 0;
                    if (scanner.hasNextInt()) {
                        x = scanner.nextInt();
                    }

                    int y = 0;
                    if (scanner.hasNextInt()) {
                        y = scanner.nextInt();
                    }

                    pieces[i] = factory.getPiece(type, new Location(x, y));
                }
            }

            System.out.println("Board loaded from " + fileName);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        return turn;
    }

    private int pieceCount() {
        int count = 0;
        for (Piece piece : pieces) {
            if (piece == null) {
                continue;
            }
            ++count;
        }
        return count;
    }
}

abstract class Piece {
    private PieceType type;
    private COLOR color;
    private final Board board;
    private boolean hasMoved;
    private Location location;

    public Piece(Board board, Location loc) {
        this.board = board;
        this.location = loc;
        hasMoved = false;
    }

    abstract public Location[] getMoves(boolean moving);

    public void move(Location loc) {
        location = loc;
        hasMoved = true;
    }

    public PieceType getType() {
        return type;
    }

    public COLOR getColor() {
        return color;
    }

    protected Board getBoard() {
        return board;
    }

    public Location getLocation() {
        return location;
    }

    public void set(PieceType type, COLOR color) {
        this.type = type;
        this.color = color;
    }

    public boolean isEnemy(Piece piece) {
        return color != piece.color;
    }

    public boolean isSameType(Piece piece) {
        return this.type == piece.getType();
    }

    public boolean moved() {
        return hasMoved;
    }

    public String toString() {
        return "Loc: " + location + " Piece Type: " + type + " Color: " + color +
            " Has Moved: " + hasMoved;
    }
}

class Pawn extends Piece {
    private final int dir;

    public Pawn(COLOR color, Board board, int dir, Location loc) {
        super(board, loc);

        switch (color) {
            case BLACK -> set(PieceType.BLACK_PAWN, COLOR.BLACK);
            case WHITE -> set(PieceType.WHITE_PAWN, COLOR.WHITE);
        }

        this.dir = dir;
    }

    private boolean hasDiagonal(Location at) {
        Piece piece = getBoard().getPiece(at);
        return piece != null && isEnemy(piece) && !getBoard().isEmptySpace(at);
    }

    private boolean hasHorizontal(Location at) {
        Piece piece = getBoard().getPiece(at);
        return piece != null && isEnemy(piece) && !getBoard().isEmptySpace(at);
    }

    private boolean canEnPassant(Location front, Location side) {
        Location back = new Location(side.getX(), side.getY() - dir);

        // Check if there is pawn blocking
        Piece piece = getBoard().getPiece(back);
        boolean isValidPawn = piece != null && isSameType(piece);
        return isValidPawn && getBoard().isEmptySpace(front);
    }

    public Location[] getMoves(boolean moving) {
        ArrayList<Location> arrayList = new ArrayList<>();

        final Location at = getLocation();

        {
            // If pawn is at original position, we can move ahead two steps if it is not blocked
            if (getColor() == COLOR.WHITE && at.getY() == 6 || getColor() == COLOR.BLACK && at.getY() == 1) {

                Location frontLoc = new Location(at.getX(), at.getY() + dir);
                Location frontTwoLoc = new Location(at.getX(), at.getY() + dir + dir);

                if (getBoard().isEmptySpace(frontLoc) && getBoard().isEmptySpace(frontTwoLoc)) {
                    arrayList.add(frontTwoLoc);
                }
            }
        }

        {
            // Moving front
            Location frontLoc = new Location(at.getX(), at.getY() + dir);
            if (getBoard().isEmptySpace(frontLoc)) {
                arrayList.add(frontLoc);
            }
        }

        
        // Diagonals
        {
            Location frontLeftLoc = new Location(at.getX() - 1, at.getY() + dir);
            Location leftLoc = new Location(at.getX() - 1, at.getY());
            if (!moving || hasDiagonal(frontLeftLoc) || canEnPassant(frontLeftLoc, leftLoc)) {
                arrayList.add(frontLeftLoc);
            }
        }
        {
            Location frontRightLoc = new Location(at.getX() + 1, at.getY() + dir);
            Location rightLoc = new Location(at.getX() + 1, at.getY());
            if (!moving || hasDiagonal(frontRightLoc) || canEnPassant(frontRightLoc, rightLoc)) {
                arrayList.add(frontRightLoc);
            }
        }

        return arrayList.toArray(new Location[0]);
    }
}

class Rook extends Piece {
    public Rook(COLOR color, Board board, Location loc) {
        super(board, loc);

        switch (color) {
            case BLACK -> set(PieceType.BLACK_ROOK, COLOR.BLACK);
            case WHITE -> set(PieceType.WHITE_ROOK, COLOR.WHITE);
        }
    }

    public Location[] getMoves(boolean moving) {
        ArrayList<Location> arrayList = new ArrayList<>();

        final Location at = getLocation();

        // Horizontal left
        for (int x = at.getX() - 1; x >= 0; --x) {
            Location loc = new Location(x, at.getY());
            if (getBoard().isEmptySpace(loc)) {
                arrayList.add(loc);
            } else {
                Piece piece = getBoard().getPiece(loc);
                if (isEnemy(piece)) {
                    arrayList.add(loc);
                }
                break;
            }
        }

        // Horizontal right
        for (int x = at.getX() + 1; x < 8; ++x) {
            Location loc = new Location(x, at.getY());
            if (getBoard().isEmptySpace(loc)) {
                arrayList.add(loc);
            } else {
                Piece piece = getBoard().getPiece(loc);
                if (isEnemy(piece)) {
                    arrayList.add(loc);
                }
                break;
            }
        }


        // Vertical top
        for (int y = at.getY() - 1; y >= 0; --y) {
            Location loc = new Location(at.getX(), y);
            if (getBoard().isEmptySpace(loc)) {
                arrayList.add(loc);
            } else {
                Piece piece = getBoard().getPiece(loc);
                if (isEnemy(piece)) {
                    arrayList.add(loc);
                }
                break;
            }
        }

        // Vertical bottom
        for (int y = at.getY() + 1; y < 8; ++y) {
            Location loc = new Location(at.getX(), y);
            if (getBoard().isEmptySpace(loc)) {
                arrayList.add(loc);
            } else {
                Piece piece = getBoard().getPiece(loc);
                if (isEnemy(piece)) {
                    arrayList.add(loc);
                }
                break;
            }
        }

        return arrayList.toArray(new Location[0]);
    }
}

class Knight extends Piece {
    public Knight(COLOR color, Board board, Location loc) {
        super(board, loc);

        switch (color) {
            case BLACK -> set(PieceType.BLACK_KNIGHT, COLOR.BLACK);
            case WHITE -> set(PieceType.WHITE_KNIGHT, COLOR.WHITE);
        }
    }

    public Location[] getMoves(boolean moving ) {
        ArrayList<Location> arrayList = new ArrayList<>();

        final Location at = getLocation();

        final int x = at.getX();
        final int y = at.getY();

        Location[] locations = {
                // Top L
                new Location(x - 1, y - 2),
                new Location(x + 1, y - 2),

                // Left L
                new Location(x - 2, y - 1),
                new Location(x - 2, y + 1),

                // Right L
                new Location(x + 2, y - 1),
                new Location(x + 2, y + 1),

                // Bottom L
                new Location(x - 1, y + 2),
                new Location(x + 1, y + 2)
        };

        for (Location loc : locations) {
            if (getBoard().isEmptySpace(loc)) {
                arrayList.add(loc);
            }
        }

        return arrayList.toArray(new Location[0]);
    }
}

class Bishop extends Piece {
    public Bishop(COLOR color, Board board, Location loc) {
        super(board, loc);

        switch (color) {
            case BLACK -> set(PieceType.BLACK_BISHOP, COLOR.BLACK);
            case WHITE -> set(PieceType.WHITE_BISHOP, COLOR.WHITE);
        }
    }

    public Location[] getMoves(boolean moving) {
        ArrayList<Location> arrayList = new ArrayList<>();

        final Location at = getLocation();

        // Diagonal top left
        for (int x = at.getX() - 1, y = at.getY() - 1; x >= 0 && y >= 0; --x, --y) {
            Location loc = new Location(x, y);
            if (getBoard().isEmptySpace(loc)) {
                arrayList.add(loc);
            } else {
                Piece piece = getBoard().getPiece(loc);
                if (isEnemy(piece)) {
                    arrayList.add(loc);
                }
                break;
            }
        }

        // Diagonal bottom right
        for (int x = at.getX() + 1, y = at.getY() + 1; x < 8 && y < 8; ++x, ++y) {
            Location loc = new Location(x, y);
            if (getBoard().isEmptySpace(loc)) {
                arrayList.add(loc);
            } else {
                Piece piece = getBoard().getPiece(loc);
                if (isEnemy(piece)) {
                    arrayList.add(loc);
                }
                break;
            }
        }

        // Diagonal top right
        for (int x = at.getX() + 1, y = at.getY() - 1; x < 8 && y >= 0; ++x, --y) {
            Location loc = new Location(x, y);
            if (getBoard().isEmptySpace(loc)) {
                arrayList.add(loc);
            } else {
                Piece piece = getBoard().getPiece(loc);
                if (isEnemy(piece)) {
                    arrayList.add(loc);
                }
                break;
            }
        }

        // Diagonal bottom left
        for (int x = at.getX() - 1, y = at.getY() + 1; x >= 0 && y < 8; --x, ++y) {
            Location loc = new Location(x, y);
            if (getBoard().isEmptySpace(loc)) {
                arrayList.add(loc);
            } else {
                Piece piece = getBoard().getPiece(loc);
                if (isEnemy(piece)) {
                    arrayList.add(loc);
                }
                break;
            }
        }

        return arrayList.toArray(new Location[0]);
    }
}

class Queen extends Piece {
    public Queen(COLOR color, Board board, Location loc) {
        super(board, loc);

        switch (color) {
            case BLACK -> set(PieceType.BLACK_QUEEN, COLOR.BLACK);
            case WHITE -> set(PieceType.WHITE_QUEEN, COLOR.WHITE);
        }
    }

    public Location[] getMoves(boolean moving) {
        ArrayList<Location> arrayList = new ArrayList<>();

        final Location at = getLocation();

        // Diagonal top left
        for (int x = at.getX() - 1, y = at.getY() - 1; x >= 0 && y >= 0; --x, --y) {
            Location loc = new Location(x, y);
            if (getBoard().isEmptySpace(loc)) {
                arrayList.add(loc);
            } else {
                Piece piece = getBoard().getPiece(loc);
                if (isEnemy(piece)) {
                    arrayList.add(loc);
                }
                break;
            }
        }

        // Diagonal bottom right
        for (int x = at.getX() + 1, y = at.getY() + 1; x < 8 && y < 8; ++x, ++y) {
            Location loc = new Location(x, y);
            if (getBoard().isEmptySpace(loc)) {
                arrayList.add(loc);
            } else {
                Piece piece = getBoard().getPiece(loc);
                if (isEnemy(piece)) {
                    arrayList.add(loc);
                }
                break;
            }
        }

        // Diagonal top right
        for (int x = at.getX() + 1, y = at.getY() - 1; x < 8 && y >= 0; ++x, --y) {
            Location loc = new Location(x, y);
            if (getBoard().isEmptySpace(loc)) {
                arrayList.add(loc);
            } else {
                Piece piece = getBoard().getPiece(loc);
                if (isEnemy(piece)) {
                    arrayList.add(loc);
                }
                break;
            }
        }

        // Diagonal bottom left
        for (int x = at.getX() - 1, y = at.getY() + 1; x >= 0 && y < 8; --x, ++y) {
            Location loc = new Location(x, y);
            if (getBoard().isEmptySpace(loc)) {
                arrayList.add(loc);
            } else {
                Piece piece = getBoard().getPiece(loc);
                if (isEnemy(piece)) {
                    arrayList.add(loc);
                }
                break;
            }
        }

        // Horizontal left
        for (int x = at.getX() - 1; x >= 0; --x) {
            Location loc = new Location(x, at.getY());
            if (getBoard().isEmptySpace(loc)) {
                arrayList.add(loc);
            } else {
                Piece piece = getBoard().getPiece(loc);
                if (isEnemy(piece)) {
                    arrayList.add(loc);
                }
                break;
            }
        }

        // Horizontal right
        for (int x = at.getX() + 1; x < 8; ++x) {
            Location loc = new Location(x, at.getY());
            if (getBoard().isEmptySpace(loc)) {
                arrayList.add(loc);
            } else {
                Piece piece = getBoard().getPiece(loc);
                if (isEnemy(piece)) {
                    arrayList.add(loc);
                }
                break;
            }
        }


        // Vertical top
        for (int y = at.getY() - 1; y >= 0; --y) {
            Location loc = new Location(at.getX(), y);
            if (getBoard().isEmptySpace(loc)) {
                arrayList.add(loc);
            } else {
                Piece piece = getBoard().getPiece(loc);
                if (isEnemy(piece)) {
                    arrayList.add(loc);
                }
                break;
            }
        }

        // Vertical bottom
        for (int y = at.getY() + 1; y < 8; ++y) {
            Location loc = new Location(at.getX(), y);
            if (getBoard().isEmptySpace(loc)) {
                arrayList.add(loc);
            } else {
                Piece piece = getBoard().getPiece(loc);
                if (isEnemy(piece)) {
                    arrayList.add(loc);
                }
                break;
            }
        }
        return arrayList.toArray(new Location[0]);
    }
}

class King extends Piece {
    public King(COLOR color, Board board, Location loc) {
        super(board, loc);

        switch (color) {
            case BLACK -> set(PieceType.BLACK_KING, COLOR.BLACK);
            case WHITE -> set(PieceType.WHITE_KING, COLOR.WHITE);
        }
    }

    public Location[] getMoves(boolean moving) {
        ArrayList<Location> arrayList = new ArrayList<>();

        final Location at = getLocation();
        final int x = at.getX();
        final int y = at.getY();

        Location[] locations = {
                // Top left
                new Location(x - 1, y - 1),

                // Top
                new Location(x, y - 1),

                // Top right
                new Location(x + 1, y - 1),

                // Left
                new Location(x - 1, y),

                // Right
                new Location(x + 1, y),

                // Bottom left
                new Location(x - 1, y + 1),

                // Bottom
                new Location(x, y + 1),

                // Bottom right
                new Location(x + 1, y + 1),
        };

        for (Location loc : locations) {
            if (getBoard().isEmptySpace(loc)) {
                arrayList.add(loc);
            }
        }

        return arrayList.toArray(new Location[0]);
    }
}

class Location {
    private final int x;
    private final int y;

    public Location(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String toString() {
        return "x: " + x + " y: " + y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return false;
        if (o == null)
            return false;
        if (getClass() != o.getClass())
            return false;
        Location loc = (Location)o;
        return loc.getX() == x && loc.getY() == y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}

class PieceFactory {
    private final Board board;

    public PieceFactory(Board board) {
        this.board = board;
    }

    public Piece getPiece(PieceType type, Location loc) {
        return switch (type) {
            case WHITE_PAWN -> new Pawn(COLOR.WHITE, board, -1, loc);
            case WHITE_ROOK -> new Rook(COLOR.WHITE, board, loc);
            case WHITE_KNIGHT -> new Knight(COLOR.WHITE, board, loc);
            case WHITE_BISHOP -> new Bishop(COLOR.WHITE, board, loc);
            case WHITE_QUEEN -> new Queen(COLOR.WHITE, board, loc);
            case WHITE_KING -> new King(COLOR.WHITE, board, loc);
            case BLACK_PAWN -> new Pawn(COLOR.BLACK, board, 1, loc);
            case BLACK_ROOK -> new Rook(COLOR.BLACK, board, loc);
            case BLACK_KNIGHT -> new Knight(COLOR.BLACK, board, loc);
            case BLACK_BISHOP -> new Bishop(COLOR.BLACK, board, loc);
            case BLACK_QUEEN -> new Queen(COLOR.BLACK, board, loc);
            case BLACK_KING -> new King(COLOR.BLACK, board, loc);
        };
    }
}
