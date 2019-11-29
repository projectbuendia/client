/*
 * Copyright (C) 2011 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.views;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.IFormElement;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.exception.ExternalParamsException;
import org.odk.collect.android.exception.JavaRosaException;
import org.odk.collect.android.external.ExternalAppsUtils;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.model.Preset;
import org.odk.collect.android.utilities.Utils;
import org.odk.collect.android.widgets.IBinaryWidget;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets.WidgetFactory;
import org.odk.collect.android.widgets2.Widget2Factory;
import org.odk.collect.android.widgets2.common.Appearance;
import org.odk.collect.android.widgets2.group.WidgetGroup;
import org.odk.collect.android.widgets2.group.WidgetGroupBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is
 *
 * @author carlhartung
 */
public class ODKView extends LinearLayout {

	// starter random number for view IDs
    private final static int VIEW_ID = 12345;

    private final static String t = "ODKView";

    private LinearLayout mView;
    private LinearLayout.LayoutParams mLayout;
    private ArrayList<QuestionWidget> mWidgets;
    private WidgetGroup mWidgetGroup;
    private Handler h = null;

    public final static String FIELD_LIST = "field-list";

    public ODKView(Context context, final FormEntryPrompt[] questionPrompts,
            FormEntryCaption[] groups, boolean advancingPage) {
        this(context, questionPrompts, groups, advancingPage, null /*fields*/);
    }

    public ODKView(Context context, final FormEntryPrompt[] questionPrompts,
                   FormEntryCaption[] groups, boolean advancingPage, Preset preset) {
        super(context);

        mWidgets = new ArrayList<QuestionWidget>();

        mView = new LinearLayout(getContext());
        mView.setOrientation(LinearLayout.VERTICAL);
        mView.setGravity(Gravity.TOP);
        mView.setPadding(0, 7, 0, 0);

        mLayout =
            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
//        mLayout.setMargins(10, 0, 10, 0);

        // display which group you are in as well as the question

        addGroupText(groups);

        // when the grouped fields are populated by an external app, this will get true.
        boolean readOnlyOverride = false;

        if (groups != null && groups.length > 0) {
            WidgetGroupBuilder<?, ?> builder =
                    Widget2Factory.INSTANCE.createGroupBuilder(context, groups[groups.length - 1]);
            if (builder != null) {
                int id = 0;
                for (FormEntryPrompt p : questionPrompts) {
                    builder.createAndAddWidget(
                            context,
                            p,
                            Appearance.fromString(p.getAppearanceHint()),
                            readOnlyOverride,
                            VIEW_ID + id++,
                            preset);
                }
                mWidgetGroup = builder.build(context);
                mView.addView((View) mWidgetGroup);
                addView(mView);
                return;
            }
        }

        // get the group we are showing -- it will be the last of the groups in the groups list
        if (groups != null && groups.length > 0) {
            final FormEntryCaption c = groups[groups.length - 1];
            final String intentString = c.getFormElement().getAdditionalAttribute(null, "intent");
            if (intentString != null && intentString.length() != 0) {

                readOnlyOverride = true;

                final String buttonText;
                final String errorString;
                String v = c.getSpecialFormQuestionText("buttonText");
                buttonText = (v != null) ? v : context.getString(R.string.launch_app);
                v = c.getSpecialFormQuestionText("noAppErrorString");
                errorString = (v != null) ? v : context.getString(R.string.no_app);

                TableLayout.LayoutParams params = new TableLayout.LayoutParams();
                params.setMargins(7, 5, 7, 5);

                // set button formatting
                Button mLaunchIntentButton = new Button(getContext());
                mLaunchIntentButton.setId(QuestionWidget.newUniqueId());
                mLaunchIntentButton.setText(Utils.localize(buttonText, context));
                mLaunchIntentButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, Collect.getQuestionFontsize() + 2);
                mLaunchIntentButton.setPadding(20, 20, 20, 20);
                mLaunchIntentButton.setLayoutParams(params);

                mLaunchIntentButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String intentName = ExternalAppsUtils.extractIntentName(intentString);
                        Map<String, String> parameters = ExternalAppsUtils.extractParameters(intentString);

                        Intent i = new Intent(intentName);
                        try {
                            ExternalAppsUtils.populateParameters(i, parameters, c.getIndex().getReference());

                            for (FormEntryPrompt p : questionPrompts) {
                                IFormElement formElement = p.getFormElement();
                                if (formElement instanceof QuestionDef) {
                                    TreeReference reference = (TreeReference) formElement.getBind().getReference();
                                    IAnswerData answerValue = p.getAnswerValue();
                                    Object value = answerValue == null ? null : answerValue.getValue();
                                    switch (p.getDataType()) {
                                        case Constants.DATATYPE_TEXT:
                                        case Constants.DATATYPE_INTEGER:
                                        case Constants.DATATYPE_DECIMAL:
                                            i.putExtra(reference.getNameLast(), (Serializable) value);
                                            break;
                                    }
                                }
                            }

                            ((Activity) getContext()).startActivityForResult(i, FormEntryActivity.EX_GROUP_CAPTURE);
                        } catch (ExternalParamsException e) {
                            Log.e("ExternalParamsException", e.getMessage(), e);

                            Toast.makeText(getContext(),
                                    e.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                        } catch (ActivityNotFoundException e) {
                            Log.e("ActivityNotFoundException", e.getMessage(), e);

                            Toast.makeText(getContext(),
                                    errorString, Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });

                View divider = new View(getContext());
                divider.setBackgroundResource(R.drawable.divider);
                divider.setMinimumHeight((int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 9, getResources().getDisplayMetrics()));
                mView.addView(divider);

//                mView.addView(
//                        LayoutInflater.from(getContext()).inflate(R.layout.template_divider, null));

                mView.addView(mLaunchIntentButton, mLayout);
            }
        }

        boolean first = true;
        int id = 0;
        for (FormEntryPrompt p : questionPrompts) {
            if (!first) {

                View divider = new View(getContext());
                divider.setBackgroundResource(R.drawable.divider);
                divider.setMinimumHeight((int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 9, getResources().getDisplayMetrics()));
//                divider.setLay
//                mView.addView(
//                        LayoutInflater.from(getContext()).inflate(R.layout.template_divider, null));
                mView.addView(divider);
            } else {
                first = false;
            }

            // if question or answer type is not supported, use text widget
            QuestionWidget qw =
                    Widget2Factory.INSTANCE.create(getContext(), p, readOnlyOverride);
            if (qw == null) {
                qw = WidgetFactory.createWidgetFromPrompt(p, getContext(), readOnlyOverride);
            }

            qw.setId(VIEW_ID + id++);

            mWidgets.add(qw);

            // The encounter properties (encounter_datetime, location_id, and
            // provider_id) are special.  We preset them according to the values
            // passed to us, and hide them if they are preset.  In particular,
            // the location_id and provider_id widgets use UUIDs as the item
            // labels because we need to identify them in order to preselect
            // them -- so we should always hide the location and provider widgets.

            String refName = p.getIndex().getReference().getNameLast();
            if (refName.equals("encounter.encounter_datetime")) {
                if (preset.encounterDatetime != null) {
                    if (qw.forceSetAnswer(preset.encounterDatetime)) {
                        continue;
                    }
                }
            }
            if (refName.equals("encounter.location_id")) {
                preselect(qw, preset.locationUuid);
                continue;
            }
            if (refName.equals("encounter.provider_id")) {
                preselect(qw, preset.providerUuid);
                continue;
            }

            mView.addView(qw, mLayout);
        }

        addView(mView);

        // see if there is an autoplay option.
        // Only execute it during forward swipes through the form
        if ( advancingPage && mWidgets.size() == 1 ) {
	        final String playOption = mWidgets.get(0).getPrompt().getFormElement().getAdditionalAttribute(null, "autoplay");
	        if ( playOption != null ) {
	        	h = new Handler();
	        	h.postDelayed(new Runnable() {
						@Override
						public void run() {
				        	if ( playOption.equalsIgnoreCase("audio") ) {
				        		mWidgets.get(0).playAudio();
				        	} else if ( playOption.equalsIgnoreCase("video") ) {
				        		mWidgets.get(0).playVideo();
				        	}
						}
					}, 150);
	        }
        }
    }

    /**
     * Preselects the option with the given label text for the given
     * question.  If there is no matching option, preselects the
     * first option.  When this function returns, something will
     * be preselected.
     */
    protected void preselect(QuestionWidget widget, String label) {
        if (widget.forceSetAnswer(label)) return;

        // Couldn't find a match, so just preselect the first option.
        List<SelectChoice> choices = widget.getPrompt().getSelectChoices();
        if (choices.isEmpty()) {
            throw new IllegalStateException(String.format(
                "Form did not provide any choices for \"%s\"",
                widget.getPrompt().getShortText()
            ));
        }
        boolean success = widget.forceSetAnswer(choices.get(0).getLabelInnerText());
        if (!success) {
            throw new IllegalStateException(String.format(
                "\"%s\" widget failed to preselect its first choice",
                widget.getPrompt().getShortText()
            ));
        }
    }

    /**
     * http://code.google.com/p/android/issues/detail?id=8488
     */
    public void recycleDrawables() {
    	this.destroyDrawingCache();
    	mView.destroyDrawingCache();
    	for ( QuestionWidget q : mWidgets) {
    		q.recycleDrawables();
    	}
    }

    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
    	Collect.getInstance().getActivityLogger().logScrollAction(this, t - oldt);
    }

    /**
     * @return a HashMap of answers entered by the user for this set of widgets
     */
    public LinkedHashMap<FormIndex, IAnswerData> getAnswers() {
        LinkedHashMap<FormIndex, IAnswerData> answers = new LinkedHashMap<FormIndex, IAnswerData>();

        for (QuestionWidget widget : mWidgetGroup != null ? mWidgetGroup.getWidgets() : mWidgets) {
            FormEntryPrompt p = widget.getPrompt();
            answers.put(p.getIndex(), widget.getAnswer());
        }
//
//
//        Iterator<QuestionWidget> i = mWidgets.iterator();
//        while (i.hasNext()) {
//            /*
//             * The FormEntryPrompt has the FormIndex, which is where the answer gets stored. The
//             * QuestionWidget has the answer the user has entered.
//             */
//            QuestionWidget q = i.next();
//            FormEntryPrompt p = q.getPrompt();
//            answers.put(p.getIndex(), q.getAnswer());
//        }

        return answers;
    }


    /**
     * // * Add a TextView containing the hierarchy of groups to which the question belongs. //
     */
    private void addGroupText(FormEntryCaption[] groups) {
        StringBuilder s = new StringBuilder("");
        String t = "";
        int i;
        // list all groups in one string
        for (FormEntryCaption g : groups) {
            i = g.getMultiplicity() + 1;
            t = g.getLongText();
            if (t != null) {
                s.append(Utils.localize(t, getContext()));
                if (g.repeats() && i > 0) {
                    s.append(" (" + i + ")");
                }
                s.append(" > ");
            }

            // if any of the groups are invisible, ignore them (and all above)
            Appearance appearance = Appearance.fromString(g.getAppearanceHint());
            if (appearance.hasQualifier("invisible")) {
                s.setLength(0);
            }
        }

        // If any groups exist, add a TextView that contains the group name.
        if (s.length() > 0) {
            TextView tv = (TextView) LayoutInflater.from(getContext())
                    .inflate(R.layout.template_text_view_group, null);
            tv.setText(s.substring(0, s.length() - 3));
            if (tv.getText().length() > 0) mView.addView(tv, mLayout);
        }
    }


    public void setFocus(Context context) {
        if (mWidgets.size() > 0) {
            mWidgets.get(0).setFocus(context);
        }
    }


    /**
     * Called when another activity returns information to answer this question.
     *
     * @param answer
     */
    public void setBinaryData(Object answer) {
        boolean set = false;
        for (QuestionWidget q : mWidgets) {
            if (q instanceof IBinaryWidget) {
                if (((IBinaryWidget) q).isWaitingForBinaryData()) {
                    try {
                        ((IBinaryWidget) q).setBinaryData(answer);
                    } catch (Exception e) {
                        Log.e(t, e.getMessage(), e);
                        Toast.makeText(getContext(), getContext().getString(R.string.error_attaching_binary_file, e.getMessage()), Toast.LENGTH_LONG).show();
                    }
                    set = true;
                    break;
                }
            }
        }

        if (!set) {
            Log.w(t, "Attempting to return data to a widget or set of widgets not looking for data");
        }
    }

    public void setDataForFields(Bundle bundle) throws JavaRosaException {
        if (bundle == null) {
            return;
        }
        FormController formController = Collect.getInstance().getFormController();
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            for (QuestionWidget questionWidget : mWidgets) {
                FormEntryPrompt prompt = questionWidget.getPrompt();
                TreeReference treeReference = (TreeReference) prompt.getFormElement().getBind().getReference();
                if (treeReference.getNameLast().equals(key)) {

                    switch (prompt.getDataType()) {
                        case Constants.DATATYPE_TEXT:
                            formController.saveAnswer(prompt.getIndex(), ExternalAppsUtils.asStringData(bundle.get(key)));
                            break;
                        case Constants.DATATYPE_INTEGER:
                            formController.saveAnswer(prompt.getIndex(), ExternalAppsUtils.asIntegerData(bundle.get(key)));
                            break;
                        case Constants.DATATYPE_DECIMAL:
                            formController.saveAnswer(prompt.getIndex(), ExternalAppsUtils.asDecimalData(bundle.get(key)));
                            break;
                        default:
                            throw new RuntimeException(getContext().getString(R.string.ext_assign_value_error, treeReference.toString(false)));
                    }

                    break;
                }
            }
        }
    }

    public void cancelWaitingForBinaryData() {
        int count = 0;
        for (QuestionWidget q : mWidgets) {
            if (q instanceof IBinaryWidget) {
                if (((IBinaryWidget) q).isWaitingForBinaryData()) {
                    ((IBinaryWidget) q).cancelWaitingForBinaryData();
                    ++count;
                }
            }
        }

        if (count != 1) {
            Log.w(t, "Attempting to cancel waiting for binary data to a widget or set of widgets not looking for data");
        }
    }

    public boolean suppressFlingGesture(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        for (QuestionWidget q : mWidgets) {
        	if ( q.suppressFlingGesture(e1, e2, velocityX, velocityY) ) {
        		return true;
        	}
        }
        return false;
    }

    /**
     * @return true if the answer was cleared, false otherwise.
     */
    public boolean clearAnswer() {
        // If there's only one widget, clear the answer.
        // If there are more, then force a long-press to clear the answer.
        if (mWidgets.size() == 1 && !mWidgets.get(0).getPrompt().isReadOnly()) {
            mWidgets.get(0).clearAnswer();
            return true;
        } else {
            return false;
        }
    }


    public ArrayList<QuestionWidget> getWidgets() {
        return mWidgets;
    }


    @Override
    public void setOnFocusChangeListener(OnFocusChangeListener l) {
        for (int i = 0; i < mWidgets.size(); i++) {
            QuestionWidget qw = mWidgets.get(i);
            qw.setOnFocusChangeListener(l);
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        for (QuestionWidget qw : mWidgets) {
            qw.cancelLongPress();
        }
    }
}
