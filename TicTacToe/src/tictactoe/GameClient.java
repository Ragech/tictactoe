package tictactoe;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.net.Socket;

public class GameClient extends JFrame {
    private static int size = 15;
    private JButton[][] button = new JButton[size][size]; // Массив кнопок для игрового поля
    private String[][] board = new String[size][size]; // Двумерный массив для хранения состояния игры
    private String symbol; // Символ игрока
    public String playerName;
    private boolean isMyTurn = false; // Индикатор хода игрока
    private Socket socket; // Сокет для соединения с сервером
    private PrintWriter outputStream; // Поток для отправки данных серверу
    private BufferedReader inputStream; // Поток для получения данных от сервера
    
    // Конструктор класса: инициализирует главное окно и вызывает главное меню
    public GameClient() {
        setTitle("Крестики-Нолики");
        setSize(800, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(255, 255, 255));
        serverDialog();
    }
    
    // Диалог для подключения к серверу
    private void serverDialog() {
        // Панель с полями ввода IP-адреса сервера и имени игрока
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 2, 5, 5));
        panel.setBackground(new Color(255, 255, 255)); // Светло-желтый цвет

        // Метки и поля ввода
        JLabel ipLabel = new JLabel("IP сервера:");
        JTextField ipField = new JTextField(15);
        JLabel nameLabel = new JLabel("Ваше имя:");
        JTextField nameField = new JTextField(15);

        // Настраиваем стиль элементов
        Font label = new Font("Arial", Font.BOLD, 16);
        ipLabel.setFont(label);
        nameLabel.setFont(label);
        ipField.setBackground(new Color(255, 255, 255));
        nameField.setBackground(new Color(255, 255, 255));

        // Добавляем элементы на панель
        panel.add(ipLabel);
        panel.add(ipField);
        panel.add(nameLabel);
        panel.add(nameField);

        // Создаем диалоговое окно
        JOptionPane optionPane = new JOptionPane(panel);

        // Настраиваем внешний вид диалога
        optionPane.setBackground(new Color(255, 255, 255));
        JDialog dialog = optionPane.createDialog(this, "Подключение к серверу");
        dialog.setBackground(new Color(255, 255, 255));

        // Показываем диалог
        dialog.setVisible(true);

        // Обработка результата ввода
        Integer result = (Integer) optionPane.getValue();
        if (result != null && result == JOptionPane.OK_OPTION) {
            String serverAddress = ipField.getText().trim(); // IP сервера
            playerName = nameField.getText().trim(); // Имя игрока
            int serverPort = 8087; // Порт сервера (по умолчанию)
            // Проверяем, что поля адреса сервера и имени игрока заполнены
            if (!serverAddress.isEmpty() && !playerName.isEmpty()) {
                initializeGame(serverAddress, serverPort, playerName); // Инициализация игры с заданными параметрами
            } else {
                // Если поля не заполнены, выводим сообщение об ошибке
                JOptionPane.showMessageDialog(
                    this,
                    "Пожалуйста, заполните все поля",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE
                );
                serverDialog(); // Возвращаемся в меню ввода ip и имени
            }
        } else {
            System.exit(0);
        }
    }
    
    // Инициализация игры
    private void initializeGame(String serverAddress, int serverPort, String playerName) {
        resetUIForGame(); // Сбрасываем интерфейс для новой игры
        initializeButtons(); // Инициализируем кнопки игрового поля
        // Пытаемся установить соединение с сервером
        if (setupConnection(serverAddress, serverPort)) {
            new Thread(this::serverMessageHandler).start(); // Запускаем обработчик сообщений от сервера в отдельном потоке
            setVisible(true); // Делаем главное окно видимым
        } else {
            // Если соединение не удалось, выводим сообщение об ошибке
            JOptionPane.showMessageDialog(
                this, 
                "Ошибка подключения к серверу", 
                "Ошибка", 
                JOptionPane.ERROR_MESSAGE
            );
            serverDialog();
        }
    }
    
    // Сброс интерфейса для игры
    private void resetUIForGame() {
        getContentPane().removeAll(); // Удаляем все компоненты из текущего окна
        setLayout(new BorderLayout()); // Устанавливаем новый макет
    }

    // Устанавливаем соединение с сервером
    private boolean setupConnection(String serverAddress, int serverPort) {
        try {// Инициализируем сокет и потоки ввода/вывода
            socket = new Socket(serverAddress, serverPort);
            outputStream = new PrintWriter(socket.getOutputStream(), true); // Возвращает выходной поток, по которому можно отправлять данные на сервер.
            inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Создаёт буферизированный поток
            return true; // Соединение успешно
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Инициализация кнопок интерфейса
    private void initializeButtons() {
        // Верхняя панель с кнопками управления
        JPanel topPanel = new JPanel(); // Центровка и отступы
        
        // Кнопка сохранения игры
        JButton saveButton = new JButton("Сохранить игру");
        saveButton.setFont(new Font("Arial", Font.BOLD, 18));
        saveButton.setBackground(new Color(255, 255, 255)); // Светло-желтый для кнопок
        saveButton.addActionListener(e -> saveGame());
        topPanel.add(saveButton);

        // Кнопка загрузки игры
        JButton loadButton = new JButton("Загрузить игру");
        loadButton.setFont(new Font("Arial", Font.BOLD, 18));
        loadButton.setBackground(new Color(255, 255, 255)); // Светло-желтый для кнопок
        loadButton.addActionListener(e -> loadGame());
        topPanel.add(loadButton);       
        
        add(topPanel, BorderLayout.NORTH); // Добавляем верхнюю панель на главное окно
        
        // Панель игрового поля
        JPanel gamePanel = new JPanel(new GridLayout(size, size));
        
        // Инициализация кнопок игрового поля
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                button[row][col] = new JButton("");
                button[row][col].setFont(new Font("Arial", Font.BOLD, 20));
                button[row][col].setBackground(new Color(255, 255, 255)); // Белый для клеток
                button[row][col].setEnabled(false);
                button[row][col].addActionListener(new ButtonClickListener(row, col));
                gamePanel.add(button[row][col]);
            }
        }
        add(gamePanel, BorderLayout.CENTER); // Добавляем игровую панель на главное окно
    }
    
    // Сохранение игры
    private void saveGame() {
        // Сохраняем текущее состояние игрового поля
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                board[row][col] = button[row][col].getText();
            }
        }
        // Сохраняем состояние игры в базу данных
        GameDatabase.saveGameState(board, isMyTurn ? symbol : 
            (symbol.equals("X") ? "O" : "X"), playerName);
        JOptionPane.showMessageDialog(this, "Игра сохранена!");
    }
    
    // Загрузка игры
    private void loadGame() {
        outputStream.println("LOAD_GAME"); // Отправляем запрос на сервер
    } 
    
    // Обработчик сообщений от сервера
    private void serverMessageHandler() {
        try {
            while (true) {
                String message = inputStream.readLine(); // Читаем сообщение от сервера
                if (message == null) break; // Соединение прервано
                // Обработка сообщений
                if (message.startsWith("SYNC_LOAD_GAME")) {
                    String[] parts = message.split(" "); // Разбираем сообщение на части
                    String boardState = parts[1]; // Состояние доски
                    String currentTurn = parts[2]; // Чей ход
                    String playerName = parts[3]; // Имя игрока, сохранившего игру
                    // Обновляем игровую доску в UI
                    SwingUtilities.invokeLater(() -> {
                        int index = 0; // Индекс для обхода строки состояния доски
                        for (int row = 0; row < size; row++) {
                            for (int col = 0; col < size; col++) {
                                char symbol = boardState.charAt(index++); // Получаем символ для каждой клетки доски
                                switch (symbol) {
                                    case '0': // Пустая клетка
                                        button[row][col].setText("");
                                        break;
                                    case '1':
                                        button[row][col].setText("X");
                                        break;
                                    case '2':
                                        button[row][col].setText("O");
                                        break;
                                }
                            }
                        }
                        isMyTurn = currentTurn.equals(symbol); // Устанавливаем текущий ход
                        updateButtonsState(); // Обновляем состояние кнопок
                        // Обновите интерфейс клиента
                        JOptionPane.showMessageDialog(this, "Игра была загружена из сохранения игрока: " + playerName);
                    });                 
                    continue;
                }
                switch (message) {
                    case String s when s.startsWith("HI"): // Сообщение о том, что игрок присоединился, и за какой символ он играет
                        symbol = message.split(" ")[1];
                        setTitle(playerName + ", вы играете за " + symbol);
                        break;
                    case "YOUR_TURN": // Сообщение о том, что теперь ход игрока
                        isMyTurn = true;
                        updateButtonsState();
                        break;
                    case "WAIT_TURN": // Сообщение о том, что игрок должен подождать ход противника
                        isMyTurn = false;
                        updateButtonsState();
                        break;
                    case String s when s.startsWith("MOVE"): // Сообщение о ходе другого игрока
                        String[] parts = message.split(" ")[1].split(",");
                        int row = Integer.parseInt(parts[0]);
                        int col = Integer.parseInt(parts[1]);
                        String moveSymbol = parts[2]; // Символ, который поставил противник
                        SwingUtilities.invokeLater(() -> {
                            button[row][col].setText(moveSymbol);
                            button[row][col].setEnabled(false);
                        });
                        break;
                    case "WIN": // Сообщение о победе игрока
                        JOptionPane.showMessageDialog(this, "Вы выиграли!");
                        outputStream.println("NEW_GAME"); // Отправляем запрос на сервер для новой игры
                        resetGameBoard(); // Сбрасываем игровую доску
                        break;
                    case "LOSE": // Сообщение о поражении игрока
                        JOptionPane.showMessageDialog(this, "Вы проиграли!");
                        outputStream.println("NEW_GAME"); // Отправляем запрос на сервер для новой игры
                        resetGameBoard(); // Сбрасываем игровую доску
                        break;
                    case "DRAW": // Сообщение о ничьей
                        JOptionPane.showMessageDialog(this, "Ничья!");
                        outputStream.println("NEW_GAME"); // Отправляем запрос на сервер для новой игры
                        resetGameBoard(); // Сбрасываем игровую доску
                        break;
                    case "LOAD_FAILED": // Сообщение о неудачной загрузке игры
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, "Не удалось загрузить игру. Пожалуйста, попробуйте еще раз.", "Ошибка загрузки", JOptionPane.ERROR_MESSAGE);
                        });
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Соединение с сервером потеряно", "Ошибка", JOptionPane.ERROR_MESSAGE);
            serverDialog();
        }
    }
    
    // Обновление состояния кнопок в зависимости от текущего хода
    private void updateButtonsState() {
        SwingUtilities.invokeLater(() -> {
            for (int row = 0; row < size; row++) {
                for (int col = 0; col < size; col++) {
                    button[row][col].setEnabled(isMyTurn && button[row][col].getText().isEmpty()); // Активируем только пустые кнопки, если ход игрока
                }
            }
        });
    }
    
    // Закрытие соединений и выход из приложения
    private void closeConnectionsAndExit() {
        closeConnections(); // Закрываем все соединения
        System.exit(0); // Завершаем приложение
    }
        
    // Закрытие соединений с сервером
    private void closeConnections() {
        try {
            if (socket != null) socket.close();
            if (outputStream != null) outputStream.close();
            if (inputStream != null) inputStream.close();
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии соединений: " + e.getMessage());
        }
    }
    
    // Сброс состояния игровой доски
    private void resetGameBoard() {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                button[row][col].setText(""); // Очищаем текст кнопок
                button[row][col].setEnabled(false); // Деактивируем кнопки
            }
        }
        isMyTurn = false; // Сбрасываем флаг хода
        setButtonsEnabled(false); // Деактивируем кнопки
    }
    
    // Установка состояния активности всех кнопок
    private void setButtonsEnabled(boolean enabled) {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                button[row][col].setEnabled(enabled);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameClient::new); // Запускаем клиент в потоке интерфейса Swing
    }
    
    // Обработчик кликов на кнопках игрового поля
    private class ButtonClickListener implements ActionListener {
        private final int row, col;
        
        // Конструктор с координатами кнопки
        public ButtonClickListener(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            handleClick(); // Обработка клика по кнопке
        }

        private void handleClick() {
            if (!isMyTurn || !button[row][col].getText().isEmpty()) return; // Проверяем, что это ход игрока и клетка пустая

            button[row][col].setText(symbol); // Устанавливаем символ игрока на кнопку
            button[row][col].setEnabled(false);
            isMyTurn = false; // Передаем ход другому игроку
            sendMoveToServer(row, col); // Отправляем координаты хода на сервер
        }

        private void sendMoveToServer(int row, int col) {
            outputStream.println(row + "," + col); // Отправляем сообщение о ходе на сервер
        }
    }
}