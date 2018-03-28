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
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;

import com.instructure.candroid.R;
import com.instructure.candroid.holders.QuizNumericalViewHolder;
import com.instructure.candroid.interfaces.QuizPostNumerical;
import com.instructure.candroid.interfaces.QuizToggleFlagState;
import com.instructure.canvasapi2.models.QuizSubmissionQuestion;
import com.instructure.pandautils.utils.ColorKeeper;
import com.instructure.pandautils.views.CanvasWebView;


public class QuizNumericalBinder {

    public static void bind(final QuizNumericalViewHolder holder, final QuizSubmissionQuestion quizSubmissionQuestion, int courseColor, int position, boolean shouldLetAnswer, final Context context, final QuizToggleFlagState flagStateCallback, CanvasWebView.CanvasEmbeddedWebViewCallback callback, CanvasWebView.CanvasWebViewClientCallback webViewClientCallback, final QuizPostNumerical numericalCallback) {

        if(holder == null) {
            return;
        }
        holder.question.loadUrl("about:blank");
        holder.question.setCanvasWebViewClientCallback(webViewClientCallback);
        holder.question.formatHTML(quizSubmissionQuestion.getQuestionText(), "");
        holder.question.setBackgroundColor(Color.TRANSPARENT);
        holder.question.setCanvasEmbeddedWebViewCallback(callback);
        holder.questionNumber.setText(context.getString(R.string.question) + " " + (position + 1));

        holder.questionId = quizSubmissionQuestion.getId();
        if(!shouldLetAnswer) {
            holder.answer.setVisibility(View.GONE);
            holder.divider.setVisibility(View.GONE);
        }

        //clear out the answer in case it was recycled
        holder.answer.setText("");
        if(quizSubmissionQuestion.getAnswer() != null) {
            if(quizSubmissionQuestion.getAnswer() instanceof Double) {
                holder.answer.setText(Html.fromHtml(Double.toString((Double)quizSubmissionQuestion.getAnswer())));
            } else if(quizSubmissionQuestion.getAnswer() instanceof String) {
                holder.answer.setText(Html.fromHtml((String)quizSubmissionQuestion.getAnswer()));
            }
        }


        holder.answer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                //save data after they're done typing
                if(holder.answer.getText() != null) {
                    numericalCallback.postNumerical(holder.questionId, editable.toString());
                }
            }
        });

        final Drawable courseColorFlag = ColorKeeper.getColoredDrawable(context, R.drawable.vd_bookmark_filled, courseColor);

        if(quizSubmissionQuestion.isFlagged()) {
            holder.flag.setImageDrawable(courseColorFlag);
        } else {
            holder.flag.setImageDrawable(ColorKeeper.getColoredDrawable(context, R.drawable.vd_navigation_bookmarks, context.getResources().getColor(R.color.defaultTextGray)));
        }

        if(shouldLetAnswer) {
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
    }
}
