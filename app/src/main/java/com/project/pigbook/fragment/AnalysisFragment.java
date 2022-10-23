package com.project.pigbook.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.project.pigbook.R;
import com.project.pigbook.fragment.abstracts.IFragment;

public class AnalysisFragment extends Fragment implements IFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analysis, container, false);

        return view;
    }

    @Override
    public boolean isExecuted() {
        return false;
    }
}
