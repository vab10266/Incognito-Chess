import java.util.Scanner;

public class AsciiChess {

    public static void main(String[] args) {
        Board board = new Board();
        Scanner in = new Scanner(System.in);

        while (board.getGameStatus() == GameStatus.IN_PROGRESS) {

            System.out.println();
            printBoard(board);

            int x1 = in.nextInt();
            int y1 = in.nextInt();

            int x2 = in.nextInt();
            int y2 = in.nextInt();

            if (!board.move(x1, y1, x2, y2)) {
                System.out.println("Invalid move");
            }
        }
        in.close();
    }

     static void printBoard(Board board) {
        char[][] arr = board.getBoard();

        System.out.println("Turn: " + (board.getTurn() ? "White" : "Black"));

        for (int y = arr[0].length - 1; y >= 0; y--) {
            for (int x = 0; x < arr.length; x++) {
                System.out.print(arr[x][y] + " ");
            }
            System.out.println();
        }
    }

}