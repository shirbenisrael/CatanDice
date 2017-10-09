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
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    private TextView m_histogram_counters[]; /* histograms values */
    private ImageView m_histogram_images[]; /* histogram bars. */
    private TextView m_histogram_text[]; /* static numbers under the histogram , 2,... 12 */
    private ImageView m_pirate_positions_images[];
    private Timer m_timer; /* time for rolling animation */
    private int m_count_down;
    private MediaPlayer m_media_player;
    private Point m_size;
    private boolean m_is_alchemist_active = false;

    /* Need to store */
    private Logic.GameType m_game_type;
    private int m_num_players;
    private Logic m_logic;
    private int m_red_dice;
    private int m_yellow_dice;
    private Card.EventDice m_event_dice;
    private int m_pirate_position;

    private enum ShownState {
        GAME,
        SELECT_GAME_TYPE,
        SELECT_NUM_PLAYERS,
        SETTING,
        HISTOGRAM
    }

    private ShownState m_shown_state;

    public static final int DEFAULT_NUMBER_OF_PLAYERS;
    public static final int DEFAULT_NUMBER_ON_DICE;
    public static final Logic.GameType DEFAULT_GAME_TYPE;

    static {
        DEFAULT_NUMBER_OF_PLAYERS = 4;
        DEFAULT_NUMBER_ON_DICE = 1;
        DEFAULT_GAME_TYPE = Logic.GameType.GAME_TYPE_REGULAR;
    }

    private void ShowState(ShownState new_state) {
        m_shown_state = new_state;

        int all_layout[] = {
                R.id.layout_for_dices,
                R.id.game_type_layout,
                R.id.num_players_layout,
                R.id.setting_layout,
                R.id.histogram_background_layout};

        int layouts_for_game[] = {R.id.layout_for_dices};
        int layouts_for_game_type[] = {R.id.game_type_layout};
        int layouts_for_num_players[] = {R.id.num_players_layout};
        int layouts_for_settings[] = {R.id.setting_layout};
        int layouts_for_histogram[] = {R.id.layout_for_dices, R.id.histogram_background_layout};

        int layouts_to_show[] = {0};

        switch (m_shown_state) {
            case GAME:
                layouts_to_show = layouts_for_game;
                break;
            case SELECT_GAME_TYPE:
                layouts_to_show = layouts_for_game_type;
                break;
            case SELECT_NUM_PLAYERS:
                layouts_to_show = layouts_for_num_players;
                break;
            case SETTING:
                layouts_to_show = layouts_for_settings;
                break;
            case HISTOGRAM:
                layouts_to_show = layouts_for_histogram;
                break;
            default:
                break;
        }

        for (int i = 0; i < all_layout.length; i++) {
            View layout = findViewById(all_layout[i]);
            layout.setVisibility(View.INVISIBLE);

            for (int j = 0; j < layouts_to_show.length; j++) {
                if (all_layout[i] == layouts_to_show[j]) {
                    layout.setVisibility(View.VISIBLE);
                    break;
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        switch (m_shown_state) {
            case GAME:
                super.onBackPressed();
                break;

            case SETTING:
                onBackFromSettingClick(null);
                break;

            case SELECT_GAME_TYPE:
            case SELECT_NUM_PLAYERS:
                onBackFromNumPlayersClick(null);
                break;

            case HISTOGRAM:
                onBackFromHistogramClick(null);
                break;
        }
    }

    private void StoreState() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.m_num_players), m_num_players);
        editor.putInt(getString(R.string.m_red_dice), m_red_dice);
        editor.putInt(getString(R.string.m_yellow_dice), m_yellow_dice);
        editor.putInt(getString(R.string.m_event_dice), m_event_dice.getValue());
        editor.putInt(getString(R.string.m_game_type), m_game_type.getValue());
        editor.putInt(getString(R.string.m_pirate_position), m_pirate_position);

        m_logic.StoreState(this, editor);
        editor.commit();
    }

    private void RestoreState() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        m_num_players = sharedPref.getInt(getString(R.string.m_num_players), DEFAULT_NUMBER_OF_PLAYERS);
        m_red_dice = sharedPref.getInt(getString(R.string.m_red_dice), DEFAULT_NUMBER_ON_DICE);
        m_yellow_dice = sharedPref.getInt(getString(R.string.m_yellow_dice), DEFAULT_NUMBER_ON_DICE);
        m_pirate_position = sharedPref.getInt(getString(R.string.m_pirate_position), Logic.DEFAULT_PIRATE_POSITION);

        int event_num = sharedPref.getInt(getString(R.string.m_event_dice), DEFAULT_NUMBER_ON_DICE);
        m_event_dice = Card.EventDice.values()[event_num];

        int game_type_num = sharedPref.getInt(getString(R.string.m_game_type), 0);
        m_game_type = Logic.GameType.values()[game_type_num];

        m_logic.RestoreState(this, sharedPref);
    }

    @Override
    protected void onDestroy() {
        StoreState();
        super.onDestroy();
    }

    private void set_square_size_view(View view, int size) {
        view.getLayoutParams().width = size;
        view.getLayoutParams().height = size;
    }

    private void set_square_size(int view_id, int size) {
        View view = findViewById(view_id);
        set_square_size_view(view, size);
    }

    private void arrange_buttons() {
        int Ids[] =
                {R.id.new_game_button, R.id.show_histogram_button, R.id.setting_button, R.id.alchemist_button};

        int width = m_size.x / Ids.length;
        for (int i = 0 ; i < Ids.length; i++) {
            set_square_size(Ids[i], width);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_num_players = DEFAULT_NUMBER_OF_PLAYERS;
        m_game_type = DEFAULT_GAME_TYPE;

        m_logic = new Logic(m_num_players, m_game_type);

        RestoreState();

        int num_bars = Card.MAX_NUMBER_ON_DICE * 2 - 1;

        m_histogram_counters = new TextView[num_bars];
        m_histogram_images = new ImageView[num_bars];
        m_histogram_text = new TextView[num_bars];
        m_pirate_positions_images = new ImageView[Card.MAX_PIRATE_POSITIONS];

        m_size = GetWindowSize();

        int dice_width = m_size.x / 2;
        int dice_height = dice_width;

        set_square_size(R.id.red_dice_result, dice_width);
        set_square_size(R.id.yellow_dice_result, dice_width);
        set_square_size(R.id.event_dice_result, dice_width);

        SetDicesImagesRolled(m_red_dice, m_yellow_dice);
        SetEventDiceImage(m_event_dice);
        SetEventDiceVisibility();

        LinearLayout histogram_images_layout = (LinearLayout) findViewById(R.id.histogram_images_layout);
        LinearLayout histogram_text_layout = (LinearLayout) findViewById(R.id.histogram_text_layout);

        LinearLayout main_histogram_layout = (LinearLayout) findViewById(R.id.histogram_layout);
        LinearLayout background_histogram_layout = (LinearLayout) findViewById(R.id.histogram_background_layout);
        LinearLayout layout_for_pirate_ship = (LinearLayout) findViewById(R.id.layout_for_pirate_ship);


        int histogram_window_width = m_size.x * 9 / 10;
        int histogram_window_height = m_size.y * 4 / 5;

        main_histogram_layout.getLayoutParams().width = histogram_window_width;
        main_histogram_layout.getLayoutParams().height = histogram_window_height;
        main_histogram_layout.setBackgroundColor(0xff101010);

        for (int i = 0; i < Card.MAX_PIRATE_POSITIONS; i++) {
            m_pirate_positions_images[i] = new ImageView(this);
            m_pirate_positions_images[i].setImageResource(R.drawable.pirate_ship);

            int position_color;

            switch (i) {
                case 0:
                    position_color = Color.GREEN;
                    break;
                case Card.MAX_PIRATE_POSITIONS - 1:
                    position_color = Color.RED;
                    break;
                default:
                    position_color = (i % 2 == 0) ? Color.LTGRAY : Color.GRAY;
                    break;
            }

            layout_for_pirate_ship.addView(m_pirate_positions_images[i]);

            set_square_size_view(m_pirate_positions_images[i], m_size.x / Card.MAX_PIRATE_POSITIONS);
            m_pirate_positions_images[i].setBackgroundColor(position_color);
            m_pirate_positions_images[i].setImageAlpha(0);
        }

        SetPiratePosition();

        for (int i = 0; i < m_histogram_images.length; i++) {
            LinearLayout layout_for_bar_and_counter = new LinearLayout(getApplicationContext());
            layout_for_bar_and_counter.setOrientation(LinearLayout.VERTICAL);
            layout_for_bar_and_counter.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

            m_histogram_counters[i] = new TextView(this);
            m_histogram_images[i] = new ImageView(this);
            m_histogram_text[i] = new TextView(this);

            int color = (i % 2) * 100 + 100;
            int bar_color = Color.rgb(0, color, 0);

            m_histogram_images[i].setBackgroundColor(bar_color);

            m_histogram_text[i].setTextColor(bar_color);
            m_histogram_text[i].setGravity(Gravity.CENTER);

            m_histogram_counters[i].setTextColor(bar_color);
            m_histogram_counters[i].setGravity(Gravity.CENTER);

            histogram_images_layout.addView(layout_for_bar_and_counter);
            layout_for_bar_and_counter.addView(m_histogram_counters[i]);
            layout_for_bar_and_counter.addView(m_histogram_images[i]);
            layout_for_bar_and_counter.setGravity(Gravity.CENTER_HORIZONTAL);

            m_histogram_images[i].getLayoutParams().width = histogram_window_width / (m_histogram_images.length * 3);
            m_histogram_images[i].getLayoutParams().height = 20;

            histogram_text_layout.addView(m_histogram_text[i]);

            m_histogram_text[i].setText(String.valueOf(i + 2));
            m_histogram_text[i].getLayoutParams().width = histogram_window_width / m_histogram_images.length;

            m_histogram_counters[i].getLayoutParams().width = histogram_window_width / m_histogram_images.length;
        }

        arrange_buttons();

        ShowState(ShownState.GAME);
    }

    private void ShowHistogram() {

        SetMainButtonsEnable(false);

        int[] histogram = m_logic.GetSumHistogram();

        LinearLayout main_histogram_layout = (LinearLayout) findViewById(R.id.histogram_layout);
        LinearLayout histogram_text_layout = (LinearLayout) findViewById(R.id.histogram_text_layout);
        Button back_from_histogram_button = (Button) findViewById(R.id.back_from_histogram_button);

        int max_bar_height = main_histogram_layout.getLayoutParams().height;
        max_bar_height = (max_bar_height * 7) / 10;

        int max_histogram_value = 1;

        for (int i = 0; i < m_histogram_images.length; i++) {
            max_histogram_value = Math.max(max_histogram_value, histogram[i]);
        }

        for (int i = 0; i < m_histogram_images.length; i++) {
            int height = histogram[i] * max_bar_height / max_histogram_value;
            m_histogram_images[i].getLayoutParams().height = height;
            m_histogram_counters[i].setText(String.valueOf(histogram[i]));
            m_histogram_images[i].requestLayout();
        }

        findViewById(R.id.histogram_background_layout).setVisibility(View.VISIBLE);
    }

    private Point GetWindowSize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public void SetEventDiceVisibility() {
        View event_dice_result = findViewById(R.id.event_dice_result);
        View layout_for_pirate_ship = findViewById(R.id.layout_for_pirate_ship);
        View alchemist_button = findViewById(R.id.alchemist_button);

        if (m_game_type == Logic.GameType.GAME_TYPE_CITIES_AND_KNIGHT) {
            event_dice_result.setVisibility(View.VISIBLE);
            layout_for_pirate_ship.setVisibility(View.VISIBLE);
            alchemist_button.setVisibility(View.VISIBLE);
        } else {
            event_dice_result.setVisibility(View.INVISIBLE);
            layout_for_pirate_ship.setVisibility(View.INVISIBLE);
            alchemist_button.setVisibility(View.INVISIBLE);
        }
    }

    public void SetPiratePosition() {
        for (int i = m_pirate_position+1; i < Card.MAX_PIRATE_POSITIONS; i++) {
            m_pirate_positions_images[i].setImageAlpha(0);
        }

        for (int i = 0 ; i <= m_pirate_position; i++) {
            int alpha = 150 >> (m_pirate_position-i);
            m_pirate_positions_images[i].setImageAlpha(alpha );
        }

    }

    public void SetEventDiceImage( Card.EventDice eventDice) {
        ImageView event_dice_result = (ImageView) findViewById(R.id.event_dice_result);

        switch (eventDice) {
            case YELLOW_CITY:
                event_dice_result.setImageResource(R.drawable.yellow_city);
                break;
            case GREEN_CITY:_CITY:
                event_dice_result.setImageResource(R.drawable.green_city);
                break;
            case BLUE_CITY:
                event_dice_result.setImageResource(R.drawable.blue_city);
                break;
            case PIRATE_SHIP:
                event_dice_result.setImageResource(R.drawable.pirate_ship);
                break;
        }
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
                card = m_logic.GetNewCard(m_is_alchemist_active);

                m_red_dice = card.m_red;
                m_yellow_dice = card.m_yellow;
                m_event_dice = card.m_event_dice;

                if (!m_is_alchemist_active) {
                    SetDicesImagesRolled(card.m_red, card.m_yellow);
                }
                SetEventDiceImage(card.m_event_dice);

                m_is_alchemist_active = false;

                ShowMessage(card.m_message, card.m_turn_number);

                m_pirate_position = card.m_pirate_position;
                SetPiratePosition();

                m_media_player.release();
                SetMainButtonsEnable(true);
            } else {
                Random rand = new Random();
                if (!m_is_alchemist_active) {
                    SetDicesImagesRolled(
                            (rand.nextInt(Card.MAX_NUMBER_ON_DICE) + 1),
                            (rand.nextInt(Card.MAX_NUMBER_ON_DICE) + 1));
                }

                int event_num = rand.nextInt(Card.MAX_EVENTS_ON_EVENT_DICE);
                Card.EventDice event = Card.EventDice.values()[event_num];
                SetEventDiceImage(event);
            }
        }
    };

    public void onSettingClick(View view) {
        ShowState(ShownState.SETTING);
    }

    public void onShowHistogramClick(View view) {
        SetMainButtonsEnable(false);
        ShowHistogram();
        ShowState(ShownState.HISTOGRAM);
    }

    public void onBackFromSettingClick(View view) {
        ShowState(ShownState.GAME);
    }

    public void onBackFromHistogramClick(View view) {
        ShowState(ShownState.GAME);
        SetMainButtonsEnable(true);
    }

    public void onBackFromNumPlayersClick(View view) {
        ShowState(ShownState.GAME);
    }

    public void SetGameType(View view) {
        switch(view.getId())
        {
            case R.id.button_regular_game:
                m_game_type = Logic.GameType.GAME_TYPE_REGULAR;
                break;

            case R.id.button_cities_and_knights:
                m_game_type = Logic.GameType.GAME_TYPE_CITIES_AND_KNIGHT;
                break;

            default:
                throw new RuntimeException("Unknow button ID");
        }
    }

    public void onNewGameClick(View view) {
        ShowState(ShownState.SELECT_GAME_TYPE);
    }

    public void onAlchemistClick(View view) {
        m_is_alchemist_active = true;
        onRollClick(view);
    }

    public void onSelectGameTypeClick(View view) {
        SetGameType(view);
        ShowState(ShownState.SELECT_NUM_PLAYERS);
    }

    public void onSelectNumPlayersClick(View view) {
        switch(view.getId())
        {
            case R.id.button_3_players:
                m_num_players = 3;
                break;

            case R.id.button_4_players:
                m_num_players = 4;
                break;

            case R.id.button_5_players:
                m_num_players = 5;
                break;

            case R.id.button_6_players:
                m_num_players = 6;
                break;

            default:
                throw new RuntimeException("Unknow button ID");
        }

        m_logic.Init(m_num_players, m_game_type);

        m_pirate_position = 0;

        SetEventDiceVisibility();
        SetPiratePosition();
        ShowMessage(Card.MessageWithCard.NEW_GAME, 0);
        ShowState(ShownState.GAME);
    }

    public void ShowMessage(Card.MessageWithCard messageType, int turn_number) {
        TextView message_text_view = (TextView) findViewById(R.id.message_text_view);

        String turn_number_message = getString(R.string.turn_number) + ": " + Integer.toString(turn_number) + "   ";
        String message_type = "";

        switch (messageType) {
            case SEVEN_WITH_ROBBER:
                message_type = getString(R.string.seven_with_robber_string);
                break;
            case SEVEN_WITHOUT_ROBBER:
                message_type = getString(R.string.seven_without_robber_string);
                break;
            case NEW_GAME:
                message_type = getString(R.string.new_game_roll_string);
                break;
            case NO_MESSAGE:
            default:
                break;
        }
        message_text_view.setText(turn_number_message + message_type);
    }

    private void SetMainButtonsEnable(boolean isEnable) {
        findViewById(R.id.roll_button).setEnabled(isEnable);
        findViewById(R.id.setting_button).setEnabled(isEnable);
        findViewById(R.id.new_game_button).setEnabled(isEnable);
        findViewById(R.id.show_histogram_button).setEnabled(isEnable);
        findViewById(R.id.alchemist_button).setEnabled(isEnable);
    }
}
