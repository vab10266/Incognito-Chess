package com.cloudydino.incognitochess;

import java.util.HashSet;
import java.util.Set;

class Board {

    static final int SIZE = 8;

    private char[][] spaces = new char[SIZE][SIZE];
    private boolean whiteTurn;
    private boolean castleWK, castleWQ, castleBK, castleBQ;
    private int enPassant;
    private Set<Integer> whiteAttack, blackAttack;
    private boolean whiteInCheck, blackInCheck;
    private int movesSincePawnOrCapture;

    Board() {
        setupBoard();
        whiteTurn = true;
        castleWK = true;
        castleWQ = true;
        castleBK = true;
        castleBQ = true;
        enPassant = -1;
        updateAttack();
        movesSincePawnOrCapture = 0;
    }

    /**
     * Results in:
     *  7) r n b q k b n r
     *  6) p p p p p p p p
     *  5) - - - - - - - -
     *  4) - - - - - - - -
     *  3) - - - - - - - -
     *  2) - - - - - - - -
     *  1) P P P P P P P P
     *  0) R N B Q K B N R
     *     0 1 2 3 4 5 6 7
     * Where spaces[4][0] == 'K'
     */
    private void setupBoard() {
        char[] setup = {'r', 'n', 'b', 'q', 'k', 'b', 'n', 'r'};
        for (int i = 0; i < SIZE; i++) {
            spaces[i][7] = setup[i];
            spaces[i][6] = 'p';
            spaces[i][1] = 'P';
            spaces[i][0] = Character.toUpperCase(setup[i]);
        }
    }

    char[][] getBoard() {
        return spaces;
    }

    boolean getTurn() {
        return whiteTurn;
    }

    private void toggleTurn() {
        whiteTurn = !whiteTurn;
    }

    static int squareToInteger(int x, int y) {
        if (x < SIZE && y < SIZE) {
            return x * SIZE + y;
        }
        return -1;
    }

    static int squareToInteger(int[] xy) {
        if (xy.length >= 2 && xy[0] < SIZE && xy[1] < SIZE) {
            return xy[0] * SIZE + xy[1];
        }
        return -1;
    }

    static int[] integerToSquare(int i) {
        return new int[]{i / SIZE, i % SIZE};
    }

    private boolean onBoard(int x, int y) {
        return 0 <= x && x < SIZE
                && 0 <= y && y < SIZE;
    }

    GameStatus getGameStatus() {
        // If there exist possible moves for the current color then the game is
        // either drawn by the 50 move rule or is still in progress
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (spaces[x][y] != 0 && whiteTurn == Character.isUpperCase(spaces[x][y]) && getLegalMoves(x, y).size() > 0) {
                    if (movesSincePawnOrCapture == 100) {
                        return GameStatus.DRAW;
                    }
                    return GameStatus.IN_PROGRESS;
                }
            }
        }

        // No moves for current color,
        if (whiteTurn && whiteInCheck) {
            return GameStatus.BLACK_WON;
        } else if (!whiteTurn && blackInCheck) {
            return GameStatus.WHITE_WON;
        }
        return GameStatus.STALEMATE;
    }

    private void updateAttack() {
        int whiteKing = -1;
        int blackKing = -1;
        whiteAttack = new HashSet<>();
        blackAttack = new HashSet<>();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                switch (spaces[x][y]) {
                    case 'K':
                        whiteKing = squareToInteger(x, y);
                        break;
                    case 'k':
                        blackKing = squareToInteger(x, y);
                        break;
                }

                Set<Integer> moves = getAttackingSquares(x, y);
                if (!moves.isEmpty()) {
                    if (Character.isUpperCase(spaces[x][y])) {
                        whiteAttack.addAll(moves);
                    } else {
                        blackAttack.addAll(moves);
                    }
                }
            }
        }

        whiteInCheck = blackAttack.contains(whiteKing);
        blackInCheck = whiteAttack.contains(blackKing);
    }

    Set<Integer> getAttacking(boolean isWhite) {
        if (isWhite) {
            return whiteAttack;
        }
        return blackAttack;
    }

    boolean doesPromote(int startX, int startY, int destY) {
        return (Character.toLowerCase(spaces[startX][startY]) == 'p') && (destY == 0 || destY == SIZE - 1);
    }

    boolean move(int startX, int startY, int destX, int destY) {
        return move(startX, startY, destX, destY, Piece.QUEEN);
    }

    /**
     * Makes the move of the piece from (startX, startY) to (destX, destY) and
     * then promotes the piece to promoteTo if it is a pawn that has reached the
     * other end. Only does the move if it is valid
     *
     * @return boolean if the move was valid and thus changed the board
     */
    boolean move(int startX, int startY, int destX, int destY, Piece promoteTo) {
        // Check for validity
        if (!isValidMove(startX, startY, destX, destY)) {
            return false;
        }

        updateCastling(startX, startY);

        // Make the update
        enPassant = -1;
        movesSincePawnOrCapture++;
        if (spaces[destX][destY] != 0) {
            movesSincePawnOrCapture = 0;
        }

        Piece piece = Piece.fromChar(spaces[startX][startY]);

        if (piece == Piece.KING && Math.abs(startX - destX) == 2) {
            // castle
            int rookX = (startX > destX ? 0 : SIZE - 1);
            spaces[(startX + destX) / 2][startY] = spaces[rookX][startY];
            spaces[rookX][startY] = 0;

        } else if (piece == Piece.PAWN) {
            movesSincePawnOrCapture = 0;
            if (Math.abs(startY - destY) == 2) {
                // moved two spaces
                enPassant = squareToInteger(destX, destY);
            } else if (destX != startX && spaces[destX][destY] == 0) {
                // en passant
                spaces[destX][startY] = 0;
            } else if (destY == 0 || destY == SIZE - 1) {
                // pawn promotion
                spaces[startX][startY] = promoteTo.toChar(whiteTurn);
            }
        }

        spaces[destX][destY] = spaces[startX][startY];
        spaces[startX][startY] = 0;

        toggleTurn();
        updateAttack();
        return true;
    }

    private void updateCastling(int startX, int startY) {
        if (startY == 0) {
            if (spaces[startX][startY] == 'K') {
                castleWK = false;
                castleWQ = false;
            } else if (startX == 0) {
                castleWQ = false;
            } else if (startX == SIZE) {
                castleWK = false;
            }
        } else if (startY == SIZE) {
            if (spaces[startX][startY] == 'k') {
                castleBK = false;
                castleBQ = false;
            } else if (startX == 0) {
                castleBQ = false;
            } else if (startX == SIZE) {
                castleBK = false;
            }
        }
    }

    /**
     * @return boolean if the piece can go from (startX, startY) to (destX, destY)
     */
    private boolean isValidMove(int startX, int startY, int destX, int destY) {

        if (spaces[startX][startY] == 0
                || whiteTurn != Character.isUpperCase(spaces[startX][startY])) {
            return false;
        }

        return getLegalMoves(startX, startY).contains(squareToInteger(destX, destY));
    }

    /**
     * @return Set<Integer> a nonnull set of integers corresponding to the
     * squares that the starting square can legally go to
     */
    private Set<Integer> getLegalMoves(int startX, int startY) {
        Set<Integer> possibleMoves = getPossibleMoves(startX, startY);
        Set<Integer> legalMoves = new HashSet<>();

        for (int pm : possibleMoves) {
            int[] pmSquare = integerToSquare(pm);
            int destX = pmSquare[0];
            int destY = pmSquare[1];

            char[][] temp = new char[SIZE][];
            for (int x = 0; x < SIZE; x++) {
                temp[x] = spaces[x].clone();
            }

            if (Piece.fromChar(spaces[startX][startY]) == Piece.PAWN
                    && destX != startX
                    && spaces[destX][destY] == 0) {
                // en passant
                spaces[destX][startY] = 0;
            }

            spaces[destX][destY] = spaces[startX][startY];
            spaces[startX][startY] = 0;

            int king = -1;
            Set<Integer> attacked = new HashSet<>();
            for (int x = 0; x < SIZE; x++) {
                for (int y = 0; y < SIZE; y++) {
                    if (spaces[x][y] == (whiteTurn ? 'K' : 'k')) {
                        king = squareToInteger(x, y);
                    }

                    Set<Integer> moves = getPossibleMoves(x, y);
                    if (!moves.isEmpty() && whiteTurn != Piece.isWhite(spaces[x][y])) {
                        attacked.addAll(moves);
                    }
                }
            }

            for (int x = 0; x < SIZE; x++) {
                spaces[x] = temp[x].clone();
            }

            if (!attacked.contains(king)) {
                legalMoves.add(pm);
            }
        }
        return legalMoves;
    }

    private Set<Integer> getAttackingSquares(int x, int y) {
        char piece = spaces[x][y];
        if (piece == 0) {
            return new HashSet<>();
        }
        if (Piece.fromChar(piece) == Piece.PAWN) {
            return getPawnAttackingSquares(x, y);
        }
        return getPossibleMoves(x, y);
    }

    private Set<Integer> getPawnAttackingSquares(int x, int y) {
        Set<Integer> attacking = new HashSet<>();
        boolean isWhite = Piece.isWhite(spaces[x][y]);
        int dy = (isWhite ? 1 : -1);
        int currY = y + dy;

        for (int dx = -1; dx <= 1; dx += 2) {
            int currX = x + dx;
            if (onBoard(currX, currY)
                    && (spaces[currX][currY] == 0 || isWhite != Piece.isWhite(spaces[currX][currY]))) {
                attacking.add(squareToInteger(currX, currY));
            }
        }

        return attacking;
    }

    private Set<Integer> getPossibleMoves(int x, int y) {
        char piece = spaces[x][y];
        if (piece == 0) {
            return new HashSet<>();
        }
        switch (Piece.fromChar(piece)) {
            case PAWN:
                return getPossiblePawnMoves(x, y);
            case KNIGHT:
                return getPossibleKnightMoves(x, y);
            case BISHOP:
                return getPossibleBishopMoves(x, y);
            case ROOK:
                return getPossibleRookMoves(x, y);
            case QUEEN:
                return getPossibleQueenMoves(x, y);
            case KING:
                return getPossibleKingMoves(x, y);
            default:
                return new HashSet<>();
        }
    }

    private Set<Integer> getPossiblePawnMoves(int x, int y) {
        Set<Integer> possibleMoves = new HashSet<>();
        boolean isWhite = Piece.isWhite(spaces[x][y]);
        int dy = (isWhite ? 1 : -1);
        int startY = (isWhite ? 1 : 6);
        int currY = y + dy;
        if (onBoard(x, currY) && spaces[x][currY] == 0) {
            possibleMoves.add(squareToInteger(x, currY));
        }
        if (possibleMoves.size() > 0 && y == startY && spaces[x][y + 2 * dy] == 0) {
            possibleMoves.add(squareToInteger(x, currY + dy));
        }

        for (int dx = -1; dx <= 1; dx += 2) {
            int currX = x + dx;
            if (onBoard(currX, currY)
                    && ((spaces[currX][currY] != 0 && isWhite != Piece.isWhite(spaces[currX][currY]))
                    || (spaces[currX][currY] == 0 && squareToInteger(currX, y) == enPassant))) {
                possibleMoves.add(squareToInteger(currX, currY));
            }
        }
        return possibleMoves;
    }

    private Set<Integer> getPossibleKnightMoves(int x, int y) {
        Set<Integer> possibleMoves = new HashSet<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx != dy && dx != 0 && dy != 0 && dx + dy != 0) {
                    int currX = x + dx;
                    int currY = y + dy;
                    if (onBoard(currX, currY) && (spaces[currX][currY] == 0
                            || whiteTurn != Character.isUpperCase(spaces[currX][currY]))) {
                        possibleMoves.add(squareToInteger(currX, currY));
                    }
                }

            }
        }
        return possibleMoves;
    }

    private Set<Integer> getPossibleRookMoves(int x, int y) {
        return getPossibleBishopRookQueenMoves(x, y, false, true);
    }

    private Set<Integer> getPossibleBishopMoves(int x, int y) {
        return getPossibleBishopRookQueenMoves(x, y, true, false);
    }

    private Set<Integer> getPossibleQueenMoves(int x, int y) {
        return getPossibleBishopRookQueenMoves(x, y, true, true);
    }

    private Set<Integer> getPossibleBishopRookQueenMoves(int x, int y, boolean isBishop, boolean isRook) {
        Set<Integer> possibleMoves = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if ((dx != 0 || dy != 0)
                        && ((isBishop && Math.abs(dx) == Math.abs(dy))
                        || (isRook && Math.abs(dx) != Math.abs(dy)))) {
                    int currX = x;
                    int currY = y;
                    boolean stop = false;
                    while (!stop) {
                        currX += dx;
                        currY += dy;
                        if (onBoard(currX, currY)) {
                            if (spaces[currX][currY] == 0) {
                                possibleMoves.add(squareToInteger(currX, currY));
                            } else {
                                stop = true;
                                if (Character.isUpperCase(spaces[x][y]) != Character.isUpperCase(spaces[currX][currY])) {
                                    possibleMoves.add(squareToInteger(currX, currY));
                                }
                            }
                        } else {
                            stop = true;
                        }
                    }
                }
            }
        }
        return possibleMoves;
    }

    private Set<Integer> getPossibleKingMoves(int x, int y) {
        Set<Integer> possibleMoves = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int currX = x + dx;
                int currY = y + dy;
                if ((dx != 0 || dy != 0)
                        && onBoard(currX, currY)
                        && (spaces[currX][currY] == 0
                        || whiteTurn != Character.isUpperCase(spaces[currX][currY]))) {
                    possibleMoves.add(squareToInteger(currX, currY));
                }
            }
        }

        Set<Integer> attacked = (whiteTurn ? blackAttack : whiteAttack);

        boolean kingInCheck = (whiteTurn ? whiteInCheck : blackInCheck);
        boolean castleK = !kingInCheck && (whiteTurn ? castleWK : castleBK);
        boolean castleQ = !kingInCheck && (whiteTurn ? castleWQ : castleBQ);

        for (int currX = x + 1; castleK && currX < SIZE - 1; currX++) {
            if (spaces[currX][y] != 0
                    || (currX - x <= 2
                    && attacked.contains(squareToInteger(currX, y)))) {
                castleK = false;
            }
        }

        for (int currX = x - 1; castleQ && currX > 0; currX--) {
            if (spaces[currX][y] != 0
                    || (x - currX <= 2
                    && attacked.contains(squareToInteger(currX, y)))) {
                castleQ = false;
            }
        }

        if (castleK) {
            possibleMoves.add(squareToInteger(x + 2, y));
        }
        if (castleQ) {
            possibleMoves.add(squareToInteger(x - 2, y));
        }

        return possibleMoves;
    }
}
