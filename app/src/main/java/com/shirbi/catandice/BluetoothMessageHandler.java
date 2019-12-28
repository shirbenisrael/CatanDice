package com.shirbi.catandice;

import android.widget.Toast;

public class BluetoothMessageHandler {
    static void ParseMessage(String message, MainActivity activity) {
        String[] strArray = message.split(",");

        int messageType = Integer.parseInt(strArray[0]);
        Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_SHORT).show();

        switch (messageType) {
            // TODO: Handle messages
            case BLUETOOTH_MESSAGES.START_GAME:
                int[] intArray = new int[strArray.length];
                for (int i = 0; i < strArray.length; i++) {
                    intArray[i] = Integer.parseInt(strArray[i]);
                }

                Logic.GameType gameType = Logic.GameType.values()[intArray[1]];
                int num_players = intArray[2];
                int starting_player = intArray[3];

                activity.SetStartingParameters(gameType, num_players, starting_player);
                activity.StartingNewGame(false);
                break;

            case BLUETOOTH_MESSAGES.DISCONNECT:
                activity.disconnect(false);
                break;
        }
    }

    static void SendStartingGameParameters(Logic.GameType game_type, int num_players, int starting_player,
                                           MainActivity activity) {
        String message = String.valueOf(BluetoothMessageHandler.BLUETOOTH_MESSAGES.START_GAME) + "," +
                String.valueOf(game_type.getValue()) + "," + String.valueOf(num_players) + "," +
                String.valueOf(starting_player);

        activity.sendMessage(message);
    }

    static void SendDisconnectMessage(MainActivity activity) {
        String message = String.valueOf(BluetoothMessageHandler.BLUETOOTH_MESSAGES.DISCONNECT);
        activity.sendMessage(message);
    }

    class BLUETOOTH_MESSAGES {
        static final int START_GAME = 0;
        static final int DISCONNECT = 1;
    }
}
