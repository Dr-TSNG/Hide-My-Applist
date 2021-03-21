package com.tsng.hidemyapplist.ui.xposed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.tsng.hidemyapplist.R;

public class XposedFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_xposed, container, false);
        boolean isXposedActivated = getXposedStatus();
        if(isXposedActivated)
            root.findViewById(R.id.xposed_activated).setVisibility(View.VISIBLE);
        else
            root.findViewById(R.id.xposed_not_activated).setVisibility(View.VISIBLE);
        return root;
    }

    private boolean getXposedStatus() {
        return false;
    }
}