package com.shirbi.catandice;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Random;
import java.util.StringTokenizer;

/**
 * Created by shirbi on 20/08/2017.
 */
public class Logic {

    private Random rand;
    private int m_histogram[];
    private int m_num_players;
    private int m_current_turn_number;
    private int m_pirate_position;
    private GameType m_game_type;
    private boolean m_is_pirate_arrive;

    static final int LOG_OF_FAIRNESS_FACTOR;
    public static final int DEFAULT_PIRATE_POSITION;

    static {
        LOG_OF_FAIRNESS_FACTOR = 4;
        DEFAULT_PIRATE_POSITION = 0;
    }

    public enum GameType {
        GAME_TYPE_REGULAR(0),
        GAME_TYPE_CITIES_AND_KNIGHT(1);

        private int value;
        private GameType(int value){
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public Logic(int num_players, GameType game_type) {
        m_histogram = new int[Card.MAX_NUMBER_ON_DICE * Card.MAX_NUMBER_ON_DICE];
        Init(num_players, game_type);
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

    public Card GetNewCard(boolean is_alchemist) {
        int i;
        Card cardToReturn;

        if (!is_alchemist) {
            int maxAppear = 0;

            for (i = 0; i < m_histogram.length; i++) {
                maxAppear = Math.max(maxAppear, m_histogram[i]);
            }

            int weights[] = new int[m_histogram.length];
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

            m_histogram[i]++;

            cardToReturn = IndexToCard(i);

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

    private Card IndexToCard(int i) {
        int redNumber = (i / Card.MAX_NUMBER_ON_DICE) + 1;
        int yellowNumber = (i % Card.MAX_NUMBER_ON_DICE) + 1;

        Card cardToReturn = new Card(redNumber, yellowNumber, Card.MessageWithCard.NO_MESSAGE);

        return cardToReturn;

    }

    public void Init(int num_players, GameType game_type) {
        for(int i=0;i<m_histogram.length;i++) {
            m_histogram[i] = 0;
        }

        rand = new Random();

        m_pirate_position = DEFAULT_PIRATE_POSITION;
        m_current_turn_number = 0;
        m_num_players = num_players;
        m_game_type = game_type;
        m_is_pirate_arrive = false;
    }

    public int[] GetSumHistogram() {
        int hisotgramToReturn[] = new int[Card.MAX_NUMBER_ON_DICE * 2 -1];

        for(int i=0;i<m_histogram.length;i++) {
            Card card = IndexToCard(i);
            hisotgramToReturn[card.m_red + card.m_yellow - 2] += m_histogram[i];
        }
        return hisotgramToReturn;
    }

    public int GetTurnNumber() {return m_current_turn_number;}

    public void StoreState(Context context, SharedPreferences.Editor editor) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < m_histogram.length; i++) {
            str.append(m_histogram[i]).append(",");
        }
        editor.putString(context.getString(R.string.m_histogram), str.toString());
        editor.putInt(context.getString(R.string.m_current_turn_number), m_current_turn_number);
        editor.putBoolean(context.getString(R.string.m_is_pirate_arrive), m_is_pirate_arrive);
    }

    public void RestoreState(Context context, SharedPreferences sharedPref) {
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
}

