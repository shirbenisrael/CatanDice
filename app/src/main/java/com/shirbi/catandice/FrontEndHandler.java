package com.shirbi.catandice;

import android.support.annotation.IdRes;
import android.view.View;
import android.widget.ImageView;

final class FrontEndHandler {
    MainActivity m_activity;

    public FrontEndHandler(MainActivity activity) {
        m_activity = activity;

        m_activity.setContentView(R.layout.activity_main);
    }

    private <T extends View> T findViewById(@IdRes int id) {
        return m_activity.getWindow().findViewById(id);
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
}
