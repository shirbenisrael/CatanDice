package com.shirbi.catandice;

import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Histogram {

    private TextView[] m_histogram_counters; /* histograms values above bars*/
    private ImageView[] m_histogram_images; /* histogram bars */

    private LinearLayout m_bar_layout;
    private LinearLayout m_static_numbers_layout;

    private int m_total_window_height;

    public Histogram(MainActivity activity,
                     int histogram_window_width,
                     int histogram_window_height,
                     int total_window_height,
                     LinearLayout bar_layout,
                     LinearLayout static_numbers_layout,
                     int min, int max) {
        int num_bars = max - min + 1;

        m_total_window_height = total_window_height;

        m_bar_layout = bar_layout;
        m_static_numbers_layout = static_numbers_layout;

        m_histogram_counters = new TextView[num_bars];
        m_histogram_images = new ImageView[num_bars];
        TextView[] histogram_text = new TextView[num_bars];  /* static numbers under the histogram */

        for (int i = 0; i < m_histogram_images.length; i++) {
            LinearLayout layout_for_one_bar_and_counter = new LinearLayout(activity.getApplicationContext());
            layout_for_one_bar_and_counter.setOrientation(LinearLayout.VERTICAL);
            layout_for_one_bar_and_counter.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            m_histogram_counters[i] = new TextView(activity);
            m_histogram_images[i] = new ImageView(activity);
            histogram_text[i] = new TextView(activity);

            int color = (i % 2) * 100 + 100;
            int bar_color = Color.rgb(0, color, 0);

            m_histogram_images[i].setBackgroundColor(bar_color);

            histogram_text[i].setTextColor(bar_color);
            histogram_text[i].setGravity(Gravity.CENTER);

            m_histogram_counters[i].setTextColor(bar_color);
            m_histogram_counters[i].setGravity(Gravity.CENTER);

            bar_layout.addView(layout_for_one_bar_and_counter);
            layout_for_one_bar_and_counter.addView(m_histogram_counters[i]);
            layout_for_one_bar_and_counter.addView(m_histogram_images[i]);
            layout_for_one_bar_and_counter.setGravity(Gravity.CENTER_HORIZONTAL);

            m_histogram_images[i].getLayoutParams().width = histogram_window_width / (m_histogram_images.length * 3);
            m_histogram_images[i].getLayoutParams().height = 20;

            static_numbers_layout.addView(histogram_text[i]);

            histogram_text[i].setText(String.valueOf(min + i));
            histogram_text[i].getLayoutParams().width = histogram_window_width / m_histogram_images.length;

            m_histogram_counters[i].getLayoutParams().width = histogram_window_width / m_histogram_images.length;
        }
    }

    public void SetVisibility(int visibility) {
        m_bar_layout.setVisibility(visibility);
        m_static_numbers_layout.setVisibility(visibility);
    }

    public void ShowHistogram(int[] histogram) {
        int max_bar_height = m_total_window_height;
        max_bar_height = (max_bar_height * 7) / 10;

        int max_histogram_value = 1;

        for (int i = 0; i < m_histogram_images.length; i++) {
            max_histogram_value = Math.max(max_histogram_value, histogram[i]);
        }

        if (max_histogram_value == 1) {
            max_bar_height = max_bar_height / 2;
        }

        for (int i = 0; i < m_histogram_images.length; i++) {
            int height = histogram[i] * max_bar_height / max_histogram_value;
            m_histogram_images[i].getLayoutParams().height = height;
            m_histogram_counters[i].setText(String.valueOf(histogram[i]));
            m_histogram_images[i].requestLayout();
        }
    }
}
