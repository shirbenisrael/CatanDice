package com.shirbi.catandice;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private ImageView m_histogram_images[];
    private TextView m_histogram_text[];
    private Timer m_timer;
    private int m_count_down;
    private MediaPlayer m_media_player;
    private Point m_size;

    /* Need to store */
    private boolean m_show_histogram;
    private int m_num_players;
    private Logic m_logic;

    public static final int DEFAULT_NUMBER_OF_PLAYERS;

    static {
        DEFAULT_NUMBER_OF_PLAYERS = 4;
    }

    private void StoreState() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.m_show_histogram), m_show_histogram);
        editor.putInt(getString(R.string.m_num_players), m_num_players);
        m_logic.StoreState(this, editor);
        editor.commit();
    }

    private void RestoreState() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        m_show_histogram = sharedPref.getBoolean(getString(R.string.m_show_histogram), true);
        m_num_players = sharedPref.getInt(getString(R.string.m_num_players), DEFAULT_NUMBER_OF_PLAYERS);
        m_logic.RestoreState(this, sharedPref);
    }

    @Override
    protected void onDestroy () {
        StoreState();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_num_players = DEFAULT_NUMBER_OF_PLAYERS;

        m_logic = new Logic(m_num_players);

        RestoreState();

        final CheckBox checkBox = (CheckBox) findViewById(R.id.histogram_visibility_checkbox);
        checkBox.setChecked(m_show_histogram);

        m_histogram_images = new ImageView[Card.MAX_NUMBER_ON_DICE * 2 - 1];
        m_histogram_text = new TextView[Card.MAX_NUMBER_ON_DICE * 2 - 1];

        m_size = GetWindowSize();

        int dice_width = m_size.x / 2;
        int dice_height = dice_width;

        ImageView red_dice_result_image = (ImageView) findViewById(R.id.red_dice_result);
        ImageView yellow_dice_result_image = (ImageView) findViewById(R.id.yellow_dice_result);

        red_dice_result_image.getLayoutParams().width = dice_width;
        red_dice_result_image.getLayoutParams().height = dice_height;

        yellow_dice_result_image.getLayoutParams().width = dice_width;
        yellow_dice_result_image.getLayoutParams().height = dice_height;

        LinearLayout histogram_images_layout = (LinearLayout) findViewById(R.id.histogram_images_layout);
        LinearLayout histogram_text_layout = (LinearLayout) findViewById(R.id.histogram_text_layout);

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

        ShowHistogram();
    }

    private void ShowHistogram() {
        if (m_show_histogram) {
            findViewById(R.id.histogram_layout).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.histogram_layout).setVisibility(View.INVISIBLE);
        }

        int[] histogram = m_logic.GetSumHistogram();

        int width = m_size.x / m_histogram_images.length;

        for (int i = 0; i < m_histogram_images.length; i++) {
            int height = histogram[i] * 10 + 1;
            m_histogram_images[i].setLayoutParams(new LinearLayout.LayoutParams(width, height));
        }
    }

    private Point GetWindowSize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
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
        m_count_down = 10;
        m_timer = new Timer();

        m_media_player = MediaPlayer.create(getApplicationContext(), R.raw.dices);
        m_media_player.start();

        SetMainButtonsEnable(false);

        m_timer.schedule(new TimerTask() {
            @Override
            public void run() {
                TimerMethod();
            }

        }, 0, 100);
    }

    private void TimerMethod() {
        //This method is called directly by the timer
        //and runs in the same thread as the timer.

        //We call the method that will work with the UI
        //through the runOnUiThread method.
        this.runOnUiThread(m_timer_tick);
    }

    private Runnable m_timer_tick = new Runnable() {
        public void run() {
            //This method runs in the same thread as the UI.
            m_count_down--;

            if (m_count_down == 0) {
                m_timer.cancel();
                Card card;
                card = m_logic.GetNewCard();
                SetDicesImagesRolled(card.m_red, card.m_yellow);
                ShowHistogram();
                ShowMessage(card.m_message);
                SetMainButtonsEnable(true);
            } else {
                Random rand = new Random();
                SetDicesImagesRolled(
                        (rand.nextInt(Card.MAX_NUMBER_ON_DICE) + 1),
                        (rand.nextInt(Card.MAX_NUMBER_ON_DICE) + 1));
            }
        }
    };



    public void onSettingClick(View view) {
        int layout_to_hide[] =
                {R.id.layout_for_dices, R.id.histogram_layout};

        for (int i = 0; i < layout_to_hide.length; i++) {
            findViewById(layout_to_hide[i]).setVisibility(View.INVISIBLE);
        }

        findViewById(R.id.setting_layout).setVisibility(View.VISIBLE);
    }

    public void onBackFromSettingClick(View view) {
        findViewById(R.id.setting_layout).setVisibility(View.INVISIBLE);
        findViewById(R.id.layout_for_dices).setVisibility(View.VISIBLE);

        final CheckBox checkBox = (CheckBox) findViewById(R.id.histogram_visibility_checkbox);
        m_show_histogram = checkBox.isChecked();

        ShowHistogram();
    }

    public void SetNumPlayers() {
        RadioGroup radioButtonGroup = (RadioGroup) findViewById(R.id.num_players_radio_group);

        int radioButtonID = radioButtonGroup.getCheckedRadioButtonId();
        View radioButton = radioButtonGroup.findViewById(radioButtonID);
        int idx = radioButtonGroup.indexOfChild(radioButton);

        m_num_players = idx + 3;
    }

    public void onNewGameClick(View view) {
        findViewById(R.id.num_players_layout_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.layout_for_dices).setVisibility(View.INVISIBLE);
        findViewById(R.id.histogram_layout).setVisibility(View.INVISIBLE);
    }

    public void onSelectNumPlayersClick(View view) {
        findViewById(R.id.num_players_layout_layout).setVisibility(View.INVISIBLE);
        findViewById(R.id.layout_for_dices).setVisibility(View.VISIBLE);

        SetNumPlayers();
        m_logic.Init(m_num_players);
        ShowHistogram();

        ShowMessage(Card.MessageWithCard.NEW_GAME);
    }

    public void onBackFromNumPlayersClick(View view) {
        findViewById(R.id.num_players_layout_layout).setVisibility(View.INVISIBLE);
        findViewById(R.id.layout_for_dices).setVisibility(View.VISIBLE);

        ShowHistogram();
    }

    public void ShowMessage(Card.MessageWithCard messageType) {
        TextView message_text_view = (TextView) findViewById(R.id.message_text_view);

        switch (messageType) {
            case NO_MESSAGE:
                message_text_view.setText("");
                break;
            case SEVEN_WITH_ROBBER:
                message_text_view.setText(R.string.seven_with_robber_string);
                break;
            case SEVEN_WITHOUT_ROBBER:
                message_text_view.setText(R.string.seven_without_robber_string);
                break;
            case NEW_GAME:
                message_text_view.setText(R.string.new_game_roll_string);
                break;
        }
    }

    private void SetMainButtonsEnable(boolean isEnable) {
        findViewById(R.id.roll_button).setEnabled(isEnable);
        findViewById(R.id.setting_button).setEnabled(isEnable);
        findViewById(R.id.new_game_button).setEnabled(isEnable);
    }
}
