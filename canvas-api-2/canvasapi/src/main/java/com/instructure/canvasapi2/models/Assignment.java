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

import android.content.Context;
import android.os.Parcel;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.instructure.canvasapi2.R;
import com.instructure.canvasapi2.utils.APIHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class Assignment extends CanvasModel<Assignment> {

    public enum SUBMISSION_TYPE {ONLINE_QUIZ, NONE, ON_PAPER, DISCUSSION_TOPIC, EXTERNAL_TOOL, BASIC_LTI_LAUNCH, ONLINE_UPLOAD, ONLINE_TEXT_ENTRY, ONLINE_URL, MEDIA_RECORDING, ATTENDANCE, NOT_GRADED}
    public enum GRADING_TYPE {PASS_FAIL, PERCENT, LETTER_GRADE, POINTS, GPA_SCALE, NOT_GRADED}
    public enum TURN_IN_TYPE {ONLINE, ON_PAPER, NONE, DISCUSSION, QUIZ, EXTERNAL_TOOL}
    public static final SUBMISSION_TYPE[] ONLINE_SUBMISSIONS = {SUBMISSION_TYPE.ONLINE_UPLOAD, SUBMISSION_TYPE.ONLINE_URL, SUBMISSION_TYPE.ONLINE_TEXT_ENTRY, SUBMISSION_TYPE.MEDIA_RECORDING};

    public static final String PASS_FAIL_TYPE = "pass_fail";
    public static final String PERCENT_TYPE = "percent";
    public static final String LETTER_GRADE_TYPE = "letter_grade";
    public static final String POINTS_TYPE = "points";
    public static final String GPA_SCALE_TYPE = "gpa_scale";
    public static final String NOT_GRADED_TYPE = "not_graded";

    // Canvas Assignment ID
    public static final String CANVAS_ASSIGNMENTS = "assignments";

    // Xinics Assignment Group ID.
    public static final String COURSE_BUILDER = "courseBuilder";
    public static final String COURSE_RESOURCE = "LectureResource";
    public static final String OPEN_BOARD = "openBoard";
    public static final String QNA_BOARD = "qnaBoard";
    public static final String CLASS_MIX = "classMix";
    public static final String ONLINE_ATTENDANCE = "onlineAttendance";

    private long id;
    private String name;
    private String description;
    @SerializedName("submission_types")
    private List<String> submissionTypes = new ArrayList<>();
    @SerializedName("due_at")
    private String dueAt;
    @SerializedName("points_possible")
    private double pointsPossible;
    @SerializedName("course_id")
    private long courseId;
    @SerializedName("grade_group_students_individually")
    private boolean isGradeGroupsIndividually;
    @SerializedName("grading_type")
    private String gradingType;
    @SerializedName("needs_grading_count")
    private long needsGradingCount;
    @SerializedName("html_url")
    private String htmlUrl;
    private String url;
    @SerializedName("quiz_id")
    private long quizId; // (Optional) id of the associated quiz (applies only when submission_types is ["online_quiz"])
    private List<RubricCriterion> rubric = new ArrayList<>();
    @SerializedName("use_rubric_for_grading")
    private boolean useRubricForGrading;
    @SerializedName("rubric_settings")
    private RubricSettings rubricSettings;
    @SerializedName("allowed_extensions")
    private List<String> allowedExtensions = new ArrayList<>();
    private Submission submission;
    @SerializedName("assignment_group_id")
    private long assignmentGroupId;
    private int position;
    @SerializedName("peer_reviews")
    private boolean peerReviews;
    @SerializedName("lock_info") //Module lock info
    private LockInfo lockInfo;
    @SerializedName("locked_for_user")
    private boolean lockedForUser;
    @SerializedName("lock_at")
    private String lockAt; //Date the teacher no longer accepts submissions.
    @SerializedName("unlock_at")
    private String unlockAt;
    @SerializedName("lock_explanation")
    private String lockExplanation;
    @SerializedName("discussion_topic")
    private DiscussionTopicHeader discussionTopic;
    @SerializedName("needs_grading_count_by_section")
    private List<NeedsGradingCount> needsGradingCountBySection = new ArrayList<>();
    @SerializedName("free_form_criterion_comments")
    private boolean freeFormCriterionComments;
    private boolean published;
    private boolean muted;
    @SerializedName("group_category_id")
    private long groupCategoryId;
    @SerializedName("all_dates")
    private List<AssignmentDueDate> allDates = new ArrayList<>();
    @SerializedName("user_submitted")
    private boolean userSubmitted;
    private boolean unpublishable;
    private List<AssignmentOverride> overrides;
    @SerializedName("only_visible_to_overrides")
    private boolean isOnlyVisibleToOverrides;
    @SerializedName("anonymous_peer_reviews")
    private boolean anonymousPeerReviews;
    @SerializedName("moderated_grading")
    private boolean isModeratedGrading;

    @Override
    public long getId() {
        return id;
    }

    @Nullable
    @Override
    public Date getComparisonDate() {
        return getDueAt();
    }

    @Nullable
    @Override
    public String getComparisonString() {
        return dueAt;
    }

    public Submission getLastActualSubmission() {
        if(submission == null) {
            return null;
        }
        if(submission.getWorkflowState() != null && submission.getWorkflowState().equals("submitted")) {
            return submission;
        }
        else {
            return null;
        }
    }

    private boolean expectsSubmissions() {
        List<SUBMISSION_TYPE> submissionTypes = getSubmissionTypes();
        return submissionTypes.size() > 0 && !submissionTypes.contains(SUBMISSION_TYPE.NONE) && !submissionTypes.contains(SUBMISSION_TYPE.NOT_GRADED) && !submissionTypes.contains(SUBMISSION_TYPE.ON_PAPER) && !submissionTypes.contains(SUBMISSION_TYPE.EXTERNAL_TOOL);
    }

    public boolean isAllowedToSubmit() {
        List<SUBMISSION_TYPE> submissionTypes = getSubmissionTypes();
        return expectsSubmissions() && !isLockedForUser() && !submissionTypes.contains(SUBMISSION_TYPE.ONLINE_QUIZ) && !submissionTypes.contains(SUBMISSION_TYPE.ATTENDANCE);
    }

    public boolean isWithoutGradedSubmission() {
        Submission submission = getLastActualSubmission();
        return submission == null || submission.isWithoutGradedSubmission();
    }

    public boolean isOnlineSubmissionType() {
        return  submissionTypes.contains(Assignment.SUBMISSION_TYPE.ONLINE_TEXT_ENTRY.name().toLowerCase(Locale.getDefault())) ||
                submissionTypes.contains(Assignment.SUBMISSION_TYPE.ONLINE_URL.name().toLowerCase(Locale.getDefault())) ||
                submissionTypes.contains(Assignment.SUBMISSION_TYPE.MEDIA_RECORDING.name().toLowerCase(Locale.getDefault())) ||
                submissionTypes.contains(Assignment.SUBMISSION_TYPE.ONLINE_UPLOAD.name().toLowerCase(Locale.getDefault())) ||
                submissionTypes.contains(Assignment.SUBMISSION_TYPE.ONLINE_QUIZ.name().toLowerCase(Locale.getDefault())) ||
                submissionTypes.contains(Assignment.SUBMISSION_TYPE.EXTERNAL_TOOL.name().toLowerCase(Locale.getDefault())) ||
                submissionTypes.contains(Assignment.SUBMISSION_TYPE.BASIC_LTI_LAUNCH.name().toLowerCase(Locale.getDefault())) ||
                submissionTypes.contains(Assignment.SUBMISSION_TYPE.DISCUSSION_TOPIC.name().toLowerCase(Locale.getDefault()));
    }

    public TURN_IN_TYPE getTurnInType(){
        return turnInTypeFromSubmissionType(getSubmissionTypes());
    }

    public static TURN_IN_TYPE stringToTurnInType(String turnInType, Context context){
        if(turnInType == null){
            return null;
        }

        if(turnInType.equals(context.getString(R.string.canvasAPI_online))){
            return TURN_IN_TYPE.ONLINE;
        } else if(turnInType.equals(context.getString(R.string.canvasAPI_onPaper))){
            return TURN_IN_TYPE.ON_PAPER;
        } else if(turnInType.equals(context.getString(R.string.canvasAPI_discussion))){
            return TURN_IN_TYPE.DISCUSSION;
        } else if(turnInType.equals(context.getString(R.string.canvasAPI_quiz))){
            return TURN_IN_TYPE.QUIZ;
        } else if(turnInType.equals(context.getString(R.string.canvasAPI_externalTool))){
            return TURN_IN_TYPE.EXTERNAL_TOOL;
        } else{
            return TURN_IN_TYPE.NONE;
        }
    }

    public static String turnInTypeToPrettyPrintString(TURN_IN_TYPE turnInType, Context context){
        if(turnInType == null){
            return null;
        }

        switch (turnInType){
            case ONLINE:
                return context.getString(R.string.canvasAPI_online);
            case ON_PAPER:
                return context.getString(R.string.canvasAPI_onPaper);
            case NONE:
                return context.getString(R.string.canvasAPI_none);
            case DISCUSSION:
                return context.getString(R.string.canvasAPI_discussion);
            case QUIZ:
                return context.getString(R.string.canvasAPI_quiz);
            case EXTERNAL_TOOL:
                return context.getString(R.string.canvasAPI_externalTool);
            default:
                return null;
        }
    }

    private TURN_IN_TYPE turnInTypeFromSubmissionType(List<SUBMISSION_TYPE> submissionTypes){

        if(submissionTypes == null || submissionTypes.size() == 0){
            return TURN_IN_TYPE.NONE;
        }

        SUBMISSION_TYPE submissionType = submissionTypes.get(0);

        if(submissionType == SUBMISSION_TYPE.MEDIA_RECORDING || submissionType == SUBMISSION_TYPE.ONLINE_TEXT_ENTRY ||
                submissionType == SUBMISSION_TYPE.ONLINE_URL || submissionType == SUBMISSION_TYPE.ONLINE_UPLOAD ){
            return TURN_IN_TYPE.ONLINE;
        }else if(submissionType == SUBMISSION_TYPE.ONLINE_QUIZ){
            return TURN_IN_TYPE.QUIZ;
        }else if(submissionType == SUBMISSION_TYPE.DISCUSSION_TOPIC){
            return TURN_IN_TYPE.DISCUSSION;
        }else if(submissionType == SUBMISSION_TYPE.ON_PAPER){
            return TURN_IN_TYPE.ON_PAPER;
        }else if(submissionType == SUBMISSION_TYPE.EXTERNAL_TOOL){
            return TURN_IN_TYPE.EXTERNAL_TOOL;
        }

        return TURN_IN_TYPE.NONE;
    }

    public boolean isLocked() {
        Date currentDate = new Date();
        if(getLockInfo() == null || getLockInfo().isEmpty()) {
            return false;
        } else if(getLockInfo().getLockedModuleName() != null && getLockInfo().getLockedModuleName().length() > 0 && !getLockInfo().getLockedModuleName().equals("null")) {
            return true;
        } else if(getLockInfo().getUnlockAt() != null && getLockInfo().getUnlockAt().after(currentDate)){
            return true;
        }
        return false;

    }

    public void populateScheduleItem(ScheduleItem scheduleItem) {
        scheduleItem.setId(this.getId());
        scheduleItem.setTitle(this.getName());
        scheduleItem.setStartAt(this.getDueAt());
        scheduleItem.setItemType(ScheduleItem.Type.TYPE_ASSIGNMENT);
        scheduleItem.setDescription(this.getDescription());
        scheduleItem.setSubmissionTypes(getSubmissionTypes());
        scheduleItem.setPointsPossible(this.getPointsPossible());
        scheduleItem.setHtmlUrl(this.getHtmlUrl());
        scheduleItem.setQuizId(this.getQuizId());
        scheduleItem.setDiscussionTopicHeader(this.getDiscussionTopicHeader());
        scheduleItem.setAssignment(this);
        if(getLockInfo() != null && getLockInfo().getLockedModuleName() != null) {
            scheduleItem.setLockedModuleName(this.getLockInfo().getLockedModuleName());
        }
    }

    public ScheduleItem toScheduleItem() {
        ScheduleItem scheduleItem = new ScheduleItem();

        populateScheduleItem(scheduleItem);

        return scheduleItem;
    }

    public boolean hasRubric() {
        if (rubric == null) {
            return false;
        }
        return rubric.size() > 0;
    }

    //region Helpers

    public static SUBMISSION_TYPE getSubmissionTypeFromAPIString(String submissionType){
        if (submissionType == null) {
            return null;
        } else if (submissionType.equals("online_quiz")){
            return SUBMISSION_TYPE.ONLINE_QUIZ;
        } else if(submissionType.equals("none")){
            return SUBMISSION_TYPE.NONE;
        } else if(submissionType.equals("on_paper")){
            return SUBMISSION_TYPE.ON_PAPER;
        } else if(submissionType.equals("discussion_topic")){
            return SUBMISSION_TYPE.DISCUSSION_TOPIC;
        } else if(submissionType.equals("external_tool")){
            return SUBMISSION_TYPE.EXTERNAL_TOOL;
        } else if(submissionType.equals("basic_lti_launch")){
            return SUBMISSION_TYPE.BASIC_LTI_LAUNCH;
        } else if(submissionType.equals("online_upload")){
            return SUBMISSION_TYPE.ONLINE_UPLOAD;
        } else if(submissionType.equals("online_text_entry")){
            return SUBMISSION_TYPE.ONLINE_TEXT_ENTRY;
        } else if(submissionType.equals("online_url")){
            return SUBMISSION_TYPE.ONLINE_URL;
        } else if(submissionType.equals("media_recording")){
            return SUBMISSION_TYPE.MEDIA_RECORDING;
        } else if(submissionType.equals("attendance")) {
            return SUBMISSION_TYPE.ATTENDANCE;
        } else if(submissionType.equals("not_graded")) {
            return SUBMISSION_TYPE.NOT_GRADED;
        } else {
            return null;
        }
    }
    public static String submissionTypeToAPIString(SUBMISSION_TYPE submissionType){

        if(submissionType == null){
            return null;
        }

        switch (submissionType){
            case  ONLINE_QUIZ:
                return "online_quiz";
            case NONE:
                return "none";
            case ON_PAPER:
                return "on_paper";
            case DISCUSSION_TOPIC:
                return "discussion_topic";
            case EXTERNAL_TOOL:
                return "external_tool";
            case BASIC_LTI_LAUNCH:
                return "basic_lti_launch";
            case ONLINE_UPLOAD:
                return "online_upload";
            case ONLINE_TEXT_ENTRY:
                return "online_text_entry";
            case ONLINE_URL:
                return "online_url";
            case MEDIA_RECORDING:
                return "media_recording";
            case ATTENDANCE:
                return "attendance";
            case NOT_GRADED:
                return "not_graded";
            default:
                return "";
        }
    }
    public static String submissionTypeToPrettyPrintString(SUBMISSION_TYPE submissionType, Context context){

        if(submissionType == null){
            return null;
        }

        switch (submissionType){
            case  ONLINE_QUIZ:
                return context.getString(R.string.canvasAPI_onlineQuiz);
            case NONE:
                return context.getString(R.string.canvasAPI_none);
            case ON_PAPER:
                return context.getString(R.string.canvasAPI_onPaper);
            case DISCUSSION_TOPIC:
                return context.getString(R.string.canvasAPI_discussionTopic);
            case EXTERNAL_TOOL:
            case BASIC_LTI_LAUNCH:
                return context.getString(R.string.canvasAPI_externalTool);
            case ONLINE_UPLOAD:
                return context.getString(R.string.canvasAPI_onlineUpload);
            case ONLINE_TEXT_ENTRY:
                return context.getString(R.string.canvasAPI_onlineTextEntry);
            case ONLINE_URL:
                return context.getString(R.string.canvasAPI_onlineURL);
            case MEDIA_RECORDING:
                return context.getString(R.string.canvasAPI_mediaRecording);
            case ATTENDANCE:
                return context.getString(R.string.canvasAPI_attendance);
            case NOT_GRADED:
                return context.getString(R.string.canvasAPI_notGraded);
            default:
                return "";
        }
    }

    public static GRADING_TYPE getGradingTypeFromString(String gradingType, Context context){
        if(gradingType.equals("pass_fail") || gradingType.equals(context.getString(R.string.canvasAPI_passFail))){
            return GRADING_TYPE.PASS_FAIL;
        } else if(gradingType.equals("percent") || gradingType.equals(context.getString(R.string.canvasAPI_percent))){
            return GRADING_TYPE.PERCENT;
        } else if(gradingType.equals("letter_grade") || gradingType.equals(context.getString(R.string.canvasAPI_letterGrade))){
            return GRADING_TYPE.LETTER_GRADE;
        } else if (gradingType.equals("points") || gradingType.equals(context.getString(R.string.canvasAPI_points))){
            return GRADING_TYPE.POINTS;
        } else if (gradingType.equals("gpa_scale") || gradingType.equals(context.getString(R.string.canvasAPI_gpaScale))){
            return GRADING_TYPE.GPA_SCALE;
        } else if(gradingType.equals("not_graded") || gradingType.equals(context.getString(R.string.canvasAPI_notGraded))){
            return GRADING_TYPE.NOT_GRADED;
        }else {
            return null;
        }
    }

    public static GRADING_TYPE getGradingTypeFromAPIString(String gradingType) {
        if ("pass_fail".equals(gradingType)) {
            return GRADING_TYPE.PASS_FAIL;
        } else if ("percent".equals(gradingType)) {
            return GRADING_TYPE.PERCENT;
        } else if ("letter_grade".equals(gradingType)) {
            return GRADING_TYPE.LETTER_GRADE;
        } else if ("points".equals(gradingType)) {
            return GRADING_TYPE.POINTS;
        } else if ("gpa_scale".equals(gradingType)) {
            return GRADING_TYPE.GPA_SCALE;
        } else if ("not_graded".equals(gradingType)) {
            return GRADING_TYPE.NOT_GRADED;
        } else {
            return null;
        }
    }

    public  static String gradingTypeToAPIString(GRADING_TYPE gradingType){
        if(gradingType == null){ return null;}

        switch (gradingType){
            case PASS_FAIL:
                return "pass_fail";
            case PERCENT:
                return "percent";
            case LETTER_GRADE:
                return "letter_grade";
            case POINTS:
                return "points";
            case GPA_SCALE:
                return "gpa_scale";
            case NOT_GRADED:
                return "not_graded";
            default:
                return "";
        }
    }

    public  static String gradingTypeToPrettyPrintString(String gradingType, Context context){
        return gradingTypeToPrettyPrintString(getGradingTypeFromAPIString(gradingType), context);
    }

    public  static String gradingTypeToPrettyPrintString(GRADING_TYPE gradingType, Context context){
        if(gradingType == null){ return null;}

        switch (gradingType){
            case PASS_FAIL:
                return context.getString(R.string.canvasAPI_passFail);
            case PERCENT:
                return context.getString(R.string.canvasAPI_percent);
            case LETTER_GRADE:
                return context.getString(R.string.canvasAPI_letterGrade);
            case POINTS:
                return context.getString(R.string.canvasAPI_points);
            case GPA_SCALE:
                return context.getString(R.string.canvasAPI_gpaScale);
            case NOT_GRADED:
                return context.getString(R.string.canvasAPI_notGraded);
            default:
                return "";
        }
    }

    //endregion

    //region Getters

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getSubmissionAPITypes() {
        if(submissionTypes == null) {
            return new ArrayList<>();
        }
        return submissionTypes;
    }

    public List<SUBMISSION_TYPE> getSubmissionTypes() {
        if(submissionTypes == null) {
            return new ArrayList<>();
        }

        List<SUBMISSION_TYPE>   submissionTypeList = new ArrayList<>();

        for(String submissionType : submissionTypes){
            submissionTypeList.add(getSubmissionTypeFromAPIString(submissionType));
        }

        return submissionTypeList;
    }

    public @Nullable Date getDueAt() {
        return APIHelper.stringToDate(dueAt);
    }

    public double getPointsPossible() {
        return pointsPossible;
    }

    public long getCourseId() {
        return courseId;
    }

    public boolean isGradeGroupsIndividually() {
        return isGradeGroupsIndividually;
    }

    public String getGradingType() {
        return gradingType;
    }

    public long getNeedsGradingCount() {
        return needsGradingCount;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public String getUrl() {
        return url;
    }

    public long getQuizId() {
        return quizId;
    }

    public List<RubricCriterion> getRubric() {
        return rubric;
    }

    public boolean isUseRubricForGrading() {
        return useRubricForGrading;
    }

    @Nullable
    public RubricSettings getRubricSettings() {
        return rubricSettings;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public Submission getSubmission() {
        return submission;
    }

    public long getAssignmentGroupId() {
        return assignmentGroupId;
    }

    public int getPosition() {
        return position;
    }

    public boolean hasPeerReviews() {
        return peerReviews;
    }

    public LockInfo getLockInfo() {
        return lockInfo;
    }

    public boolean isLockedForUser() {
        return lockedForUser;
    }

    public @Nullable Date getLockAt() {
        return APIHelper.stringToDate(lockAt);
    }

    public @Nullable Date getUnlockAt() {
        return APIHelper.stringToDate(unlockAt);
    }

    public String getLockExplanation() {
        return lockExplanation;
    }

    public DiscussionTopicHeader getDiscussionTopicHeader() {
        return discussionTopic;
    }

    public List<NeedsGradingCount> getNeedsGradingCountBySection() {
        return needsGradingCountBySection;
    }

    public boolean isFreeFormCriterionComments() {
        return freeFormCriterionComments;
    }

    public boolean isPublished() {
        return published;
    }

    public boolean isMuted() {
        return muted;
    }

    public long getGroupCategoryId() {
        return groupCategoryId;
    }

    public List<AssignmentDueDate> getAllDates() {
        return allDates;
    }

    public boolean hasUserSubmitted() {
        return userSubmitted;
    }

    public boolean isUnpublishable() {
        return unpublishable;
    }

    public void setUnpublishable(boolean unpublishable) {
        this.unpublishable = unpublishable;
    }

    @Nullable
    public List<AssignmentOverride> getOverrides() {
        return overrides;
    }

    public boolean isOnlyVisibleToOverrides() {
        return isOnlyVisibleToOverrides;
    }

    public boolean isPeerReviews() {
        return peerReviews;
    }

    public boolean isAnonymousPeerReviews() {
        return anonymousPeerReviews;
    }

    public boolean isModeratedGrading() {
        return isModeratedGrading;
    }

    //endregion

    //region Setters

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSubmissionTypes(List<String> submissionTypes) {
        this.submissionTypes = submissionTypes;
    }

    public void setSubmissionTypes(SUBMISSION_TYPE[] submissionTypes){
        if(submissionTypes == null){
            return;
        }

        ArrayList<String> listSubmissionTypes = new ArrayList<String>();

        for(SUBMISSION_TYPE submissionType: submissionTypes){
            listSubmissionTypes.add(submissionTypeToAPIString(submissionType));
        }

        setSubmissionTypes(listSubmissionTypes);
    }

    public void setDueAt(String dueAt) {
        this.dueAt = dueAt;
    }

    public void setPointsPossible(double pointsPossible) {
        this.pointsPossible = pointsPossible;
    }

    public void setCourseId(long courseId) {
        this.courseId = courseId;
    }

    public void setGradeGroupsIndividually(boolean gradeGroupsIndividually) {
        isGradeGroupsIndividually = gradeGroupsIndividually;
    }

    public void setGradingType(String gradingType) {
        this.gradingType = gradingType;
    }

    public void setGradingType(GRADING_TYPE grading_type) {
        this.gradingType = gradingTypeToAPIString(grading_type);
    }

    public void setNeedsGradingCount(long needsGradingCount) {
        this.needsGradingCount = needsGradingCount;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setQuizId(long quizId) {
        this.quizId = quizId;
    }

    public void setRubric(List<RubricCriterion> rubric) {
        this.rubric = rubric;
    }

    public void setUseRubricForGrading(boolean useRubricForGrading) {
        this.useRubricForGrading = useRubricForGrading;
    }

    public void setRubricSettings(@Nullable RubricSettings rubricSettings) {
        this.rubricSettings = rubricSettings;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public void setSubmission(Submission submission) {
        this.submission = submission;
    }

    public void setAssignmentGroupId(long assignmentGroupId) {
        this.assignmentGroupId = assignmentGroupId;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setPeerReviews(boolean peerReviews) {
        this.peerReviews = peerReviews;
    }

    public void setLockInfo(LockInfo lockInfo) {
        this.lockInfo = lockInfo;
    }

    public void setLockedForUser(boolean lockedForUser) {
        this.lockedForUser = lockedForUser;
    }

    public void setLockAt(String lockAt) {
        this.lockAt = lockAt;
    }

    public void setUnlockAt(String unlockAt) {
        this.unlockAt = unlockAt;
    }

    public void setLockExplanation(String lockExplanation) {
        this.lockExplanation = lockExplanation;
    }

    public void setDiscussionTopic(DiscussionTopicHeader discussionTopic) {
        this.discussionTopic = discussionTopic;
    }

    public void setNeedsGradingCountBySection(List<NeedsGradingCount> needsGradingCountBySection) {
        this.needsGradingCountBySection = needsGradingCountBySection;
    }

    public void setFreeFormCriterionComments(boolean freeFormCriterionComments) {
        this.freeFormCriterionComments = freeFormCriterionComments;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public void setGroupCategoryId(long groupCategoryId) {
        this.groupCategoryId = groupCategoryId;
    }

    public void setAllDates(List<AssignmentDueDate> allDates) {
        this.allDates = allDates;
    }

    public void setUserSubmitted(boolean userSubmitted) {
        this.userSubmitted = userSubmitted;
    }

    public void setOverrides(List<AssignmentOverride> overrides) {
        this.overrides = overrides;
    }

    public void setOnlyVisibleToOverrides(boolean onlyVisibleToOverrides) {
        isOnlyVisibleToOverrides = onlyVisibleToOverrides;
    }

    public void setAnonymousPeerReviews(boolean anonymousPeerReviews) {
        this.anonymousPeerReviews = anonymousPeerReviews;
    }

    public void setModeratedGrading(boolean moderatedGrading) {
        isModeratedGrading = moderatedGrading;
    }

    //endregion

    //region Parcelable

    public Assignment() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.name);
        dest.writeString(this.description);
        dest.writeStringList(this.submissionTypes);
        dest.writeString(this.dueAt);
        dest.writeDouble(this.pointsPossible);
        dest.writeLong(this.courseId);
        dest.writeByte(this.isGradeGroupsIndividually ? (byte) 1 : (byte) 0);
        dest.writeString(this.gradingType);
        dest.writeLong(this.needsGradingCount);
        dest.writeString(this.htmlUrl);
        dest.writeString(this.url);
        dest.writeLong(this.quizId);
        dest.writeTypedList(this.rubric);
        dest.writeByte(this.useRubricForGrading ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.rubricSettings, flags);
        dest.writeStringList(this.allowedExtensions);
        dest.writeParcelable(this.submission, flags);
        dest.writeLong(this.assignmentGroupId);
        dest.writeInt(this.position);
        dest.writeByte(this.peerReviews ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.lockInfo, flags);
        dest.writeByte(this.lockedForUser ? (byte) 1 : (byte) 0);
        dest.writeString(this.lockAt);
        dest.writeString(this.unlockAt);
        dest.writeString(this.lockExplanation);
        dest.writeParcelable(this.discussionTopic, flags);
        dest.writeTypedList(this.needsGradingCountBySection);
        dest.writeByte(this.freeFormCriterionComments ? (byte) 1 : (byte) 0);
        dest.writeByte(this.published ? (byte) 1 : (byte) 0);
        dest.writeByte(this.muted ? (byte) 1 : (byte) 0);
        dest.writeLong(this.groupCategoryId);
        dest.writeTypedList(this.allDates);
        dest.writeByte(this.userSubmitted ? (byte) 1 : (byte) 0);
        dest.writeByte(unpublishable ? (byte) 1 : (byte) 0);
        dest.writeTypedList(this.overrides);
        dest.writeByte(this.isOnlyVisibleToOverrides ? (byte) 1 : (byte) 0);
        dest.writeByte(this.anonymousPeerReviews ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isModeratedGrading ? (byte) 1 : (byte) 0);
    }

    protected Assignment(Parcel in) {
        this.id = in.readLong();
        this.name = in.readString();
        this.description = in.readString();
        this.submissionTypes = in.createStringArrayList();
        this.dueAt = in.readString();
        this.pointsPossible = in.readDouble();
        this.courseId = in.readLong();
        this.isGradeGroupsIndividually = in.readByte() != 0;
        this.gradingType = in.readString();
        this.needsGradingCount = in.readLong();
        this.htmlUrl = in.readString();
        this.url = in.readString();
        this.quizId = in.readLong();
        this.rubric = in.createTypedArrayList(RubricCriterion.CREATOR);
        this.useRubricForGrading = in.readByte() != 0;
        this.rubricSettings = in.readParcelable(RubricSettings.class.getClassLoader());
        this.allowedExtensions = in.createStringArrayList();
        this.submission = in.readParcelable(Submission.class.getClassLoader());
        this.assignmentGroupId = in.readLong();
        this.position = in.readInt();
        this.peerReviews = in.readByte() != 0;
        this.lockInfo = in.readParcelable(LockInfo.class.getClassLoader());
        this.lockedForUser = in.readByte() != 0;
        this.lockAt = in.readString();
        this.unlockAt = in.readString();
        this.lockExplanation = in.readString();
        this.discussionTopic = in.readParcelable(DiscussionTopicHeader.class.getClassLoader());
        this.needsGradingCountBySection = in.createTypedArrayList(NeedsGradingCount.CREATOR);
        this.freeFormCriterionComments = in.readByte() != 0;
        this.published = in.readByte() != 0;
        this.muted = in.readByte() != 0;
        this.groupCategoryId = in.readLong();
        this.allDates = in.createTypedArrayList(AssignmentDueDate.CREATOR);
        this.userSubmitted = in.readByte() != 0;
        this.unpublishable = in.readByte() != 0;
        this.overrides = in.createTypedArrayList(AssignmentOverride.CREATOR);
        this.isOnlyVisibleToOverrides = in.readByte() != 0;
        this.anonymousPeerReviews = in.readByte() != 0;
        this.isModeratedGrading = in.readByte() != 0;
    }

    public static final Creator<Assignment> CREATOR = new Creator<Assignment>() {
        @Override
        public Assignment createFromParcel(Parcel source) {
            return new Assignment(source);
        }

        @Override
        public Assignment[] newArray(int size) {
            return new Assignment[size];
        }
    };

    //endregion
}
