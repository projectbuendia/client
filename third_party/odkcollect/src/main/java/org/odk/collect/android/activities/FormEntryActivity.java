/*
 * Copyright (C) 2009 University of Washington
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

package org.odk.collect.android.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.exception.JavaRosaException;
import org.odk.collect.android.listeners.FormLoaderListener;
import org.odk.collect.android.listeners.FormSavedListener;
import org.odk.collect.android.listeners.SavePointListener;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.logic.FormController.FailedConstraint;
import org.odk.collect.android.logic.FormTraverser;
import org.odk.collect.android.logic.FormVisitor;
import org.odk.collect.android.model.Patient;
import org.odk.collect.android.model.Preset;
import org.odk.collect.android.preferences.PreferencesActivity;
import org.odk.collect.android.provider.FormsProviderAPI.FormsColumns;
import org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import org.odk.collect.android.tasks.FormLoaderTask;
import org.odk.collect.android.tasks.SavePointTask;
import org.odk.collect.android.tasks.SaveResult;
import org.odk.collect.android.tasks.SaveToDiskTask;
import org.odk.collect.android.utilities.CompatibilityUtils;
import org.odk.collect.android.utilities.FileUtils;
import org.odk.collect.android.utilities.MediaUtils;
import org.odk.collect.android.views.ODKView;
import org.odk.collect.android.widgets.QuestionWidget;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

//import android.view.GestureDetector;
//import android.view.GestureDetector.OnGestureListener;

/**
 * FormEntryActivity is responsible for displaying questions, animating
 * transitions between questions, and allowing the user to enter data.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Thomas Smyth, Sassafras Tech Collective (tom@sassafrastech.com; constraint behavior option)
 */
public class FormEntryActivity
        extends Activity implements FormLoaderListener, FormSavedListener, SavePointListener {

	private static final String TAG = FormEntryActivity.class.getName();

	// save with every swipe forward or back. Timings indicate this takes .25
	// seconds.
	// if it ever becomes an issue, this value can be changed to save every n'th
	// screen.
//	private static final int SAVEPOINT_INTERVAL = 1;

	// Defines for FormEntryActivity
	private static final boolean EXIT = true;
	private static final boolean DO_NOT_EXIT = false;
	private static final boolean EVALUATE_CONSTRAINTS = true;
	private static final boolean DO_NOT_EVALUATE_CONSTRAINTS = false;

	// Request codes for returning data from specified intent.
	public static final int IMAGE_CAPTURE = 1;
	public static final int BARCODE_CAPTURE = 2;
	public static final int AUDIO_CAPTURE = 3;
	public static final int VIDEO_CAPTURE = 4;
	public static final int LOCATION_CAPTURE = 5;
//	public static final int HIERARCHY_ACTIVITY = 6;
	public static final int IMAGE_CHOOSER = 7;
	public static final int AUDIO_CHOOSER = 8;
	public static final int VIDEO_CHOOSER = 9;
	public static final int EX_STRING_CAPTURE = 10;
	public static final int EX_INT_CAPTURE = 11;
	public static final int EX_DECIMAL_CAPTURE = 12;
	public static final int DRAW_IMAGE = 13;
	public static final int SIGNATURE_CAPTURE = 14;
	public static final int ANNOTATE_IMAGE = 15;
	public static final int ALIGNED_IMAGE = 16;
	public static final int BEARING_CAPTURE = 17;
    public static final int EX_GROUP_CAPTURE = 18;

	// Extra returned from gp activity
	public static final String LOCATION_RESULT = "LOCATION_RESULT";
	public static final String BEARING_RESULT = "BEARING_RESULT";

	public static final String KEY_INSTANCES = "instances";
	public static final String KEY_SUCCESS = "success";
	public static final String KEY_ERROR = "error";

	// Identifies the gp of the form used to launch form entry
	public static final String KEY_FORMPATH = "formpath";

	// Identifies whether this is a new form, or reloading a form after a screen
	// rotation (or similar)
	private static final String NEWFORM = "newform";
	// these are only processed if we shut down and are restoring after an
	// external intent fires

	public static final String KEY_INSTANCEPATH = "instancepath";
	public static final String KEY_XPATH = "xpath";
	public static final String KEY_XPATH_WAITING_FOR_DATA = "xpathwaiting";

	// Tracks whether we are autosaving
	public static final String KEY_AUTO_SAVED = "autosaved";

//	private static final int MENU_LANGUAGES = Menu.FIRST;
//	private static final int MENU_HIERARCHY_VIEW = Menu.FIRST + 1;
//    private static final int MENU_CANCEL = Menu.FIRST;
//	private static final int MENU_SAVE = MENU_CANCEL + 1;
//	private static final int MENU_PREFERENCES = Menu.FIRST + 3;

	private static final int PROGRESS_DIALOG = 1;
	private static final int SAVING_DIALOG = 2;

    // Alert dialog styling.
    private static final float ALERT_DIALOG_TEXT_SIZE = 32.0f;
    private static final float ALERT_DIALOG_TITLE_TEXT_SIZE = 34.0f;
    private static final int ALERT_DIALOG_PADDING = 32;

    private boolean mAutoSaved;

	// Random ID
	private static final int DELETE_REPEAT = 654321;

	private String mFormPath;
//	private GestureDetector mGestureDetector;
//
//	private Animation mInAnimation;
//	private Animation mOutAnimation;
//	private View mStaleView = null;

    private ScrollView mScrollView;
	private LinearLayout mQuestionHolder;
	private View mCurrentView;
    private ImageButton mUpButton;
    private ImageButton mDownButton;

    private Button mCancelButton;
    private Button mDoneButton;

	private AlertDialog mAlertDialog;
	private ProgressDialog mProgressDialog;
	private String mErrorMessage;

	// used to limit forward/backward swipes to one per question
//	private boolean mBeenSwiped = false;

    private final Object saveDialogLock = new Object();
	private int viewCount = 0;

	private FormLoaderTask mFormLoaderTask;
	private SaveToDiskTask mSaveToDiskTask;

    private String stepMessage = "";
    private ODKView mTargetView;
    private Map<FormIndex, IAnswerData> mOriginalAnswerData;

    private static final double MAX_SCROLL_FRACTION = 0.9; // fraction of mScrollView height
    private View mBottomPaddingView;

    // ScrollY values that we try to scroll to when paging up and down.
    private List<Integer> mPageBreaks = new ArrayList<>();

    private enum ScrollDirection {UP, DOWN}

//	enum AnimationType {
//		LEFT, RIGHT, FADE
//	}

//	private SharedPreferences mAdminPreferences;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// must be at the beginning of any activity that can be called from an
		// external intent
		try {
			Collect.getInstance().createODKDirs();
		} catch (RuntimeException e) {
			createErrorDialog(e.getMessage(), EXIT);
			return;
		}

		setContentView(R.layout.form_entry);

        setTitle(getString(R.string.title_loading_form));

        getActionBar().setDisplayHomeAsUpEnabled(true);
//
//		setTitle(getString(R.string.app_name) + " > "
//				+ getString(R.string.loading_form));

        mErrorMessage = null;

//        mBeenSwiped = false;
		mAlertDialog = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(getString(R.string.title_discard_observations))
                //.setMessage(R.string.observations_are_you_sure)
                .setPositiveButton(
                        R.string.yes,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                finish();
                            }
                        }
                )
                .setNegativeButton(R.string.no, null)
                .create();

		mCurrentView = null;
//		mInAnimation = null;
//		mOutAnimation = null;
//		mGestureDetector = new GestureDetector(this, this);
        mScrollView = (ScrollView) findViewById(R.id.question_holder_scroller);
		mQuestionHolder = (LinearLayout) findViewById(R.id.questionholder);

        findViewById(R.id.button_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scrollPage(ScrollDirection.UP);
            }
        });
        findViewById(R.id.button_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scrollPage(ScrollDirection.DOWN);
            }
        });

        mCancelButton = (Button) findViewById(R.id.form_entry_button_cancel);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (allContentsUnchanged()) {
                    finish();
                } else {
                    showAlertDialog();
                }
            }
        });

        mDoneButton = (Button) findViewById(R.id.form_entry_button_done);
        mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance()
                        .getActivityLogger()
                        .logInstanceAction(this, "onOptionsItemSelected",
                                "MENU_SAVE");

                InputMethodManager imm = (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

                saveDataToDisk(EXIT, true /*complete*/, null);
            }
        });

        // get admin preference settings
//		mAdminPreferences = getSharedPreferences(
//				AdminPreferencesActivity.ADMIN_PREFERENCES, 0);

		String startingXPath = null;
		String waitingXPath = null;
		String instancePath = null;
		Boolean newForm = true;
		mAutoSaved = false;
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(KEY_FORMPATH)) {
				mFormPath = savedInstanceState.getString(KEY_FORMPATH);
			}
			if (savedInstanceState.containsKey(KEY_INSTANCEPATH)) {
				instancePath = savedInstanceState.getString(KEY_INSTANCEPATH);
			}
			if (savedInstanceState.containsKey(KEY_XPATH)) {
				startingXPath = savedInstanceState.getString(KEY_XPATH);
				Log.i(TAG, "startingXPath is: " + startingXPath);
			}
			if (savedInstanceState.containsKey(KEY_XPATH_WAITING_FOR_DATA)) {
				waitingXPath = savedInstanceState
						.getString(KEY_XPATH_WAITING_FOR_DATA);
				Log.i(TAG, "waitingXPath is: " + waitingXPath);
			}
			if (savedInstanceState.containsKey(NEWFORM)) {
				newForm = savedInstanceState.getBoolean(NEWFORM, true);
			}
			if (savedInstanceState.containsKey(KEY_ERROR)) {
				mErrorMessage = savedInstanceState.getString(KEY_ERROR);
			}
			if (savedInstanceState.containsKey(KEY_AUTO_SAVED)) {
			    mAutoSaved = savedInstanceState.getBoolean(KEY_AUTO_SAVED);
			}
		}

		// If a parse error message is showing then nothing else is loaded
		// Dialogs mid form just disappear on rotation.
		if (mErrorMessage != null) {
			createErrorDialog(mErrorMessage, EXIT);
			return;
		}

		// Check to see if this is a screen flip or a new form load.
		Object data = getLastNonConfigurationInstance();
		if (data instanceof FormLoaderTask) {
			mFormLoaderTask = (FormLoaderTask) data;
		} else if (data instanceof SaveToDiskTask) {
			mSaveToDiskTask = (SaveToDiskTask) data;
		} else if (data == null) {
			if (!newForm) {
				if (Collect.getInstance().getFormController() != null) {
					populateViews((Preset) getIntent().getParcelableExtra("fields"));
				} else {
					Log.w(TAG, "Reloading form and restoring state.");
					// we need to launch the form loader to load the form
					// controller...
					mFormLoaderTask = new FormLoaderTask(instancePath,
							startingXPath, waitingXPath);
					Collect.getInstance().getActivityLogger()
							.logAction(this, "formReloaded", mFormPath);
                    // TODO: this doesn't work (dialog does not get removed):
					// showDialog(PROGRESS_DIALOG);
					// show dialog before we execute...
					mFormLoaderTask.execute(mFormPath);
				}
				return;
			}

			// Not a restart from a screen orientation change (or other).
			Collect.getInstance().setFormController(null);
			CompatibilityUtils.invalidateOptionsMenu(this);

			Intent intent = getIntent();
			if (intent != null) {
				Uri uri = intent.getData();

				if (getContentResolver().getType(uri).equals(InstanceColumns.CONTENT_ITEM_TYPE)) {
					// get the formId and version for this instance...
					String jrFormId = null;
					String jrVersion = null;
					{
						Cursor instanceCursor = null;
						try {
							instanceCursor = getContentResolver().query(uri,
									null, null, null, null);
							if (instanceCursor.getCount() != 1) {
								this.createErrorDialog("Bad URI: " + uri, EXIT);
								return;
							} else {
								instanceCursor.moveToFirst();
								instancePath = instanceCursor
										.getString(instanceCursor
												.getColumnIndex(InstanceColumns.INSTANCE_FILE_PATH));
								Collect.getInstance()
										.getActivityLogger()
										.logAction(this, "instanceLoaded",
												instancePath);

								jrFormId = instanceCursor
										.getString(instanceCursor
												.getColumnIndex(InstanceColumns.JR_FORM_ID));
								int idxJrVersion = instanceCursor
										.getColumnIndex(InstanceColumns.JR_VERSION);

								jrVersion = instanceCursor.isNull(idxJrVersion) ? null
										: instanceCursor
												.getString(idxJrVersion);
							}
						} finally {
							if (instanceCursor != null) {
								instanceCursor.close();
							}
						}
					}

					String[] selectionArgs;
					String selection;

					if (jrVersion == null) {
						selectionArgs = new String[] { jrFormId };
						selection = FormsColumns.JR_FORM_ID + "=? AND "
								+ FormsColumns.JR_VERSION + " IS NULL";
					} else {
						selectionArgs = new String[] { jrFormId, jrVersion };
						selection = FormsColumns.JR_FORM_ID + "=? AND "
								+ FormsColumns.JR_VERSION + "=?";
					}

					{
						Cursor formCursor = null;
						try {
							formCursor = getContentResolver().query(
									FormsColumns.CONTENT_URI, null, selection,
									selectionArgs, null);
							if (formCursor.getCount() == 1) {
								formCursor.moveToFirst();
								mFormPath = formCursor
										.getString(formCursor
												.getColumnIndex(FormsColumns.FORM_FILE_PATH));
							} else if (formCursor.getCount() < 1) {
								this.createErrorDialog(
										getString(
												R.string.parent_form_not_present,
												jrFormId)
												+ ((jrVersion == null) ? ""
														: "\n"
																+ getString(R.string.version)
																+ " "
																+ jrVersion),
										EXIT);
								return;
							} else if (formCursor.getCount() > 1) {
								// still take the first entry, but warn that
								// there are multiple rows.
								// user will need to hand-edit the SQLite
								// database to fix it.
								formCursor.moveToFirst();
								mFormPath = formCursor.getString(formCursor.getColumnIndex(FormsColumns.FORM_FILE_PATH));
								this.createErrorDialog(getString(R.string.survey_multiple_forms_error),	EXIT);
                                return;
							}
						} finally {
							if (formCursor != null) {
								formCursor.close();
							}
						}
					}
				} else if (getContentResolver().getType(uri).equals(FormsColumns.CONTENT_ITEM_TYPE)) {
					Cursor c = null;
					try {
						c = getContentResolver().query(uri, null, null, null,
								null);
						if (c.getCount() != 1) {
							this.createErrorDialog("Bad URI: " + uri, EXIT);
							return;
						} else {
							c.moveToFirst();
							mFormPath = c.getString(c.getColumnIndex(FormsColumns.FORM_FILE_PATH));
							// This is the fill-blank-form code path.
							// See if there is a savepoint for this form that
							// has never been
							// explicitly saved
							// by the user. If there is, open this savepoint
							// (resume this filled-in
							// form).
							// Savepoints for forms that were explicitly saved
							// will be recovered
							// when that
							// explicitly saved instance is edited via
							// edit-saved-form.
							final String filePrefix = mFormPath.substring(
									mFormPath.lastIndexOf('/') + 1,
									mFormPath.lastIndexOf('.'))
									+ "_";
							final String fileSuffix = ".xml.save";
							File cacheDir = new File(Collect.getInstance().getCachePath());
							File[] files = cacheDir.listFiles(new FileFilter() {
								@Override
								public boolean accept(File pathname) {
									String name = pathname.getName();
									return name.startsWith(filePrefix)
											&& name.endsWith(fileSuffix);
								}
							});
							// see if any of these savepoints are for a
							// filled-in form that has never been
							// explicitly saved by the user...
							for (int i = 0; i < files.length; ++i) {
								File candidate = files[i];
								String instanceDirName = candidate.getName()
										.substring(
												0,
												candidate.getName().length()
														- fileSuffix.length());
								File instanceDir = new File(
										Collect.getInstance().getInstancesPath() + File.separator
												+ instanceDirName);
								File instanceFile = new File(instanceDir,
										instanceDirName + ".xml");
								if (instanceDir.exists()
										&& instanceDir.isDirectory()
										&& !instanceFile.exists()) {
									// yes! -- use this savepoint file
									instancePath = instanceFile
											.getAbsolutePath();
									break;
								}
							}
						}
					} finally {
						if (c != null) {
							c.close();
						}
					}
				} else {
					Log.e(TAG, "unrecognized URI");
					this.createErrorDialog("Unrecognized URI: " + uri, EXIT);
					return;
				}

				mFormLoaderTask = new FormLoaderTask(instancePath, null, null);
				Collect.getInstance().getActivityLogger()
						.logAction(this, "formLoaded", mFormPath);
				showDialog(PROGRESS_DIALOG);
				// show dialog before we execute...
				mFormLoaderTask.execute(mFormPath);
			}
		}
	}

    private void populateViews(Preset preset) {
        FormTraverser traverser = new FormTraverser.Builder()
                .addVisitor(new QuestionHolderFormVisitor(preset))
                .build();
        traverser.traverse(Collect.getInstance().getFormController());

        mBottomPaddingView = new View(this);
        mBottomPaddingView.setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
        mQuestionHolder.addView(mBottomPaddingView);

        // After the form elements have all been sized and laid out, figure out
        // where (vertically) the up/down buttons should jump to.
        mScrollView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        paginate();
                    }
                }
        );

        if (mTargetView != null) {
            (new Handler(Looper.getMainLooper())).post(new Runnable() {
                @Override
                public void run() {
                    mScrollView.scrollTo(0, mTargetView.getTop());
                }
            });
        }

        // Store any pre-populated answers to enable performing a diff later on.
        mOriginalAnswerData = getAnswers();
    }

    /**
     * Determines the vertical positions that the up/down buttons will jump to.
     * These positions are selected so that the height of each page is up to
     * MAX_SCROLL_FRACTION of the ScrollView's height, and (if possible) each
     * page break is at the top edge of a group or question.
     */
    private void paginate() {
        // Gather a list of the top edges of the question groups and widgets.
        List<Integer> edges = new ArrayList<>();
        for (int i = 0; i < mQuestionHolder.getChildCount(); i++) {
            View child = mQuestionHolder.getChildAt(i);
            edges.add(child.getTop());
            if (child instanceof ODKView) {
                List<QuestionWidget> widgets = ((ODKView) child).getWidgets();
                // Skip the first widget: it's better to land above the group title
                // than between the group title and the first question widget.
                for (int j = 1; j < widgets.size(); j++) {
                    edges.add(child.getTop() + widgets.get(j).getTop());
                }
            }
        }

        // Select a subset of these edges to be the page breaks.  Dividing the
        // form into fixed pages ensures that each question appears at a fixed
        // vertical position on the screen within its page, which helps keep
        // the user oriented.
        int maxPageHeight = (int) (mScrollView.getMeasuredHeight() * MAX_SCROLL_FRACTION);
        mPageBreaks.clear();
        mPageBreaks.add(0);
        int prevBreak = 0;
        int nextBreak = prevBreak + maxPageHeight;
        for (int y : edges) {
            while (y > prevBreak + maxPageHeight) { // next edge won't land on this page
                mPageBreaks.add(nextBreak);
                prevBreak = nextBreak;
                // Usually this initial value of nextBreak will be overwritten
                // by a value from edges (nextBreak = y below); this value is
                // just a fallback in case the nearest edge is too far away.
                nextBreak = prevBreak + maxPageHeight;
            }
            if (y > prevBreak) {
                nextBreak = y;
            }
        }

        // Add enough padding so that when the form is scrolled all the way to
        // the end, the last page break lands at the top of the ScrollView.
        // This can sometimes be a lot of padding (e.g. most of a page), but
        // we think the benefit of having a consistent n:1 relationship between
        // questions and pages is worth the occasionally large wasted space.
        int bottom = prevBreak + mScrollView.getMeasuredHeight();
        int paddingHeight = bottom - mBottomPaddingView.getTop();
        ViewGroup.LayoutParams params = mBottomPaddingView.getLayoutParams();
        if (params.height != paddingHeight) {
            // To avoid triggering an infinite loop, only invoke
            // requestLayout() when the height actually changes.
            params.height = paddingHeight;
            mBottomPaddingView.requestLayout();
        }
    }

    /**
     * Scrolls the form up or down to the next page break.  To keep the user
     * oriented, we always go to the nearest page break (even if it is nearby),
     * giving a consistent set of pages with a consistent layout on each page.
     */
    private void scrollPage(ScrollDirection direction) {
        int inc = direction == ScrollDirection.DOWN ? 1 : -1;
        int y = mScrollView.getScrollY();
        int n = mPageBreaks.size();
        for (int i = (inc > 0) ? 0 : n - 1; i >= 0 && i < n; i += inc) {
            int deltaY = mPageBreaks.get(i) - y;
            if (inc * deltaY > 0) {
                mScrollView.smoothScrollTo(0, y + deltaY);
                break;
            }
        }
    }

    private class QuestionHolderFormVisitor implements FormVisitor {
        private final Preset mPreset;

        public QuestionHolderFormVisitor(Preset preset) {
            mPreset = preset;
        }

        @Override
        public void visit(int event, FormController formController) {
            View view = createView(event, false /*advancingPage*/, mPreset);
            if (view != null) {
                mQuestionHolder.addView(view);
            }
        }
    }

    /**
     * Create save-points asynchronously in order to not affect swiping performance
     * on larger forms.
     */
    private void nonblockingCreateSavePointData() {
        try {
            SavePointTask savePointTask = new SavePointTask(this);
            savePointTask.execute();
        } catch (Exception e) {
            Log.e(TAG, "Could not schedule SavePointTask. Perhaps a lot of swiping is taking place?");
        }
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_FORMPATH, mFormPath);
		FormController formController = Collect.getInstance()
				.getFormController();
		if (formController != null) {
			outState.putString(KEY_INSTANCEPATH, formController
					.getInstancePath().getAbsolutePath());
			outState.putString(KEY_XPATH,
					formController.getXPath(formController.getFormIndex()));
			FormIndex waiting = formController.getIndexWaitingForData();
			if (waiting != null) {
				outState.putString(KEY_XPATH_WAITING_FOR_DATA,
						formController.getXPath(waiting));
			}
			// save the instance to a temp path...
			nonblockingCreateSavePointData();
		}
		outState.putBoolean(NEWFORM, false);
		outState.putString(KEY_ERROR, mErrorMessage);
		outState.putBoolean(KEY_AUTO_SAVED, mAutoSaved);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		FormController formController = Collect.getInstance()
				.getFormController();
		if (formController == null) {
			// we must be in the midst of a reload of the FormController.
			// try to save this callback data to the FormLoaderTask
			if (mFormLoaderTask != null
					&& mFormLoaderTask.getStatus() != AsyncTask.Status.FINISHED) {
				mFormLoaderTask.setActivityResult(requestCode, resultCode,
						intent);
			} else {
				Log.e(TAG,
						"Got an activityResult without any pending form loader");
			}
			return;
		}

		if (resultCode == RESULT_CANCELED) {
			// request was canceled...
//			if (requestCode != HIERARCHY_ACTIVITY) {
				((ODKView) mCurrentView).cancelWaitingForBinaryData();
//			}
			return;
		}

		switch (requestCode) {
            case BARCODE_CAPTURE:
                String sb = intent.getStringExtra("SCAN_RESULT");
                ((ODKView) mCurrentView).setBinaryData(sb);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case EX_STRING_CAPTURE:
            case EX_INT_CAPTURE:
            case EX_DECIMAL_CAPTURE:
                String key = "value";
                boolean exists = intent.getExtras().containsKey(key);
                if (exists) {
                    Object externalValue = intent.getExtras().get(key);
                    ((ODKView) mCurrentView).setBinaryData(externalValue);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                }
                break;
            case EX_GROUP_CAPTURE:
                try {
                    Bundle extras = intent.getExtras();
                    ((ODKView) mCurrentView).setDataForFields(extras);
                } catch (JavaRosaException e) {
                    Log.e(TAG, e.getMessage(), e);
                    createErrorDialog(e.getCause().getMessage(), DO_NOT_EXIT);
                }
                break;
            case DRAW_IMAGE:
            case ANNOTATE_IMAGE:
            case SIGNATURE_CAPTURE:
            case IMAGE_CAPTURE:
                /*
                 * We saved the image to the tempfile_path, but we really want it to
                 * be in: /sdcard/odk/instances/[current instnace]/something.jpg so
                 * we move it there before inserting it into the content provider.
                 * Once the android image capture bug gets fixed, (read, we move on
                 * from Android 1.6) we want to handle images the audio and video
                 */
                // The intent is empty, but we know we saved the image to the temp
                // file
                File fi = new File(Collect.getInstance().getTmpFilePath());
                String mInstanceFolder = formController.getInstancePath()
                        .getParent();
                String s = mInstanceFolder + File.separator
                        + System.currentTimeMillis() + ".jpg";

                File nf = new File(s);
                if (!fi.renameTo(nf)) {
                    Log.e(TAG, "Failed to rename " + fi.getAbsolutePath());
                } else {
                    Log.i(TAG,
                            "renamed " + fi.getAbsolutePath() + " to "
                                    + nf.getAbsolutePath());
                }

                ((ODKView) mCurrentView).setBinaryData(nf);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case ALIGNED_IMAGE:
                /*
                 * We saved the image to the tempfile_path; the app returns the full
                 * path to the saved file in the EXTRA_OUTPUT extra. Take that file
                 * and move it into the instance folder.
                 */
                String path = intent
                        .getStringExtra(android.provider.MediaStore.EXTRA_OUTPUT);
                fi = new File(path);
                mInstanceFolder = formController.getInstancePath().getParent();
                s = mInstanceFolder + File.separator + System.currentTimeMillis()
                        + ".jpg";

                nf = new File(s);
                if (!fi.renameTo(nf)) {
                    Log.e(TAG, "Failed to rename " + fi.getAbsolutePath());
                } else {
                    Log.i(TAG,
                            "renamed " + fi.getAbsolutePath() + " to "
                                    + nf.getAbsolutePath());
                }

                ((ODKView) mCurrentView).setBinaryData(nf);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case IMAGE_CHOOSER:
                /*
                 * We have a saved image somewhere, but we really want it to be in:
                 * /sdcard/odk/instances/[current instnace]/something.jpg so we move
                 * it there before inserting it into the content provider. Once the
                 * android image capture bug gets fixed, (read, we move on from
                 * Android 1.6) we want to handle images the audio and video
                 */

                // get gp of chosen file
                Uri selectedImage = intent.getData();
                String sourceImagePath =
                        MediaUtils.getPathFromUri(this, selectedImage, Images.Media.DATA);

                // Copy file to sdcard
                String mInstanceFolder1 = formController.getInstancePath()
                        .getParent();
                String destImagePath = mInstanceFolder1 + File.separator
                        + System.currentTimeMillis() + ".jpg";

                File source = new File(sourceImagePath);
                File newImage = new File(destImagePath);
                FileUtils.copyFile(source, newImage);

                ((ODKView) mCurrentView).setBinaryData(newImage);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case AUDIO_CAPTURE:
            case VIDEO_CAPTURE:
            case AUDIO_CHOOSER:
            case VIDEO_CHOOSER:
                // For audio/video capture/chooser, we get the URI from the content
                // provider
                // then the widget copies the file and makes a new entry in the
                // content provider.
                Uri media = intent.getData();
                ((ODKView) mCurrentView).setBinaryData(media);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case LOCATION_CAPTURE:
                String sl = intent.getStringExtra(LOCATION_RESULT);
                ((ODKView) mCurrentView).setBinaryData(sl);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case BEARING_CAPTURE:
                String bearing = intent.getStringExtra(BEARING_RESULT);
                ((ODKView) mCurrentView).setBinaryData(bearing);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
//		case HIERARCHY_ACTIVITY:
//			// We may have jumped to a new index in hierarchy activity, so
//			// refresh
//			break;

		}
//		refreshCurrentView();
	}
//
//	/**
//	 * Refreshes the current view. the controller and the displayed view can get
//	 * out of sync due to dialogs and restarts caused by screen orientation
//	 * changes, so they're resynchronized here.
//	 */
//	public void refreshCurrentView() {
//		FormController formController = Collect.getInstance()
//				.getFormController();
//		int event = formController.getEvent();
//
//		// When we refresh, repeat dialog state isn't maintained, so step back
//		// to the previous
//		// question.
//		// Also, if we're within a group labeled 'field list', step back to the
//		// beginning of that
//		// group.
//		// That is, skip backwards over repeat prompts, groups that are not
//		// field-lists,
//		// repeat events, and indexes in field-lists that is not the containing
//		// group.
//		if (event == FormEntryController.EVENT_PROMPT_NEW_REPEAT) {
//			createRepeatDialog();
//		} else {
//			View current = createView(event, false);
//			showView(current, AnimationType.FADE);
//		}
//	}

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            showAlertDialog();
            return true;
        }
        else {
            return super.onKeyDown(keyCode, event);
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Collect.getInstance().getActivityLogger()
				.logInstanceAction(this, "onCreateOptionsMenu", "show");
		super.onCreateOptionsMenu(menu);

//        CompatibilityUtils.setShowAsAction(
//                menu.add(0, MENU_CANCEL, 0, R.string.cancel),
//                MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

//		CompatibilityUtils.setShowAsAction(
//				menu.add(0, MENU_SAVE, 0, R.string.save_all_answers),
//				MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

//		CompatibilityUtils.setShowAsAction(
//				menu.add(0, MENU_HIERARCHY_VIEW, 0, R.string.view_hierarchy)
//						.setIcon(R.drawable.ic_menu_goto),
//				MenuItem.SHOW_AS_ACTION_IF_ROOM);

//		CompatibilityUtils.setShowAsAction(
//				menu.add(0, MENU_LANGUAGES, 0, R.string.change_language)
//						.setIcon(R.drawable.ic_menu_start_conversation),
//				MenuItem.SHOW_AS_ACTION_NEVER);

//		CompatibilityUtils.setShowAsAction(
//				menu.add(0, MENU_PREFERENCES, 0, R.string.general_preferences)
//						.setIcon(R.drawable.ic_menu_preferences),
//				MenuItem.SHOW_AS_ACTION_NEVER);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

//        menu.findItem(MENU_CANCEL).setVisible(true).setEnabled(true);
//        menu.findItem(MENU_SAVE).setVisible(true).setEnabled(true);
//
//		FormController formController = Collect.getInstance()
//				.getFormController();
//
//		boolean useability;
//		useability = mAdminPreferences.getBoolean(
//				AdminPreferencesActivity.KEY_SAVE_MID, true);
//
//		menu.findItem(MENU_SAVE).setVisible(useability).setEnabled(useability);
//
//		useability = mAdminPreferences.getBoolean(
//				AdminPreferencesActivity.KEY_JUMP_TO, true);
//
//		menu.findItem(MENU_HIERARCHY_VIEW).setVisible(useability)
//				.setEnabled(useability);
//
//		useability = mAdminPreferences.getBoolean(
//				AdminPreferencesActivity.KEY_CHANGE_LANGUAGE, true)
//				&& (formController != null)
//				&& formController.getLanguages() != null
//				&& formController.getLanguages().length > 1;
//
//		menu.findItem(MENU_LANGUAGES).setVisible(useability)
//				.setEnabled(useability);
//
//		useability = mAdminPreferences.getBoolean(
//				AdminPreferencesActivity.KEY_ACCESS_SETTINGS, true);
//
//		menu.findItem(MENU_PREFERENCES).setVisible(useability)
//				.setEnabled(useability);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
//		FormController formController = Collect.getInstance()
//				.getFormController();
		switch (item.getItemId()) {
			case android.R.id.home :
				// Go back rather than reloading the activity, so that the patient list retains its
				// filter state.
				onBackPressed();
				return true;
////		case MENU_LANGUAGES:
////			Collect.getInstance()
////					.getActivityLogger()
////					.logInstanceAction(this, "onOptionsItemSelected",
////							"MENU_LANGUAGES");
////			createLanguageDialog();
////			return true;
//        case MENU_CANCEL:
//            showAlertDialog();
//            return true;
//		case MENU_SAVE:
//			Collect.getInstance()
//					.getActivityLogger()
//					.logInstanceAction(this, "onOptionsItemSelected",
//							"MENU_SAVE");

//            InputMethodManager imm = (InputMethodManager)getSystemService(
//                    Context.INPUT_METHOD_SERVICE);
//            imm.hideSoftInputFromWindow( findViewById(android.R.id.content).getWindowToken(), 0);

//			saveDataToDisk(EXIT, true /*complete*/, null);
//			return true;
//		case MENU_HIERARCHY_VIEW:
//			Collect.getInstance()
//					.getActivityLogger()
//					.logInstanceAction(this, "onOptionsItemSelected",
//							"MENU_HIERARCHY_VIEW");
//			if (formController.currentPromptIsQuestion()) {
//				saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
//			}
//			Intent i = new Intent(this, FormHierarchyActivity.class);
//			startActivityForResult(i, HIERARCHY_ACTIVITY);
//			return true;
//		case MENU_PREFERENCES:
//			Collect.getInstance()
//					.getActivityLogger()
//					.logInstanceAction(this, "onOptionsItemSelected",
//							"MENU_PREFERENCES");
//			Intent pref = new Intent(this, PreferencesActivity.class);
//			startActivity(pref);
//			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Attempt to save the answer(s) in the current screen to into the data
	 * model.
	 *
	 * @param evaluateConstraints
	 * @return false if any error occurs while saving (constraint violated,
	 *         etc...), true otherwise.
	 */
	private boolean saveAnswersForCurrentScreen(boolean evaluateConstraints) {
		FormController formController = Collect.getInstance()
				.getFormController();
		// only try to save if the current event is a question or a field-list
		// group
		if (formController.currentPromptIsQuestion()) {
			LinkedHashMap<FormIndex, IAnswerData> answers = ((ODKView) mCurrentView)
					.getAnswers();
            try {
                FailedConstraint constraint = formController.saveAnswers(answers, evaluateConstraints);
                if (constraint != null) {
                    createConstraintToast(constraint.index, constraint.status);
                    return false;
                }
            } catch (JavaRosaException e) {
                Log.e(TAG, e.getMessage(), e);
                createErrorDialog(e.getCause().getMessage(), DO_NOT_EXIT);
                return false;
            }
		}
		return true;
	}

    /**
     * Collects all answers in the form into a single map.
     * @return a {@link java.util.LinkedHashMap} of all answers in the form.
     */
    private LinkedHashMap<FormIndex, IAnswerData> getAnswers() {
        int childCount = mQuestionHolder.getChildCount();
        LinkedHashMap<FormIndex, IAnswerData> answers = new LinkedHashMap<FormIndex, IAnswerData>();
        for (int i = 0; i < childCount; i++) {
            View view = mQuestionHolder.getChildAt(i);
            if (!(view instanceof ODKView)) {
                continue;
            }
            answers.putAll(((ODKView) view).getAnswers());
        }

        return answers;
    }

    /**
     * Saves all answers in the form.
     *
     * @return false if any error occurs while saving (constraint violated,
     *         etc...), true otherwise
     */
    private boolean saveAnswers(boolean evaluateConstraints) {
        FormController formController = Collect.getInstance().getFormController();
        int childCount = mQuestionHolder.getChildCount();
        LinkedHashMap<FormIndex, IAnswerData> answers = getAnswers();

        try {
            FailedConstraint constraint = formController.saveAnswers(answers, evaluateConstraints);
            if (constraint != null) {
                createConstraintToast(constraint.index, constraint.status);
                return false;
            }
        } catch (JavaRosaException e) {
            Log.e(TAG, e.getMessage(), e);
            createErrorDialog(e.getCause().getMessage(), DO_NOT_EXIT);
            return false;
        }

        return true;
    }

	/**
	 * Clears the answer on the screen.
	 */
	private void clearAnswer(QuestionWidget qw) {
		if (qw.getAnswer() != null) {
			qw.clearAnswer();
		}
	}

//	@Override
//	public void onCreateContextMenu(ContextMenu menu, View v,
//			ContextMenuInfo menuInfo) {
//		super.onCreateContextMenu(menu, v, menuInfo);
//		Collect.getInstance().getActivityLogger()
//				.logInstanceAction(this, "onCreateContextMenu", "show");
//		FormController formController = Collect.getInstance()
//				.getFormController();
//
//		menu.add(0, v.getId(), 0, getString(R.string.clear_answer));
////		if (formController.indexContainsRepeatableGroup()) {
////			menu.add(0, DELETE_REPEAT, 0, getString(R.string.delete_repeat));
////		}
//		menu.setHeaderTitle(getString(R.string.edit_prompt));
//	}

//	@Override
//	public boolean onContextItemSelected(MenuItem item) {
//		/*
//		 * We don't have the right view here, so we store the View's ID as the
//		 * item ID and loop through the possible views to find the one the user
//		 * clicked on.
//		 */
//		for (QuestionWidget qw : ((ODKView) mCurrentView).getWidgets()) {
//			if (item.getItemId() == qw.getId()) {
//				Collect.getInstance()
//						.getActivityLogger()
//						.logInstanceAction(this, "onContextItemSelected",
//								"createClearDialog", qw.getPrompt().getIndex());
//				createClearDialog(qw);
//			}
//		}
////		if (item.getItemId() == DELETE_REPEAT) {
////			Collect.getInstance()
////					.getActivityLogger()
////					.logInstanceAction(this, "onContextItemSelected",
////							"createDeleteRepeatConfirmDialog");
////			createDeleteRepeatConfirmDialog();
////		}
//
//		return super.onContextItemSelected(item);
//	}

	/**
	 * If we're loading, then we pass the loading thread to our next instance.
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		FormController formController = Collect.getInstance()
				.getFormController();
		// if a form is loading, pass the loader task
		if (mFormLoaderTask != null
				&& mFormLoaderTask.getStatus() != AsyncTask.Status.FINISHED)
			return mFormLoaderTask;

		// if a form is writing to disk, pass the save to disk task
		if (mSaveToDiskTask != null
				&& mSaveToDiskTask.getStatus() != AsyncTask.Status.FINISHED)
			return mSaveToDiskTask;

		// mFormEntryController is static so we don't need to pass it.
		if (formController != null && formController.currentPromptIsQuestion()) {
			saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
		}
		return null;
	}

	/**
	 * Creates a view given the View type and an event
	 *
	 * @param event
	 * @param advancingPage
	 *            -- true if this results from advancing through the form
	 * @param preset
     * @return newly created View
	 */
	private View createView(int event, boolean advancingPage, Preset preset) {
		FormController formController = Collect.getInstance()
				.getFormController();
//		setTitle(getString(R.string.app_name) + " > "
//				+ formController.getFormTitle());

		switch (event) {
//		case FormEntryController.EVENT_BEGINNING_OF_FORM:
//			View startView = View
//					.inflate(this, R.layout.form_entry_start, null);
//			setTitle(getString(R.string.app_name) + " > "
//					+ formController.getFormTitle());
//
//			Drawable image = null;
//			File mediaFolder = formController.getMediaFolder();
//			String mediaDir = mediaFolder.getAbsolutePath();
//			BitmapDrawable bitImage = null;
//			// attempt to load the form-specific logo...
//			// this is arbitrarily silly
//			bitImage = new BitmapDrawable(getResources(), mediaDir + File.separator
//					+ "form_logo.png");
//
//			if (bitImage != null && bitImage.getBitmap() != null
//					&& bitImage.getIntrinsicHeight() > 0
//					&& bitImage.getIntrinsicWidth() > 0) {
//				image = bitImage;
//			}
//
//			if (image == null) {
//				// show the opendatakit zig...
//				// image =
//				// getResources().getDrawable(R.drawable.opendatakit_zig);
//				((ImageView) startView.findViewById(R.id.form_start_bling))
//						.setVisibility(View.GONE);
//			} else {
//				ImageView v = ((ImageView) startView
//						.findViewById(R.id.form_start_bling));
//				v.setImageDrawable(image);
//				v.setContentDescription(formController.getFormTitle());
//			}
//
//			// change start screen based on navigation prefs
//			String navigationChoice = PreferenceManager
//					.getDefaultSharedPreferences(this).getString(
//							PreferencesActivity.KEY_NAVIGATION,
//							PreferencesActivity.KEY_NAVIGATION);
//			Boolean useSwipe = false;
//			Boolean useButtons = false;
//			ImageView ia = ((ImageView) startView
//					.findViewById(R.id.image_advance));
//			ImageView ib = ((ImageView) startView
//					.findViewById(R.id.image_backup));
//			TextView ta = ((TextView) startView.findViewById(R.id.text_advance));
//			TextView tb = ((TextView) startView.findViewById(R.id.text_backup));
//			TextView d = ((TextView) startView.findViewById(R.id.description));
//
//			if (navigationChoice != null) {
//				if (navigationChoice
//						.contains(PreferencesActivity.NAVIGATION_SWIPE)) {
//					useSwipe = true;
//				}
//				if (navigationChoice
//						.contains(PreferencesActivity.NAVIGATION_BUTTONS)) {
//					useButtons = true;
//				}
//			}
//			if (useSwipe && !useButtons) {
//				d.setText(getString(R.string.swipe_instructions,
//						formController.getFormTitle()));
//			} else if (useButtons && !useSwipe) {
//				ia.setVisibility(View.GONE);
//				ib.setVisibility(View.GONE);
//				ta.setVisibility(View.GONE);
//				tb.setVisibility(View.GONE);
//				d.setText(getString(R.string.buttons_instructions,
//						formController.getFormTitle()));
//			} else {
//				d.setText(getString(R.string.swipe_buttons_instructions,
//						formController.getFormTitle()));
//			}
//
//			return startView;
//		case FormEntryController.EVENT_END_OF_FORM:
//			View endView = View.inflate(this, R.layout.form_entry_end, null);
//			((TextView) endView.findViewById(R.id.description))
//					.setText(getString(R.string.save_enter_data_description,
//							formController.getFormTitle()));
//
//			// checkbox for if finished or ready to send
//			final CheckBox instanceComplete = ((CheckBox) endView
//					.findViewById(R.id.mark_finished));
//			instanceComplete.setChecked(isInstanceComplete(true));
//
//			if (!mAdminPreferences.getBoolean(
//					AdminPreferencesActivity.KEY_MARK_AS_FINALIZED, true)) {
//				instanceComplete.setVisibility(View.GONE);
//			}
//
//			// edittext to change the displayed name of the instance
//			final EditText saveAs = (EditText) endView
//					.findViewById(R.id.save_name);
//
//			// disallow carriage returns in the name
//			InputFilter returnFilter = new InputFilter() {
//				public CharSequence filter(CharSequence source, int start,
//						int end, Spanned dest, int dstart, int dend) {
//					for (int i = start; i < end; i++) {
//						if (Character.getType((source.charAt(i))) == Character.CONTROL) {
//							return "";
//						}
//					}
//					return null;
//				}
//			};
//			saveAs.setFilters(new InputFilter[] { returnFilter });
//
//			String saveName = formController.getSubmissionMetadata().instanceName;
//			if (saveName == null) {
//				// no meta/instanceName field in the form -- see if we have a
//				// name for this instance from a previous save attempt...
//				if (getContentResolver().getType(getIntent().getData()) == InstanceColumns.CONTENT_ITEM_TYPE) {
//					Uri instanceUri = getIntent().getData();
//					Cursor instance = null;
//					try {
//						instance = getContentResolver().query(instanceUri,
//								null, null, null, null);
//						if (instance.getCount() == 1) {
//							instance.moveToFirst();
//							saveName = instance
//									.getString(instance
//											.getColumnIndex(InstanceColumns.DISPLAY_NAME));
//						}
//					} finally {
//						if (instance != null) {
//							instance.close();
//						}
//					}
//				}
//				if (saveName == null) {
//					// last resort, default to the form title
//					saveName = formController.getFormTitle();
//				}
//				// present the prompt to allow user to name the form
//				TextView sa = (TextView) endView
//						.findViewById(R.id.save_form_as);
//				sa.setVisibility(View.VISIBLE);
//				saveAs.setText(saveName);
//				saveAs.setEnabled(true);
//				saveAs.setVisibility(View.VISIBLE);
//			} else {
//				// if instanceName is defined in form, this is the name -- no
//				// revisions
//				// display only the name, not the prompt, and disable edits
//				TextView sa = (TextView) endView
//						.findViewById(R.id.save_form_as);
//				sa.setVisibility(View.GONE);
//				saveAs.setText(saveName);
//				saveAs.setEnabled(false);
//				saveAs.setBackgroundColor(Color.WHITE);
//				saveAs.setVisibility(View.VISIBLE);
//			}
//
//			// override the visibility settings based upon admin preferences
//			if (!mAdminPreferences.getBoolean(
//					AdminPreferencesActivity.KEY_SAVE_AS, true)) {
//				saveAs.setVisibility(View.GONE);
//				TextView sa = (TextView) endView
//						.findViewById(R.id.save_form_as);
//				sa.setVisibility(View.GONE);
//			}
//
//			// Create 'save' button
//			((Button) endView.findViewById(R.id.save_exit_button))
//					.setOnClickListener(new OnClickListener() {
//						@Override
//						public void onClick(View v) {
//							Collect.getInstance()
//									.getActivityLogger()
//									.logInstanceAction(
//											this,
//											"createView.saveAndExit",
//											instanceComplete.isChecked() ? "saveAsComplete"
//													: "saveIncomplete");
//							// Form is marked as 'saved' here.
//							if (saveAs.getText().length() < 1) {
//								Toast.makeText(FormEntryActivity.this,
//										R.string.save_as_error,
//										Toast.LENGTH_SHORT).show();
//							} else {
//								saveDataToDisk(EXIT, instanceComplete
//										.isChecked(), saveAs.getText()
//										.toString());
//							}
//						}
//					});
//
//			return endView;
//        case FormEntryController.EVENT_GROUP:
//            FormEntryCaption[] groups = formController.getGroupsForCurrentIndex();
//            if (groups.length == 0) {
//                Log.e(
//                        TAG,
//                        "Attempted to handle a FormEntryController.EVENT_GROUP when the form was "
//                                + "not on a group.");
//                break;
//            }
//            WidgetGroupBuilder builder =
//                    Widget2Factory.INSTANCE.createGroupBuilder(this, groups[groups.length - 1]);
//            if (builder != null) {
//                // TODO: Use the builder.
//                break;
//            }
//
//            // Fall through to the next case if we didn't manage to create a builder.
		case FormEntryController.EVENT_QUESTION:
		case FormEntryController.EVENT_GROUP:
		case FormEntryController.EVENT_REPEAT:
			ODKView odkv = null;
			// should only be a group here if the event_group is a field-list
			try {
				FormEntryPrompt[] prompts = formController.getQuestionPrompts();
				FormEntryCaption[] groups = formController
						.getGroupsForCurrentIndex();
				odkv = new ODKView(this, prompts, groups, advancingPage, preset);
                if (preset != null
                        && preset.targetGroup != null
                        && preset.targetGroup.equals(groups[groups.length - 1].getLongText())) {
                    mTargetView = odkv;
                }
				Log.i(TAG,
						"created view for group "
								+ (groups.length > 0 ? groups[groups.length - 1]
										.getLongText() : "[top]")
								+ " "
								+ (prompts.length > 0 ? prompts[0]
										.getQuestionText() : "[no question]"));
			} catch (RuntimeException e) {
				Log.e(TAG, e.getMessage(), e);
				// this is badness to avoid a crash.
                try {
                    event = formController.stepToNextScreenEvent();
                    createErrorDialog(e.getMessage(), DO_NOT_EXIT);
                } catch (JavaRosaException e1) {
                    Log.e(TAG, e1.getMessage(), e1);
                    createErrorDialog(e.getMessage() + "\n\n" + e1.getCause().getMessage(), DO_NOT_EXIT);
                }
                return createView(event, advancingPage, preset);
            }

			// Makes a "clear answer" menu pop up on long-click
			for (QuestionWidget qw : odkv.getWidgets()) {
				if (!qw.getPrompt().isReadOnly()) {
					registerForContextMenu(qw);
				}
			}
			return odkv;
		default:
            return null;
//			Log.e(TAG, "Attempted to create a view that does not exist.");
//			// this is badness to avoid a crash.
//            try {
//                event = formController.stepToNextScreenEvent();
//                createErrorDialog(getString(R.string.survey_internal_error), EXIT);
//            } catch (JavaRosaException e) {
//                Log.e(TAG, e.getMessage(), e);
//                createErrorDialog(e.getCause().getMessage(), EXIT);
//            }
//            return createView(event, advancingPage);
		}
	}

//	@Override
//	public boolean dispatchTouchEvent(MotionEvent mv) {
//		boolean handled = mGestureDetector.onTouchEvent(mv);
//		if (!handled) {
//			return super.dispatchTouchEvent(mv);
//		}
//
//		return handled; // this is always true
//	}
//
//	/**
//	 * Determines what should be displayed on the screen. Possible options are:
//	 * a question, an ask repeat dialog, or the submit screen. Also saves
//	 * answers to the data model after checking constraints.
//	 */
//	private void showNextView() {
//		try {
//            FormController formController = Collect.getInstance()
//                    .getFormController();
//
//            // get constraint behavior preference value with appropriate default
//            String constraint_behavior = PreferenceManager.getDefaultSharedPreferences(this)
//                    .getString(PreferencesActivity.KEY_CONSTRAINT_BEHAVIOR,
//                            PreferencesActivity.CONSTRAINT_BEHAVIOR_DEFAULT);
//
//            if (formController.currentPromptIsQuestion()) {
//
//                // if constraint behavior says we should validate on swipe, do so
//                if (constraint_behavior.equals(PreferencesActivity.CONSTRAINT_BEHAVIOR_ON_SWIPE)) {
//                    if (!saveAnswersForCurrentScreen(EVALUATE_CONSTRAINTS)) {
//                        // A constraint was violated so a dialog should be showing.
//                        mBeenSwiped = false;
//                        return;
//                    }
//
//                    // otherwise, just save without validating (constraints will be validated on finalize)
//                } else
//                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
//            }
//
//            View next;
//            int event = formController.stepToNextScreenEvent();
//
//
//            switch (event) {
//                case FormEntryController.EVENT_QUESTION:
//                case FormEntryController.EVENT_GROUP:
//                    // create a savepoint
//                    if ((++viewCount) % SAVEPOINT_INTERVAL == 0) {
//                        nonblockingCreateSavePointData();
//                    }
//                    next = createView(event, true);
//                    showView(next, AnimationType.RIGHT);
//                    break;
//                case FormEntryController.EVENT_END_OF_FORM:
//                case FormEntryController.EVENT_REPEAT:
//                    next = createView(event, true);
//                    showView(next, AnimationType.RIGHT);
//                    break;
//                case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
//                    createRepeatDialog();
//                    break;
//                case FormEntryController.EVENT_REPEAT_JUNCTURE:
//                    Log.i(TAG, "repeat juncture: "
//                            + formController.getFormIndex().getReference());
//                    // skip repeat junctures until we implement them
//                    break;
//                default:
//                    Log.w(TAG,
//                            "JavaRosa added a new EVENT type and didn't tell us... shame on them.");
//                    break;
//            }
//        } catch (JavaRosaException e) {
//            Log.e(TAG, e.getMessage(), e);
//            createErrorDialog(e.getCause().getMessage(), DO_NOT_EXIT);
//        }
//    }
//
//	/**
//	 * Determines what should be displayed between a question, or the start
//	 * screen and displays the appropriate view. Also saves answers to the data
//	 * model without checking constraints.
//	 */
//	private void showPreviousView() {
//        try {
//            FormController formController = Collect.getInstance()
//                    .getFormController();
//            // The answer is saved on a back swipe, but question constraints are
//            // ignored.
//            if (formController.currentPromptIsQuestion()) {
//                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
//            }
//
//            if (formController.getEvent() != FormEntryController.EVENT_BEGINNING_OF_FORM) {
//                int event = formController.stepToPreviousScreenEvent();
//
//                if (event == FormEntryController.EVENT_BEGINNING_OF_FORM
//                        || event == FormEntryController.EVENT_GROUP
//                        || event == FormEntryController.EVENT_QUESTION) {
//                    // create savepoint
//                    if ((++viewCount) % SAVEPOINT_INTERVAL == 0) {
//                        nonblockingCreateSavePointData();
//                    }
//                }
//                View next = createView(event, false);
//                showView(next, AnimationType.LEFT);
//            } else {
//                mBeenSwiped = false;
//            }
//        } catch (JavaRosaException e) {
//            Log.e(TAG, e.getMessage(), e);
//            createErrorDialog(e.getCause().getMessage(), DO_NOT_EXIT);
//        }
//    }
//
//	/**
//	 * Displays the View specified by the parameter 'next', animating both the
//	 * current view and next appropriately given the AnimationType. Also updates
//	 * the progress bar.
//	 */
//	public void showView(View next, AnimationType from) {
//
//		// disable notifications...
//		if (mInAnimation != null) {
//			mInAnimation.setAnimationListener(null);
//		}
//		if (mOutAnimation != null) {
//			mOutAnimation.setAnimationListener(null);
//		}
//
//		// logging of the view being shown is already done, as this was handled
//		// by createView()
//		switch (from) {
//		case RIGHT:
//			mInAnimation = AnimationUtils.loadAnimation(this,
//					R.anim.push_left_in);
//			mOutAnimation = AnimationUtils.loadAnimation(this,
//					R.anim.push_left_out);
//			// if animation is left or right then it was a swipe, and we want to re-save on entry
//			mAutoSaved = false;
//			break;
//		case LEFT:
//			mInAnimation = AnimationUtils.loadAnimation(this,
//					R.anim.push_right_in);
//			mOutAnimation = AnimationUtils.loadAnimation(this,
//					R.anim.push_right_out);
//			mAutoSaved = false;
//			break;
//		case FADE:
//			mInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
//			mOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
//			break;
//		}
//
//		// complete setup for animations...
//		mInAnimation.setAnimationListener(this);
//		mOutAnimation.setAnimationListener(this);
//
//		// drop keyboard before transition...
//		if (mCurrentView != null) {
//			InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//			inputManager.hideSoftInputFromWindow(mCurrentView.getWindowToken(),
//					0);
//		}
//
//		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
//				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
//
//		// adjust which view is in the layout container...
//		mStaleView = mCurrentView;
//		mCurrentView = next;
//		mQuestionHolder.addView(mCurrentView, lp);
//		mAnimationCompletionSet = 0;
//
//		if (mStaleView != null) {
//			// start OutAnimation for transition...
//			mStaleView.startAnimation(mOutAnimation);
//			// and remove the old view (MUST occur after start of animation!!!)
//			mQuestionHolder.removeView(mStaleView);
//		} else {
//			mAnimationCompletionSet = 2;
//		}
//		// start InAnimation for transition...
//		mCurrentView.startAnimation(mInAnimation);
//
//		String logString = "";
//		switch (from) {
//		case RIGHT:
//			logString = "next";
//			break;
//		case LEFT:
//			logString = "previous";
//			break;
//		case FADE:
//			logString = "refresh";
//			break;
//		}
//
//        Collect.getInstance().getActivityLogger().logInstanceAction(this, "showView", logString);
//
//        FormController formController = Collect.getInstance().getFormController();
//        if (formController.getEvent() == FormEntryController.EVENT_QUESTION
//                || formController.getEvent() == FormEntryController.EVENT_GROUP
//                || formController.getEvent() == FormEntryController.EVENT_REPEAT) {
//            FormEntryPrompt[] prompts = Collect.getInstance().getFormController()
//                    .getQuestionPrompts();
//            for (FormEntryPrompt p : prompts) {
//                Vector<TreeElement> attrs = p.getBindAttributes();
//                for (int i = 0; i < attrs.size(); i++) {
//                    if (!mAutoSaved && "saveIncomplete".equals(attrs.get(i).getName())) {
//                        saveDataToDisk(false, false, null, false);
//                        mAutoSaved = true;
//                    }
//                }
//            }
//        }
//	}

	// Hopefully someday we can use managed dialogs when the bugs are fixed
	/*
	 * Ideally, we'd like to use Android to manage dialogs with onCreateDialog()
	 * and onPrepareDialog(), but dialogs with dynamic content are broken in 1.5
	 * (cupcake). We do use managed dialogs for our static loading
	 * ProgressDialog. The main issue we noticed and are waiting to see fixed
	 * is: onPrepareDialog() is not called after a screen orientation change.
	 * http://code.google.com/p/android/issues/detail?id=1639
	 */

	//
	/**
	 * Creates and displays a dialog displaying the violated constraint.
	 */
	private void createConstraintToast(FormIndex index, int saveStatus) {
		FormController formController = Collect.getInstance()
				.getFormController();
		String constraintText;
		switch (saveStatus) {
		case FormEntryController.ANSWER_CONSTRAINT_VIOLATED:
			Collect.getInstance()
					.getActivityLogger()
					.logInstanceAction(this,
							"createConstraintToast.ANSWER_CONSTRAINT_VIOLATED",
							"show", index);
			constraintText = formController
					.getQuestionPromptConstraintText(index);
			if (constraintText == null) {
				constraintText = formController.getQuestionPrompt(index)
						.getSpecialFormQuestionText("constraintMsg");
				if (constraintText == null) {
					constraintText = formController.getQuestionPrompt(index).getQuestionText() + " "
                            + "is invalid.";
				}
			}
			break;
		case FormEntryController.ANSWER_REQUIRED_BUT_EMPTY:
			Collect.getInstance()
					.getActivityLogger()
					.logInstanceAction(this,
							"createConstraintToast.ANSWER_REQUIRED_BUT_EMPTY",
							"show", index);
			constraintText = formController
					.getQuestionPromptRequiredText(index);
			if (constraintText == null) {
				constraintText = formController.getQuestionPrompt(index)
						.getSpecialFormQuestionText("requiredMsg");
				if (constraintText == null) {
                    constraintText = formController.getQuestionPrompt(index).getQuestionText() + " "
                            + " is required.";
				}
			}
			break;
		default:
			return;
		}

		showCustomToast(constraintText, Toast.LENGTH_SHORT);
	}

	/**
	 * Creates a toast with the specified message.
	 *
	 * @param message
	 */
	private void showCustomToast(String message, int duration) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View view = inflater.inflate(R.layout.toast_view, null);

		// set the text in the view
		TextView tv = (TextView) view.findViewById(R.id.message);
		tv.setText(message);

		Toast t = new Toast(this);
		t.setView(view);
		t.setDuration(duration);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}

//	/**
//	 * Creates and displays a dialog asking the user if they'd like to create a
//	 * repeat of the current group.
//	 */
//	private void createRepeatDialog() {
//		FormController formController = Collect.getInstance()
//				.getFormController();
//		Collect.getInstance().getActivityLogger()
//				.logInstanceAction(this, "createRepeatDialog", "show");
//		mAlertDialog = new AlertDialog.Builder(this).create();
//		mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
//		DialogInterface.OnClickListener repeatListener = new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int i) {
//				FormController formController = Collect.getInstance()
//						.getFormController();
//				switch (i) {
//				case DialogInterface.BUTTON_POSITIVE: // yes, repeat
//					Collect.getInstance()
//							.getActivityLogger()
//							.logInstanceAction(this, "createRepeatDialog",
//									"addRepeat");
//					try {
//						formController.newRepeat();
//					} catch (Exception e) {
//						FormEntryActivity.this.createErrorDialog(
//								e.getMessage(), DO_NOT_EXIT);
//						return;
//					}
//					if (!formController.indexIsInFieldList()) {
//						// we are at a REPEAT event that does not have a
//						// field-list appearance
//						// step to the next visible field...
//						// which could be the start of a new repeat group...
//						showNextView();
//					} else {
//						// we are at a REPEAT event that has a field-list
//						// appearance
//						// just display this REPEAT event's group.
//						refreshCurrentView();
//					}
//					break;
//				case DialogInterface. BUTTON_NEGATIVE: // no, no repeat
//					Collect.getInstance()
//							.getActivityLogger()
//							.logInstanceAction(this, "createRepeatDialog",
//									"showNext");
//
//                    //
//                    // Make sure the error dialog will not disappear.
//                    //
//                    // When showNextView() popups an error dialog (because of a JavaRosaException)
//                    // the issue is that the "add new repeat dialog" is referenced by mAlertDialog
//                    // like the error dialog. When the "no repeat" is clicked, the error dialog
//                    // is shown. Android by default dismisses the dialogs when a button is clicked,
//                    // so instead of closing the first dialog, it closes the second.
//                    new Thread() {
//
//                        @Override
//                        public void run() {
//                            FormEntryActivity.this.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    try {
//                                        Thread.sleep(500);
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
//                                    showNextView();
//                                }
//                            });
//                        }
//                    }.start();
//
//					break;
//				}
//			}
//		};
//		if (formController.getLastRepeatCount() > 0) {
//			mAlertDialog.setTitle(getString(R.string.leaving_repeat_ask));
//			mAlertDialog.setMessage(getString(R.string.add_another_repeat,
//					formController.getLastGroupText()));
//			mAlertDialog.setButton(getString(R.string.add_another),
//					repeatListener);
//			mAlertDialog.setButton2(getString(R.string.leave_repeat_yes),
//					repeatListener);
//
//		} else {
//			mAlertDialog.setTitle(getString(R.string.entering_repeat_ask));
//			mAlertDialog.setMessage(getString(R.string.add_repeat,
//					formController.getLastGroupText()));
//			mAlertDialog.setButton(getString(R.string.entering_repeat),
//					repeatListener);
//			mAlertDialog.setButton2(getString(R.string.add_repeat_no),
//					repeatListener);
//		}
//		mAlertDialog.setCancelable(false);
//		mBeenSwiped = false;
//		showAlertDialog();
//	}

	/**
	 * Creates and displays dialog with the given errorMsg.
	 */
	private void createErrorDialog(String errorMsg, final boolean shouldExit) {
		Collect.getInstance()
				.getActivityLogger()
				.logInstanceAction(this, "createErrorDialog",
						"show." + Boolean.toString(shouldExit));

        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            errorMsg = mErrorMessage + "\n\n" + errorMsg;
            mErrorMessage = errorMsg;
        } else {
            mAlertDialog = new AlertDialog.Builder(this).create();
            mErrorMessage = errorMsg;
        }

		mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
		mAlertDialog.setTitle(getString(R.string.error_occured));
		mAlertDialog.setMessage(errorMsg);
		DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int i) {
				switch (i) {
				case DialogInterface.BUTTON_POSITIVE:
					Collect.getInstance().getActivityLogger()
							.logInstanceAction(this, "createErrorDialog", "OK");
					if (shouldExit) {
                        mErrorMessage = null;
						finish();
					}
					break;
				}
			}
		};
		mAlertDialog.setCancelable(false);
		mAlertDialog.setButton(getString(R.string.ok), errorListener);
		showAlertDialog();
	}

//	/**
//	 * Creates a confirm/cancel dialog for deleting repeats.
//	 */
//	private void createDeleteRepeatConfirmDialog() {
//		Collect.getInstance()
//				.getActivityLogger()
//				.logInstanceAction(this, "createDeleteRepeatConfirmDialog",
//						"show");
//		FormController formController = Collect.getInstance()
//				.getFormController();
//		mAlertDialog = new AlertDialog.Builder(this).create();
//		mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
//		String name = formController.getLastRepeatedGroupName();
//		int repeatcount = formController.getLastRepeatedGroupRepeatCount();
//		if (repeatcount != -1) {
//			name += " (" + (repeatcount + 1) + ")";
//		}
//		mAlertDialog.setTitle(getString(R.string.delete_repeat_ask));
//		mAlertDialog
//				.setMessage(getString(R.string.delete_repeat_confirm, name));
//		DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int i) {
//				FormController formController = Collect.getInstance()
//						.getFormController();
//				switch (i) {
//				case DialogInterface.BUTTON_POSITIVE: // yes
//					Collect.getInstance()
//							.getActivityLogger()
//							.logInstanceAction(this,
//									"createDeleteRepeatConfirmDialog", "OK");
//					formController.deleteRepeat();
//					showPreviousView();
//					break;
//				case DialogInterface. BUTTON_NEGATIVE: // no
//					Collect.getInstance()
//							.getActivityLogger()
//							.logInstanceAction(this,
//									"createDeleteRepeatConfirmDialog", "cancel");
//					break;
//				}
//			}
//		};
//		mAlertDialog.setCancelable(false);
//		mAlertDialog.setButton(getString(R.string.discard_group), quitListener);
//		mAlertDialog.setButton2(getString(R.string.delete_repeat_no),
//				quitListener);
//		showAlertDialog();
//	}

	/**
	 * Saves data and writes it to disk. If exit is set, program will exit after
	 * save completes. Complete indicates whether the user has marked the
	 * isntancs as complete. If updatedSaveName is non-null, the instances
	 * content provider is updated with the new name
	 */
    // by default, save the current screen
    private boolean saveDataToDisk(boolean exit, boolean complete, String updatedSaveName) {
        return saveDataToDisk(exit, complete, updatedSaveName, true);
    }

    // but if you want save in the background, can't be current screen
    private boolean saveDataToDisk(boolean exit, boolean complete, String updatedSaveName,
            boolean current) {
        // save current answer
        if (!saveAnswers(EVALUATE_CONSTRAINTS)) {
            Toast.makeText(this, getString(R.string.data_saved_error), Toast.LENGTH_SHORT)
                    .show();
            return false;
        }

        synchronized (saveDialogLock) {
            mSaveToDiskTask = new SaveToDiskTask(getIntent().getData(), exit, complete,
                    updatedSaveName);
            mSaveToDiskTask.setFormSavedListener(this);
            mAutoSaved = true;
            showDialog(SAVING_DIALOG);
            // show dialog before we execute...
            mSaveToDiskTask.execute();
        }

        return true;
    }

	/**
	 * Create a dialog with options to save and exit, save, or quit without
	 * saving
	 */
	private void createQuitDialog() {
		FormController formController = Collect.getInstance()
				.getFormController();
		String[] items;
        String[] two = { getString(R.string.keep_changes),
                getString(R.string.do_not_save) };
        items = two;

		Collect.getInstance().getActivityLogger()
				.logInstanceAction(this, "createQuitDialog", "show");
		showAlertDialog();
	}

	/**
	 * this method cleans up unneeded files when the user selects 'discard and
	 * exit'
	 */
	private void removeTempInstance() {
		FormController formController = Collect.getInstance()
				.getFormController();

		// attempt to remove any scratch file
		File temp = SaveToDiskTask.savepointFile(formController
				.getInstancePath());
		if (temp.exists()) {
			temp.delete();
		}

		String selection = InstanceColumns.INSTANCE_FILE_PATH + "=?";
		String[] selectionArgs = { formController.getInstancePath()
				.getAbsolutePath() };

		boolean erase = false;
		{
			Cursor c = null;
			try {
				c = getContentResolver().query(InstanceColumns.CONTENT_URI,
						null, selection, selectionArgs, null);
				erase = (c.getCount() < 1);
			} finally {
				if (c != null) {
					c.close();
				}
			}
		}

		// if it's not already saved, erase everything
		if (erase) {
			// delete media first
			String instanceFolder = formController.getInstancePath()
					.getParent();
			Log.i(TAG, "attempting to delete: " + instanceFolder);
			int images = MediaUtils
					.deleteImagesInFolderFromMediaProvider(formController
							.getInstancePath().getParentFile());
			int audio = MediaUtils
					.deleteAudioInFolderFromMediaProvider(formController
							.getInstancePath().getParentFile());
			int video = MediaUtils
					.deleteVideoInFolderFromMediaProvider(formController
							.getInstancePath().getParentFile());

			Log.i(TAG, "removed from content providers: " + images
					+ " image files, " + audio + " audio files," + " and "
					+ video + " video files.");
			File f = new File(instanceFolder);
			if (f.exists() && f.isDirectory()) {
				for (File del : f.listFiles()) {
					Log.i(TAG, "deleting file: " + del.getAbsolutePath());
					del.delete();
				}
				f.delete();
			}
		}
	}

	/**
	 * Confirm clear answer dialog
	 */
	private void createClearDialog(final QuestionWidget qw) {
		Collect.getInstance()
				.getActivityLogger()
				.logInstanceAction(this, "createClearDialog", "show",
						qw.getPrompt().getIndex());
		mAlertDialog = new AlertDialog.Builder(this).create();
		mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);

		mAlertDialog.setTitle(getString(R.string.clear_answer_ask));

		String question = qw.getPrompt().getLongText();
		if (question == null) {
			question = "";
		}
		if (question.length() > 50) {
			question = question.substring(0, 50) + "...";
		}

		mAlertDialog.setMessage(getString(R.string.clearanswer_confirm,
				question));

		DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int i) {
				switch (i) {
				case DialogInterface.BUTTON_POSITIVE: // yes
					Collect.getInstance()
							.getActivityLogger()
							.logInstanceAction(this, "createClearDialog",
									"clearAnswer", qw.getPrompt().getIndex());
					clearAnswer(qw);
					saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
					break;
				case DialogInterface. BUTTON_NEGATIVE: // no
					Collect.getInstance()
							.getActivityLogger()
							.logInstanceAction(this, "createClearDialog",
                                    "cancel", qw.getPrompt().getIndex());
					break;
				}
			}
		};
		mAlertDialog.setCancelable(false);
		mAlertDialog
				.setButton(getString(R.string.discard_answer), quitListener);
		mAlertDialog.setButton2(getString(R.string.clear_answer_no),
				quitListener);
		showAlertDialog();
	}
//
//	/**
//	 * Creates and displays a dialog allowing the user to set the language for
//	 * the form.
//	 */
//	private void createLanguageDialog() {
//		Collect.getInstance().getActivityLogger()
//				.logInstanceAction(this, "createLanguageDialog", "show");
//		FormController formController = Collect.getInstance()
//				.getFormController();
//		final String[] languages = formController.getLanguages();
//		int selected = -1;
//		if (languages != null) {
//			String language = formController.getLanguage();
//			for (int i = 0; i < languages.length; i++) {
//				if (language.equals(languages[i])) {
//					selected = i;
//				}
//			}
//		}
//		mAlertDialog = new AlertDialog.Builder(this)
//				.setSingleChoiceItems(languages, selected,
//						new DialogInterface.OnClickListener() {
//							@Override
//							public void onClick(DialogInterface dialog,
//									int whichButton) {
//								FormController formController = Collect
//										.getInstance().getFormController();
//								// Update the language in the content provider
//								// when selecting a new
//								// language
//								ContentValues values = new ContentValues();
//								values.put(FormsColumns.LANGUAGE,
//										languages[whichButton]);
//								String selection = FormsColumns.FORM_FILE_PATH
//										+ "=?";
//								String selectArgs[] = { mFormPath };
//								int updated = getContentResolver().update(
//										FormsColumns.CONTENT_URI, values,
//										selection, selectArgs);
//								Log.i(TAG, "Updated language to: "
//										+ languages[whichButton] + " in "
//										+ updated + " rows");
//
//								Collect.getInstance()
//										.getActivityLogger()
//										.logInstanceAction(
//												this,
//												"createLanguageDialog",
//												"changeLanguage."
//														+ languages[whichButton]);
//								formController
//										.setLanguage(languages[whichButton]);
//								dialog.dismiss();
//								if (formController.currentPromptIsQuestion()) {
//									saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
//								}
//								refreshCurrentView();
//							}
//						})
//				.setTitle(getString(R.string.change_language))
//				.setNegativeButton(getString(R.string.do_not_change),
//						new DialogInterface.OnClickListener() {
//							@Override
//							public void onClick(DialogInterface dialog,
//									int whichButton) {
//								Collect.getInstance()
//										.getActivityLogger()
//										.logInstanceAction(this,
//												"createLanguageDialog",
//												"cancel");
//							}
//						}).create();
//		showAlertDialog();
//	}

	/**
	 * We use Android's dialog management for loading/saving progress dialogs
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROGRESS_DIALOG:
			Log.e(TAG, "Creating PROGRESS_DIALOG");
			Collect.getInstance()
					.getActivityLogger()
					.logInstanceAction(this, "onCreateDialog.PROGRESS_DIALOG",
							"show");
			mProgressDialog = new ProgressDialog(this);
			DialogInterface.OnClickListener loadingButtonListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Collect.getInstance()
							.getActivityLogger()
							.logInstanceAction(this,
									"onCreateDialog.PROGRESS_DIALOG", "cancel");
					dialog.dismiss();
					mFormLoaderTask.setFormLoaderListener(null);
					FormLoaderTask t = mFormLoaderTask;
					mFormLoaderTask = null;
					t.cancel(true);
					t.destroy();
					finish();
				}
			};
			mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
			mProgressDialog.setTitle(getString(R.string.loading_form));
			mProgressDialog.setMessage(getString(R.string.please_wait));
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setCancelable(false);
			mProgressDialog.setButton(getString(R.string.cancel_loading_form),
					loadingButtonListener);
			return mProgressDialog;
		case SAVING_DIALOG:
            Log.e(TAG, "Creating SAVING_DIALOG");
			Collect.getInstance()
					.getActivityLogger()
					.logInstanceAction(this, "onCreateDialog.SAVING_DIALOG",
							"show");
			mProgressDialog = new ProgressDialog(this);
			DialogInterface.OnClickListener cancelSavingButtonListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Collect.getInstance()
							.getActivityLogger()
							.logInstanceAction(this,
									"onCreateDialog.SAVING_DIALOG", "cancel");
					dialog.dismiss();
                    cancelSaveToDiskTask();
				}
			};
			mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
			mProgressDialog.setTitle(getString(R.string.saving_form));
			mProgressDialog.setMessage(getString(R.string.please_wait));
			mProgressDialog.setIndeterminate(true);
            mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
					Collect.getInstance()
					.getActivityLogger()
					.logInstanceAction(this,
							"onCreateDialog.SAVING_DIALOG", "OnDismissListener");
                    cancelSaveToDiskTask();
                }
            });
			return mProgressDialog;
		}
		return null;
	}

    private void cancelSaveToDiskTask() {
        synchronized (saveDialogLock) {
            mSaveToDiskTask.setFormSavedListener(null);
            boolean cancelled = mSaveToDiskTask.cancel(true);
            Log.w(TAG, "Cancelled SaveToDiskTask! (" + cancelled + ")");
            mSaveToDiskTask = null;
        }
    }
	/**
	 * Dismiss any showing dialogs that we manually manage.
	 */
	private void dismissDialogs() {
		Log.e(TAG, "Dismiss dialogs");
		if (mAlertDialog != null && mAlertDialog.isShowing()) {
			mAlertDialog.dismiss();
		}
	}

	@Override
	protected void onPause() {
		FormController formController = Collect.getInstance()
				.getFormController();
		dismissDialogs();
		// make sure we're not already saving to disk. if we are, currentPrompt
		// is getting constantly updated
		if (mSaveToDiskTask == null
				|| mSaveToDiskTask.getStatus() == AsyncTask.Status.FINISHED) {
			if (mCurrentView != null && formController != null
					&& formController.currentPromptIsQuestion()) {
				saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
			}
		}

		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

        if (mErrorMessage != null) {
            if (mAlertDialog != null && !mAlertDialog.isShowing()) {
                createErrorDialog(mErrorMessage, EXIT);
            } else {
                return;
            }
        }

        FormController formController = Collect.getInstance().getFormController();
        Collect.getInstance().getActivityLogger().open();

		if (mFormLoaderTask != null) {
			mFormLoaderTask.setFormLoaderListener(this);
			if (formController == null
					&& mFormLoaderTask.getStatus() == AsyncTask.Status.FINISHED) {
				FormController fec = mFormLoaderTask.getFormController();
				if (fec != null) {
					loadingComplete(mFormLoaderTask);
				} else {
					dismissDialog(PROGRESS_DIALOG);
					FormLoaderTask t = mFormLoaderTask;
					mFormLoaderTask = null;
					t.cancel(true);
					t.destroy();
					// there is no formController -- fire MainMenu activity?
					startActivity(new Intent(this, MainMenuActivity.class));
				}
			}
		} else {
            if (formController == null) {
                // there is no formController -- fire MainMenu activity?
                startActivity(new Intent(this, MainMenuActivity.class));
                return;
            }
//            else {
//                refreshCurrentView();
//            }
		}

		if (mSaveToDiskTask != null) {
			mSaveToDiskTask.setFormSavedListener(this);
		}

		// only check the buttons if it's enabled in preferences
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		String navigation = sharedPreferences.getString(
				PreferencesActivity.KEY_NAVIGATION,
				PreferencesActivity.KEY_NAVIGATION);
		Boolean showButtons = false;
		if (navigation.contains(PreferencesActivity.NAVIGATION_BUTTONS)) {
			showButtons = true;
		}
	}
//
//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		switch (keyCode) {
//		case KeyEvent.KEYCODE_BACK:
//			Collect.getInstance().getActivityLogger()
//					.logInstanceAction(this, "onKeyDown.KEYCODE_BACK", "quit");
//			createQuitDialog();
//			return true;
//		case KeyEvent.KEYCODE_DPAD_RIGHT:
//			if (event.isAltPressed() && !mBeenSwiped) {
//				mBeenSwiped = true;
//				Collect.getInstance()
//						.getActivityLogger()
//						.logInstanceAction(this,
//								"onKeyDown.KEYCODE_DPAD_RIGHT", "showNext");
//				showNextView();
//				return true;
//			}
//			break;
//		case KeyEvent.KEYCODE_DPAD_LEFT:
//			if (event.isAltPressed() && !mBeenSwiped) {
//				mBeenSwiped = true;
//				Collect.getInstance()
//						.getActivityLogger()
//						.logInstanceAction(this, "onKeyDown.KEYCODE_DPAD_LEFT",
//								"showPrevious");
//				showPreviousView();
//				return true;
//			}
//			break;
//		}
//		return super.onKeyDown(keyCode, event);
//	}

	@Override
	protected void onDestroy() {
		if (mFormLoaderTask != null) {
			mFormLoaderTask.setFormLoaderListener(null);
			// We have to call cancel to terminate the thread, otherwise it
			// lives on and retains the FEC in memory.
			// but only if it's done, otherwise the thread never returns
			if (mFormLoaderTask.getStatus() == AsyncTask.Status.FINISHED) {
				FormLoaderTask t = mFormLoaderTask;
				mFormLoaderTask = null;
				t.cancel(true);
				t.destroy();
			}
		}
		if (mSaveToDiskTask != null) {
			mSaveToDiskTask.setFormSavedListener(null);
			// We have to call cancel to terminate the thread, otherwise it
			// lives on and retains the FEC in memory.
			if (mSaveToDiskTask.getStatus() == AsyncTask.Status.FINISHED) {
				mSaveToDiskTask.cancel(true);
				mSaveToDiskTask = null;
			}
		}

		super.onDestroy();
	}
//
//	private int mAnimationCompletionSet = 0;
//
//	private void afterAllAnimations() {
//		Log.i(TAG, "afterAllAnimations");
//		if (mStaleView != null) {
//			if (mStaleView instanceof ODKView) {
//				// http://code.google.com/p/android/issues/detail?id=8488
//				((ODKView) mStaleView).recycleDrawables();
//			}
//			mStaleView = null;
//		}
//
//		if (mCurrentView instanceof ODKView) {
//			((ODKView) mCurrentView).setFocus(this);
//		}
//		mBeenSwiped = false;
//	}
//
//	@Override
//	public void onAnimationEnd(Animation animation) {
//		Log.i(TAG, "onAnimationEnd "
//				+ ((animation == mInAnimation) ? "in"
//						: ((animation == mOutAnimation) ? "out" : "other")));
//		if (mInAnimation == animation) {
//			mAnimationCompletionSet |= 1;
//		} else if (mOutAnimation == animation) {
//			mAnimationCompletionSet |= 2;
//		} else {
//			Log.e(TAG, "Unexpected animation");
//		}
//
//		if (mAnimationCompletionSet == 3) {
//			this.afterAllAnimations();
//		}
//	}
//
//	@Override
//	public void onAnimationRepeat(Animation animation) {
//		// Added by AnimationListener interface.
//		Log.i(TAG, "onAnimationRepeat "
//				+ ((animation == mInAnimation) ? "in"
//						: ((animation == mOutAnimation) ? "out" : "other")));
//	}
//
//	@Override
//	public void onAnimationStart(Animation animation) {
//		// Added by AnimationListener interface.
//		Log.i(TAG, "onAnimationStart "
//				+ ((animation == mInAnimation) ? "in"
//						: ((animation == mOutAnimation) ? "out" : "other")));
//	}

	/**
	 * loadingComplete() is called by FormLoaderTask once it has finished
	 * loading a form.
	 */
	@Override
	public void loadingComplete(FormLoaderTask task) {
		dismissDialog(PROGRESS_DIALOG);

		FormController formController = task.getFormController();
		boolean pendingActivityResult = task.hasPendingActivityResult();
		boolean hasUsedSavepoint = task.hasUsedSavepoint();
		int requestCode = task.getRequestCode(); // these are bogus if
													// pendingActivityResult is
													// false
		int resultCode = task.getResultCode();
		Intent intent = task.getIntent();

		mFormLoaderTask.setFormLoaderListener(null);
		FormLoaderTask t = mFormLoaderTask;
		mFormLoaderTask = null;
		t.cancel(true);
		t.destroy();
		Collect.getInstance().setFormController(formController);
		CompatibilityUtils.invalidateOptionsMenu(this);

        Collect.getInstance().setExternalDataManager(task.getExternalDataManager());

		// Set the language if one has already been set in the past
		String[] languageTest = formController.getLanguages();
		if (languageTest != null) {
			String defaultLanguage = formController.getLanguage();
			String newLanguage = "";
			String selection = FormsColumns.FORM_FILE_PATH + "=?";
			String selectArgs[] = { mFormPath };
			Cursor c = null;
			try {
				c = getContentResolver().query(FormsColumns.CONTENT_URI, null,
						selection, selectArgs, null);
				if (c.getCount() == 1) {
					c.moveToFirst();
					newLanguage = c.getString(c
							.getColumnIndex(FormsColumns.LANGUAGE));
				}
			} finally {
				if (c != null) {
					c.close();
				}
			}

			// if somehow we end up with a bad language, set it to the default
			try {
				formController.setLanguage(newLanguage);
			} catch (Exception e) {
				formController.setLanguage(defaultLanguage);
			}
		}

		if (pendingActivityResult) {
			// set the current view to whatever group we were at...
			populateViews((Preset) getIntent().getParcelableExtra("fields"));
			// process the pending activity request...
			onActivityResult(requestCode, resultCode, intent);
			return;
		}
//
//		// it can be a normal flow for a pending activity result to restore from
//		// a savepoint
//		// (the call flow handled by the above if statement). For all other use
//		// cases, the
//		// user should be notified, as it means they wandered off doing other
//		// things then
//		// returned to ODK Collect and chose Edit Saved Form, but that the
//		// savepoint for that
//		// form is newer than the last saved version of their form data.
//		if (hasUsedSavepoint) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					Toast.makeText(FormEntryActivity.this,
//							getString(R.string.savepoint_used),
//							Toast.LENGTH_LONG).show();
//				}
//			});
//		}

		// Set saved answer path
		if (formController.getInstancePath() == null) {

			// Create new answer folder.
			String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",
					Locale.ENGLISH).format(Calendar.getInstance().getTime());
			String file = mFormPath.substring(mFormPath.lastIndexOf('/') + 1,
					mFormPath.lastIndexOf('.'));
			String path = Collect.getInstance().getInstancesPath() + File.separator + file + "_"
					+ time;
			if (FileUtils.createFolder(path)) {
				formController.setInstancePath(new File(path + File.separator
						+ file + "_" + time + ".xml"));
			}
		}
//        else {
//			Intent reqIntent = getIntent();
//			boolean showFirst = reqIntent.getBooleanExtra("start", false);
//
//			if (!showFirst) {
//				// we've just loaded a saved form, so start in the hierarchy
//				// view
//				Intent i = new Intent(this, FormHierarchyActivity.class);
//				startActivity(i);
//				return; // so we don't show the intro screen before jumping to
//						// the hierarchy
//			}
//		}

        Patient patient = getIntent().getParcelableExtra("patient");
        if (patient != null) {
            setTitle(patient.id + ": " + patient.givenName + " " + patient.familyName);
        } else {
            setTitle(formController.getFormTitle());
        }

		populateViews((Preset) getIntent().getParcelableExtra("fields"));
	}

	/**
	 * called by the FormLoaderTask if something goes wrong.
	 */
	@Override
	public void loadingError(String errorMsg) {
		dismissDialog(PROGRESS_DIALOG);
		if (errorMsg != null) {
			createErrorDialog(errorMsg, EXIT);
		} else {
			createErrorDialog(getString(R.string.parse_error), EXIT);
		}
	}

	/**
	 * Called by SavetoDiskTask if everything saves correctly.
	 */
	@Override
	public void savingComplete(SaveResult saveResult) {
		dismissDialog(SAVING_DIALOG);

        int saveStatus = saveResult.getSaveResult();
        switch (saveStatus) {
		case SaveToDiskTask.SAVED:
//			Toast.makeText(this, getString(R.string.data_saved_ok),
//					Toast.LENGTH_SHORT).show();
			sendSavedBroadcast();
			break;
		case SaveToDiskTask.SAVED_AND_EXIT:
//			Toast.makeText(this, getString(R.string.data_saved_ok),
//					Toast.LENGTH_SHORT).show();
			sendSavedBroadcast();
			finishReturnInstance();
			break;
		case SaveToDiskTask.SAVE_ERROR:
            String message;
            if (saveResult.getSaveErrorMessage() != null) {
                message = getString(R.string.data_saved_error) + ": " + saveResult.getSaveErrorMessage();
            } else {
                message = getString(R.string.data_saved_error);
            }
            Toast.makeText(this, message,
                    Toast.LENGTH_LONG).show();
			break;
		case FormEntryController.ANSWER_CONSTRAINT_VIOLATED:
		case FormEntryController.ANSWER_REQUIRED_BUT_EMPTY:
//			refreshCurrentView();

			// get constraint behavior preference value with appropriate default
			String constraint_behavior = PreferenceManager.getDefaultSharedPreferences(this)
				.getString(PreferencesActivity.KEY_CONSTRAINT_BEHAVIOR,
					PreferencesActivity.CONSTRAINT_BEHAVIOR_DEFAULT);
//
//			// an answer constraint was violated, so we need to display the proper toast(s)
//			// if constraint behavior is on_swipe, this will happen if we do a 'swipe' to the next question
//			if (constraint_behavior.equals(PreferencesActivity.CONSTRAINT_BEHAVIOR_ON_SWIPE))
//				next();
//			// otherwise, we can get the proper toast(s) by saving with constraint check
//			else
//				saveAnswersForCurrentScreen(EVALUATE_CONSTRAINTS);

			break;
		}
	}

    @Override
    public void onProgressStep(String stepMessage) {
        this.stepMessage = stepMessage;
        if (mProgressDialog != null) {
            mProgressDialog.setMessage(getString(R.string.please_wait) + "\n\n" + stepMessage);
        }
    }

	/**
	 * Attempts to save an answer to the specified index.
	 *
	 * @param answer
	 * @param index
	 * @param evaluateConstraints
	 * @return status as determined in FormEntryController
	 */
	public int saveAnswer(IAnswerData answer, FormIndex index,
			boolean evaluateConstraints) throws JavaRosaException {
		FormController formController = Collect.getInstance()
				.getFormController();
		if (evaluateConstraints) {
			return formController.answerQuestion(index, answer);
		} else {
			formController.saveAnswer(index, answer);
			return FormEntryController.ANSWER_OK;
		}
	}

//	/**
//	 * Checks the database to determine if the current instance being edited has
//	 * already been 'marked completed'. A form can be 'unmarked' complete and
//	 * then resaved.
//	 *
//	 * @return true if form has been marked completed, false otherwise.
//	 */
//	private boolean isInstanceComplete(boolean end) {
//		FormController formController = Collect.getInstance()
//				.getFormController();
//		// default to false if we're mid form
//		boolean complete = false;
//
//		// if we're at the end of the form, then check the preferences
//		if (end) {
//			// First get the value from the preferences
//			SharedPreferences sharedPreferences = PreferenceManager
//					.getDefaultSharedPreferences(this);
//			complete = sharedPreferences.getBoolean(
//					PreferencesActivity.KEY_COMPLETED_DEFAULT, true);
//		}
//
//		// Then see if we've already marked this form as complete before
//		String selection = InstanceColumns.INSTANCE_FILE_PATH + "=?";
//		String[] selectionArgs = { formController.getInstancePath()
//				.getAbsolutePath() };
//		Cursor c = null;
//		try {
//			c = getContentResolver().query(InstanceColumns.CONTENT_URI, null,
//					selection, selectionArgs, null);
//			if (c != null && c.getCount() > 0) {
//				c.moveToFirst();
//				String status = c.getString(c
//						.getColumnIndex(InstanceColumns.STATUS));
//				if (InstanceProviderAPI.STATUS_COMPLETE.compareTo(status) == 0) {
//					complete = true;
//				}
//			}
//		} finally {
//			if (c != null) {
//				c.close();
//			}
//		}
//		return complete;
//	}
//
//	public void next() {
//		if (!mBeenSwiped) {
//			mBeenSwiped = true;
//			showNextView();
//		}
//	}

	/**
	 * Returns the instance that was just filled out to the calling activity, if
	 * requested.
	 */
	private void finishReturnInstance() {
		FormController formController = Collect.getInstance()
				.getFormController();
		String action = getIntent().getAction();
		if (Intent.ACTION_PICK.equals(action)
				|| Intent.ACTION_EDIT.equals(action)) {
			// caller is waiting on a picked form
			String selection = InstanceColumns.INSTANCE_FILE_PATH + "=?";
			String[] selectionArgs = { formController.getInstancePath()
					.getAbsolutePath() };
			Cursor c = null;
			try {
				c = getContentResolver().query(InstanceColumns.CONTENT_URI,
						null, selection, selectionArgs, null);
				if (c.getCount() > 0) {
					// should only be one...
					c.moveToFirst();
					String id = c.getString(c
							.getColumnIndex(InstanceColumns._ID));
					Uri instance = Uri.withAppendedPath(
							InstanceColumns.CONTENT_URI, id);
					setResult(RESULT_OK, new Intent().setData(instance));
				}
			} finally {
				if (c != null) {
					c.close();
				}
			}
		}
		finish();
	}
//
//	@Override
//	public boolean onDown(MotionEvent e) {
//		return false;
//	}
//
//	@Override
//	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
//			float velocityY) {
//		// only check the swipe if it's enabled in preferences
//		SharedPreferences sharedPreferences = PreferenceManager
//				.getDefaultSharedPreferences(this);
//		String navigation = sharedPreferences.getString(
//				PreferencesActivity.KEY_NAVIGATION,
//				PreferencesActivity.NAVIGATION_SWIPE);
//		Boolean doSwipe = false;
//		if (navigation.contains(PreferencesActivity.NAVIGATION_SWIPE)) {
//			doSwipe = true;
//		}
//		if (doSwipe) {
//			// Looks for user swipes. If the user has swiped, move to the
//			// appropriate screen.
//
//			// for all screens a swipe is left/right of at least
//			// .25" and up/down of less than .25"
//			// OR left/right of > .5"
//			DisplayMetrics dm = new DisplayMetrics();
//			getWindowManager().getDefaultDisplay().getMetrics(dm);
//			int xPixelLimit = (int) (dm.xdpi * .25);
//			int yPixelLimit = (int) (dm.ydpi * .25);
//
//			if (mCurrentView instanceof ODKView) {
//				if (((ODKView) mCurrentView).suppressFlingGesture(e1, e2,
//						velocityX, velocityY)) {
//					return false;
//				}
//			}
//
//			if (mBeenSwiped) {
//				return false;
//			}
//
//			if ((Math.abs(e1.getX() - e2.getX()) > xPixelLimit && Math.abs(e1
//					.getY() - e2.getY()) < yPixelLimit)
//					|| Math.abs(e1.getX() - e2.getX()) > xPixelLimit * 2) {
//				mBeenSwiped = true;
//				if (velocityX > 0) {
//					if (e1.getX() > e2.getX()) {
//						Log.e(TAG,
//								"showNextView VelocityX is bogus! " + e1.getX()
//										+ " > " + e2.getX());
//						Collect.getInstance().getActivityLogger()
//								.logInstanceAction(this, "onFling", "showNext");
//						showNextView();
//					} else {
//						Collect.getInstance()
//								.getActivityLogger()
//								.logInstanceAction(this, "onFling",
//										"showPrevious");
//						showPreviousView();
//					}
//				} else {
//					if (e1.getX() < e2.getX()) {
//						Log.e(TAG,
//								"showPreviousView VelocityX is bogus! "
//										+ e1.getX() + " < " + e2.getX());
//						Collect.getInstance()
//								.getActivityLogger()
//								.logInstanceAction(this, "onFling",
//										"showPrevious");
//						showPreviousView();
//					} else {
//						Collect.getInstance().getActivityLogger()
//								.logInstanceAction(this, "onFling", "showNext");
//						showNextView();
//					}
//				}
//				return true;
//			}
//		}
//
//		return false;
//	}
//
//	@Override
//	public void onLongPress(MotionEvent e) {
//	}
//
//	@Override
//	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
//			float distanceY) {
//		// The onFling() captures the 'up' event so our view thinks it gets long
//		// pressed.
//		// We don't wnat that, so cancel it.
//        if (mCurrentView != null) {
//            mCurrentView.cancelLongPress();
//        }
//		return false;
//	}
//
//	@Override
//	public void onShowPress(MotionEvent e) {
//	}
//
//	@Override
//	public boolean onSingleTapUp(MotionEvent e) {
//		return false;
//	}
//
//	@Override
//	public void advance() {
//		next();
//	}

    public static void hideKeyboard(Context context, View view) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

	@Override
	protected void onStart() {
		super.onStart();
		Collect.getInstance().getActivityLogger().logOnStart(this);
	}

	@Override
	protected void onStop() {
		Collect.getInstance().getActivityLogger().logOnStop(this);
		super.onStop();
	}

	private void sendSavedBroadcast() {
		Intent i = new Intent();
		i.setAction("org.odk.collect.android.FormSaved");
		this.sendBroadcast(i);
	}

    @Override
    public void onSavePointError(String errorMessage) {
        if (errorMessage != null && errorMessage.trim().length() > 0) {
            Toast.makeText(this, getString(R.string.save_point_error, errorMessage), Toast.LENGTH_LONG).show();
        }
    }

    private void showAlertDialog() {
        if (mAlertDialog == null) {
            return;
        }

        mAlertDialog.show();

        // Increase text sizes in dialog, which must be done after the alert is shown when not
        // specifying a custom alert dialog theme or layout.
        TextView[] views = {
                (TextView) mAlertDialog.findViewById(android.R.id.message),
                mAlertDialog.getButton(DialogInterface.BUTTON_NEGATIVE),
                mAlertDialog.getButton(DialogInterface.BUTTON_NEUTRAL),
                mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE)

        };
        for (TextView view : views) {
            if (view != null) {
                view.setTextSize(ALERT_DIALOG_TEXT_SIZE);
                view.setPadding(
                        ALERT_DIALOG_PADDING, ALERT_DIALOG_PADDING,
                        ALERT_DIALOG_PADDING, ALERT_DIALOG_PADDING);
            }
        }

        // Title should be bigger than message and button text.
        int alertTitleResource = getResources().getIdentifier("alertTitle", "id", "android");
        TextView title = (TextView)mAlertDialog.findViewById(alertTitleResource);
        if (title != null) {
            title.setTextSize(ALERT_DIALOG_TITLE_TEXT_SIZE);
            title.setPadding(
                    ALERT_DIALOG_PADDING, ALERT_DIALOG_PADDING,
                    ALERT_DIALOG_PADDING, ALERT_DIALOG_PADDING);
        }
    }

    /**
     * Returns true iff no values in the form have changed from the
     * pre-populated defaults. This will return true even if a value
     * has been given and subsequently cleared.
     *
     * @return true iff no form values have changed, false otherwise
     */
    private boolean allContentsUnchanged() {
        Map<FormIndex, IAnswerData> answers = getAnswers();

        // Compare each individual answer to the original answers (after
        // pre-population), returning false for any unmatched answer.
        for (Map.Entry<FormIndex, IAnswerData> entry : answers.entrySet()) {
            // Double-check the key even exists before continuing to avoid
            // a NullPointerException, but this should never happen in
            // practice.
            if (!mOriginalAnswerData.containsKey(entry.getKey())) {
                return false;
            }

            // Check for differences based on the display text of answers. This
            // has the (rare) downside that different values may be considered
            // equal if they have the same display text, but equals() is not
            // reliable for IAnswerData objects.
            String newText = getDisplayText(entry.getValue());
            String oldText = getDisplayText(mOriginalAnswerData.get(entry.getKey()));
            if (!equalOrBothNull(newText, oldText)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Retrieves the displayed text for an {@link IAnswerData} object.
     *
     * @param data the field
     * @return the displayed text, or null if the data is null
     */
    @Nullable
    private String getDisplayText(@Nullable IAnswerData data) {
        if (data == null) {
            return null;
        }

        return data.getDisplayText();
    }

    /**
     * Checks if the given objects are both equal or both null (equivalent to Objects.equals,
     * but without the requirement for API level 19).
     * @param obj1 the first object to compare
     * @param obj2 the second object to compare
     * @return true if obj1 == obj2 or obj1 and obj2 are both null, false otherwise
     */
    private boolean equalOrBothNull(Object obj1, Object obj2) {
        // Check if the Objects refer to the same Object or are both null.
        if (obj1 == obj2) {
            return true;
        }

        // If either Object is null, then the two cannot be equal.
        if (obj1 == null || obj2 == null) {
            return false;
        }

        return obj1.equals(obj2);
    }
}
