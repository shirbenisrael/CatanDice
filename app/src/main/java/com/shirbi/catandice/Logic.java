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

    static final int LOG_OF_FAIRNESS_FACTOR;

    static {
        LOG_OF_FAIRNESS_FACTOR = 4;
    }

    public Logic(int num_players) {
        m_histogram = new int[Card.MAX_NUMBER_ON_DICE * Card.MAX_NUMBER_ON_DICE];
        Init(num_players);
    }

    public Card GetNewCard() {

        int maxAppear = 0;
        int i;

        for(i=0;i<m_histogram.length;i++) {
            maxAppear = Math.max(maxAppear, m_histogram[i]);
        }

        int weights[] = new int[m_histogram.length];
        int sumWeights = 0;

        for(i=0;i<weights.length;i++) {
            int log_weight = LOG_OF_FAIRNESS_FACTOR *(maxAppear - m_histogram[i]);
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

        m_current_turn_number++;

        Card cardToReturn = IndexToCard(i);

        if (cardToReturn.m_red + cardToReturn.m_yellow == 7) {
            if (m_current_turn_number <= m_num_players * 2) {
                cardToReturn.m_message = Card.MessageWithCard.SEVEN_WITHOUT_ROBBER;
            } else {
                cardToReturn.m_message = Card.MessageWithCard.SEVEN_WITH_ROBBER;
            }
        }

        cardToReturn.m_turn_number = m_current_turn_number;

        return cardToReturn;
    }

    private Card IndexToCard(int i) {
        int redNumber = (i / Card.MAX_NUMBER_ON_DICE) + 1;
        int yellowNumber = (i % Card.MAX_NUMBER_ON_DICE) + 1;

        Card cardToReturn = new Card(redNumber, yellowNumber, Card.MessageWithCard.NO_MESSAGE);

        return cardToReturn;

    }

    public void Init(int num_players) {
        for(int i=0;i<m_histogram.length;i++) {
            m_histogram[i] = 0;
        }

        rand = new Random();

        m_current_turn_number = 0;
        m_num_players = num_players;
    }

    public int[] GetSumHistogram() {
        int hisotgramToReturn[] = new int[Card.MAX_NUMBER_ON_DICE * 2 -1];

        for(int i=0;i<m_histogram.length;i++) {
            Card card = IndexToCard(i);
            hisotgramToReturn[card.m_red + card.m_yellow - 2] += m_histogram[i];
        }
        return hisotgramToReturn;
    }



    public void StoreState(Context context, SharedPreferences.Editor editor) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < m_histogram.length; i++) {
            str.append(m_histogram[i]).append(",");
        }
        editor.putString(context.getString(R.string.m_histogram), str.toString());
        editor.putInt(context.getString(R.string.m_current_turn_number), m_current_turn_number);
    }

    public void RestoreState(Context context, SharedPreferences sharedPref) {
        m_num_players = sharedPref.getInt(context.getString(R.string.m_num_players), MainActivity.DEFAULT_NUMBER_OF_PLAYERS);
        m_current_turn_number = sharedPref.getInt(context.getString(R.string.m_current_turn_number), 0);

        String savedString = sharedPref.getString(context.getString(R.string.m_histogram), "");
        StringTokenizer st = new StringTokenizer(savedString, ",");

        for (int i = 0; i < m_histogram.length; i++) {
            if (st.hasMoreTokens()){
                m_histogram[i] = Integer.parseInt(st.nextToken());
            }
        }
    }
}

