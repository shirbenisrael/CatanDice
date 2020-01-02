package com.shirbi.catandice;

import android.widget.Toast;

public class BluetoothMessageHandler {
    private  static int[] ParseAsInts(String[] strArray) {
        int[] intArray = new int[strArray.length];

        for (int i = 0; i < strArray.length; i++) {
            intArray[i] = Integer.parseInt(strArray[i]);
        }

        return intArray;
    }

    static void ParseMessage(String message, MainActivity activity) {
        String[] strArray = message.split(",");
        int[] intArray;

        int messageType = Integer.parseInt(strArray[0]);
        Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_SHORT).show();

        switch (messageType) {
            // TODO: Handle messages
            case BLUETOOTH_MESSAGES.START_GAME:
                intArray = ParseAsInts(strArray);

                Logic.GameType gameType = Logic.GameType.values()[intArray[1]];
                int num_players = intArray[2];
                int starting_player = intArray[3];

                activity.SetStartingParameters(gameType, num_players, starting_player);
                activity.StartingNewGame(false);
                break;

            case BLUETOOTH_MESSAGES.DISCONNECT:
                activity.disconnect(false);
                break;

            case BLUETOOTH_MESSAGES.FIX_RED_DICE:
                intArray = ParseAsInts(strArray);
                activity.FixDice(intArray[1], true);
                break;

            case BLUETOOTH_MESSAGES.FIX_YELLOW_DICE:
                intArray = ParseAsInts(strArray);
                activity.FixDice(intArray[1], false);
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

    static void SendFixDice(MainActivity activity, boolean is_red, int fixed_value) {
        int id = is_red ? BLUETOOTH_MESSAGES.FIX_RED_DICE : BLUETOOTH_MESSAGES.FIX_YELLOW_DICE;
        String message = String.valueOf(id) + "," + String.valueOf(fixed_value);
        activity.sendMessage(message);
    }

    static void SendDisconnectMessage(MainActivity activity) {
        String message = String.valueOf(BluetoothMessageHandler.BLUETOOTH_MESSAGES.DISCONNECT);
        activity.sendMessage(message);
    }

    class BLUETOOTH_MESSAGES {
        static final int START_GAME = 0;
        static final int DISCONNECT = 1;
        static final int FIX_RED_DICE = 2;
        static final int FIX_YELLOW_DICE = 3;
    }
}
