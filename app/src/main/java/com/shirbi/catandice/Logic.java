package com.shirbi.catandice;

import java.util.Random;

/**
 * Created by shirbi on 20/08/2017.
 */
public class Logic {

    private Random rand;

        private int m_histogram[];

    public Logic() {
        m_histogram = new int[Card.MAX_NUMBER_ON_DICE * Card.MAX_NUMBER_ON_DICE];
        Init();
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
            weights[i] = 1 << (maxAppear - m_histogram[i]);
            sumWeights += weights[i];
        }

        int randomValue = rand.nextInt(sumWeights);

        i = 0;
        while (weights[i] < randomValue) {
            randomValue -= weights[i];
            i++;
        }

        m_histogram[i]++;
        return IndexToCard(i);
    }

    private Card IndexToCard(int i) {
        int redNumber = (i / Card.MAX_NUMBER_ON_DICE) + 1;
        int yellowNumber = (i % Card.MAX_NUMBER_ON_DICE) + 1;

        Card cardToReturn = new Card(redNumber, yellowNumber);

        return cardToReturn;

    }

    public void Init() {
        for(int i=0;i<m_histogram.length;i++) {
            m_histogram[i] = 0;
        }

        rand = new Random();
    }

    public int[] GetSumHistogram() {
        int hisotgramToReturn[] = new int[Card.MAX_NUMBER_ON_DICE * 2 -1];

        for(int i=0;i<m_histogram.length;i++) {
            Card card = IndexToCard(i);
            hisotgramToReturn[card.m_red + card.m_yellow - 2] += m_histogram[i];
        }
        return hisotgramToReturn;
    }
}
