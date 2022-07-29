
package com.example.signalcapturer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class StartupActivity extends AppCompatActivity {

    // prefs
    private static final String SHARED_PREFERENCES_NAME = "prefs";
    private static final String IS_SURVEY_DONE_PREF = "isSurveyDone";
    private static final String IS_CONSENT_TAKEN_PREF = "isConsentTaken";
    private static final String FORM_AGE = "FORM_AGE", DEFAULT_FORM_AGE = "<12";
//    private static final String FORM_GENDER = "FORM_GENDER", DEFAULT_FORM_GENDER = "Male";
    private static final String FORM_RADIO = "FORM_RADIO";
    private static final int DEFAULT_RADIO = -1;
    private static final String FORM_Q6 = "FORM_Q6", DEFAULT_FORM_Q6 = "";

    public static String qAge;
//    public static String qGender;
    private int[] radioAnswers = { -1, -1, -1, -1, -1 };

    // view pager declarations
    private static final int NUM_PAGES = 3;
    private ViewPager2 viewPager;
    private FragmentStateAdapter pagerAdapter;

    private Button nextButton, backButton;

    private Fragment introFragment = null;
    private Fragment formFragment = null;
    private Fragment consentFragment = null;

    private Spinner spinnerAge;
    private static final String[] ageGroups = {"<12", "12-17", "18-24", "24-30", "30-50", "50-60", ">60"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        formFragment = null;

        // set up the next button
        nextButton = findViewById(R.id.nextButton);
        backButton = findViewById(R.id.backButton);

        // Instantiate a ViewPager2 and a PagerAdapter.
        viewPager = findViewById(R.id.introPagerView);
        pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Attach the bottom tab layout with it
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabDots);
        new TabLayoutMediator(tabLayout, viewPager,
                new TabLayoutMediator.TabConfigurationStrategy() {
                    @Override
                    public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) { /* do nothing */ }
                }
        ).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                setBackButtonVisibility(position);
                setNextButtonText(position);

            }
        });

    }

    private void setBackButtonVisibility(int position) {
        if (position == 0) {
            backButton.setVisibility(View.GONE);
        } else {
            backButton.setVisibility(View.VISIBLE);
        }
    }

    private void setNextButtonText(int position) {
        if (position == NUM_PAGES - 1) {
            nextButton.setText("Start App");
        } else {
            nextButton.setText("Next");
        }
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                introFragment = new IntroductionFragment();
                return introFragment;
            } else if (position == 1) {
                formFragment = new FormFragment();
                return formFragment;
            } else {
                consentFragment = new ConsentFragment();
                return consentFragment;
            }
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }
    }

    private String getAgeFromForm() {
        return ((MyApplication) this.getApplication()).getAgeAns();
    }
    private String getGenderFromForm() {
        return ((MyApplication) this.getApplication()).getGenderAns();
    }
    private String getQ6Ans() {
        if (formFragment != null) {
            View formFragmentView = formFragment.getView();
            if (formFragmentView != null) {
                EditText q6_editText = formFragmentView.findViewById(R.id.q6multi_text);
                if (q6_editText != null) {
                    return q6_editText.getText().toString();
                }
            }
        }
        return "";
    }

    private boolean isConsentGiven() {
        return getConsentCheckBoxState();
    }

    private boolean getConsentCheckBoxState() {
        if (consentFragment != null) {
            View consentFragmentView = consentFragment.getView();
            if (consentFragmentView != null) {
                CheckBox checkBox = consentFragmentView.findViewById(R.id.consentCheckBox);
                if (checkBox != null) {
                    return checkBox.isChecked();
                }
            }
        }
        return false;
    }

    private boolean isFormFilled(int[] radioAnswers) {
        for (int ans : radioAnswers) {
            if (ans == -1) {
                return false;
            }
        }
        return true;
    }

    public void onClickNextButton(View view) {
        // if the last page
        if (viewPager.getCurrentItem() == NUM_PAGES - 1) {
            attemptToStartApplication();
        }
        // else go to the next page
        else {
            viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
        }
    }

    private void attemptToStartApplication() {

        // get form contents
        String ageAns = getAgeFromForm();
//        String genderAns = getGenderFromForm();
        String q6Ans = getQ6Ans();

        // log the form contents
        logFormContents(ageAns, /* genderAns, */ radioAnswers, q6Ans);

        // check if form is filled
        if (isFormFilled(radioAnswers)) {
            // save them in prefs && set that form is filled
            saveFormContentsAsPreferences(ageAns, /*genderAns,*/ radioAnswers, q6Ans);
            // see if consent is given
            if (isConsentGiven()) {

                saveThatConsentIsGiven();
                startActivity(new Intent(this, MainActivity.class));
                finish();

                /* WE DON'T NEED TO DO THE FOLLOWING FOR API > 19
                *  alert the user that we would need read/write permission too
                *  if we get that
                *       1. set that consent is given
                *       2. start the logging from main activity!
                */
//                startActivityIfUserGivesReadWritePermissions(this);
            }
            else {
                // alert user to give consent
                alertUser("Consent not given!", "Please check the box to give " +
                        "your consent before proceeding.");
            }
        } else {
            // alert do
            alertUser("Form not filled!", "Please complete the form before proceeding.");
        }

    }

    private void logFormContents(String ageAns,
                                 /*String genderAns,*/
                                 int[] radioAnswers,
                                 String q6Ans) {
//        Log.d("STATE_OF_FORM", String.format("Age: %s, Gender: %s, Radio: %d %d %d %d %d, Q6: %s",
//                ageAns, genderAns,
//                radioAnswers[0], radioAnswers[1], radioAnswers[2], radioAnswers[3], radioAnswers[4],
//                q6Ans));
        Log.d("STATE_OF_FORM", String.format("Age: %s Radio: %d %d %d %d %d, Q6: %s",
                ageAns, /*genderAns,*/
                radioAnswers[0], radioAnswers[1], radioAnswers[2], radioAnswers[3], radioAnswers[4],
                q6Ans));
    }

//    private void startActivityIfUserGivesReadWritePermissions(final Context context) {
//        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
//        alertDialog.setTitle("Read/Write Permission");
//        alertDialog.setMessage("SignalCapturer would require your permission to read/write files " +
//                "to temporarily store logs on the device until they are uploaded to the server.");
//        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                });
//        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
//                new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        // start main activity
//                        saveThatConsentIsGiven();
//                        startActivity(new Intent(context, MainActivity.class));
//                    }
//                });
//        alertDialog.show();
//    }


    private void alertUser(String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    private void saveFormContentsAsPreferences(String ageAns,
                                               /*String genderAns,*/
                                               int[] radioAnswers,
                                               String q6Ans) {
        SharedPreferences sharedPref = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(FORM_AGE, ageAns);
//        editor.putString(FORM_GENDER, genderAns);
        for (int i = 0; i < radioAnswers.length; ++i) {
            editor.putInt(FORM_RADIO + Integer.toString(i + 1), radioAnswers[i]);
        }
        editor.putString(FORM_Q6, q6Ans);
        editor.putBoolean(IS_SURVEY_DONE_PREF, true);
        editor.apply();
    }

    private void saveThatConsentIsGiven() {
        SharedPreferences sharedPref = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(IS_CONSENT_TAKEN_PREF, true);
        editor.apply();
    }

    public void onClickBackButton(View view) {
        // if this is not the last page
        if (!(viewPager.getCurrentItem() == 0)) {
            // go a page back
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }
    }

    private static int formIDs[][] = {
            { R.id.form1_1, R.id.form1_2, R.id.form1_3, R.id.form1_4, R.id.form1_5 },
            { R.id.form2_1, R.id.form2_2, R.id.form2_3, R.id.form2_4, R.id.form2_5 },
            { R.id.form3_1, R.id.form3_2, R.id.form3_3, R.id.form3_4, R.id.form3_5 },
            { R.id.form4_1, R.id.form4_2, R.id.form4_3, R.id.form4_4, R.id.form4_5 },
            { R.id.form5_1, R.id.form5_2, R.id.form5_3, R.id.form5_4, R.id.form5_5 }
    };

    public void onRadioButtonClicked1(View view) { handleRadioButtonClick(1, view); }
    public void onRadioButtonClicked2(View view) { handleRadioButtonClick(2, view); }
    public void onRadioButtonClicked3(View view) { handleRadioButtonClick(3, view); }
    public void onRadioButtonClicked4(View view) { handleRadioButtonClick(4, view); }
    public void onRadioButtonClicked5(View view) { handleRadioButtonClick(5, view); }

    private void handleRadioButtonClick(int radioGroupNum, View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        // Check which radio button was clicked
        int id = view.getId();
        int radioIndex = findRadioIndex(formIDs[radioGroupNum - 1], id);
        if (radioIndex != -1) {
            Log.d("RADIO_GRP "+ radioGroupNum, radioIndex + 1 + " pressed, checked = " + checked);
            if (checked) {
                radioAnswers[radioGroupNum - 1] = radioIndex + 1;
            }
        }
    }

    private int findRadioIndex(int[] arr, int value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == value) {
                return i;
            }
        }
        return -1;
    }

    public void showDetailMemoryStats(View view) {
        if (consentFragment != null) {
            View consentFragmentView = consentFragment.getView();
            if (consentFragmentView != null) {
                TextView tvButton = consentFragmentView.findViewById(R.id.consentShowDetailsButton);
                TextView tvDetails = consentFragmentView.findViewById(R.id.consentMemoryStatsDetails);
                if (tvButton != null && tvDetails != null) {
                    if (tvDetails.getVisibility() == View.GONE) {
                        tvButton.setText(R.string.hide_details);
                        tvDetails.setVisibility(View.VISIBLE);
                    } else {
                        tvButton.setText(R.string.show_details);
                        tvDetails.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    public void seePrivacyPolicyOnClick(View view) {
        if (introFragment != null) {
            View fragmentView = introFragment.getView();
            if (fragmentView != null) {
                TextView tvShowPrivacy = fragmentView.findViewById(R.id.seePrivacyTVButton);
                TextView tvIntroAbout2 = fragmentView.findViewById(R.id.introAbout2);
                TextView tvIntroAbout3 = fragmentView.findViewById(R.id.introAbout3);
                if (tvShowPrivacy != null && tvIntroAbout2 != null && tvIntroAbout3 != null) {
                    if (tvIntroAbout2.getVisibility() == View.GONE) {
                        tvShowPrivacy.setText(R.string.hide_privacy_policy);
                        tvIntroAbout2.setVisibility(View.VISIBLE);
                        tvIntroAbout3.setVisibility(View.VISIBLE);
                    } else {
                        tvShowPrivacy.setText(R.string.see_privacy_policy);
                        tvIntroAbout2.setVisibility(View.GONE);
                        tvIntroAbout3.setVisibility(View.GONE);
                    }
                } else {
                    Log.e("STARTUP_ACT", "introFragmentView's views are null");
                }
            } else {
                Log.e("STARTUP_ACT", "introFragmentView is null");
            }
        } else {
            Log.e("STARTUP_ACT", "introFragmen is null");
        }
    }

}