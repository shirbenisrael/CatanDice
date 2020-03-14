package com.shirbi.catandice;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

final class FrontEndHandler {
    MainActivity m_activity;
    private Point m_size;
    private ImageView[] m_pirate_positions_images;

    public FrontEndHandler(MainActivity activity) {
        m_activity = activity;

        m_activity.setContentView(R.layout.activity_main);

        m_size = GetWindowSize();

        arrange_buttons();
        arrange_dice_dimensions();
        arrange_histogram_layout();
        arrange_pirate_ship();
    }

    private <T extends View> T findViewById(@IdRes int id) {
        return m_activity.getWindow().findViewById(id);
    }

    public final String getString(@StringRes int resId) {
        return m_activity.getString(resId);
    }

    private Point GetWindowSize() {
        Display display = m_activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    private void SetDicesImages(int red_dice_number, int yellow_dice_number,
                                ImageView red_dice_result_image, ImageView yellow_dice_result_image) {
        int[] red_images =
                {R.drawable.red_1, R.drawable.red_2, R.drawable.red_3, R.drawable.red_4, R.drawable.red_5, R.drawable.red_6};

        red_dice_result_image.setImageResource(red_images[red_dice_number - 1]);

        int[] yellow_images =
                {R.drawable.yellow_1, R.drawable.yellow_2, R.drawable.yellow_3, R.drawable.yellow_4, R.drawable.yellow_5, R.drawable.yellow_6};

        yellow_dice_result_image.setImageResource(yellow_images[yellow_dice_number - 1]);
    }

    void SetDicesImagesRolled(int red_dice_number, int yellow_dice_number) {
        ImageView red_dice_result_image = findViewById(R.id.red_dice_result);
        ImageView yellow_dice_result_image = findViewById(R.id.yellow_dice_result);

        SetDicesImages(red_dice_number, yellow_dice_number, red_dice_result_image, yellow_dice_result_image);
    }

    void SetEventDiceImage(Card.EventDice eventDice) {
        ImageView event_dice_result = findViewById(R.id.event_dice_result);

        switch (eventDice) {
            case YELLOW_CITY:
                event_dice_result.setImageResource(R.drawable.yellow_city);
                break;
            case GREEN_CITY:
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

    void ShowState(MainActivity.ShownState new_state) {
        int[] all_layout = {
                R.id.layout_for_dices,
                R.id.game_type_layout,
                R.id.num_players_layout,
                R.id.setting_layout,
                R.id.histogram_background_layout};

        int[] layouts_for_game = {R.id.layout_for_dices};
        int[] layouts_for_game_type = {R.id.game_type_layout};
        int[] layouts_for_num_players = {R.id.num_players_layout};
        int[] layouts_for_settings = {R.id.setting_layout};
        int[] layouts_for_histogram = {R.id.layout_for_dices, R.id.histogram_background_layout};

        int[] layouts_to_show = {0};

        switch (new_state) {
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

        for (int i : all_layout) {
            View layout = findViewById(i);
            layout.setVisibility(View.INVISIBLE);

            for (int j : layouts_to_show) {
                if (i == j) {
                    layout.setVisibility(View.VISIBLE);
                    break;
                }
            }
        }
    }

    void showExitDialog() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(m_activity, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(m_activity);
        }
        builder.setTitle(getString(R.string.exit_app));
        builder.setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                m_activity.Exit();
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

    private void set_square_size_view(View view, int size) {
        view.getLayoutParams().width = size;
        view.getLayoutParams().height = size;
    }

    private void set_square_size(int view_id, int size) {
        View view = findViewById(view_id);
        set_square_size_view(view, size);
    }

    private void set_square_size_with_margin(int view_id,
                                             int size,
                                             int top_margin,
                                             int bottom_margin,
                                             int left_margin,
                                             int right_margin) {
        set_square_size(view_id, size);

        View view = findViewById(view_id);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) view.getLayoutParams();
        lp.setMargins(left_margin, top_margin, right_margin, bottom_margin);
    }

    private void arrange_buttons() {
        View roll_button = findViewById(R.id.roll_button);
        roll_button.getLayoutParams().width = m_size.x * 5 /6;
        roll_button.getLayoutParams().height = m_size.x * 45 / 100;

        View menu_button = findViewById(R.id.menu_button);
        menu_button.getLayoutParams().width = m_size.x / 6;
        menu_button.getLayoutParams().height = m_size.x / 6;
        ((LinearLayout.LayoutParams)(menu_button.getLayoutParams())).setMargins(
                0,m_size.x * 32 / 100,m_size.x * 5 / 100,0);

        int[] Ids = new int[] {R.id.fix_red_button, R.id.fix_yellow_button};
        int width = m_size.x / 3;
        for (int id : Ids) {
            View view = findViewById(id);
            view.getLayoutParams().width = width;
            view.getLayoutParams().height = width / 2;
        }
    }

    private void arrange_dice_dimensions() {
        int dice_margin = m_size.x / 40;
        int dice_num_horizontal = 2;
        int dice_num_margins = dice_num_horizontal + 1;
        int dice_width = (m_size.x - (dice_num_margins * dice_margin)) / dice_num_horizontal;

        set_square_size_with_margin(R.id.red_dice_result, dice_width, dice_margin,
                dice_margin / 2, dice_margin, dice_margin / 2);
        set_square_size_with_margin(R.id.yellow_dice_result, dice_width, dice_margin,
                dice_margin / 2, dice_margin / 2, dice_margin);
        set_square_size_with_margin(R.id.event_dice_result, dice_width, dice_margin / 2,
                dice_margin, dice_margin / 2, dice_margin / 2);
    }

    private void arrange_histogram_layout() {
        LinearLayout main_histogram_layout = findViewById(R.id.histogram_layout);

        main_histogram_layout.getLayoutParams().width = GetHistogramWindowWidth();
        main_histogram_layout.getLayoutParams().height = GetHistogramWindowHeight();
    }

    private void arrange_pirate_ship() {
        m_pirate_positions_images = new ImageView[Card.MAX_PIRATE_POSITIONS];

        LinearLayout layout_for_pirate_ship = findViewById(R.id.layout_for_pirate_ship);

        for (int i = 0; i < Card.MAX_PIRATE_POSITIONS; i++) {
            m_pirate_positions_images[i] = new ImageView(m_activity);
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
    }

    int GetHistogramWindowWidth() {
        return m_size.x * 9 / 10;
    }

    int GetHistogramWindowHeight() {
        return m_size.y * 4 / 5;
    }

    void SetPiratePosition(int pirate_position) {
        for (int i = pirate_position + 1; i < Card.MAX_PIRATE_POSITIONS; i++) {
            m_pirate_positions_images[i].setImageAlpha(0);
        }

        for (int i = 0; i <= pirate_position; i++) {
            int alpha = 150 >> (pirate_position - i);
            m_pirate_positions_images[i].setImageAlpha(alpha);
        }
    }
}
