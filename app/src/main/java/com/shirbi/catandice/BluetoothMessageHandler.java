package com.shirbi.catandice;

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
        //Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_SHORT).show();

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

            case BLUETOOTH_MESSAGES.ROLL_ONE_DICE:
                intArray = ParseAsInts(strArray);
                activity.RollOneDice(intArray[1], intArray[2] != 0);
                break;

            case BLUETOOTH_MESSAGES.ROLL_ALL_DICE:
                intArray = ParseAsInts(strArray);
                Card card = new Card(intArray, 2);
                activity.rollAllDice(card, intArray[1] == 1);
                break;

            case BLUETOOTH_MESSAGES.CANCEL_LSAT_MOVE:
                activity.CancelLastMove(false);
                break;

            case BLUETOOTH_MESSAGES.SET_FAIR_DICE:
                intArray = ParseAsInts(strArray);
                activity.SetFairDice( intArray[1] == 1);
                break;

            case BLUETOOTH_MESSAGES.FULL_STATE:
                intArray = ParseAsInts(strArray);
                activity.SetFullState(
                        intArray[1],
                        intArray[2],
                        Card.EventDice.values()[intArray[3]],
                        intArray, 4);
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

    static void SendRoleOneDice(MainActivity activity, boolean is_red, int value_rolled) {
        String message = String.valueOf(BLUETOOTH_MESSAGES.ROLL_ONE_DICE) + "," + String.valueOf(value_rolled)+ ","
                + String.valueOf(is_red ? 1 : 0);
        activity.sendMessage(message);
    }

    static void SendRoleAllDice(MainActivity activity, Card card, boolean is_alchemist_active) {
        String message = String.valueOf(BLUETOOTH_MESSAGES.ROLL_ALL_DICE) + "," +
                String.valueOf(is_alchemist_active ? 1 : 0) + "," + card.ToString();
        activity.sendMessage(message);
    }

    static void SendDisconnectMessage(MainActivity activity) {
        String message = String.valueOf(BluetoothMessageHandler.BLUETOOTH_MESSAGES.DISCONNECT);
        activity.sendMessage(message);
    }

    static void SendCancelLastMove(MainActivity activity) {
        String message = String.valueOf(BluetoothMessageHandler.BLUETOOTH_MESSAGES.CANCEL_LSAT_MOVE);
        activity.sendMessage(message);
    }

    static void SendSetFairDice(MainActivity activity, boolean is_fair) {
        String message = String.valueOf(BluetoothMessageHandler.BLUETOOTH_MESSAGES.SET_FAIR_DICE) + "," +
                String.valueOf(is_fair ? 1 : 0);
        activity.sendMessage(message);
    }

    static void SendFullState(MainActivity activity, String logic_state, int red_dice, int yellow_dice,
                              Card.EventDice event_dice) {
        String message = String.valueOf(BluetoothMessageHandler.BLUETOOTH_MESSAGES.FULL_STATE) + "," +
                String.valueOf(red_dice) + "," + String.valueOf(yellow_dice)  + "," +
                String.valueOf(event_dice.getValue()) + "," + logic_state;

            activity.sendMessage(message);
    }

    class BLUETOOTH_MESSAGES {
        static final int START_GAME = 0;
        static final int DISCONNECT = 1;
        static final int FIX_RED_DICE = 2;
        static final int FIX_YELLOW_DICE = 3;
        static final int ROLL_ONE_DICE = 4;
        static final int ROLL_ALL_DICE = 5;
        static final int CANCEL_LSAT_MOVE = 6;
        static final int SET_FAIR_DICE = 7;
        static final int FULL_STATE = 8;
    }
}
