import java.util.Scanner;
import java.util.Objects;
import java.util.ArrayList;

public class Game {
  public static void main(String[] args) {
    Chess chess = new Chess();
    chess.play();
  }

  public static void clearScreen() {  
    System.out.print("\033[H\033[2J");  
    System.out.flush();  
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

class Chess {
  private Board board;
  private COLOR turn;

  public Chess() {
    // Init board
    board = new Board(8, 8);
    turn = COLOR.WHITE;
  }

  public void play() {
    Game.clearScreen();
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

      System.out.print("Input instructions, for more info type help: ");
      String input = sc.nextLine();

      if (input.equals("help") || input.equals("?")) {
        System.out.println("Moves can be input as from-to, e.g. A4-D7, G8-G1");
        System.out.println("Castling can be input as 0-0 (King side), 0-0-0 (Queen side)");
        continue;
      }
      else if (input.equals("quit") || input.equals("q")) {
        break;
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

      if (zero != moves.length && moves.length != 2) {
          System.out.println("Invalid input");
          continue;
      }

      if (zero > 0) {
        //TODO: Implement castling
        System.out.println("Castling");
        continue;
      }

      // Check if valid moves
      Location from = board.translateMove(moves[0]);
      Location to = board.translateMove(moves[1]);
    
      if (from != null && to != null) {
        // Move pieces
        if (board.movePiece(turn, from, to)) {
          Game.clearScreen();

          changeTurn();

          // Check for Pawn promotion
          Location promotedLoc = board.checkPromotion();
          while (promotedLoc != null) {
            System.out.println("Pawn promotion, you can choose one of the following");
            System.out.print("[q]ueen,[b]ishop, [k]night, [r]ook: ");

            input = sc.nextLine();
            switch (input) {
              case "q":
              case "queen":
                board.setPiece(new Queen(turn, board), promotedLoc);
                promotedLoc = null;
                break;

              case "b":
              case "bishop":
                board.setPiece(new Bishop(turn, board), promotedLoc);
                promotedLoc = null;
                break;

              case "k":
              case "knight":
                board.setPiece(new Knight(turn, board), promotedLoc);
                promotedLoc = null;
                break;

              case "r":
              case "rook":
                board.setPiece(new Rook(turn, board), promotedLoc);
                promotedLoc = null;
                break;

              default:
                System.out.println("Wrong key detected, type q for queen, b for bishop, k for knight, r for rook");
                break;
            }
          }
        } else {
          continue;
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

        // Draw board
        // https://qwerty.dev/chess-symbols-to-copy-and-paste/
        board.draw();
      } else {
        System.out.println("Invalid input!");
      }
    }
  }

  private void changeTurn() {
    switch (turn) {
      case WHITE:
        turn = COLOR.BLACK;
        break;

      case BLACK:
        turn = COLOR.WHITE;
        break;
    }
  }
}

class Board {
  private int width;
  private int height;
  private Piece[] boards;

  public Board(int w, int h) {
    width = w;
    height = h;

    boards = new Piece[w * h];

    for (int y = 0; y < h; ++y) {
      for (int x = 0; x < w; ++x) {
        if (y == 0) {
          if (x == 0 || x == (w - 1)) {
            boards[y * h + x] = new Rook(COLOR.BLACK, this);
          } else if (x == 1 || x == (w - 2)) {
            boards[y * h + x] = new Knight(COLOR.BLACK, this);
          } else if (x == 2 || x == (w - 3)) {
            boards[y * h + x] = new Bishop(COLOR.BLACK, this);
          } else if (x == 3) {
            boards[y * h + x] = new Queen(COLOR.BLACK, this);
          } else if (x == 4) {
            boards[y * h + x] = new King(COLOR.BLACK, this);
          }
        } else if (y == 1) {
          boards[y * h + x] = new Pawn(COLOR.BLACK, this, 1);
        } else if (y == 6) {
          boards[y * h + x] = new Pawn(COLOR.WHITE, this, -1);
        } else if (y == 7) {
          if (x == 0 || x == (w - 1)) {
            boards[y * h + x] = new Rook(COLOR.WHITE, this);
          } else if (x == 1 || x == (w - 2)) {
            boards[y * h + x] = new Knight(COLOR.WHITE, this);
          } else if (x == 2 || x == (w - 3)) {
            boards[y * h + x] = new Bishop(COLOR.WHITE, this);
          } else if (x == 3) {
            boards[y * h + x] = new Queen(COLOR.WHITE, this);
          } else if (x == 4) {
            boards[y * h + x] = new King(COLOR.WHITE, this);
          }
        } else {
          if (x % 2 == 0) {
            if (y % 2 == 0) {
              boards[y * h + x] = new Squares(COLOR.WHITE, this);
            } else {
              boards[y * h + x] = new Squares(COLOR.BLACK, this);
            }
          } else {
            if (y % 2 == 1) {
              boards[y * h + x] = new Squares(COLOR.WHITE, this);
            } else {
              boards[y * h + x] = new Squares(COLOR.BLACK, this);
            }
          }
        }
      }
    }
  }

  public boolean hasWinner() {
    int count = 0;
    for (Piece piece : boards) {
      if (piece.getType() == PIECE_TYPE.BLACK_KING ||
          piece.getType() == PIECE_TYPE.WHITE_KING) {
        ++count;
      }
    }

    return count == 1;
  }

  public Location checkPromotion() {
    // Check only top and bottom row
    for (int y = 0; y < height; y += (height - 1)) {
      for (int x = 0; x < width; ++x) {
        Piece piece = boards[y * height + x];
        if (piece != null) {
          if (piece.getType() == PIECE_TYPE.BLACK_PAWN ||
              piece.getType() == PIECE_TYPE.WHITE_PAWN) {

            System.out.println("Promotion");
            return new Location(x, y);
          }
        }
      }
    }
    return null;
  }

  public boolean movePiece(COLOR turn, Location from, Location to) {

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

    // Check if piece we are moving is a blank piece
    if (isEmptySpace(from)) {
      System.out.println("Must select a valid piece!");
      return false;
    }

    // Check if piece can move according to rule
    Piece piece = getPiece(from);

    if (piece != null) {
      // Check if valid player's turn
      if (piece.getColor() != turn) {
        System.out.println("Not your turn yet!");

        //TODO: Disabled for now
        // return false;
      }

      Location[] locations = piece.getMoves(from);
      if (locations != null) {
        for (Location loc : locations) {
          if (loc.equals(to)) {

            // Update the board's white/black box
            final int x = from.getX();
            final int y = from.getY();

            if (x % 2 == 0) {
              if (y % 2 == 0) {
                setPiece(new Squares(COLOR.WHITE, this), x, y);
              } else {
                setPiece(new Squares(COLOR.BLACK, this), x, y);
              }
            } else {
              if (y % 2 == 1) {
                setPiece(new Squares(COLOR.WHITE, this), x, y);
              } else {
                setPiece(new Squares(COLOR.BLACK, this), x, y);
              }
            }

            setPiece(piece, to);
            return true;
          }
        }
      }
    }

    System.out.println("Not a valid move!");
    return false;
  }

  public Piece getPiece(Location loc) {
    return getPiece(loc.getX(), loc.getY());
  }

  public Piece getPiece(int x, int y) {
    if (isInsideBoard(x, y)) {
      return boards[y * height + x];
    }
    return null;
  }

  public void setPiece(Piece piece, Location loc) {
    if (piece != null && loc != null) {
      if (isInsideBoard(loc)) {
        boards[loc.getY() * height + loc.getX()] = piece;
      }
    }
  }

  private void setPiece(Piece piece, int x, int y) {
    setPiece(piece, new Location(x, y));
  }

  public Location translateMove(String input) {
    input = input.toUpperCase();
    if (input.length() == 2) {
      if (Character.isLetter(input.charAt(0))) {
        if (Character.isDigit(input.charAt(1))) {
          final int x = input.charAt(0) - 'A';
          final int y = input.charAt(1) - '1';
          return new Location(x, height - y - 1);
        }
      }
    }
    return null;
  }

  public boolean isInsideBoard(Location loc) {
    return isInsideBoard(loc.getX(), loc.getY());
  }

  public boolean isInsideBoard(int x, int y) {
    return x >= 0 &&
           x < width &&
           y >= 0 &&
           y < height;
  }

  public void draw() {

    final int bh = (height * 2) + 1;
    final int bw = (width * 2) + 1;

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
                System.out.print(boards[id].getIcon());
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
    return isEmptySpace(at.getX(), at.getY());
  }

  public boolean isEmptySpace(int x, int y) {
    Piece piece = getPiece(x, y);
    if (piece != null) {
      return piece.getType() == PIECE_TYPE.BLACK_SQUARE || 
             piece.getType() == PIECE_TYPE.WHITE_SQUARE;
    }
    return false;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }
}

abstract class Piece {
  private PIECE_TYPE type;
  private char icon;
  private COLOR color;
  private Board board;

  public Piece(Board board) {
    this.board = board;
  }

  abstract public Location[] getMoves(Location at);

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

  public void set(PIECE_TYPE type, char icon, COLOR color) {
    this.type = type;
    this.icon = icon;
    this.color = color;
  }

  public boolean isEnemy(Piece piece) {
    return color != piece.color;
  }
}

class Squares extends Piece {
  public Squares(COLOR color, Board board) {
    super(board);

    switch (color) {
      case BLACK:
        set(PIECE_TYPE.BLACK_SQUARE, '░', COLOR.BLACK);
        break;
      case WHITE:
        set(PIECE_TYPE.WHITE_SQUARE, ' ', COLOR.WHITE);
        break;
    }
  }

  public Location[] getMoves(Location at) {
    return new Location[0];
  }
}

class Pawn extends Piece {
  private int dir;

  public Pawn(COLOR color, Board board, int dir) {
    super(board);

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

  public Location[] getMoves(Location at) {
    ArrayList<Location> arrayList = new ArrayList<>();

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
    if (hasDiagonal(frontLeftLoc)) {
      arrayList.add(frontLeftLoc);
    }

    Location frontRightLoc = new Location(at.getX() + 1, at.getY() + dir);
    if (hasDiagonal(frontRightLoc)) {
      arrayList.add(frontRightLoc);
    }

    return arrayList.toArray(new Location[arrayList.size()]);
  }
}

class Rook extends Piece {
  // For castling
  private boolean hasMoved;
  public Rook(COLOR color, Board board) {
    super(board);

    hasMoved = false;

    switch (color) {
      case BLACK:
        set(PIECE_TYPE.BLACK_ROOK, '♜', COLOR.BLACK);
        break;
      case WHITE:
        set(PIECE_TYPE.WHITE_ROOK, '♖', COLOR.WHITE);
        break;
    }
  }

  public Location[] getMoves(Location at) {
    ArrayList<Location> arrayList = new ArrayList<>();

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
    for (int x = at.getX() + 1; x < getBoard().getWidth(); ++x) {
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
    for (int y = at.getY() + 1; y < getBoard().getHeight(); ++y) {
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
  public Knight(COLOR color, Board board) {
    super(board);

    switch (color) {
      case BLACK:
        set(PIECE_TYPE.BLACK_KNIGHT, '♞', COLOR.BLACK);
        break;
      case WHITE:
        set(PIECE_TYPE.WHITE_KNIGHT, '♘', COLOR.WHITE);
        break;
    }
  }

  public Location[] getMoves(Location at) {
    ArrayList<Location> arrayList = new ArrayList<>();

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
  public Bishop(COLOR color, Board board) {
    super(board);

    switch (color) {
      case BLACK:
        set(PIECE_TYPE.BLACK_BISHOP, '♝', COLOR.BLACK);
        break;
      case WHITE:
        set(PIECE_TYPE.WHITE_BISHOP, '♗', COLOR.WHITE);
        break;
    }
  }

  public Location[] getMoves(Location at) {
    ArrayList<Location> arrayList = new ArrayList<>();

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
    for (int x = at.getX() + 1, y = at.getY() + 1; x < getBoard().getWidth() && y < getBoard().getHeight(); ++x, ++y) {
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
    for (int x = at.getX() + 1, y = at.getY() - 1; x < getBoard().getWidth() && y >= 0; ++x, --y) {
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
    for (int x = at.getX() - 1, y = at.getY() + 1; x >= 0 && y < getBoard().getHeight(); --x, ++y) {
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
  public Queen(COLOR color, Board board) {
    super(board);

    switch (color) {
      case BLACK:
        set(PIECE_TYPE.BLACK_QUEEN, '♛', COLOR.BLACK);
        break;
      case WHITE:
        set(PIECE_TYPE.WHITE_QUEEN, '♕', COLOR.WHITE);
        break;
    }
  }

  public Location[] getMoves(Location at) {
    ArrayList<Location> arrayList = new ArrayList<>();

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
    for (int x = at.getX() + 1, y = at.getY() + 1; x < getBoard().getWidth() && y < getBoard().getHeight(); ++x, ++y) {
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
    for (int x = at.getX() + 1, y = at.getY() - 1; x < getBoard().getWidth() && y >= 0; ++x, --y) {
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
    for (int x = at.getX() - 1, y = at.getY() + 1; x >= 0 && y < getBoard().getHeight(); --x, ++y) {
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
    for (int x = at.getX() + 1; x < getBoard().getWidth(); ++x) {
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
    for (int y = at.getY() + 1; y < getBoard().getHeight(); ++y) {
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
  // For castling
  private boolean hasMoved;

  public King(COLOR color, Board board) {
    super(board);

    hasMoved = false;

    switch (color) {
      case BLACK:
        set(PIECE_TYPE.BLACK_KING, '♚', COLOR.BLACK);
        break;
      case WHITE:
        set(PIECE_TYPE.WHITE_KING, '♔', COLOR.WHITE);
        break;
    }
  }

  public Location[] getMoves(Location at) {
    ArrayList<Location> arrayList = new ArrayList<>();

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
      //TODO: Check if any of this spaces are in check
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
    return x + " : " + y;
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
