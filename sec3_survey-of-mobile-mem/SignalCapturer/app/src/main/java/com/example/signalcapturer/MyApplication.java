package com.example.signalcapturer;

import android.app.Application;

public class MyApplication extends Application {

    private String ageAns, genderAns, q6Ans;

    public String getAgeAns() {
        return ageAns;
    }

    public void setAgeAns(String ageAns) {
        this.ageAns = ageAns;
    }

    public String getGenderAns() {
        return genderAns;
    }

    public void setGenderAns(String genderAns) {
        this.genderAns = genderAns;
    }

    public String getQ6Ans() {
        return q6Ans;
    }

    public void setQ6Ans(String q6Ans) {
        this.q6Ans = q6Ans;
    }

}
