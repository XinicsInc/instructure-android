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
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.instructure.candroid.R;
import com.instructure.candroid.holders.QuizMultiChoiceViewHolder;
import com.instructure.candroid.interfaces.QuizPostMultiAnswers;
import com.instructure.candroid.interfaces.QuizToggleFlagState;
import com.instructure.candroid.util.StringUtilities;
import com.instructure.canvasapi2.models.QuizSubmissionAnswer;
import com.instructure.canvasapi2.models.QuizSubmissionQuestion;
import com.instructure.pandautils.utils.ColorKeeper;
import com.instructure.pandautils.views.CanvasWebView;

public class QuizMultiAnswerBinder {

    public static void bind(
            final QuizMultiChoiceViewHolder holder,
            final QuizSubmissionQuestion quizSubmissionQuestion,
            final int courseColor,
            final int position,
            final boolean shouldLetAnswer,
            final Context context,
            final QuizPostMultiAnswers callback,
            final QuizToggleFlagState flagStateCallback) {

        if (holder == null) {
            return;
        }

        holder.question.loadUrl("about:blank");

        holder.question.setBackgroundColor(Color.TRANSPARENT);

        holder.question.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl("javascript:MyApp.resize(document.body.getBoundingClientRect().height)");
                super.onPageFinished(view, url);
            }
        });
        holder.question.formatHTML(quizSubmissionQuestion.getQuestionText(), "");

        holder.questionNumber.setText(quizSubmissionQuestion.getQuestionName() + " " + (position + 1));

        holder.questionId = quizSubmissionQuestion.getId();

        LayoutInflater inflater = LayoutInflater.from(context);

        //sometimes when we recycle views it keeps the old views in there, so clear them out if there
        //are any in there
        if (holder.answerContainer.getChildCount() > 0) {
            holder.answerContainer.removeAllViews();
        }
        //add answers to the answer container
        int index = 0;
        for (final QuizSubmissionAnswer answer : quizSubmissionQuestion.getAnswers()) {

            final LinearLayout answerContainer = (LinearLayout) inflater.inflate(R.layout.quiz_multi_choice_answer, null, false);
            final CanvasWebView webView = (CanvasWebView) answerContainer.findViewById(R.id.html_answer);
            webView.setClickable(false);
            webView.setFocusableInTouchMode(false);
            final TextView textView = (TextView) answerContainer.findViewById(R.id.text_answer);
            final CheckBox checkBox = (CheckBox) answerContainer.findViewById(R.id.answer_checkbox);

            if (!TextUtils.isEmpty(answer.getHtml())) {
                textView.setVisibility(View.GONE);

                final String html = StringUtilities.trimTrailingWhitespace(answer.getHtml()).toString();

                webView.formatHTML(html, "");

                webView.setBackgroundColor(Color.TRANSPARENT);
                //we only care about marking the answers if they can actually answer
                if (shouldLetAnswer) {
                    webView.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            //this may or may not be how we do the selection of things when we get the final UI
                            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                                answerContainer.performClick();

                                return true;
                            }
                            return true;
                        }
                    });
                }
            } else if (!TextUtils.isEmpty(answer.getText())) {
                webView.setVisibility(View.GONE);
                textView.setText(answer.getText());
            }

            if (shouldLetAnswer) {
                answerContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (checkBox.isChecked()) {
                            checkBox.setChecked(false);
                            answerContainer.setBackgroundColor(context.getResources().getColor(R.color.canvasBackgroundLight));
                            callback.answerUnselected(holder.questionId, answer.getId());
                        } else {
                            checkBox.setChecked(true);
                            answerContainer.setBackgroundColor(context.getResources().getColor(R.color.canvasBackgroundMedium));
                            callback.answerSelected(holder.questionId, answer.getId());
                        }
                    }
                });
            }

            final Drawable courseColorFlag = ColorKeeper.getColoredDrawable(context, R.drawable.vd_bookmark_filled, courseColor);

            if(quizSubmissionQuestion.isFlagged()) {
                holder.flag.setImageDrawable(courseColorFlag);
            } else {
                holder.flag.setImageDrawable(ColorKeeper.getColoredDrawable(context, R.drawable.vd_navigation_bookmarks, context.getResources().getColor(R.color.defaultTextGray)));
            }

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


            holder.answerContainer.addView(answerContainer);

            if (index == quizSubmissionQuestion.getAnswers().length - 1) {
                //if we're on the last answer remove the bottom divider
                answerContainer.findViewById(R.id.divider).setVisibility(View.GONE);
            } else {
                answerContainer.findViewById(R.id.divider).setVisibility(View.VISIBLE);
                index++;
            }

        }
    }
}
