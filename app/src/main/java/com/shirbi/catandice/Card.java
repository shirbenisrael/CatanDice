package com.shirbi.catandice;

/**
 * Created by shirbi on 20/08/2017.
 */
public class Card {
    public int m_red;
    public int m_yellow;

    public static final int MAX_NUMBER_ON_DICE;

    static {
        MAX_NUMBER_ON_DICE = 6;
    }

    public Card(int red, int yellow) {
        m_red = red;
        m_yellow = yellow;

    }
}
