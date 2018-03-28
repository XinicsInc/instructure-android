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

package com.instructure.candroid.binders

import android.content.Context
import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.view.View
import com.instructure.candroid.R
import com.instructure.candroid.holders.RubricViewHolder
import com.instructure.candroid.model.RubricCommentItem
import com.instructure.candroid.model.RubricItem
import com.instructure.candroid.model.RubricRatingItem
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.RubricCriterion
import com.instructure.canvasapi2.models.RubricCriterionAssessment
import com.instructure.canvasapi2.models.RubricCriterionRating
import com.instructure.pandautils.utils.ColorKeeper
import java.text.DecimalFormat
import java.util.*

class RubricBinder : BaseBinder() {
    companion object {

        fun bind(
                context: Context,
                holder: RubricViewHolder,
                rubricItem: RubricItem,
                criterion: RubricCriterion,
                isFreeForm: Boolean,
                assessment: RubricCriterionAssessment?,
                canvasContext: CanvasContext) {
            if (holder.rubricType == RubricViewHolder.TYPE_ITEM_COMMENT) {
                val comment = (rubricItem as RubricCommentItem).comment
                val color = ColorKeeper.getOrGenerateColor(canvasContext)
                val d = ColorKeeper.getColoredDrawable(context, R.drawable.vd_chat, color)

                holder.descriptionView.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null)
                holder.descriptionView.text = comment

                if (!isFreeForm) {
                    holder.pointView.visibility = View.GONE
                } else {
                    holder.pointView.visibility = View.VISIBLE
                    holder.pointView.text = getScoreText(context, assessment?.points ?: 0.0, criterion.points, true, criterion)
                }

            } else {
                val rating = (rubricItem as RubricRatingItem).rating
                val isGrade = assessment != null && isRubricGraded(assessment, rubricItem, criterion)
                var color = getColor(isGrade, criterion, rating, context, canvasContext)

                holder.descriptionView.text = rating.description
                holder.descriptionView.setTextColor(color)
                holder.pointView.text = getScoreText(context, rating.points, rating.points, isFreeForm, criterion, rating)
                holder.pointView.setTextColor(color)
                // set the content description
                holder.pointView.contentDescription = getScoreText(context, rating.points, rating.points, isFreeForm, criterion, rating, true)

                // Make it bold if it's the grade
                holder.pointView.setTypeface(null, if(isGrade) Typeface.BOLD else Typeface.NORMAL)
                holder.descriptionView.setTypeface(null, if(isGrade) Typeface.BOLD else Typeface.NORMAL)

                holder.checkmark.setBackgroundColor(color)
            }
        }

        private fun getColor(isGrade: Boolean, criterion: RubricCriterion, rating: RubricCriterionRating, context: Context, canvasContext: CanvasContext): Int {
            return if (isGrade) {
                // if we're using rubric range, we only want the points summary to be the course color
                if ((criterion.criterionUseRange && rating.id?.contains("null") ?: false) || !criterion.criterionUseRange) {
                    ColorKeeper.getOrGenerateColor(canvasContext)
                } else {
                    ContextCompat.getColor(context, R.color.canvasTextMedium)
                }
            } else ContextCompat.getColor(context, R.color.canvasTextMedium)
        }

        /*
         * We need to check which criterion needs to be highlighted so the user can know how they did
         */
        private fun isRubricGraded(assessment: RubricCriterionAssessment, ratingItem: RubricRatingItem, criterion: RubricCriterion) : Boolean {
            if (criterion.criterionUseRange && assessment.points != null) {

                // check if it's the total points row that we added
                if (ratingItem.rating.id?.contains("null") ?: false) {
                    return true
                }

                val index = criterion.ratings.indexOfFirst { rubricCriterionRating -> rubricCriterionRating.points == ratingItem.rating.points } + 1

                if (index >= 1 && index < criterion.ratings.size) {
                    val firstPoints = criterion.ratings[index - 1].points
                    val secondPoints = criterion.ratings[index].points
                    // check to see if the current points is in the range of the criterion
                    return (assessment.points!! <= firstPoints && assessment.points!! > secondPoints)
                } else {

                    // if it's less than the last rubric score, the last criterion needs to be highlighted
                    return (assessment.points!! <= criterion.ratings[criterion.ratings.size - 1].points)
                }
            } else {
                return assessment.points == ratingItem.rating.points
            }
        }

        private fun getScoreText(context: Context, value: Double, maxValue: Double, isFreeForm: Boolean, criterion: RubricCriterion, rating: RubricCriterionRating? = null, isContentDesription: Boolean = false): String {
            var points = ""
            val format = DecimalFormat("0.#")

            when {
                isFreeForm -> {
                    points = String.format(Locale.getDefault(),
                            context.getString(R.string.freeFormRubricPoints),
                            format.format(value),
                            format.format(maxValue))
                }
                Math.floor(value) == value -> points += value.toInt()
                else -> points += value
            }

            if (criterion.criterionUseRange) {

                // check if it's the total points row that we added
                if (rating?.id?.contains("null") ?: false) {
                    // get total points for criterion
                    var criterionPoints = criterion.ratings[0].points
                    if (isContentDesription) {
                        return String.format(Locale.getDefault(),
                                context.getString(R.string.contentDescriptionRubricRangeGrade),
                                points,
                                format.format(criterionPoints))
                    } else {
                        return String.format(Locale.getDefault(),
                                context.getString(R.string.rangedRubricTotal),
                                points,
                                format.format(criterionPoints))
                    }
                }

                // get the current index of the rating
                var index = criterion.ratings.indexOfFirst { rubricCriterionRating -> rubricCriterionRating.points == maxValue } + 1

                if (index >= 1 && index < criterion.ratings.size) {
                    var nextPoints =""
                    val criterionPoints = criterion.ratings[index].points
                    if(Math.floor(criterionPoints) == criterionPoints) nextPoints += criterionPoints.toInt()
                    if (isContentDesription) {
                        return String.format(Locale.getDefault(),
                                context.getString(R.string.contentDescriptionRubricRangePointsText),
                                points,
                                format.format(criterionPoints))
                    } else {
                        points += String.format(Locale.getDefault(),
                                context.getString(R.string.rubricRangePointsText),
                                format.format(criterionPoints))
                    }
                } else {

                    // add ' > 0 pts' on the end
                    if (isContentDesription) {
                        return String.format(Locale.getDefault(),
                                context.getString(R.string.contentDescriptionRubricRangePointsText),
                                points,
                                format.format(0))
                    } else {
                        points += String.format(Locale.getDefault(),
                                context.getString(R.string.rubricRangePointsText),
                                format.format(0))
                    }
                }
            } else {
                points = String.format(Locale.getDefault(),
                        context.getString(R.string.totalPoints), points)
            }
            return points
        }
    }
}
