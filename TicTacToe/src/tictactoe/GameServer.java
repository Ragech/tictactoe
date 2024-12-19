package tictactoe;

import java.io.*;
import java.net.*;

public class GameServer {
    private static final int PORT = 8087;
    private static final int BOARD_SIZE = 15;
    private static final int WIN_CONDITION = 5; // Условие победы (5 подряд)
    
    private Socket player1, player2; // Сокеты для подключения игроков
    private PrintWriter player1Out, player2Out; // Потоки вывода для отправки сообщений игрокам
    private BufferedReader player1In, player2In; // Потоки ввода для получения сообщений от игроков

    private String[][] board = new String[BOARD_SIZE][BOARD_SIZE]; // Игровое поле
    private boolean isPlayer1Turn = true; // Ход игрока 1 
        
    // Запуск сервера
    public static void main(String[] args) throws IOException {
        new GameServer();
    }
    
    public GameServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT); // Создаем серверный сокет на заданном порту
        System.out.println("Server started in port " + PORT);

        // Подключение игроков
        player1 = serverSocket.accept();
        System.out.println("First player connected");
        player2 = serverSocket.accept();
        System.out.println("Second player connected");

        // Инициализация потоков ввода/вывода
        player1Out = new PrintWriter(player1.getOutputStream(), true);
        player2Out = new PrintWriter(player2.getOutputStream(), true);
        player1In = new BufferedReader(new InputStreamReader(player1.getInputStream()));
        player2In = new BufferedReader(new InputStreamReader(player2.getInputStream()));

        // Начало игры
        startGame();
    }

    // Инициализация игрового поля (очищаем его)
    private void initializeBoard() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                board[row][col] = ""; // Каждая клетка пуста
            }
        }
    }

    // Основной игровой цикл
    private void startGame() {
        try {
            while (true) {
                // Подготовка новой игры
                initializeBoard();
                isPlayer1Turn = true;

                // Сообщения для игроков
                player1Out.println("HI X");
                player2Out.println("HI O");
                player1Out.println("YOUR_TURN");
                player2Out.println("WAIT_TURN");

                // Основной цикл партии
                while (true) {
                    //Проверка на наличие входящих данных от каждого игрока
                    String move1 = player1In.ready() ? player1In.readLine() : null;
                    String move2 = player2In.ready() ? player2In.readLine() : null;
                    // Обработка запроса на загрузку сохраненного состояния игры
                    if (move1 != null && move1.startsWith("LOAD_GAME") || move2 != null && move2.startsWith("LOAD_GAME")) {
                        handleLoadGame();
                        continue;
                    }
                    // Считывание хода в зависимости от текущего игрока
                    String move = isPlayer1Turn ? move1 : move2;
                    if (move == null) continue;
                    // Разбор координат хода
                    String[] parts = move.split(",");
                    int row = Integer.parseInt(parts[0]); // Строка хода
                    int col = Integer.parseInt(parts[1]); // Колонка хода
                    String symbol = isPlayer1Turn ? "X" : "O"; // Символ игрока

                    board[row][col] = symbol; // Обновляем игровое поле

                    // Проверка на победу
                    if (checkWin(row, col, symbol)) {
                        player1Out.println(isPlayer1Turn ? "WIN" : "LOSE");
                        player2Out.println(isPlayer1Turn ? "LOSE" : "WIN");
                        break;
                    }

                    // Проверка на ничью
                    if (isBoardFull()) {
                        player1Out.println("DRAW");
                        player2Out.println("DRAW");
                        break;
                    }

                    // Передача хода другому игроку
                    (isPlayer1Turn ? player2Out : player1Out).println("MOVE " + row + "," + col + "," + symbol);
                    (isPlayer1Turn ? player2Out : player1Out).println("YOUR_TURN");
                    (isPlayer1Turn ? player1Out : player2Out).println("WAIT_TURN");

                    isPlayer1Turn = !isPlayer1Turn; // Меняем ход
                }

                // Предложение начать новую игру
                if (!promptNewGame()) break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnections();
        }
    }
    
    // Запрос на новую игру у игроков
    private boolean promptNewGame() throws IOException {
        player1Out.println("GAME_OVER");
        player2Out.println("GAME_OVER");
        // Если оба игрока согласны на новую игру, возвращаем true
        return player1In.readLine().equalsIgnoreCase("NEW_GAME") &&
               player2In.readLine().equalsIgnoreCase("NEW_GAME");
    }

    // Обработка загрузки игры
    private void handleLoadGame() {
        try {
            String[] loadedState = GameDatabase.loadGameState(); // Загрузка состояния игры
            updateBoardState(loadedState[0]); // Обновляем поле
            isPlayer1Turn = loadedState[1].equals("X"); // Устанавливаем чей ход
            // Отправляем синхронизированное состояние игры игрокам
            player1Out.println("SYNC_LOAD_GAME " + loadedState[0] + " " + loadedState[1] + " " + loadedState[2]);
            player2Out.println("SYNC_LOAD_GAME " + loadedState[0] + " " + loadedState[1] + " " + loadedState[2]);
            // Определяем, кто будет ходить первым после загрузки
            if (isPlayer1Turn) { 
                player1Out.println("YOUR_TURN");
                player2Out.println("WAIT_TURN");
            } else {
                player2Out.println("YOUR_TURN");
                player1Out.println("WAIT_TURN");
            }
        } catch (Exception e) {
            player1Out.println("LOAD_FAILED");
            player2Out.println("LOAD_FAILED");
            e.printStackTrace();
        }
    }
    
    // Метод для синхронизации игрового состояния
    private void updateBoardState(String boardState) {
        int index = 0;
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                char symbol = boardState.charAt(index++); // Берем символ из строки
                switch (symbol) { // Заполняем поле соответствующими символами
                    case '0': board[row][col] = ""; break;
                    case '1': board[row][col] = "X"; break;
                    case '2': board[row][col] = "O"; break;
                }
            }
        }
    }
    
    // Проверка на заполненность доски
    private boolean isBoardFull() {
        for (String[] row : board) {
            for (String cell : row) {
                if (cell.isEmpty()) return false; // Если хотя бы одна клетка пуста
            }
        }
        return true; // Доска полна
    }
    
    // Проверка на победу по четырем направлениям
    private boolean checkWin(int row, int col, String symbol) {
        return checkDirection(row, col, 1, 0, symbol) ||  // горизонталь
               checkDirection(row, col, 0, 1, symbol) ||  // вертикаль
               checkDirection(row, col, 1, 1, symbol) ||  // диагональ \
               checkDirection(row, col, 1, -1, symbol);   // диагональ /
    }
    
    // Проверка по конкретному направлению
    private boolean checkDirection(int row, int col, int rowDir, int colDir, String symbol) {
        int count = 1; // Начинаем с текущей клетки
        for (int i = 1; i < WIN_CONDITION; i++) {
            int r = row + i * rowDir;
            int c = col + i * colDir;
            if (isValid(r, c) && board[r][c].equals(symbol)) count++; // Проверка в нужном направлении
            else break;
        }
        for (int i = 1; i < WIN_CONDITION; i++) {
            int r = row - i * rowDir; 
            int c = col - i * colDir;
            if (isValid(r, c) && board[r][c].equals(symbol)) count++; // Проверка в обратном направлении
            else break;
        }
        return count >= WIN_CONDITION; // Проверка, если количество символов >= 5
    }
    
    // Проверка на допустимость координат
    private boolean isValid(int row, int col) {
        return row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE; // Координаты в пределах доски
    }
    
    // Закрытие соединений с игроками
    private void closeConnections() {
        try {
            if (player1 != null) player1.close();
            if (player2 != null) player2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
