package com.ryansteckler.nlpunbounce;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.ryansteckler.nlpunbounce.helpers.UidNameResolver;
import com.ryansteckler.nlpunbounce.models.UnbounceStatsCollection;
import com.ryansteckler.nlpunbounce.models.WakelockStats;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.ryansteckler.nlpunbounce.WakelockDetailFragment.FragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link WakelockDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WakelockDetailFragment extends BaseDetailFragment {

    public long getSeconds() {
        EditText editSeconds = (EditText) getActivity().findViewById(R.id.editWakelockSeconds);
        String text = editSeconds.getText().toString();
        long seconds = Long.parseLong(text);
        return seconds;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView description = (TextView) view.findViewById(R.id.textViewDescription);
        String descriptionText = description.getText().toString();



        descriptionText = descriptionText + "\n\n" + "Package Name: " + mStat.getDerivedPackageName(getActivity().getApplicationContext());


        description.setText(descriptionText);

        SharedPreferences prefs = getActivity().getSharedPreferences(AlarmDetailFragment.class.getPackage().getName() + "_preferences", Context.MODE_WORLD_READABLE);

        final EditText edit = (EditText) view.findViewById(R.id.editWakelockSeconds);

        String blockSeconds = "wakelock_" + mStat.getName() + "_seconds";
        edit.setText(String.valueOf(prefs.getLong(blockSeconds, 240)));
        edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                return i == EditorInfo.IME_ACTION_DONE && handleTextChange(textView, edit);
            }
        });
        edit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) {
                    handleTextChange((TextView) view, edit);
                }
            }
        });

        final Switch onOff = (Switch) view.findViewById(R.id.switchStat);
        String blockName = "wakelock_" + mStat.getName() + "_enabled";
        boolean enabled = prefs.getBoolean(blockName, false);
        onOff.setChecked(enabled);

        getView().findViewById(R.id.editWakelockSeconds).setEnabled(onOff.isChecked());

        View panel = (View) getView().findViewById(R.id.settingsPanel);
        TypedValue backgroundValue = new TypedValue();
        Resources.Theme theme = getActivity().getTheme();
        int resId = enabled ? R.attr.background_panel_enabled : R.attr.background_panel_disabled;
        boolean success = theme.resolveAttribute(resId, backgroundValue, true);
        Drawable backgroundColor = getResources().getDrawable(R.drawable.header_background_dark);
        if (success) {
            backgroundColor = getResources().getDrawable(backgroundValue.resourceId);
        }
        panel.setBackgroundDrawable(backgroundColor);
        panel.setAlpha(enabled ? 1 : (float) .4);

    }

    private boolean handleTextChange(TextView textView, EditText edit) {
        try {
            long seconds = Long.parseLong(textView.getText().toString());
            if (!mTaskerMode) {
                //Save to prefs
                SharedPreferences prefs = getActivity().getSharedPreferences(WakelockDetailFragment.class.getPackage().getName() + "_preferences", Context.MODE_WORLD_READABLE);
                String blockName = "wakelock_" + mStat.getName() + "_seconds";
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(blockName, seconds);
                editor.apply();
            }

            textView.clearFocus();
            // hide virtual keyboard
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
            return true;

        } catch (NumberFormatException nfe) {
            //Not a number.  Android let us down.
        }
        return false;
    }

    @Override
    protected void warnUnknown(final Switch onOff) {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.alert_unknown_wakelock_title)
                .setMessage(R.string.alert_unknown_wakelock_content)
                .setPositiveButton(R.string.dialog_unbounce, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        onOff.setChecked(true);
                        updateEnabled(true);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //don't set the switch
                        onOff.setChecked(false);
                        updateEnabled(false);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    protected void updateEnabled(boolean b) {
        String blockName = "wakelock_" + mStat.getName() + "_enabled";
        if (!mTaskerMode) {
            //Save to prefs
            SharedPreferences prefs = getActivity().getSharedPreferences("com.ryansteckler.nlpunbounce" + "_preferences", Context.MODE_WORLD_READABLE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(blockName, b);
            editor.apply();
        }

        //Enable or disable the seconds setting.
        getView().findViewById(R.id.editWakelockSeconds).setEnabled(b);
        View panel = (View) getView().findViewById(R.id.settingsPanel);
        TypedValue backgroundValue = new TypedValue();
        Resources.Theme theme = getActivity().getTheme();
        int resId = b ? R.attr.background_panel_enabled : R.attr.background_panel_disabled;
        boolean success = theme.resolveAttribute(resId, backgroundValue, true);
        Drawable backgroundColor = getResources().getDrawable(R.drawable.header_background_dark);
        if (success) {
            backgroundColor = getResources().getDrawable(backgroundValue.resourceId);
        }
        panel.setBackgroundDrawable(backgroundColor);
        panel.setAlpha(b ? 1 : (float) .4);

        if (mClearListener != null) {
            mClearListener.onCleared();
        }
    }

    @Override
    protected void loadStatsFromSource(View view) {
        UnbounceStatsCollection coll = UnbounceStatsCollection.getInstance();
        mStat = coll.getWakelockStats(getActivity(), mStat.getName());

        TextView textView = (TextView) view.findViewById(R.id.textLocalTimeAllowed);
        textView.setText(((WakelockStats) mStat).getDurationAllowedFormatted());
        textView = (TextView) view.findViewById(R.id.textLocalTimeBlocked);
        textView.setText(((WakelockStats) mStat).getDurationBlockedFormatted());
        textView = (TextView) view.findViewById(R.id.textLocalBlocked);
        textView.setText(String.valueOf(mStat.getBlockCount()));
        textView = (TextView) view.findViewById(R.id.textLocalAcquired);
        textView.setText(String.valueOf(mStat.getAllowedCount()));
    }

    public WakelockDetailFragment() {
        // Required empty public constructor
    }

    @Override
    protected WakelockDetailFragment newInstance() {
        return new WakelockDetailFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_wakelock_detail, container, false);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        getActivity().getMenuInflater().inflate(R.menu.wakelock_detail, menu);
    }

}
