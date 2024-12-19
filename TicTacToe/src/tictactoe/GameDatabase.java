package tictactoe;

import java.sql.*;
import java.io.*;
import java.net.*;

public class GameDatabase {
        // Метод для сохранения состояния игры в базу данных
    public static void saveGameState(String[][] board, String current_player, String playerName) {
        StringBuilder boardState = new StringBuilder();
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                String symbol = board[row][col];
                if (symbol == null || symbol.isEmpty()) {
                    boardState.append("0");
                } else if (symbol.equals("X")) {
                    boardState.append("1");
                } else if (symbol.equals("O")) {
                    boardState.append("2");
                }
            }
        }

        // Шаг 1: Получить или вставить игрока в таблицу players и получить его player_id
        int playerId = getPlayerId(playerName);

        // SQL-запрос для вставки состояния игры в таблицу базы данных
        String query = "INSERT INTO tic_state (board_state, current_player, player_id) VALUES (?, ?, ?)";
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/tictactoe?zeroDateTimeBehavior=CONVERT_TO_NULL", "root", "1234");
             PreparedStatement prepared_statement = connection.prepareStatement(query)) {

            prepared_statement.setString(1, boardState.toString()); // Устанавливаем строку состояния доски
            prepared_statement.setString(2, current_player); // Устанавливаем текущего игрока
            prepared_statement.setInt(3, playerId); // Устанавливаем player_id
            prepared_statement.executeUpdate(); // Выполняем запрос для сохранения данных в базе
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static int getPlayerId(String playerName) {
        String query = "SELECT player_id FROM players WHERE name = ?";
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/tictactoe?zeroDateTimeBehavior=CONVERT_TO_NULL", "root", "1234");
             PreparedStatement prepared_statement = connection.prepareStatement(query)) {

            prepared_statement.setString(1, playerName);
            ResultSet result_set = prepared_statement.executeQuery();

            if (result_set.next()) {
                return result_set.getInt("player_id"); // Если игрок найден, возвращаем его player_id            
            } else {
                // Если игрок не найден, добавляем его в таблицу
                String insertQuery = "INSERT INTO players (name) VALUES (?)";
                try (PreparedStatement insertPstmt = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                    insertPstmt.setString(1, playerName);
                    insertPstmt.executeUpdate();

                    // Получаем сгенерированный player_id
                    ResultSet generatedKeys = insertPstmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Возвращаем -1, если не удалось найти или добавить игрока
    }

    
    // Метод для загрузки состояния игры из базы данных по идентификатору игры
    public static String[] loadGameState() {
        String query = "SELECT ts.board_state, ts.current_player, p.name FROM tic_state ts JOIN players p ON ts.player_id = p.player_id ORDER BY ts.move_time DESC LIMIT 1";
        String[] result = new String[3]; // Массив для хранения результата (состояние доски, текущий игрок, имя игрока)

        try (PreparedStatement prepared_statement = DriverManager.getConnection("jdbc:mysql://localhost:3306/tictactoe?zeroDateTimeBehavior=CONVERT_TO_NULL", "root", "1234").prepareStatement(query)) {
            ResultSet result_set = prepared_statement.executeQuery();  // Выполняем запрос и получаем результат
            if (result_set.next()) {
                result[0] = result_set.getString("board_state");  // Состояние доски
                result[1] = result_set.getString("current_player");  // Текущий игрок
                result[2] = result_set.getString("name");  // Имя игрока
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result; // Возвращаем массив с состоянием доски, текущим игроком и именем игрока
    }
}