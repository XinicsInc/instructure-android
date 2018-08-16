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

package com.instructure.candroid.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.instructure.candroid.R;
import com.instructure.pandautils.utils.Const;
import com.instructure.canvasapi2.models.Assignment;

public class WhatIfDialogStyled extends DialogFragment {

    public static final String TAG = "whatIfDialog";

    private static String totalScore;
    private static Assignment assignment;
    private static WhatIfDialogCallback callback;
    private static int courseColor;
    private static int position;

    //views
    private EditText whatIfScore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Bundle args = getArguments();
        if(args != null) {
            assignment = args.getParcelable(Const.ASSIGNMENT);
            totalScore = args.getString(Const.SCORE);
            courseColor = args.getInt(Const.COURSE_COLOR);
            position = args.getInt(Const.POSITION);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.whatIfDialogText))
                .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(callback != null){
                            final String whatIf = whatIfScore.getText().toString();
                            callback.onOkayClick(whatIf, Double.parseDouble(totalScore), assignment, position);
                        }
                        dismissAllowingStateLoss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismissAllowingStateLoss();
                    }
                });

        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(activity).inflate(R.layout.what_if_dialog, null);

        EditText totalScoreEdit = view.findViewById(R.id.totalScore);
        totalScoreEdit.setText(totalScore);

        whatIfScore = view.findViewById(R.id.currentScore);

        builder.setView(view);

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface worthless) {
                if(courseColor != 0){
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(courseColor);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(courseColor);
                }
            }
        });

        dialog.setCanceledOnTouchOutside(true);

        return dialog;
    }

    public interface WhatIfDialogCallback{
        void onOkayClick(String whatIf, double total, Assignment assignment, int position);
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);

        super.onDestroyView();
    }

    public static void show(FragmentActivity activity, double score, WhatIfDialogCallback callback, Assignment assignment, int courseColor, int position)  {
        WhatIfDialogStyled frag = new WhatIfDialogStyled();
        frag.callback = callback;

        Bundle args = new Bundle();
        args.putInt(Const.COURSE_COLOR, courseColor);
        args.putInt(Const.POSITION, position);
        args.putParcelable(Const.ASSIGNMENT, assignment);
        args.putString(Const.SCORE, Double.toString(score));
        frag.setArguments(args);

        frag.show(activity.getSupportFragmentManager(), TAG);
    }
}
