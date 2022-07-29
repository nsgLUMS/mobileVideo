package com.example.signalcapturer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

public class FormFragment extends Fragment {


    private Spinner spinnerAge;
    private static final String[] ageGroups = { "<12", "12-17", "18-24", "25-30", "31-50", "51-60", ">60" };
    private String ageAns;

//    private Spinner spinnerGender;
//    private static final String[] genders = { "Male", "Female", "Other" };
//    private String genderAns;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public FormFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FormFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static FormFragment newInstance(String param1, String param2) {
        FormFragment fragment = new FormFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_form, container, false);

        // age spinner
        spinnerAge = view.findViewById(R.id.spinnerAge);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_spinner_item, ageGroups);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAge.setAdapter(adapter);
        spinnerAge.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d("AGE_GROUP", ageGroups[i]);
//                StartupActivity.qAge = ageGroups[i];
                // set pref
                ((MyApplication) getActivity().getApplication()).setAgeAns(ageGroups[i]);
//                SharedPreferences sharedPref = view.getContext().getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
//                SharedPreferences.Editor editor = sharedPref.edit();
//                editor.putString(AGE_ANS, ageGroups[i]);
//                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // do nothing
            }
        });

        // gender spinner
//        spinnerGender = view.findViewById(R.id.spinnerGender);
//        ArrayAdapter<String> adapterGedner = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_spinner_item, genders);
//        adapterGedner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinnerGender.setAdapter(adapterGedner);
//        spinnerGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//                // set pref
//                ((MyApplication) getActivity().getApplication()).setGenderAns(genders[i]);
////                SharedPreferences sharedPref = view.getContext().getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
////                SharedPreferences.Editor editor = sharedPref.edit();
////                editor.putString(GENDER_ANS, genders[i]);
////                editor.apply();
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> adapterView) {
//                // do nothing
//            }
//        });

        // Inflate the layout for this fragment
        return view;
    }



}