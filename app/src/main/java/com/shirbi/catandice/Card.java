package com.shirbi.catandice;

/**
 * Created by shirbi on 20/08/2017.
 */
public class Card {
    public int m_red;
    public int m_yellow;
    public int m_turn_number;
    public MessageWithCard m_message;
    public EventDice m_event_dice;
    public int m_pirate_position;

    public static final int MAX_NUMBER_ON_DICE;
    public static final int MAX_EVENTS_ON_EVENT_DICE;
    public static final int MAX_PIRATE_POSITIONS = 8;

    static {
        MAX_NUMBER_ON_DICE = 6;
        MAX_EVENTS_ON_EVENT_DICE = 4;
    }

    public enum EventDice {
        PIRATE_SHIP(0),
        YELLOW_CITY(1),
        BLUE_CITY(2),
        GREEN_CITY(3);

        private int value;
        private EventDice(int value){
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum MessageWithCard {
        NO_MESSAGE(0),
        SEVEN_WITHOUT_ROBBER(1),
        SEVEN_WITH_ROBBER(2),
        NEW_GAME(3),
        PIRATE_ATTACK(4),
        PIRATE_ATTACK_ROBBER_ATTACK(5),
        PIRATE_ATTACK_ROBBER_IS_SLEEPING(6),
        LAST_MOVE_CANCELED(7);

        private int value;
        private MessageWithCard(int value){
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public Card(int red, int yellow, MessageWithCard message) {
        m_red = red;
        m_yellow = yellow;
        m_message = message;
        m_turn_number = 0;
        m_event_dice = EventDice.PIRATE_SHIP;
        m_pirate_position = 0;
    }

    public String ToString() {
        String string = String.valueOf(m_red) + "," +
                String.valueOf(m_yellow) + "," +
                String.valueOf(m_message.getValue()) + "," +
                String.valueOf(m_turn_number) + "," +
                String.valueOf(m_event_dice.getValue()) + "," +
                String.valueOf(m_pirate_position) + ",";

        return string;
    }

    public Card(int[] intArray, int startIndex) {
        m_red = intArray[startIndex + 0];
        m_yellow = intArray[startIndex + 1];
        m_message = MessageWithCard.values()[intArray[startIndex + 2]];
        m_turn_number = intArray[startIndex + 3];
        m_event_dice = EventDice.values()[intArray[startIndex + 4]];
        m_pirate_position = intArray[startIndex + 5];
    }
}


