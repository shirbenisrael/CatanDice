package com.shirbi.catandice;

import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    ImageView m_histogram_images[];
    TextView m_histogram_text[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_logic = new Logic();

        m_histogram_images = new ImageView[Card.MAX_NUMBER_ON_DICE*2-1];
        m_histogram_text = new TextView[Card.MAX_NUMBER_ON_DICE*2-1];

        m_size = GetWindowSize();

        int dice_width = m_size.x / 2;
        int dice_height = dice_width;

        ImageView red_dice_result_image = (ImageView)findViewById(R.id.red_dice_result);
        ImageView yellow_dice_result_image = (ImageView)findViewById(R.id.yellow_dice_result);

        red_dice_result_image.getLayoutParams().width = dice_width;
        red_dice_result_image.getLayoutParams().height = dice_height;

        yellow_dice_result_image.getLayoutParams().width = dice_width;
        yellow_dice_result_image.getLayoutParams().height = dice_height;

        LinearLayout histogram_images_layout = (LinearLayout)findViewById(R.id.histogram_images_layout);
        LinearLayout histogram_text_layout = (LinearLayout)findViewById(R.id.histogram_text_layout);

        for (int i = 0; i < m_histogram_images.length; i++) {
            m_histogram_images[i] = new ImageView(this);
            m_histogram_text[i] = new TextView(this);

            int color = (i % 2) * 100 + 100;
            m_histogram_images[i].setBackgroundColor(Color.argb(150, 0, color, 0));

            m_histogram_text[i].setTextColor(Color.YELLOW);
            m_histogram_text[i].setGravity(Gravity.CENTER);

            histogram_images_layout.addView(m_histogram_images[i]);

            m_histogram_images[i].getLayoutParams().width = m_size.x / m_histogram_images.length;
            m_histogram_images[i].getLayoutParams().height = 20;

            histogram_text_layout.addView(m_histogram_text[i]);

            m_histogram_text[i].setText(String.valueOf(i + 2));
            m_histogram_text[i].getLayoutParams().width = m_size.x / m_histogram_images.length;
        }
    }

    private void ShowHistogram() {
        int[] histogram = m_logic.GetSumHistogram();

        for (int i = 0; i < m_histogram_images.length; i++) {
            int width=m_histogram_images[i].getWidth();
            int height= histogram[i] * 10 + 1;
            m_histogram_images[i].setLayoutParams(new LinearLayout.LayoutParams(width, height));
        }
    }

    private Point GetWindowSize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    private Logic m_logic;

    private Point m_size;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void SetDicesImagesRolled(int red_dice_number, int yellow_dice_number) {
        ImageView red_dice_result_image = (ImageView) findViewById(R.id.red_dice_result);
        ImageView yellow_dice_result_image = (ImageView) findViewById(R.id.yellow_dice_result);

        SetDicesImages(red_dice_number, yellow_dice_number, red_dice_result_image, yellow_dice_result_image);
    }

    public void SetDicesImages(int red_dice_number, int yellow_dice_number,
                               ImageView red_dice_result_image, ImageView yellow_dice_result_image) {
        int red_images[] =
                {R.drawable.red_1, R.drawable.red_2, R.drawable.red_3, R.drawable.red_4, R.drawable.red_5, R.drawable.red_6};

        red_dice_result_image.setImageResource(red_images[red_dice_number - 1]);

        int yellow_images[] =
                {R.drawable.yellow_1, R.drawable.yellow_2, R.drawable.yellow_3, R.drawable.yellow_4, R.drawable.yellow_5, R.drawable.yellow_6};

        yellow_dice_result_image.setImageResource(yellow_images[yellow_dice_number - 1]);
    }

    public void onRollClick(View view) {

        Card card;
        card = m_logic.GetNewCard();

        SetDicesImagesRolled(card.m_red, card.m_yellow);

        ShowHistogram();

        //TODO: Add sound?
    }
}
