package org.msf.records.ui;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.Filter;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.TextView;

import org.msf.records.R;
import org.msf.records.filter.FilterGroup;
import org.msf.records.filter.FilterQueryProviderFactory;
import org.msf.records.filter.LocationUuidFilter;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.location.LocationTree;
import org.msf.records.location.LocationTree.LocationSubtree;
import org.msf.records.model.Concept;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.sync.PatientProjection;
import org.msf.records.sync.PatientProviderContract;
import org.msf.records.utils.PatientCountDisplay;

import java.util.Map;

import javax.annotation.Nullable;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Gil on 24/11/14.
 */
public class ExpandablePatientListAdapter extends CursorTreeAdapter {

    private static final String TAG = ExpandablePatientListAdapter.class.getSimpleName();

    private final Context mContext;
    private String mQueryFilterTerm;
    private SimpleSelectionFilter mFilter;

    public ExpandablePatientListAdapter(
            Cursor cursor, Context context, String queryFilterTerm,
            SimpleSelectionFilter filter) {
        super(cursor, context);
        mContext = context;
        mQueryFilterTerm = queryFilterTerm;
        mFilter = filter;
    }

    public SimpleSelectionFilter getSelectionFilter() {
        return mFilter;
    }

    public void setSelectionFilter(SimpleSelectionFilter filter) {
        mFilter = filter;
    }

    public void filter(CharSequence constraint, Filter.FilterListener listener) {
        setGroupCursor(null); // Reset the group cursor.

        // Set the query filter term as a member variable so it can be retrieved when
        // getting patients-per-tent.
        mQueryFilterTerm = constraint.toString();

        // Perform the actual filtering.
        getFilter().filter(constraint, listener);
    }

    @Override
    protected Cursor getChildrenCursor(Cursor groupCursor) {
        Cursor itemCursor = getGroup(groupCursor.getPosition());

        String tent = itemCursor.getString(PatientProjection.COUNTS_COLUMN_LOCATION_UUID);

        StringBuilder sortBuilder = new StringBuilder();

        sortBuilder.append(PatientProviderContract.PatientColumns._ID);
        sortBuilder.append(",");
        sortBuilder.append(PatientProviderContract.PatientColumns.COLUMN_NAME_FAMILY_NAME);
        sortBuilder.append(",");
        sortBuilder.append(PatientProviderContract.PatientColumns.COLUMN_NAME_GIVEN_NAME);

        // TODO: Don't use the singleton here.
        @Nullable LocationTree locationTree = LocationTree.SINGLETON_INSTANCE;
        @Nullable LocationSubtree tentSubtree = null;
        if (locationTree != null) {
        	 tentSubtree = locationTree.getLocationByUuid(tent);
        }

        FilterQueryProvider queryProvider =
                new FilterQueryProviderFactory(mContext)
                        .setSortClause(sortBuilder.toString())
                        .getFilterQueryProvider(
                                new FilterGroup(getSelectionFilter(),
                                        new LocationUuidFilter(tentSubtree)));

        Cursor patientsCursor = null;

        try {
            patientsCursor = queryProvider.runQuery(mQueryFilterTerm);
            Log.d(TAG, "childCursor " + patientsCursor.getCount());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return patientsCursor;
    }

    @Override
    protected View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.listview_tent_header, null);
    }

    @Override
    protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
        int patientCount = getChildrenCursor(cursor).getCount();
        String locationUuid = cursor.getString(PatientProjection.COUNTS_COLUMN_LOCATION_UUID);
        String tentName = context.getResources().getString(R.string.unknown_tent);
        @Nullable LocationTree locationTree = LocationTree.SINGLETON_INSTANCE;
        if (locationTree != null) {
	        	LocationSubtree location = LocationTree.SINGLETON_INSTANCE.getTentForUuid(locationUuid);
	        if (location != null) {
	            tentName = location.toString();
	        }
        }

        TextView item = (TextView) view.findViewById(R.id.patient_list_tent_tv);
        item.setText(PatientCountDisplay.getPatientCountTitle(context, patientCount, tentName));
    }

    @Override
    protected View newChildView(Context context, Cursor cursor, boolean isLastChild, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.listview_cell_search_results, parent, false);
        ViewHolder holder = new ViewHolder(view);
        view.setTag(holder);
        return view;
    }

    @Override
    protected void bindChildView(View convertView, Context context, Cursor cursor, boolean isLastChild) {

        String givenName = cursor.getString(PatientProjection.COLUMN_GIVEN_NAME);
        String familyName = cursor.getString(PatientProjection.COLUMN_FAMILY_NAME);
        String id = cursor.getString(PatientProjection.COLUMN_ID);
        String uuid = cursor.getString(PatientProjection.COLUMN_UUID);
        String gender = cursor.getString(PatientProjection.COLUMN_GENDER);
        int ageMonths = cursor.getInt(PatientProjection.COLUMN_AGE_MONTHS);
        int ageYears = cursor.getInt(PatientProjection.COLUMN_AGE_YEARS);


        // Grab observations for this patient so we can determine condition and pregnant status.
        // TODO(akalachman): Get rid of this whole block as it's inefficient.
        boolean pregnant = false;
        String condition = null;
        Map<String, LocalizedChartHelper.LocalizedObservation> observationMap =
                LocalizedChartHelper.getMostRecentObservations(mContext.getContentResolver(), uuid);
        if (observationMap != null) {
            pregnant = observationMap.containsKey(Concept.PREGNANCY_UUID) &&
                    observationMap.get(Concept.PREGNANCY_UUID).value.equals(Concept.YES_UUID);
            if (observationMap.containsKey(Concept.GENERAL_CONDITION_UUID)) {
                condition = observationMap.get(Concept.GENERAL_CONDITION_UUID).value;
            }
        }

        if (convertView == null) {
            return;
        }
        ViewHolder holder = (ViewHolder) convertView.getTag();
        holder.mPatientName.setText(givenName + " " + familyName);
        holder.mPatientId.setText(id);
        holder.mPatientId.setBackgroundResource(
                Concept.getColorResourceForGeneralCondition(condition));

        if (ageMonths > 0) {
            holder.mPatientAge.setText(
                    context.getResources().getString(R.string.age_months, ageMonths));
        } else if (ageYears > 0) {
            holder.mPatientAge.setText(
                    context.getResources().getString(R.string.age_years, ageYears));
        } else {
            holder.mPatientAge.setText(
                    context.getResources().getString(R.string.age_years, 99));
            holder.mPatientAge.setTextColor(context.getResources().getColor(R.color.transparent));
        }

        if (gender != null && gender.equals("M")) {
            holder.mPatientGender.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_gender_male));
        }

        if (gender != null && gender.equals("F")) {
            if (pregnant) {
                holder.mPatientGender.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_gender_female_pregnant));
            } else {
                holder.mPatientGender.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_gender_female));
            }
        }

        if (gender == null) {
            holder.mPatientGender.setVisibility(View.GONE);
        }

        // Add a bottom border and extra padding to the last item in each group.
        if (isLastChild) {
            convertView.setBackgroundResource(R.drawable.bottom_border_1dp);
            convertView.setPadding(
                    convertView.getPaddingLeft(), convertView.getPaddingTop(),
                    convertView.getPaddingRight(), 40);
        } else {
            convertView.setBackgroundResource(0);
            convertView.setPadding(
                    convertView.getPaddingLeft(), convertView.getPaddingTop(),
                    convertView.getPaddingRight(), 20);
        }
    }

    static class ViewHolder {
        @InjectView(R.id.listview_cell_search_results_name) TextView mPatientName;
        @InjectView(R.id.listview_cell_search_results_id) TextView mPatientId;
        @InjectView(R.id.listview_cell_search_results_gender) ImageView mPatientGender;
        @InjectView(R.id.listview_cell_search_results_age) TextView mPatientAge;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
