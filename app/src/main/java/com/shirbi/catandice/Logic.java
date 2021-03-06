package com.shirbi.catandice;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Random;
import java.util.StringTokenizer;

/**
 * Created by shirbi on 20/08/2017.
 */
class Logic {

    private class Move {
        int histogram_cell_increase;
        boolean pirate_moved;
        boolean cell_increase;
    }

    private Random rand;
    private int[] m_histogram;
    private int m_num_players;
    private int m_current_turn_number;
    private int m_pirate_position;
    private GameType m_game_type;
    private boolean m_is_pirate_arrive;
    private Move m_last_move;
    private boolean m_is_enable_fair_dice;

    private static final int LOG_OF_FAIRNESS_FACTOR;
    static final int DEFAULT_PIRATE_POSITION;

    static {
        LOG_OF_FAIRNESS_FACTOR = 4;
        DEFAULT_PIRATE_POSITION = 0;
    }

    public enum GameType {
        GAME_TYPE_REGULAR(0),
        GAME_TYPE_CITIES_AND_KNIGHT(1),
        GAME_TYPE_SIMPLE_DICE(2);

        private final int value;
        GameType(int value){
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public Logic(int num_players, GameType game_type, Boolean is_fair_dice) {
        m_histogram = new int[Card.MAX_NUMBER_ON_DICE * Card.MAX_NUMBER_ON_DICE];
        Init(num_players, game_type, is_fair_dice);
    }

    private void AddPirateToCardMessage(Card cardToReturn) {
        if( cardToReturn.m_message == Card.MessageWithCard.SEVEN_WITH_ROBBER) {
            cardToReturn.m_message = Card.MessageWithCard.PIRATE_ATTACK_ROBBER_ATTACK;
        } else if( cardToReturn.m_message == Card.MessageWithCard.SEVEN_WITHOUT_ROBBER) {
            cardToReturn.m_message = Card.MessageWithCard.PIRATE_ATTACK_ROBBER_IS_SLEEPING;
        } else {
            cardToReturn.m_message = Card.MessageWithCard.PIRATE_ATTACK;
        }
    }

    // Called from outside to update state after message from bluetooth
    public void SetCard(Card card) {
        m_last_move = new Move();
        m_last_move.pirate_moved = (card.m_event_dice == Card.EventDice.PIRATE_SHIP);
        int histogramIndex = CardToIndex(card);
        m_histogram[histogramIndex]++;
        m_last_move.histogram_cell_increase = histogramIndex;
        m_last_move.cell_increase = true;
        m_current_turn_number++;
        m_pirate_position = card.m_pirate_position;

        if (m_pirate_position == Card.MAX_PIRATE_POSITIONS-1) {
            m_is_pirate_arrive = true;
        }
    }

    public Card GetNewCard(boolean is_alchemist) {
        int i;
        Card cardToReturn;

        m_last_move = new Move();
        m_last_move.cell_increase = false;
        m_last_move.pirate_moved = false;

        if (!is_alchemist) {
            if (m_is_enable_fair_dice) {
                int maxAppear = 0;

                for (i = 0; i < m_histogram.length; i++) {
                    maxAppear = Math.max(maxAppear, m_histogram[i]);
                }

                int[] weights = new int[m_histogram.length];
                int sumWeights = 0;

                for (i = 0; i < weights.length; i++) {
                    int log_weight = LOG_OF_FAIRNESS_FACTOR * (maxAppear - m_histogram[i]);
                    weights[i] = 1 << Math.min(64, log_weight);

                    sumWeights += weights[i];
                }

                int randomValue = rand.nextInt(sumWeights);

                i = 0;
                while (weights[i] < randomValue) {
                    randomValue -= weights[i];
                    i++;
                }
            } else {
                i = rand.nextInt(m_histogram.length);
            }

            m_histogram[i]++;
            m_last_move.histogram_cell_increase = i;
            m_last_move.cell_increase = true;

            cardToReturn = IndexToCard(i);

            if (m_game_type != GameType.GAME_TYPE_SIMPLE_DICE) {
                if (cardToReturn.m_red + cardToReturn.m_yellow == 7) {
                    if (m_game_type == GameType.GAME_TYPE_REGULAR) {
                        if (m_current_turn_number <= m_num_players * 2) {
                            cardToReturn.m_message = Card.MessageWithCard.SEVEN_WITHOUT_ROBBER;
                        } else {
                            cardToReturn.m_message = Card.MessageWithCard.SEVEN_WITH_ROBBER;
                        }
                    } else {
                        if (!m_is_pirate_arrive) {
                            cardToReturn.m_message = Card.MessageWithCard.SEVEN_WITHOUT_ROBBER;
                        } else {
                            cardToReturn.m_message = Card.MessageWithCard.SEVEN_WITH_ROBBER;
                        }
                    }
                }
            }
        } else {
            cardToReturn = IndexToCard(0);
        }

        m_current_turn_number++;

        cardToReturn.m_turn_number = m_current_turn_number;

        if (m_pirate_position == Card.MAX_PIRATE_POSITIONS-1) {
            m_pirate_position = 0;
        }

        if (m_game_type == GameType.GAME_TYPE_CITIES_AND_KNIGHT) {
            int event_dice = rand.nextInt(6);
            switch (event_dice) {
                case 0:
                    cardToReturn.m_event_dice = Card.EventDice.YELLOW_CITY;
                    break;
                case 1:
                    cardToReturn.m_event_dice = Card.EventDice.GREEN_CITY;
                    break;
                case 2:
                    cardToReturn.m_event_dice = Card.EventDice.BLUE_CITY;
                    break;
                default:
                    cardToReturn.m_event_dice = Card.EventDice.PIRATE_SHIP;
                    m_pirate_position++;
                    m_last_move.pirate_moved = true;

                    if (m_pirate_position == Card.MAX_PIRATE_POSITIONS-1) {
                        AddPirateToCardMessage(cardToReturn);
                        m_is_pirate_arrive = true;
                    }

                    break;
            }
        }

        cardToReturn.m_pirate_position = m_pirate_position;

        return cardToReturn;
    }

    private int CardToIndex(Card card) {
        return (card.m_red -1) * Card.MAX_NUMBER_ON_DICE + (card.m_yellow -1);
    }

    private Card IndexToCard(int i) {
        int redNumber = (i / Card.MAX_NUMBER_ON_DICE) + 1;
        int yellowNumber = (i % Card.MAX_NUMBER_ON_DICE) + 1;

        return new Card(redNumber, yellowNumber);
    }

    void Init(int num_players, GameType game_type, Boolean is_fair_dice) {
        for(int i=0;i<m_histogram.length;i++) {
            m_histogram[i] = 0;
        }

        DisableCancelLastMove();

        rand = new Random();

        m_pirate_position = DEFAULT_PIRATE_POSITION;
        m_current_turn_number = 0;
        m_num_players = num_players;
        m_game_type = game_type;
        m_is_pirate_arrive = false;
        m_is_enable_fair_dice = is_fair_dice;
    }

    void SetEnableFairDice(boolean is_enable) {
        m_is_enable_fair_dice = is_enable;
    }

    int[] GetOneDiceHistogram() {
        int[] histogramToReturn = new int[Card.MAX_NUMBER_ON_DICE];

        for(int i=0;i<m_histogram.length;i++) {
            Card card = IndexToCard(i);
            histogramToReturn[card.m_red -1] += m_histogram[i];
            histogramToReturn[card.m_yellow - 1] += m_histogram[i];
        }
        return histogramToReturn;
    }

    int[] GetSumHistogram() {
        int[] histogramToReturn = new int[Card.MAX_NUMBER_ON_DICE * 2 - 1];

        for(int i=0;i<m_histogram.length;i++) {
            Card card = IndexToCard(i);
            histogramToReturn[card.m_red + card.m_yellow - 2] += m_histogram[i];
        }
        return histogramToReturn;
    }

    int[][] GetCombinationHistogram() {
        int[][] combinationHistogram = new int[Card.MAX_NUMBER_ON_DICE][Card.MAX_NUMBER_ON_DICE];

        for(int i = 0; i < m_histogram.length; i++) {
            Card card = IndexToCard(i);
            combinationHistogram[card.m_red - 1][card.m_yellow - 1] = m_histogram[i];
        }

        return combinationHistogram;
    }

    int GetTurnNumber() {return m_current_turn_number;}

    void IncreaseTurnNumber() { m_current_turn_number++; }

    void StoreState(Context context, SharedPreferences.Editor editor) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < m_histogram.length; i++) {
            str.append(m_histogram[i]).append(",");
        }
        editor.putString(context.getString(R.string.m_histogram), str.toString());
        editor.putInt(context.getString(R.string.m_current_turn_number), m_current_turn_number);
        editor.putBoolean(context.getString(R.string.m_is_pirate_arrive), m_is_pirate_arrive);
    }

    void RestoreState(Context context, SharedPreferences sharedPref) {
        m_num_players = sharedPref.getInt(context.getString(R.string.m_num_players), MainActivity.DEFAULT_NUMBER_OF_PLAYERS);
        m_current_turn_number = sharedPref.getInt(context.getString(R.string.m_current_turn_number), 0);
        m_pirate_position = sharedPref.getInt(context.getString(R.string.m_pirate_position), DEFAULT_PIRATE_POSITION);
        m_is_pirate_arrive = sharedPref.getBoolean(context.getString(R.string.m_is_pirate_arrive), false);
        int game_type_num = sharedPref.getInt(context.getString(R.string.m_game_type), 0);
        m_game_type = Logic.GameType.values()[game_type_num];

        String savedString = sharedPref.getString(context.getString(R.string.m_histogram), "");
        StringTokenizer st = new StringTokenizer(savedString, ",");

        for (int i = 0; i < m_histogram.length; i++) {
            if (st.hasMoreTokens()){
                m_histogram[i] = Integer.parseInt(st.nextToken());
            }
        }
    }

    boolean CanCancelLastMove() {
        return (m_last_move != null);
    }

    int GetPiratePosition() {
        return m_pirate_position;
    }

    void DisableCancelLastMove() {
        m_last_move = null;
    }

    void CancelLastMove() {
        if (m_current_turn_number == 0) {
            return;
        }

        if (m_last_move == null) {
            return;
        }

        if (m_last_move.cell_increase) {
            m_histogram[m_last_move.histogram_cell_increase]--;
        }

        if (m_last_move.pirate_moved) {
            m_pirate_position--;
            if (m_pirate_position < 0) {
                m_pirate_position = Card.MAX_PIRATE_POSITIONS-2;
            }
        }

        m_current_turn_number--;

        DisableCancelLastMove();
    }

    // used to send to other device by bluetooth.
    String ToString() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < m_histogram.length; i++) {
            str.append(m_histogram[i]).append(",");
        }

        str.append(
                m_num_players + "," +
                        m_current_turn_number + "," +
                        m_pirate_position + "," +
                        (m_is_pirate_arrive ? 1 : 0) + "," +
                        (m_is_enable_fair_dice ? 1 : 0) + "," +
                        m_game_type.getValue() + ",");

        if (m_last_move != null) {
            str.append("1").append(",");
            str.append(m_last_move.histogram_cell_increase).append(",");
            str.append(m_last_move.pirate_moved ? 1 : 0).append(",");
            str.append(m_last_move.cell_increase ? 1 : 0);
        } else {
            str.append("0");
        }

        return str.toString();
    }

    // used to get from other device by bluetooth.
    int UpdateFromIntArray(int[] intArray, int startIndex) {
        for (int i = 0; i < m_histogram.length; i++) {
            m_histogram[i] = intArray[i + startIndex];
        }

        startIndex += m_histogram.length;

        m_num_players = intArray[startIndex++];
        m_current_turn_number = intArray[startIndex++];
        m_pirate_position = intArray[startIndex++];
        m_is_pirate_arrive = intArray[startIndex++] == 1;
        m_is_enable_fair_dice = intArray[startIndex++] == 1;
        m_game_type = GameType.values()[intArray[startIndex++]];

        if (intArray[startIndex++] == 1) {
            m_last_move = new Move();
            m_last_move.histogram_cell_increase = intArray[startIndex++];
            m_last_move.pirate_moved = (intArray[startIndex++] == 1);
            m_last_move.cell_increase = (intArray[startIndex++] == 1);
        } else {
            DisableCancelLastMove();
        }

        return startIndex;
    }

    GameType GetGameType() { return m_game_type; }

    int GetNumPlayers() { return m_num_players; }


}

