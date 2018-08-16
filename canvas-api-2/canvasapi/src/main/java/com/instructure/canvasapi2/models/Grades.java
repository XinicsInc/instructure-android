/*
 * Copyright (C) 2017 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.instructure.canvasapi2.models;

import android.os.Parcel;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Date;


public class Grades extends CanvasModel<Grades> {

    @SerializedName("html_url")
    private String htmlUrl;

    @Nullable
    @SerializedName("current_score")
    private Double currentScore;
    @Nullable
    @SerializedName("final_score")
    private Double finalScore;
    @SerializedName("current_grade")
    private String currentGrade;
    @SerializedName("final_grade")
    private String finalGrade;

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public Date getComparisonDate() {
        return null;
    }

    @Override
    public String getComparisonString() {
        return null;
    }

    //region Getters

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public Double getCurrentScore() {
        return currentScore;
    }

    public Double getFinalScore() {
        return finalScore;
    }

    public String getCurrentGrade() {
        return currentGrade;
    }

    public String getFinalGrade() {
        return finalGrade;
    }

    //endregion

    //region Setters

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public void setCurrentScore(double currentScore) {
        this.currentScore = currentScore;
    }

    public void setFinalScore(double finalScore) {
        this.finalScore = finalScore;
    }

    public void setCurrentGrade(String currentGrade) {
        this.currentGrade = currentGrade;
    }

    public void setFinalGrade(String finalGrade) {
        this.finalGrade = finalGrade;
    }

    //endregion

    //region Parcelable

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.htmlUrl);
        dest.writeValue(this.currentScore);
        dest.writeValue(this.finalScore);
        dest.writeString(this.currentGrade);
        dest.writeString(this.finalGrade);
    }

    public Grades() {
    }

    protected Grades(Parcel in) {
        this.htmlUrl = in.readString();
        this.currentScore = (Double) in.readValue(Double.class.getClassLoader());
        this.finalScore = (Double) in.readValue(Double.class.getClassLoader());
        this.currentGrade = in.readString();
        this.finalGrade = in.readString();
    }

    public static final Creator<Grades> CREATOR = new Creator<Grades>() {
        @Override
        public Grades createFromParcel(Parcel source) {
            return new Grades(source);
        }

        @Override
        public Grades[] newArray(int size) {
            return new Grades[size];
        }
    };

    //endregion
}
