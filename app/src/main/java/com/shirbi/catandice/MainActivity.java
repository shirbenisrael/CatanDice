package com.shirbi.catandice;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static android.os.SystemClock.elapsedRealtime;
import static com.shirbi.catandice.BluetoothChatService.TOAST;
import static com.shirbi.catandice.Card.MAX_NUMBER_ON_DICE;
import static com.shirbi.catandice.FrontEndHandler.HistogramType.COMBINATION;
import static com.shirbi.catandice.FrontEndHandler.HistogramType.SUM;
import static com.shirbi.catandice.FrontEndHandler.HistogramType.UNIQUE;

public class MainActivity extends Activity {
    FrontEndHandler m_frontend_handler;
    private TextView[][] m_histogram_combination_counters;
    private Histogram m_sum_histogram;
    private Histogram m_one_dice_histogram;
    private Timer m_timer; /* time for rolling animation */
    private int m_count_down;
    private boolean m_is_alchemist_active = false;
    private ShakeDetector m_shakeDetector;
    private boolean m_roll_red, m_roll_yellow;
    private BluetoothAdapter mBluetoothAdapter = null;
    private com.shirbi.catandice.BluetoothChatService mChatService = null;
    private final IncomingHandler mHandler = new IncomingHandler(this);
    private Boolean mTwoPlayerGame = false;
    private int m_starting_player;
    private Card m_last_card;
    private Card m_previous_card;
    private CountDownTimer m_count_down_timer;
    private final int[] m_count_down_timer_seconds_values = {15, 30, 45, 60, 90, 120, 180};
    private final CharSequence[] m_count_down_timer_seconds_strings = { "0:15", "0:30", "0:45", "1:00", "1:30", "2:00", "3:00" };

    /* Need to store */
    private Logic.GameType m_game_type;
    private int m_num_players;
    private Logic m_logic;
    private int m_red_dice;
    private int m_yellow_dice;
    private Card.EventDice m_event_dice;
    private int m_pirate_position;
    private boolean m_is_sound_enable;
    private boolean m_is_fair_dice = true;
    private boolean m_is_shake_enable;
    private boolean m_is_prevent_accidental_roll;
    private long m_last_roll_time_ms = 0;
    private int m_count_down_timer_selection;
    private boolean m_is_timer_enable;

    enum ShownState {
        GAME,
        SELECT_GAME_TYPE,
        SELECT_NUM_PLAYERS,
        SETTING,
        HISTOGRAM,
    }

    private ShownState m_shown_state;

    private static final long MILLISECONDS_BETWEEN_ROLLS = 3000;
    public static final int DEFAULT_NUMBER_OF_PLAYERS;
    private static final int DEFAULT_TIMER_SELECTION = 5;
    private static final int DEFAULT_NUMBER_ON_DICE;
    private static final Logic.GameType DEFAULT_GAME_TYPE;
    private static final int NUM_SHAKES_TO_ROLL_DICE;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_DISCOVERABLE = 3;

    static {
        DEFAULT_NUMBER_OF_PLAYERS = 4;
        DEFAULT_NUMBER_ON_DICE = 1;
        DEFAULT_GAME_TYPE = Logic.GameType.GAME_TYPE_REGULAR;
        NUM_SHAKES_TO_ROLL_DICE = 2;
    }

    private void ShowState(ShownState new_state) {
        m_shown_state = new_state;
        m_frontend_handler.ShowState(m_shown_state);
    }

    void Exit() {
        super.onBackPressed();
    }

    @Override
    public void onBackPressed() {
        switch (m_shown_state) {
            case GAME:
                m_frontend_handler.showExitDialog();
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
        editor.putInt(getString(R.string.m_count_down_timer_selection), m_count_down_timer_selection);
        editor.putBoolean(getString(R.string.m_is_timer_enable), m_is_timer_enable);
        editor.putBoolean(getString(R.string.m_is_sound_enable), m_is_sound_enable);
        editor.putBoolean(getString(R.string.m_is_fair_dice), m_is_fair_dice);
        editor.putBoolean(getString(R.string.m_is_shake_enable), m_is_shake_enable);
        editor.putBoolean(getString(R.string.m_is_prevent_accidental_roll), m_is_prevent_accidental_roll);

        m_logic.StoreState(this, editor);
        editor.apply();
    }

    private void RestoreState() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        m_num_players = sharedPref.getInt(getString(R.string.m_num_players), DEFAULT_NUMBER_OF_PLAYERS);
        m_red_dice = sharedPref.getInt(getString(R.string.m_red_dice), DEFAULT_NUMBER_ON_DICE);
        m_yellow_dice = sharedPref.getInt(getString(R.string.m_yellow_dice), DEFAULT_NUMBER_ON_DICE);
        m_pirate_position = sharedPref.getInt(getString(R.string.m_pirate_position), Logic.DEFAULT_PIRATE_POSITION);
        m_count_down_timer_selection = sharedPref.getInt(getString(R.string.m_count_down_timer_selection), DEFAULT_TIMER_SELECTION);
        m_is_timer_enable = sharedPref.getBoolean(getString(R.string.m_is_timer_enable), false);

        int event_num = sharedPref.getInt(getString(R.string.m_event_dice), DEFAULT_NUMBER_ON_DICE);
        m_event_dice = Card.EventDice.values()[event_num];

        int game_type_num = sharedPref.getInt(getString(R.string.m_game_type), 0);
        m_game_type = Logic.GameType.values()[game_type_num];

        m_is_sound_enable = sharedPref.getBoolean(getString(R.string.m_is_sound_enable), true);
        m_is_fair_dice = sharedPref.getBoolean(getString(R.string.m_is_fair_dice), true);
        m_is_shake_enable = sharedPref.getBoolean(getString(R.string.m_is_shake_enable), true);
        m_is_prevent_accidental_roll = sharedPref.getBoolean(getString(R.string.m_is_prevent_accidental_roll), false);

        m_logic.RestoreState(this, sharedPref);

        m_logic.SetEnableFairDice(m_is_fair_dice);
    }

    @Override
    protected void onDestroy() {
        StoreState();
        super.onDestroy();
    }

    private final ShakeDetector.OnShakeListener m_shakeListener = new ShakeDetector.OnShakeListener() {
        public void onShake(int count) {
            if (findViewById(R.id.roll_button).isEnabled() && (count >= NUM_SHAKES_TO_ROLL_DICE) && m_is_shake_enable) {
                onRollClick(null);
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();

        SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.unregisterListener(m_shakeDetector);

        stopCountDownTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(m_shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_frontend_handler = new FrontEndHandler(this);

        m_num_players = DEFAULT_NUMBER_OF_PLAYERS;
        m_game_type = DEFAULT_GAME_TYPE;

        m_logic = new Logic(m_num_players, m_game_type, m_is_fair_dice);
        m_shakeDetector = new ShakeDetector();
        m_shakeDetector.setOnShakeListener(m_shakeListener);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        RestoreState();

        int num_bars = MAX_NUMBER_ON_DICE * 2 - 1;

        m_histogram_combination_counters = new TextView[MAX_NUMBER_ON_DICE][MAX_NUMBER_ON_DICE];

        m_frontend_handler.SetDicesImagesRolled(m_red_dice, m_yellow_dice);
        m_frontend_handler.SetEventDiceImage(m_event_dice);
        SetEventDiceVisibility();
        SetOneDiceOperationVisibility();
        SetTwoPlayerGame(false);
        
        SetPiratePosition();

        m_sum_histogram = new Histogram(this,
                m_frontend_handler.GetHistogramWindowWidth(),
                m_frontend_handler.GetHistogramWindowHeight(),
                (LinearLayout)findViewById(R.id.sum_histogram_images_layout),
                (LinearLayout)findViewById(R.id.sum_histogram_text_layout),
                2, 12);

        m_one_dice_histogram = new Histogram(this,
                m_frontend_handler.GetHistogramWindowWidth(),
                m_frontend_handler.GetHistogramWindowHeight(),
                (LinearLayout)findViewById(R.id.one_dice_histogram_images_layout),
                (LinearLayout)findViewById(R.id.one_dice_histogram_text_layout),
                1, 6);

        int cell_count = MAX_NUMBER_ON_DICE + 1;
        TableLayout histogram_table_layout = findViewById(R.id.combination_table);
        TableRow newRow = new TableRow(this);
        TextView textView = new TextView(this);
        newRow.addView(textView);
        int cell_size = m_frontend_handler.GetHistogramWindowWidth() / cell_count;
        textView.getLayoutParams().width = cell_size;
        textView.getLayoutParams().height = cell_size;
        textView.setGravity(Gravity.CENTER);

        for (int i = 0; i < MAX_NUMBER_ON_DICE; i++) {
            textView = new TextView(this);
            newRow.addView(textView);
            textView.setText(String.valueOf(i + 1));
            textView.setTextColor(Color.RED);
            textView.getLayoutParams().width = cell_size;
            textView.getLayoutParams().height = cell_size;
            textView.setGravity(Gravity.CENTER);
        }
        histogram_table_layout.addView(newRow);

        for (int i = 0; i < MAX_NUMBER_ON_DICE; i++) {
            newRow = new TableRow(this);
            textView = new TextView(this);
            newRow.addView(textView);
            textView.setText(String.valueOf(i + 1));
            textView.setTextColor(Color.YELLOW);
            textView.getLayoutParams().width = cell_size;
            textView.getLayoutParams().height = cell_size;
            textView.setGravity(Gravity.CENTER);

            for (int j = 0; j < MAX_NUMBER_ON_DICE; j++) {
                textView = new TextView(this);
                newRow.addView(textView);
                m_histogram_combination_counters[j][i] = textView;
                textView.setTextColor(Color.GREEN);
                textView.getLayoutParams().width = m_frontend_handler.GetHistogramWindowWidth() / cell_count;
                // Not a bug - a square size.
                textView.getLayoutParams().height = m_frontend_handler.GetHistogramWindowWidth() / cell_count;
                textView.setGravity(Gravity.CENTER);
                textView.setTypeface(null, Typeface.BOLD);
                textView.setTextSize(20);
            }
            histogram_table_layout.addView(newRow);
        }

        histogram_table_layout.setPadding(0,0,0, cell_size * 3 / 2);

        enableCheckBox(R.id.enable_sound_checkbox, m_is_sound_enable);
        enableCheckBox(R.id.enable_fair_dice_checkbox, m_is_fair_dice);
        enableCheckBox(R.id.enable_shake_checkbox, m_is_shake_enable);
        enableCheckBox(R.id.prevent_accidental_roll_checkbox, m_is_prevent_accidental_roll);
        enableCheckBox(R.id.enable_turn_timer_checkbox, m_is_timer_enable);

        SetBackGround();

        ShowState(ShownState.GAME);

        ShowTurnNumber(m_logic.GetTurnNumber());

        SetMainButtonsEnable(true);

        selectCountDownTimerValue(m_count_down_timer_selection);
    }

    private void enableCheckBox(int id, boolean is_enable) {
        ((CheckBox)findViewById(id)).setChecked(is_enable);
    }

    private void ShowHistogram() {

        SetMainButtonsEnable(false);

        int[] sum_histogram = m_logic.GetSumHistogram();
        m_sum_histogram.ShowHistogram(sum_histogram);

        int[] one_dice_histogram = m_logic.GetOneDiceHistogram();
        m_one_dice_histogram.ShowHistogram(one_dice_histogram);

        int[][]combination_histogram = m_logic.GetCombinationHistogram();
        int max_appeared_combination = 0;

        for (int i = 0; i < MAX_NUMBER_ON_DICE; i++) {
            for (int j = 0; j < MAX_NUMBER_ON_DICE; j++) {
                max_appeared_combination = Math.max(max_appeared_combination, combination_histogram[i][j]);
            }
        }

        if (max_appeared_combination == 0) {
            max_appeared_combination = 1;
        }

        for (int i = 0; i < MAX_NUMBER_ON_DICE; i++) {
            for (int j = 0; j < MAX_NUMBER_ON_DICE; j++) {
                TextView textView = m_histogram_combination_counters[i][j];
                int value = combination_histogram[i][j];
                int background_color = (255 * value) / max_appeared_combination;

                textView.setText(String.valueOf(value));
                textView.setBackgroundColor(0xFF000000 + background_color +
                        (background_color << 8) + (background_color << 16));
            }
        }

        findViewById(R.id.histogram_background_layout).setVisibility(View.VISIBLE);
    }

    private void SetEventDiceVisibility() {
        View event_dice_result = findViewById(R.id.event_dice_result);
        View layout_for_pirate_ship = findViewById(R.id.layout_for_pirate_ship);

        if (m_game_type == Logic.GameType.GAME_TYPE_CITIES_AND_KNIGHT) {
            event_dice_result.setVisibility(View.VISIBLE);
            layout_for_pirate_ship.setVisibility(View.VISIBLE);
        } else {
            event_dice_result.setVisibility(View.INVISIBLE);
            layout_for_pirate_ship.setVisibility(View.INVISIBLE);
        }
    }

    private void SetOneDiceOperationVisibility() {
        View layout = findViewById(R.id.layout_for_one_dice_operation);
        if (m_game_type == Logic.GameType.GAME_TYPE_SIMPLE_DICE) {
            layout.setVisibility(View.VISIBLE);
        } else {
            layout.setVisibility(View.INVISIBLE);
        }
    }

    private void SetPiratePosition() {
        m_frontend_handler.SetPiratePosition(m_pirate_position);
    }

    private void onRollRedClick() {
        m_roll_red = true;
        m_roll_yellow = false;
        m_previous_card = new Card(m_red_dice, m_yellow_dice, m_event_dice);
        m_logic.DisableCancelLastMove();
        Random rand = new Random();
        m_red_dice = (rand.nextInt(MAX_NUMBER_ON_DICE) + 1);
        if (mTwoPlayerGame) {
            BluetoothMessageHandler.SendRoleOneDice(MainActivity.this, true, m_red_dice);
        }
        rollDice();
    }

    private void onRollYellowClick() {
        m_roll_red = false;
        m_roll_yellow = true;
        m_previous_card = new Card(m_red_dice, m_yellow_dice, m_event_dice);
        m_logic.DisableCancelLastMove();
        Random rand = new Random();
        m_yellow_dice = (rand.nextInt(MAX_NUMBER_ON_DICE) + 1);
        if (mTwoPlayerGame) {
            BluetoothMessageHandler.SendRoleOneDice(MainActivity.this, false, m_yellow_dice);
        }
        rollDice();
    }

    // Called as a result of Bluetooth message from other device
    public void RollOneDice(int dice_value, boolean is_red) {
        m_roll_red = is_red;
        m_roll_yellow = !is_red;
        m_previous_card = new Card(m_red_dice, m_yellow_dice, m_event_dice);
        m_logic.DisableCancelLastMove();
        if (is_red) {
            m_red_dice = dice_value;
        } else {
            m_yellow_dice = dice_value;
        }
        rollDice();
    }

    private void fixOneDice(int title_id, final boolean is_red) {
        AlertDialog.Builder builder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(getString(title_id));

        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });

        CharSequence[] charSequence = new CharSequence[MAX_NUMBER_ON_DICE + 1];
        for (int i = 0; i < MAX_NUMBER_ON_DICE ; i++) {
            charSequence[i] =
                    getString(is_red ? R.string.fix_red: R.string.fix_yellow) + " - " +
                    (i + 1);
        }
        charSequence[MAX_NUMBER_ON_DICE] = getString(is_red ? R.string.roll_red: R.string.roll_yellow);

        builder.setItems(charSequence,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which < MAX_NUMBER_ON_DICE) {
                            int dice_value = which + 1;
                            FixDice(dice_value, is_red);

                            if (mTwoPlayerGame) {
                                BluetoothMessageHandler.SendFixDice(MainActivity.this, is_red, dice_value);
                            }
                        } else {
                            if (is_red) {
                                onRollRedClick();
                            } else {
                                onRollYellowClick();
                            }
                        }

                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void FixDice(int fixed_value, boolean is_red) {
        m_previous_card = new Card(m_red_dice, m_yellow_dice, m_event_dice);
        m_logic.DisableCancelLastMove();

        if (is_red) {
            m_red_dice = fixed_value;
        } else {
            m_yellow_dice = fixed_value;
        }
        m_frontend_handler.SetDicesImagesRolled(m_red_dice, m_yellow_dice);
    }

    public void onFixRedClick(View view) {
        fixOneDice(R.string.fix_red, true);
    }

    public void onFixYellowClick(View view) {
        fixOneDice(R.string.fix_yellow, false);
    }

    public void onRollClick(View view) {
        long new_time_stamp = elapsedRealtime();
        if (m_is_prevent_accidental_roll) {
            if (new_time_stamp - m_last_roll_time_ms <= MILLISECONDS_BETWEEN_ROLLS) {
                return;
            }
        }

        m_last_roll_time_ms = new_time_stamp;
        m_roll_red = true;
        m_roll_yellow = true;
        m_previous_card = new Card(m_red_dice, m_yellow_dice, m_event_dice);
        m_last_card = m_logic.GetNewCard(m_is_alchemist_active);
        if (!m_is_alchemist_active) {
            m_red_dice = m_last_card.m_red;
            m_yellow_dice = m_last_card.m_yellow;
        }
        m_event_dice = m_last_card.m_event_dice;

        if (mTwoPlayerGame) {
            BluetoothMessageHandler.SendRoleAllDice(this, m_last_card, m_is_alchemist_active);
        }
        rollDice();
    }

    // Called as a result of Bluetooth message from other device
    public void rollAllDice(Card card, boolean is_alchemist_active) {
        m_is_alchemist_active = is_alchemist_active;
        m_roll_red = true;
        m_roll_yellow = true;
        m_previous_card = new Card(m_red_dice, m_yellow_dice, m_event_dice);
        m_last_card = card;
        if (!m_is_alchemist_active) {
            m_red_dice = m_last_card.m_red;
            m_yellow_dice = m_last_card.m_yellow;
        }
        m_event_dice = m_last_card.m_event_dice;
        m_logic.SetCard(card);

        rollDice();
    }

    private void rollDice() {
        m_count_down = 10;
        m_timer = new Timer();

        int[] dices_sound_ids = {
                R.raw.dices1,
                R.raw.dices2,
                R.raw.dices3,
                R.raw.dices4,
                R.raw.dices5
        };

        if (m_is_sound_enable) {
            PlaySound(dices_sound_ids[new Random().nextInt(dices_sound_ids.length)]);
        }

        SetMainButtonsEnable(false);

        stopCountDownTimer();

        m_timer.schedule(new TimerTask() {
            @Override
            public void run() {
                TimerMethod();
            }

        }, 0, 100);
    }

    private void SetBackGround() {
        m_frontend_handler.SetBackGround(m_game_type);
    }

    // Set of all media players which are currently working. Used to prevent garbage collector from
    // clean them and stop the sounds.
    private final Set<MediaPlayer> m_media_players = new HashSet<MediaPlayer>();

    private void PlaySound(int sound_id) {
        if (!m_is_sound_enable) {
            return;
        }

        MediaPlayer media_player;
        media_player = MediaPlayer.create(this, sound_id);
        m_media_players.add(media_player);
        media_player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
                m_media_players.remove(mp);
            }
        });
        media_player.start();
    }

    private void TimerMethod() {
        //This method is called directly by the timer
        //and runs in the same thread as the timer.

        //We call the method that will work with the UI
        //through the runOnUiThread method.
        this.runOnUiThread(m_timer_tick);
    }

    private final Runnable m_timer_tick = new Runnable() {
        public void run() {
            //This method runs in the same thread as the UI.
            m_count_down--;

            if (m_count_down == 0) {
                m_timer.cancel();

                m_frontend_handler.SetDicesImagesRolled(m_red_dice, m_yellow_dice);

                if (m_roll_red && m_roll_yellow) {
                    m_frontend_handler.SetEventDiceImage(m_event_dice);
                    m_is_alchemist_active = false;
                    ShowMessage(m_last_card.m_message, m_last_card.m_turn_number);
                    m_pirate_position = m_last_card.m_pirate_position;
                    SetPiratePosition();
                } else {
                    m_logic.IncreaseTurnNumber();
                    ShowMessage(Card.MessageWithCard.NO_MESSAGE, m_logic.GetTurnNumber());
                }

                SetMainButtonsEnable(true);

                startCountDownTimer();
            } else {
                Random rand = new Random();
                if (!m_is_alchemist_active) {
                    int red_to_show = m_roll_red ? (rand.nextInt(MAX_NUMBER_ON_DICE) + 1) : m_red_dice;
                    int yellow_to_show = m_roll_yellow ? (rand.nextInt(MAX_NUMBER_ON_DICE) + 1) : m_yellow_dice;
                    m_frontend_handler.SetDicesImagesRolled(red_to_show, yellow_to_show);
                }

                int event_num = rand.nextInt(Card.MAX_EVENTS_ON_EVENT_DICE);
                Card.EventDice event = Card.EventDice.values()[event_num];
                m_frontend_handler.SetEventDiceImage(event);
            }
        }
    };

    private void onSettingClick(View view) {
        ShowState(ShownState.SETTING);
    }

    private void onShowHistogramClick(View view) {
        SetMainButtonsEnable(false);
        ShowHistogram();
        ShowState(ShownState.HISTOGRAM);
    }

    public void onSendFullStateButtonClick(View view) {
        if (mTwoPlayerGame) {
            ShowSendStateDialog();
        }
    }
    private void ShowSendStateDialog() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(getString(R.string.send_state_title));
        builder.setMessage(getString(R.string.send_state_message));
        builder.setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SendFullState();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });
        //builder.setIcon(R.drawable.new_game_icon); // TODO: Add this
        builder.show();
    }

    private void SendFullState() {
        BluetoothMessageHandler.SendFullState(this, m_logic.ToString(),
                m_red_dice, m_yellow_dice, m_event_dice);
    }

    public void SetFullState(int red_dice, int yellow_dice, Card.EventDice event_dice, int[] intArray, int startIndex) {
        int nextIndex = m_logic.UpdateFromIntArray(intArray, startIndex);
        m_game_type = m_logic.GetGameType();
        m_pirate_position = m_logic.GetPiratePosition();
        m_num_players = m_logic.GetNumPlayers();
        m_red_dice = red_dice;
        m_yellow_dice = yellow_dice;
        m_event_dice = event_dice;

        SetBackGround();
        SetEventDiceVisibility();
        SetOneDiceOperationVisibility();
        SetPiratePosition();
        m_frontend_handler.SetDicesImagesRolled(m_red_dice, m_yellow_dice);
        m_frontend_handler.SetEventDiceImage(m_event_dice);
        ShowTurnNumber(m_logic.GetTurnNumber());
        SetMainButtonsEnable(true);
    }

    // Called as a result of Bluetooth message from other device
    public void SetFairDice(boolean is_fair) {
        m_is_fair_dice = is_fair;

        CheckBox enable_fair_dice_check_box = findViewById(R.id.enable_fair_dice_checkbox);
        enable_fair_dice_check_box.setChecked(m_is_fair_dice);
        m_logic.SetEnableFairDice(m_is_fair_dice);
    }

    public void onBackFromSettingClick(View view) {
        CheckBox enable_sound_check_box = findViewById(R.id.enable_sound_checkbox);
        m_is_sound_enable = enable_sound_check_box.isChecked();

        CheckBox enable_fair_dice_check_box = findViewById(R.id.enable_fair_dice_checkbox);
        m_is_fair_dice = enable_fair_dice_check_box.isChecked();

        m_logic.SetEnableFairDice(m_is_fair_dice);

        CheckBox enable_shake_check_box = findViewById(R.id.enable_shake_checkbox);
        m_is_shake_enable = enable_shake_check_box.isChecked();

        CheckBox prevent_accidental_roll_check_box = findViewById(R.id.prevent_accidental_roll_checkbox);
        m_is_prevent_accidental_roll = prevent_accidental_roll_check_box.isChecked();

        CheckBox enable_timer = findViewById(R.id.enable_turn_timer_checkbox);
        m_is_timer_enable = enable_timer.isChecked();
        if (!m_is_timer_enable) {
            stopCountDownTimer();
        }

        if (mTwoPlayerGame) {
            BluetoothMessageHandler.SendSetFairDice(this, m_is_fair_dice);
        }

        ShowState(ShownState.GAME);
    }

    public void onSumButtonClick(View view) {
        m_one_dice_histogram.SetVisibility(View.GONE);
        m_sum_histogram.SetVisibility(View.VISIBLE);
        findViewById(R.id.combination_table).setVisibility(View.GONE);
        m_frontend_handler.SetHistogramTypeButtons(SUM);
    }

    public void onOneDiceButtonClick(View view) {
        m_one_dice_histogram.SetVisibility(View.VISIBLE);
        m_sum_histogram.SetVisibility(View.GONE);
        findViewById(R.id.combination_table).setVisibility(View.GONE);
        m_frontend_handler.SetHistogramTypeButtons(UNIQUE);
    }

    public void onCombinationButtonClick(View view) {
        m_one_dice_histogram.SetVisibility(View.GONE);
        m_sum_histogram.SetVisibility(View.GONE);
        findViewById(R.id.combination_table).setVisibility(View.VISIBLE);
        m_frontend_handler.SetHistogramTypeButtons(COMBINATION);
    }

    public void onBackFromHistogramClick(View view) {
        ShowState(ShownState.GAME);
        SetMainButtonsEnable(true);
    }

    public void onBackFromNumPlayersClick(View view) {
        ShowState(ShownState.GAME);
    }

    private void SetGameType(View view) {
        switch (view.getId()) {
            case R.id.button_regular_game:
                m_game_type = Logic.GameType.GAME_TYPE_REGULAR;
                break;

            case R.id.button_cities_and_knights:
                m_game_type = Logic.GameType.GAME_TYPE_CITIES_AND_KNIGHT;
                break;

            case R.id.button_simple_dice:
                m_game_type = Logic.GameType.GAME_TYPE_SIMPLE_DICE;
                break;

            default:
                throw new RuntimeException("Unknown button ID");
        }
    }

    private void onNewGameClick(View view) {
        ShowState(ShownState.SELECT_GAME_TYPE);
    }

    private void onAlchemistClick(View view) {
        m_is_alchemist_active = true;
        onRollClick(view);
    }

    public void onSelectGameTypeClick(View view) {
        SetGameType(view);
        ShowState(ShownState.SELECT_NUM_PLAYERS);
    }

    public void onSelectNumPlayersClick(View view) {
        switch (view.getId()) {
            case R.id.button_2_players:
                m_num_players = 2;
                break;

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
                throw new RuntimeException("Unknown button ID");
        }

        m_starting_player = new Random().nextInt(m_num_players) + 1;

        StartingNewGame(true);
    }

    public void SetStartingParameters(Logic.GameType game_type, int num_players, int starting_player) {
        m_game_type = game_type;
        m_num_players = num_players;
        m_starting_player = starting_player;
    }

    public void StartingNewGame(boolean send_message_to_other_player) {
        SetBackGround();

        m_logic.Init(m_num_players, m_game_type, m_is_fair_dice);
        m_pirate_position = 0;

        if (mTwoPlayerGame && send_message_to_other_player) {
            BluetoothMessageHandler.SendStartingGameParameters(m_game_type, m_num_players, m_starting_player, this);
        }

        SetEventDiceVisibility();
        SetOneDiceOperationVisibility();
        SetPiratePosition();
        stopCountDownTimer();

        ShowState(ShownState.GAME);
        ShowMessage(Card.MessageWithCard.NEW_GAME, 0);
    }

    private void ShowTurnNumber(int turn_number) {
        TextView turn_number_text_view = findViewById(R.id.turn_number_text_view);

        String turn_number_message = getString(R.string.turn_number) + ": " + turn_number + "   ";
        turn_number_text_view.setText(turn_number_message);
    }

    private void ShowMessage(Card.MessageWithCard messageType, int turn_number) {
        ShowTurnNumber(turn_number);

        if (messageType == Card.MessageWithCard.NO_MESSAGE) {
            return;
        }

        String message_type = "";

        switch (messageType) {
            case SEVEN_WITH_ROBBER:
                message_type = getString(R.string.seven_with_robber_string);
                break;
            case SEVEN_WITHOUT_ROBBER:
                message_type = getString(R.string.seven_without_robber_string);
                break;
            case NEW_GAME:
                message_type = String.format(getString(R.string.new_game_player_start), m_starting_player);
                break;
            case PIRATE_ATTACK:
                message_type = getString(R.string.pirate_attack);
                break;
            case PIRATE_ATTACK_ROBBER_ATTACK:
                message_type = getString(R.string.pirate_attack_seven);
                break;
            case PIRATE_ATTACK_ROBBER_IS_SLEEPING:
                message_type = getString(R.string.pirate_attack_seven_first);
                break;
            case LAST_MOVE_CANCELED:
                message_type = getString(R.string.last_move_canceled);
                break;
            default:
                break;
        }

        m_frontend_handler.ShowAlertDialogMessage(message_type, "");
    }

    private void SetMainButtonsEnable(boolean isEnable) {
        findViewById(R.id.fix_red_button).setEnabled(isEnable);
        findViewById(R.id.fix_yellow_button).setEnabled(isEnable);

        setImageButtonEnabled(isEnable, R.id.roll_button, R.drawable.roll_dice_button);
        setImageButtonEnabled(isEnable, R.id.menu_button, R.drawable.menu_icon);
    }

    public void CancelLastMove(boolean is_send_message) {
        m_logic.CancelLastMove();
        if (m_previous_card != null) {
            m_red_dice = m_previous_card.m_red;
            m_yellow_dice = m_previous_card.m_yellow;
            m_event_dice = m_previous_card.m_event_dice;
        } else {
            m_red_dice = 1;
            m_yellow_dice = 1;
            m_event_dice = Card.EventDice.PIRATE_SHIP;
        }

        m_frontend_handler.SetDicesImagesRolled(m_red_dice, m_yellow_dice);
        m_frontend_handler.SetEventDiceImage(m_event_dice);

        m_last_card = m_previous_card;
        m_previous_card = null;
        m_logic.DisableCancelLastMove();

        m_pirate_position = m_logic.GetPiratePosition();
        SetPiratePosition();
        if (mTwoPlayerGame && is_send_message) {
            BluetoothMessageHandler.SendCancelLastMove(this);
        }
        ShowMessage(Card.MessageWithCard.LAST_MOVE_CANCELED, m_logic.GetTurnNumber());
    }

    interface Worker {
        void onClick(View view);
    }

    public void onMenuButtonClick(View view) {
        AlertDialog.Builder builder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(getString(R.string.menu_title_string));

        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });

        final List<Worker> listWorkers = new ArrayList<Worker>();
        List<String> listItems = new ArrayList<String>();

        listWorkers.add(new Worker() {
            @Override
            public void onClick(View view) {
                onSettingClick(view);
            }
        });
        listItems.add(getString(R.string.setting_string));

        listWorkers.add(new Worker() {
            @Override
            public void onClick(View view) {
                onNewGameClick(view);
            }
        });
        listItems.add(getString(R.string.new_game_string));

        listWorkers.add(new Worker() {
            @Override
            public void onClick(View view) {
                onShowHistogramClick(view);
            }
        });
        listItems.add(getString(R.string.show_histogram));

        if (m_logic.CanCancelLastMove()) {
            listWorkers.add(new Worker() {
                @Override
                public void onClick(View view) {
                    onCancelLastMoveClick(view);
                }
            });
            listItems.add(getString(R.string.cancel_last_move_in_menu));
        }

        if (m_game_type == Logic.GameType.GAME_TYPE_CITIES_AND_KNIGHT) {
            listWorkers.add(new Worker() {
                @Override
                public void onClick(View view) {
                    onAlchemistClick(view);
                }
            });
            listItems.add(getString(R.string.alchemist));
        }

        CharSequence[] charSequences = listItems.toArray(new CharSequence[listItems.size()]);

        builder.setItems(charSequences,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        listWorkers.get(which).onClick(null);
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void onCancelLastMoveClick(View view) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(R.string.cancel_last_move);
        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                CancelLastMove(true);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        });
        builder.setIcon(R.drawable.cancel_last_move);
        builder.show();

    }

    private void setImageButtonEnabled(boolean enabled, int itemId, int iconResId) {
        ImageButton item = findViewById(itemId);
        item.setEnabled(enabled);

        Drawable originalIcon = ContextCompat.getDrawable(this, iconResId);
        Drawable icon = enabled ? originalIcon : convertDrawableToGrayScale(originalIcon);
        item.setImageDrawable(icon);
    }

    private static Drawable convertDrawableToGrayScale(Drawable drawable) {
        if (drawable == null) {
            return null;
        }

        Drawable res = drawable.mutate();

        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);

        res.setColorFilter(filter);

        return res;
    }

    public void onHelpFairDiceClick(View view) {
        String message = getString(R.string.fair_dice_help_message);
        String title = getString(R.string.fair_dice_title);
        m_frontend_handler.ShowAlertDialogMessage(message, title);
    }

    public void onHelpPreventAccidentalRollClick(View view) {
        String message = getString(R.string.prevent_accidental_roll_message);
        String title = getString(R.string.prevent_accidental_roll_title);
        m_frontend_handler.ShowAlertDialogMessage(message, title);
    }

    public void onRateAppClick(View view) {
        final String appPackageName = getPackageName();

        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
    }

    private void selectCountDownTimerValue(int index) {
        Button turn_timer_values_button = findViewById(R.id.turn_timer_values_button);

        m_count_down_timer_selection = index;
        turn_timer_values_button.setText(m_count_down_timer_seconds_strings[index]);
    }

    public void onTurnTimerValuesButtonClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle(getString(R.string.select_turn_time_title));

        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });

        builder.setItems(m_count_down_timer_seconds_strings,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        selectCountDownTimerValue(which);
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void SetCountDownTimerVisible(boolean is_visible) {
        if (is_visible) {
            GetCountDownTextView().setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.count_down_timer_text_view).setVisibility(View.GONE);
            findViewById(R.id.count_down_timer_text_view_2).setVisibility(View.INVISIBLE);
        }
    }

    private void stopCountDownTimer() {
        if (m_count_down_timer != null) {
            m_count_down_timer.cancel();
            m_count_down_timer = null;
        }

        SetCountDownTimerVisible(false);
    }

    private TextView GetCountDownTextView() {
        return (TextView)findViewById(m_game_type == Logic.GameType.GAME_TYPE_CITIES_AND_KNIGHT ?
                R.id.count_down_timer_text_view : R.id.count_down_timer_text_view_2);
    }

    private void startCountDownTimer() {
        if (!m_is_timer_enable) {
            return;
        }

        SetCountDownTimerVisible(true);

        int seconds = m_count_down_timer_seconds_values[m_count_down_timer_selection];

        m_count_down_timer = new CountDownTimer(1000 * seconds, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                TextView textView = GetCountDownTextView();
                int total_seconds = (int) (millisUntilFinished / 1000);
                int minutes = total_seconds / 60;
                int seconds = total_seconds % 60;

                if (seconds < 10) {
                    textView.setText(minutes + ":0" + seconds);
                } else {
                    textView.setText(minutes + ":" + seconds);
                }

                if (minutes == 0 && (seconds < 10)) {
                    PlaySound((seconds > 0) ? R.raw.second_passed : R.raw.timeout);
                }
            }

            @Override
            public void onFinish() {
                stopCountDownTimer();
            }
        };

        m_count_down_timer.start();
    }

    private Boolean VerifyBlueToothEnabled() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            return false;
        } else {
            return true;
        }
    }

    private void RunConnectActivity() {
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    public void onConnectClick(View view) {
        if (VerifyBlueToothEnabled()) {
            RunConnectActivity();
        }
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
        } else {
            if (mChatService == null) {
                setupChat();
                mChatService.start();
            }
        }
    }

    public void discoverable(View view) {
        ensureDiscoverable();
    }

    public void onDisconnectClick(View view) {
        disconnect(true);
    }

    public void disconnect(boolean send_message) {
        if (!mTwoPlayerGame) {
            return;
        }

        if (mChatService != null) {

            if (send_message) {
                BluetoothMessageHandler.SendDisconnectMessage(this);
            }

            mChatService.stop();
            mChatService = null;
        }

        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }

        SetTwoPlayerGame(false);
    }

    private void SetTwoPlayerGame(boolean is_two_player_game) {
        mTwoPlayerGame = is_two_player_game;

        findViewById(R.id.disconnectButton).setEnabled(mTwoPlayerGame);
        findViewById(R.id.sendFullStateButton).setEnabled(mTwoPlayerGame);

        findViewById(R.id.connectButton).setEnabled(!mTwoPlayerGame);
        findViewById(R.id.discoverableButton).setEnabled(!mTwoPlayerGame);
    }

    public void sendMessage(String message) {

        // Check that we're actually connected before trying anything
        if (mChatService.getState() != com.shirbi.catandice.BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            //Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
    private void handleMessage(Message msg) {
        switch (msg.what) {
            case com.shirbi.catandice.BluetoothChatService.MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                break;
            case com.shirbi.catandice.BluetoothChatService.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                BluetoothMessageHandler.ParseMessage(readMessage, this);
                break;
            case com.shirbi.catandice.BluetoothChatService.MESSAGE_DEVICE_NAME:
                // save the connected device's name
                String connectedDeviceName = msg.getData().getString(com.shirbi.catandice.BluetoothChatService.DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                        + connectedDeviceName, Toast.LENGTH_SHORT).show();
                SetTwoPlayerGame(true);
                ShowSendStateDialog();
                break;
            case com.shirbi.catandice.BluetoothChatService.MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void setupChat() {
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new com.shirbi.catandice.BluetoothChatService(mHandler);

        // Initialize the buffer for outgoing messages
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BluetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device

                    if (mChatService == null) {
                        setupChat();
                    }

                    mChatService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    RunConnectActivity();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_DISCOVERABLE:
                if (resultCode != Activity.RESULT_CANCELED) {
                    // Bluetooth is now enabled, so set up a chat session
                    if (mChatService == null) {
                        setupChat();
                        mChatService.start();
                    }
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    // The Handler that gets information back from the BluetoothChatService
    static class IncomingHandler extends Handler {
        private final WeakReference<MainActivity> m_activity;

        IncomingHandler(MainActivity activity) {
            m_activity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = m_activity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }
}
