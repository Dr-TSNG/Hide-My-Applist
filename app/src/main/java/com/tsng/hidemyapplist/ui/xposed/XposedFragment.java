package com.tsng.hidemyapplist.ui.xposed;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.tsng.hidemyapplist.R;
import com.tsng.hidemyapplist.TemplateManageActivity;

public class XposedFragment extends Fragment implements View.OnClickListener {

    View root;
    Activity main;
    private boolean getXposedStatus() { return false; }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_xposed, container, false);
        main = getActivity();
        boolean isXposedActivated = getXposedStatus();
        if(isXposedActivated)
            root.findViewById(R.id.xposed_activated).setVisibility(View.VISIBLE);
        else
            root.findViewById(R.id.xposed_not_activated).setVisibility(View.VISIBLE);
        root.findViewById(R.id.xposed_tv_template_manage).setOnClickListener(this);
        root.findViewById(R.id.xposed_tv_scope_manage).setOnClickListener(this);
        return root;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.xposed_tv_template_manage:
                startActivity(new Intent(main, TemplateManageActivity.class));
                break;
            case R.id.xposed_tv_scope_manage:

                break;
        }
    }
}