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
        NO_MESSAGE,
        SEVEN_WITHOUT_ROBBER,
        SEVEN_WITH_ROBBER,
        NEW_GAME,
        PIRATE_ATTACK,
        PIRATE_ATTACK_ROBBER_ATTACK,
        PIRATE_ATTACK_ROBBER_IS_SLEEPING,
        LAST_MOVE_CANCELED
    }

    public Card(int red, int yellow, MessageWithCard message) {
        m_red = red;
        m_yellow = yellow;
        m_message = message;
        m_turn_number = 0;
        m_event_dice = EventDice.PIRATE_SHIP;
        m_pirate_position = 0;
    }

}


