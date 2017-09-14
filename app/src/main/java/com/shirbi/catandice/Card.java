package com.shirbi.catandice;

/**
 * Created by shirbi on 20/08/2017.
 */
public class Card {
    public int m_red;
    public int m_yellow;
    public MessageWithCard m_message;

    public static final int MAX_NUMBER_ON_DICE;

    static {
        MAX_NUMBER_ON_DICE = 6;
    }

    public enum MessageWithCard {
        NO_MESSAGE,
        SEVEN_WITHOUT_ROBBER,
        SEVEN_WITH_ROBBER
    }

    public Card(int red, int yellow, MessageWithCard message) {
        m_red = red;
        m_yellow = yellow;
        m_message = message;
    }


}
