package io.eodc.planit.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.DateTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import io.eodc.planit.R;
import io.eodc.planit.activity.MainActivity;
import io.eodc.planit.db.PlannerContract;
import io.eodc.planit.fragment.EditAssignmentFragment;
import io.eodc.planit.listener.OnAssignmentChangeListener;

/**
 * Adapter for interfacing mAssignments to {@link RecyclerView}
 *
 * @author 2n
 */
public class AssignmentsAdapter extends RecyclerView.Adapter<AssignmentViewHolder> {

    private static final int NEVER_SHOW_DIVIDER         = 0;

    private static final int VIEW_TYPE_NORMAL           = 0;
    private static final int VIEW_TYPE_DIVIDER          = 1;
    private static final int VIEW_TYPE_NORMAL_NOTES     = 2;
    private static final int VIEW_TYPE_DIVIDER_NOTES    = 3;

    private OnAssignmentChangeListener  mListener;
    private Context                     mContext;
    private Cursor                      mAssignments;
    private Cursor                      mClasses;

    private boolean mShowAssignmnentsCompleted;
    private int     mShowDividerFlag;

    /**
     * Constructs a new instance of AssignmentsAdapter. Dynamically displays relevant information
     * about the assignment in its view depending on its position in the list and it due date.
     * <p>
     * {@link #swapAssignmentsCursor(Cursor)} should be called as soon as the mAssignments cursor is
     * available, or else nothing will be shown.
     *
     * @param context     The context to use for grabbing strings, colors, etc.
     * @param classCursor The cursor containing all the user's mClasses.
     * @param listener    The mListener listening for assignment attribute changes.
     * @see AssignmentViewHolder
     * @see OnAssignmentChangeListener
     **/
    public AssignmentsAdapter(Context context, Cursor classCursor, OnAssignmentChangeListener listener) {
        this.mContext = context;
        this.mClasses = classCursor;
        this.mListener = listener;
        this.mShowDividerFlag = -1;
        mShowAssignmnentsCompleted = false;
    }

    /**
     * Constructs a new instance of AssignmentsAdapter while specifying if dividers should be shown.
     * This constructor should be used in instances where the full list of mAssignments is not being
     * displayed, or the context of when the mAssignments are due have been established.
     *
     * @param context      The context to use for grabbing strings, colors, etc.
     * @param classCursor  The cursor containing all the user's mClasses.
     * @param listener     The mListener listening for assignment attribute changes.
     * @param showDividers Whether or not dividers should be shown.
     */
    public AssignmentsAdapter(Context context, Cursor classCursor, OnAssignmentChangeListener listener, boolean showDividers) {
        this.mContext = context;
        this.mClasses = classCursor;
        this.mListener = listener;
        this.mShowDividerFlag = showDividers ? -1 : NEVER_SHOW_DIVIDER;
        mShowAssignmnentsCompleted = false;
    }

    /**
     * Swaps the current assignment cursor, then notifies the parent {@link RecyclerView} that the
     * current data set has been changed.
     *
     * @param c The new {@link Cursor} that should be used as the data set.
     */
    public void swapAssignmentsCursor(Cursor c) {
        mAssignments = c;
        notifyDataSetChanged();
    }

    /**
     * Differentiates one assignment from another, and decides whether to show that it has notes,
     * is on a different day, etc.
     *
     * @param position Position in the cursor
     * @return Type of item view
     */
    @Override
    public int getItemViewType(int position) {
        if (mShowDividerFlag == NEVER_SHOW_DIVIDER) return VIEW_TYPE_NORMAL;
        mAssignments.moveToPosition(position);
        int notesIndex = mAssignments.getColumnIndex(PlannerContract.AssignmentColumns.NOTES);
        String notes = mAssignments.getString(notesIndex).trim();

        if (position == 0) {
            if (notes.equals("")) return VIEW_TYPE_DIVIDER;
            else return VIEW_TYPE_DIVIDER_NOTES;
        } else {
            int dueDateIndex = mAssignments.getColumnIndex(PlannerContract.AssignmentColumns.DUE_DATE);

            String[] dateSegments = mAssignments.getString(dueDateIndex).split("-");

            DateTime dtCurrent = new DateTime(Integer.valueOf(dateSegments[0]),
                    Integer.valueOf(dateSegments[1]),
                    Integer.valueOf(dateSegments[2]), 0, 0);
            DateTime dtNow = new DateTime();

            if (dtCurrent.isBeforeNow() && dtNow.getDayOfYear() - dtCurrent.getDayOfYear() > 1) { // Overdue
                if (notes.equals("")) return VIEW_TYPE_NORMAL;
                else return VIEW_TYPE_NORMAL_NOTES;
            }

            mAssignments.moveToPosition(position - 1);

            dateSegments = mAssignments.getString(dueDateIndex).split("-");

            DateTime dtLast = new DateTime(Integer.valueOf(dateSegments[0]),
                    Integer.valueOf(dateSegments[1]),
                    Integer.valueOf(dateSegments[2]), 0, 0);
            if (dtCurrent.getYear() == dtLast.getYear()) {
                if (dtCurrent.getMonthOfYear() != dtNow.getMonthOfYear() &&
                        dtCurrent.getMonthOfYear() == dtLast.getMonthOfYear()) {
                    if (notes.equals("")) return VIEW_TYPE_NORMAL;
                    else return VIEW_TYPE_NORMAL_NOTES;
                } else if (dtCurrent.getMonthOfYear() == dtNow.getMonthOfYear()) {
                    if (dtCurrent.getWeekOfWeekyear() == dtNow.getWeekOfWeekyear()) {
                        if (dtCurrent.getDayOfYear() == dtLast.getDayOfYear()) {
                            if (notes.equals("")) return VIEW_TYPE_NORMAL;
                            else return VIEW_TYPE_NORMAL_NOTES;
                        } else {
                            if (notes.equals("")) return VIEW_TYPE_DIVIDER;
                            else return VIEW_TYPE_DIVIDER_NOTES;
                        }
                    } else {
                        if (dtLast.getWeekOfWeekyear() - dtNow.getWeekOfWeekyear() == 1 &&
                                getItemViewType(position - 1) == VIEW_TYPE_DIVIDER ||
                                dtLast.getWeekOfWeekyear() - dtNow.getWeekOfWeekyear() > 0 &&
                                        dtCurrent.getWeekOfWeekyear() - dtNow.getWeekOfWeekyear() > 0 &&
                                        getItemViewType(position - 1) == VIEW_TYPE_NORMAL) {
                            if (notes.equals("")) return VIEW_TYPE_NORMAL;
                            else return VIEW_TYPE_NORMAL_NOTES;
                        } else {
                            if (notes.equals("")) return VIEW_TYPE_DIVIDER;
                            else return VIEW_TYPE_DIVIDER_NOTES;
                        }
                    }
                } else {
                    if (notes.equals("")) return VIEW_TYPE_DIVIDER;
                    else return VIEW_TYPE_DIVIDER_NOTES;
                }
            } else {
                if (notes.equals("")) return VIEW_TYPE_DIVIDER;
                else return VIEW_TYPE_DIVIDER_NOTES;
            }
        }
    }

    @NonNull
    @Override
    public AssignmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AssignmentViewHolder(LayoutInflater.from(mContext)
                .inflate(R.layout.item_assignment, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final AssignmentViewHolder holder, int position) {
        mAssignments.moveToPosition(position);
        mClasses.moveToFirst();

        final int idIndex = mAssignments.getColumnIndex(PlannerContract.AssignmentColumns._ID);
        int dueDateIndex = mAssignments.getColumnIndex(PlannerContract.AssignmentColumns.DUE_DATE);
        int typeIndex = mAssignments.getColumnIndex(PlannerContract.AssignmentColumns.TYPE);
        final int notesIndex = mAssignments.getColumnIndex(PlannerContract.AssignmentColumns.NOTES);

        holder.id = mAssignments.getInt(idIndex);

        while (mClasses.getInt(mClasses.getColumnIndex(PlannerContract.ClassColumns._ID)) != mAssignments.getInt(mAssignments.getColumnIndex(PlannerContract.AssignmentColumns.CLASS_ID)))
            mClasses.moveToNext();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        try {
            DateTime dtCurrent = new DateTime(sdf.parse(mAssignments.getString(dueDateIndex)));

            String assignmentType = mAssignments.getString(typeIndex);

            switch (assignmentType) {
                case PlannerContract.TYPE_HOMEWORK:
                    assignmentType = "Homework";
                    break;
                case PlannerContract.TYPE_TEST:
                    assignmentType = "Test/Quiz";
                    break;
                case PlannerContract.TYPE_PROJECT:
                    assignmentType = "Project";
                    break;
            }

            SimpleDateFormat ddSdf = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
            String dueDate = ddSdf.format(dtCurrent.toDate());

            holder.dueDate.setText(dueDate);

            if (holder.getItemViewType() == VIEW_TYPE_DIVIDER ||
                    holder.getItemViewType() == VIEW_TYPE_DIVIDER_NOTES) {
                String headerText = getHeaderText(dtCurrent);
                holder.headerLabel.setText(headerText);
                holder.header.setVisibility(View.VISIBLE);
                holder.dueDateIcon.setVisibility(View.VISIBLE);
                holder.dueDate.setVisibility(View.VISIBLE);
            } else {
                DateTime dtNow = new DateTime();
                if (dtCurrent.isBeforeNow() && dtNow.getDayOfYear() - dtCurrent.getDayOfYear() > 0 ||
                        dtCurrent.isAfterNow() && dtCurrent.getWeekOfWeekyear() != dtNow.getWeekOfWeekyear() &&
                                mShowDividerFlag != NEVER_SHOW_DIVIDER) {
                    holder.dueDateIcon.setVisibility(View.VISIBLE);
                    holder.dueDate.setVisibility(View.VISIBLE);
                }
            }

            if (holder.getItemViewType() == VIEW_TYPE_DIVIDER_NOTES ||
                    holder.getItemViewType() == VIEW_TYPE_NORMAL_NOTES) {
                holder.expandButton.setVisibility(View.VISIBLE);
                holder.notes.setText(mAssignments.getString(notesIndex));
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (holder.isExpanded) {
                            holder.shrinkNotes();
                        } else {
                            holder.expandNotes();
                        }
                    }
                });
            }

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (mContext instanceof MainActivity) {
                        MainActivity activity = (MainActivity) mContext;
                        DialogFragment editFragment = EditAssignmentFragment.newInstance(holder.id,
                                mListener);

                        editFragment.show(activity.getSupportFragmentManager(), null);

                        Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                        if (v != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                v.vibrate(VibrationEffect
                                        .createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                            else v.vibrate(500);
                        }
                    }
                    return true;
                }
            });

            String classAndTypeText = mContext.getString(R.string.class_name_and_type_text,
                    mClasses.getString(mClasses.getColumnIndex(PlannerContract.ClassColumns.NAME)),
                    assignmentType);

            holder.classColor.setBackgroundColor(Color.parseColor(mClasses.getString(
                    mClasses.getColumnIndex(PlannerContract.ClassColumns.COLOR)
            )));

            holder.assignmentName.setText(mAssignments.getString(
                    mAssignments.getColumnIndex(PlannerContract.AssignmentColumns.TITLE)
            ));

            if (mShowAssignmnentsCompleted) {
                holder.assignmentName.setPaintFlags(holder.assignmentName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }

            holder.classAndTypeName.setText(classAndTypeText);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    /**
     * Specifies whether to show completed mAssignments
     *
     * @param showAssignmnentsCompleted Whether to show completed mAssignments
     */
    public void setShowAssignmnentsCompleted(boolean showAssignmnentsCompleted) {
        this.mShowAssignmnentsCompleted = showAssignmnentsCompleted;
    }

    @Override
    public int getItemCount() {
        if (mAssignments != null) return mAssignments.getCount();
        else return 0;
    }

    /**
     * Gets the text shown at the divider (header) based off of the assignment's due date and the
     * current day.
     *
     * @param dtCurrent The {@link DateTime} of the assignment with the header shown
     * @return The parsed header text
     */
    private String getHeaderText(DateTime dtCurrent) {
        DateTime dtNow = new DateTime();
        if (dtCurrent.isBeforeNow() && dtNow.getDayOfYear() - dtCurrent.getDayOfYear() > 0) {
            return "Overdue";
        }
        if (dtCurrent.getYear() == dtNow.getYear()) {
            if (dtCurrent.getMonthOfYear() == dtNow.getMonthOfYear()) {
                if (dtCurrent.getWeekOfWeekyear() == dtNow.getWeekOfWeekyear()) {
                    if (dtCurrent.getDayOfYear() == dtNow.getDayOfYear()) return mContext.getString(R.string.assignment_header_near_future_text, "Today");
                    if (dtCurrent.getDayOfYear() - dtNow.getDayOfYear() == 1) return mContext.getString(R.string.assignment_header_near_future_text, "Tomorrow");
                    else return mContext.getString(R.string.assignment_header_far_future_text,
                            String.valueOf(dtCurrent.getDayOfYear() - dtNow.getDayOfYear()) + " Days");
                } else
                    return mContext.getString(R.string.assignment_header_near_future_text, "This Month");
            } else return dtCurrent.getMonthOfYear() - dtNow.getMonthOfYear() == 1 ?
                    mContext.getString(R.string.assignment_header_near_future_text, "Next Month") :
                    mContext.getString(R.string.assignment_header_far_future_text,
                            String.valueOf(dtCurrent.getMonthOfYear() - dtNow.getMonthOfYear()) + " Months");
        } else
            return dtCurrent.getYear() - dtNow.getYear() == 1 ? mContext.getString(R.string.assignment_header_near_future_text,
                    "Next Near") :
                    mContext.getString(R.string.assignment_header_far_future_text,
                            String.valueOf(dtCurrent.getYear() - dtNow.getYear())) + " Years";
    }

}
