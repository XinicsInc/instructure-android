/*
 * Copyright (C) 2016 - present Instructure, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.instructure.candroid.binders;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.internal.LinkedTreeMap;
import com.instructure.candroid.R;
import com.instructure.candroid.adapter.QuizMultipleDropdownSpinnerAdapter;
import com.instructure.candroid.holders.QuizMultipleDropdownViewHolder;
import com.instructure.candroid.interfaces.QuizPostMultipleDropdown;
import com.instructure.candroid.interfaces.QuizToggleFlagState;
import com.instructure.canvasapi2.models.QuizSubmissionAnswer;
import com.instructure.canvasapi2.models.QuizSubmissionQuestion;
import com.instructure.pandautils.utils.ColorKeeper;
import com.instructure.pandautils.views.CanvasWebView;

import java.util.ArrayList;
import java.util.HashMap;

public class QuizMultipleDropdownBinder {

    public static void bind(final QuizMultipleDropdownViewHolder holder,
                            final QuizSubmissionQuestion quizSubmissionQuestion,
                            final int courseColor,
                            final int position,
                            final boolean shouldLetAnswer,
                            final Context context,
                            final CanvasWebView.CanvasEmbeddedWebViewCallback embeddedWebViewCallback,
                            final CanvasWebView.CanvasWebViewClientCallback webViewClientCallback,
                            final QuizPostMultipleDropdown callback,
                            final QuizToggleFlagState flagStateCallback) {
        if(holder == null) {
            return;
        }

        setupViews(holder, quizSubmissionQuestion, position, context, embeddedWebViewCallback, webViewClientCallback);

        LayoutInflater inflater = LayoutInflater.from(context);

        //use a map to store the data, each blank id will have an array list of potential answers
        HashMap<String, ArrayList<QuizSubmissionAnswer>> answerMap = new HashMap<>();
        for(QuizSubmissionAnswer answer : quizSubmissionQuestion.getAnswers()) {
            ArrayList<QuizSubmissionAnswer> potentialAnswers = answerMap.get(answer.getBlankId());
            if(potentialAnswers == null) {
                potentialAnswers = new ArrayList<>();
                QuizSubmissionAnswer selectAnswer = new QuizSubmissionAnswer();
                selectAnswer.setText(context.getString(R.string.quizMatchingDefaultDisplay));
                potentialAnswers.add(selectAnswer);
            }
            potentialAnswers.add(answer);
            answerMap.put(answer.getBlankId(), potentialAnswers);
        }

        if(answerMap.size() > 1) {
            //we have more than one, update the string to reflect that
            holder.chooseAnswer.setText(context.getString(R.string.choose_answers_below));
        } else {
            holder.chooseAnswer.setText(context.getString(R.string.choose_answer_below));
        }

        //add answers to the answer container
        for(final String blankId : answerMap.keySet()) {

            final LinearLayout answerWrapper = (LinearLayout)inflater.inflate(R.layout.quiz_multiple_dropdown_answer, null, false);

            final TextView answerTextView = (TextView) answerWrapper.findViewById(R.id.text_answer);
            final Spinner spinner = (Spinner) answerWrapper.findViewById(R.id.answer_spinner);


            ArrayList<QuizSubmissionAnswer> list = new ArrayList<>();

            for(QuizSubmissionAnswer dropdownAnswer : answerMap.get(blankId)) {
                list.add(dropdownAnswer);
            }
            QuizMultipleDropdownSpinnerAdapter adapter = new QuizMultipleDropdownSpinnerAdapter(context, android.R.layout.simple_spinner_item, list);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            if(!TextUtils.isEmpty(blankId)) {
                answerTextView.setText(blankId);

                setPreviouslySelectedAnswer(quizSubmissionQuestion, blankId, spinner, list);
            }

            final Drawable courseColorFlag = ColorKeeper.getColoredDrawable(context, R.drawable.vd_bookmark_filled, courseColor);

            if(quizSubmissionQuestion.isFlagged()) {
                holder.flag.setImageDrawable(courseColorFlag);
            } else {
                holder.flag.setImageDrawable(ColorKeeper.getColoredDrawable(context, R.drawable.vd_navigation_bookmarks, context.getResources().getColor(R.color.defaultTextGray)));
            }


            if(shouldLetAnswer) {
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        //post the answer to the api
                        HashMap<String, Long> answerMap = new HashMap<>();

                        if(((QuizSubmissionAnswer) adapterView.getSelectedItem()).getId() != 0) {
                            answerMap.put(blankId, ((QuizSubmissionAnswer) adapterView.getSelectedItem()).getId());
                            callback.postMultipleDropdown(holder.questionId, answerMap);
                        }

                    }

                    @Override
                    public void onNothingSelected (AdapterView < ? > adapterView){

                    }
                });

                holder.flag.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (quizSubmissionQuestion.isFlagged()) {
                            //unflag it
                            holder.flag.setImageDrawable(ColorKeeper.getColoredDrawable(context, R.drawable.vd_navigation_bookmarks, context.getResources().getColor(R.color.defaultTextGray)));
                            flagStateCallback.toggleFlagged(false, quizSubmissionQuestion.getId());
                            quizSubmissionQuestion.setFlagged(false);
                        } else {
                            //flag it
                            holder.flag.setImageDrawable(courseColorFlag);
                            flagStateCallback.toggleFlagged(true, quizSubmissionQuestion.getId());
                            quizSubmissionQuestion.setFlagged(true);
                        }
                    }
                });
            } else {
                holder.flag.setEnabled(false);
            }

            holder.answerContainer.addView(answerWrapper);
        }
    }

    private static void setupViews(QuizMultipleDropdownViewHolder holder, QuizSubmissionQuestion quizSubmissionQuestion, int position, Context context, CanvasWebView.CanvasEmbeddedWebViewCallback embeddedWebViewCallback, CanvasWebView.CanvasWebViewClientCallback webViewClientCallback) {
        holder.question.loadUrl("about:blank");

        holder.question.setBackgroundColor(Color.TRANSPARENT);

        holder.question.setCanvasWebViewClientCallback(webViewClientCallback);

        holder.question.formatHTML(quizSubmissionQuestion.getQuestionText(), "");
        holder.question.setCanvasEmbeddedWebViewCallback(embeddedWebViewCallback);

        holder.questionNumber.setText(context.getString(R.string.question) + " " + (position + 1));

        holder.questionId = quizSubmissionQuestion.getId();

        //sometimes when we recycle views it keeps the old views in there, so clear them out if there
        //are any in there
        if(holder.answerContainer.getChildCount() > 0) {
            holder.answerContainer.removeAllViews();
        }
    }

    private static void setPreviouslySelectedAnswer(QuizSubmissionQuestion quizSubmissionQuestion, String answer, Spinner spinner, ArrayList<QuizSubmissionAnswer> list) {
        if(quizSubmissionQuestion.getAnswer() != null) {
            // set the one they selected last time
            for(String map :((LinkedTreeMap<String, String>) quizSubmissionQuestion.getAnswer()).keySet()) {
                if(!answer.equals("null") && map.equals(answer)) {

                    String matchId = ((LinkedTreeMap<String, String>) quizSubmissionQuestion.getAnswer()).get(map);
                    //now see if we have a match in the list of matches
                    int listIndex = 0;
                    for(QuizSubmissionAnswer match : list) {
                        if (Long.toString(match.getId()).equals(matchId)) {
                            spinner.setSelection(listIndex);
                            break;
                        }
                        listIndex++;
                    }
                }
            }
        }
    }
}
