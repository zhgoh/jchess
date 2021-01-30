import java.util.Scanner;
import java.util.Objects;
import java.util.ArrayList;

public class Game {
  public static void main(String[] args) {
    Game game = new Game();
    game.play();
  }

  private void clearScreen() {  
    System.out.print("\033[H\033[2J");  
    System.out.flush();  
  }  

  private Board board;
  private COLOR turn;

  public Game() {
    // Init board
    board = new Board();
    turn = COLOR.WHITE;
  }

  public void play() {
    clearScreen();
    board.draw();

    Scanner sc = new Scanner(System.in);
    while (true) {
      // Wait for user inputs
      switch (turn) {
        case WHITE:
          System.out.println("Current turn: White");
          break;

        case BLACK:
          System.out.println("Current turn: Black");
          break;
      }

      System.out.print("Input move: ");
      String input = sc.nextLine();

      if (input.equals("help") || input.equals("h") || input.equals("?")) {
        System.out.println("List of commands: (e)nd, (h)elp, (q)uit");
        System.out.println("Input:            A1-B2 (to move from A1-B2)");
        System.out.println("Castle:           0-0 (King side), 0-0-0 (Queen side).");
        System.out.println("End:              End current turn.");
        System.out.println("Help:             Show this help message.");
        System.out.println("Quit:             Quit the chess app.");
        continue;
      }
      else if (input.equals("quit") || input.equals("q")) {
        break;
      } else if (input.equals("end") || input.equals("e")) {
        endTurn();
        continue;
      }

      // Check if valid input
      String[] moves = input.split("-");

      // For checking of castling notation 0-0 or 0-0-0
      int zero = 0;
      for (String elem : moves) {
        if (elem.equals("0")) {
          ++zero;
        }
      }

      // if (zero != moves.length && moves.length != 2) {
      //   System.out.println("Invalid input, type help to learn more.");
      //   continue;
      // }

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
              System.out.print("[q]ueen,[b]ishop, [k]night, [r]ook: ");

              input = sc.nextLine();
              board.promotePawn(promoted, input);
            }
          } else {
            continue;
          }
        }

        if (board.hasWinner()) {
          switch (turn) {
            case WHITE:
              System.out.println("Congrats, white has win");
              return;

            case BLACK:
              System.out.println("Congrats, black has win");
              return;
          }
        }
      }

      endTurn();
    }
  }

private void endTurn() {
    switch (turn) {
      case WHITE:
        turn = COLOR.BLACK;
        break;

      case BLACK:
        turn = COLOR.WHITE;
        break;
    }

    clearScreen();
    // Draw board
    // https://qwerty.dev/chess-symbols-to-copy-and-paste/
    board.draw();
  }
}

enum PIECE_TYPE {
  BLACK_SQUARE,
  WHITE_SQUARE,
  BLACK_PAWN,
  WHITE_PAWN,
  BLACK_ROOK,
  WHITE_ROOK,
  BLACK_KNIGHT,
  WHITE_KNIGHT,
  BLACK_BISHOP,
  WHITE_BISHOP,
  BLACK_QUEEN,
  WHITE_QUEEN,
  BLACK_KING,
  WHITE_KING
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
  private char[] boards;

  private char WHITE = ' ';
  private char BLACK = '░';

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

      if (piece.getType() == PIECE_TYPE.BLACK_KING ||
          piece.getType() == PIECE_TYPE.WHITE_KING) {
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

      if (piece.getType() == PIECE_TYPE.BLACK_PAWN || 
          piece.getType() == PIECE_TYPE.WHITE_PAWN) {
        if (piece.getLocation().getY() == 8 ||
            piece.getLocation().getY() == 0) {
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
          if (piece.getType() == PIECE_TYPE.BLACK_KING ||
              piece.getType() == PIECE_TYPE.WHITE_KING) {
            if (isCheck(piece.getColor(), to)) {
              System.out.println("King is moving to checked position");
              return false;
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

    final int bh = 17;
    final int bw = 17;

    for (int y = 0, id = 0, index = 8; y < bh; ++y) {
      for (int x = 0; x < bw; ++x) {
        if (y == 0) {               // Top piece
          if (x == 0) {
            System.out.print("╔");
          } else if (x == (bw - 1)) {
            System.out.print("╗╮");
          } else {
            if (x % 2 == 0) {
              System.out.print("╤");
            } else {
              System.out.print("═");
            }
          }
        } else if (y == (bh - 1)) { // Bottom piece
          if (x == 0) {
            System.out.print("╚");
          } else if (x == (bw - 1)) {
            System.out.print("╝┊");
          } else {
            if (x % 2 == 0) {
              System.out.print("╧");
            } else {
              System.out.print("═");
            }
          }
        } else {                    // Center
          if (x == 0) {
            if (y % 2 == 0) {
              System.out.print("╟");
            } else {
              System.out.print("║");
            }
          } else if (x == (bw - 1)) {
            if (y % 2 == 0) {
              System.out.print("╢┊");
            } else {
              System.out.print("║");
              System.out.print(index--);
            }
          } else {
            if (y % 2 == 0) {
              if (x % 2 == 0) {
                System.out.print("┼");
              } else {
                System.out.print("─");
              }
            } else {
              if (x % 2 == 0) {
                System.out.print("│");
              } else {
                // Draw pieces
                System.out.print(boards[id]);
                ++id;
              }
            }
          }
        }
      }
      System.out.println();
    }
    System.out.println("╰A┈B┈C┈D┈E┈F┈G┈H┈╯");
  }

  public boolean isEmptySpace(Location at) {
    if (isInsideBoard(at)) {
      char board = boards[at.getY() * 8 + at.getX()];
      return board == WHITE || board == BLACK;
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
            int r = (x == 0) ? 0 : 1;
            pieces[id++] = new Rook(COLOR.BLACK, this, loc);
          } else if (x == 1 || x == 6) {
            pieces[id++] = new Knight(COLOR.BLACK, this, loc);
          } else if (x == 2 || x == 5) {
            pieces[id++] = new Bishop(COLOR.BLACK, this, loc);
          } else if (x == 3) {
            pieces[id++] = new Queen(COLOR.BLACK, this, loc);
          } else if (x == 4) {
            pieces[id++] = new King(COLOR.BLACK, this, loc);
          }
        } else if (y == 1) {
          pieces[id++] = new Pawn(COLOR.BLACK, this, 1, loc);
        } else if (y == 6) {
          pieces[id++] = new Pawn(COLOR.WHITE, this, -1, loc);
        } else if (y == 7) {
          if (x == 0 || x == 7) {
            int r = (x == 0) ? 0 : 1;
            pieces[id++] = new Rook(COLOR.WHITE, this, loc);
          } else if (x == 1 || x == 6) {
            pieces[id++] = new Knight(COLOR.WHITE, this, loc);
          } else if (x == 2 || x == 5) {
            pieces[id++] = new Bishop(COLOR.WHITE, this, loc);
          } else if (x == 3) {
            pieces[id++] = new Queen(COLOR.WHITE, this, loc);
          } else if (x == 4) {
            pieces[id++] = new King(COLOR.WHITE, this, loc);
          }
        }
      }
    }
  }

  private void fillBoards() {
    for (int y = 0; y < 8; ++y) {
      for (int x = 0; x < 8; ++x) {
        if (x % 2 == 0) {
          if (y % 2 == 0) {
            boards[y * 8 + x] = WHITE;
          } else {
            boards[y * 8 + x] = BLACK;
          }
        } else {
          if (y % 2 == 1) {
            boards[y * 8 + x] = WHITE;
          } else {
            boards[y * 8 + x] = BLACK;
          }
        }
      }
    }
  }

  private void fillPieces() {
    for (int i = 0; i < pieces.length; ++i) {
      Piece piece = pieces[i];
      if (piece != null) {
        Location loc = piece.getLocation();
        if (loc != null) {
          boards[loc.getY() * 8 + loc.getX()] = piece.getIcon();
        }
      }
    }
  }

  public boolean promotePawn(Pawn pawn, String input) {
    if (pawn == null)
      return false;

    final Location loc = pawn.getLocation();
    final COLOR color = pawn.getColor();

    switch (input) {
      case "q":
      case "queen":
        replacePiece(pawn, new Queen(color, this, loc));
        return true;

      case "b":
      case "bishop":
        replacePiece(pawn, new Bishop(color, this, loc));
        return true;

      case "k":
      case "knight":
        replacePiece(pawn, new Knight(color, this, loc));
        return true;

      case "r":
      case "rook":
        replacePiece(pawn, new Rook(color, this, loc));
        return true;

      default:
        System.out.println("Wrong key detected, type q for queen, b for bishop, k for knight, r for rook");
        return false;
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

  private Piece getPieceBy(COLOR color, PIECE_TYPE type) {
    for (Piece piece : pieces)  {
      if (piece == null) {
        continue;
      }

      if (piece.getColor() == color && piece.getType() == type) {
        return piece;
      }
    }
    return null;
  }

  public boolean castle(COLOR color, CASTLE side) {
    System.out.println("Castle");

    Location kingLoc = null;
    Location rookLoc = null;
    Location newKingSideLoc = null;
    Location newKingLoc = null;
    Location newRookLoc = null;

    switch (color) {
      case BLACK:
        kingLoc = new Location(4, 0);

        switch (side) {
          case KING:
            // King side
            rookLoc = new Location(7, 0);
            newKingLoc = new Location(6, 0);
            newRookLoc = new Location(5, 0);
            newKingSideLoc = new Location(5, 0);
            break;

          case QUEEN:
            // Queen side
            rookLoc = new Location(0, 0);
            newKingLoc = new Location(2, 0);
            newRookLoc = new Location(3, 0);
            newKingSideLoc = new Location(3, 0);
            break;
        }

        break;

      case WHITE:
        kingLoc = new Location(4, 7);

        switch (side) {
          case KING:
            // King side
            rookLoc = new Location(7, 7);
            newKingLoc = new Location(6, 7);
            newRookLoc = new Location(5, 7);
            newKingSideLoc = new Location(3, 7);
            break;

          case QUEEN:
            // Queen side
            rookLoc = new Location(0, 7);
            newKingLoc = new Location(2, 7);
            newRookLoc = new Location(3, 7);
            newKingSideLoc = new Location(3, 7);
            break;
        }
        break;
    }

    if (kingLoc != null && rookLoc != null && newKingLoc != null && newRookLoc != null && newKingSideLoc != null) {
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
}


abstract class Piece {
  private PIECE_TYPE type;
  private char icon;
  private COLOR color;
  private Board board;
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

  public PIECE_TYPE getType() {
    return type;
  }

  public char getIcon() {
    return icon;
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

  public void set(PIECE_TYPE type, char icon, COLOR color) {
    this.type = type;
    this.icon = icon;
    this.color = color;
  }

  public boolean isEnemy(Piece piece) {
    return color != piece.color;
  }

  public boolean moved() {
    return hasMoved;
  }
}

class Pawn extends Piece {
  private int dir;

  public Pawn(COLOR color, Board board, int dir, Location loc) {
    super(board, loc);

    switch (color) {
      case BLACK:
        set(PIECE_TYPE.BLACK_PAWN, '♟', COLOR.BLACK);
        break;
      case WHITE:
        set(PIECE_TYPE.WHITE_PAWN, '♙', COLOR.WHITE);
        break;
    }

    this.dir = dir;
  }

  private boolean hasDiagonal(Location at) {
    Piece piece = getBoard().getPiece(at);
    if (piece != null && 
        isEnemy(piece) &&
        !getBoard().isEmptySpace(at)) {
      return true;
        }
    return false;
  }

  public Location[] getMoves(boolean moving) {
    ArrayList<Location> arrayList = new ArrayList<>();

    final Location at = getLocation();

    {
      // If pawn is at original position, we can move ahead two steps if it is not blocked
      if (getColor() == COLOR.WHITE && at.getY() == 6 ||
          getColor() == COLOR.BLACK && at.getY() == 1) {

        Location frontLoc = new Location(at.getX(), at.getY() + dir);
        Location frontTwoLoc = new Location(at.getX(), at.getY() + dir + dir);

        if (getBoard().isEmptySpace(frontLoc) && getBoard().isEmptySpace(frontTwoLoc)) {
          arrayList.add(frontTwoLoc);
        }
          }
    }

    {
      Location frontLoc = new Location(at.getX(), at.getY() + dir);
      if (getBoard().isEmptySpace(frontLoc)) {
        arrayList.add(frontLoc);
      }
    }

    Location frontLeftLoc = new Location(at.getX() - 1, at.getY() + dir);
    if (!moving || hasDiagonal(frontLeftLoc)) {
      arrayList.add(frontLeftLoc);
    }

    Location frontRightLoc = new Location(at.getX() + 1, at.getY() + dir);
    if (!moving || hasDiagonal(frontRightLoc)) {
      arrayList.add(frontRightLoc);
    }

    return arrayList.toArray(new Location[arrayList.size()]);
  }
}

class Rook extends Piece {
  public Rook(COLOR color, Board board, Location loc) {
    super(board, loc);

    switch (color) {
      case BLACK:
        set(PIECE_TYPE.BLACK_ROOK, '♜', COLOR.BLACK);
        break;
      case WHITE:
        set(PIECE_TYPE.WHITE_ROOK, '♖', COLOR.WHITE);
        break;
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

    return arrayList.toArray(new Location[arrayList.size()]);
  }
}

class Knight extends Piece {
  public Knight(COLOR color, Board board, Location loc) {
    super(board, loc);

    switch (color) {
      case BLACK:
        set(PIECE_TYPE.BLACK_KNIGHT, '♞', COLOR.BLACK);
        break;
      case WHITE:
        set(PIECE_TYPE.WHITE_KNIGHT, '♘', COLOR.WHITE);
        break;
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

    return arrayList.toArray(new Location[arrayList.size()]);
  }
}

class Bishop extends Piece {
  public Bishop(COLOR color, Board board, Location loc) {
    super(board, loc);

    switch (color) {
      case BLACK:
        set(PIECE_TYPE.BLACK_BISHOP, '♝', COLOR.BLACK);
        break;
      case WHITE:
        set(PIECE_TYPE.WHITE_BISHOP, '♗', COLOR.WHITE);
        break;
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

    return arrayList.toArray(new Location[arrayList.size()]);
  }
}

class Queen extends Piece {
  public Queen(COLOR color, Board board, Location loc) {
    super(board, loc);

    switch (color) {
      case BLACK:
        set(PIECE_TYPE.BLACK_QUEEN, '♛', COLOR.BLACK);
        break;
      case WHITE:
        set(PIECE_TYPE.WHITE_QUEEN, '♕', COLOR.WHITE);
        break;
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
    return arrayList.toArray(new Location[arrayList.size()]);
  }
}

class King extends Piece {
  public King(COLOR color, Board board, Location loc) {
    super(board, loc);

    switch (color) {
      case BLACK:
        set(PIECE_TYPE.BLACK_KING, '♚', COLOR.BLACK);
        break;
      case WHITE:
        set(PIECE_TYPE.WHITE_KING, '♔', COLOR.WHITE);
        break;
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

    return arrayList.toArray(new Location[arrayList.size()]);
  }
}

class Location {
  private int x;
  private int y;

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
