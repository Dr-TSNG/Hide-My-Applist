package com.tsng.hidemyapplist.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.tsng.hidemyapplist.R;

public class XposedFragment extends Fragment implements View.OnClickListener {

    View root;
    Activity main;
    boolean isHookSelf, isHookSelfOnCreate;

    private boolean getXposedStatus() {
        return false;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_xposed, container, false);
        main = getActivity();
        root.findViewById(getXposedStatus() ? R.id.xposed_activated : R.id.xposed_not_activated).setVisibility(View.VISIBLE);
        Button btn_HookSelf = root.findViewById(R.id.hook_self);
        isHookSelf = isHookSelfOnCreate = getActivity().getSharedPreferences("Templates", Context.MODE_WORLD_READABLE).getBoolean("HookSelf", false);
        btn_HookSelf.setBackgroundColor(isHookSelf ? ContextCompat.getColor(getContext(), R.color.cyan_A700) : Color.GRAY);
        btn_HookSelf.setOnClickListener(this);
        root.findViewById(R.id.xposed_tv_template_manage).setOnClickListener(this);
        root.findViewById(R.id.xposed_tv_scope_manage).setOnClickListener(this);
        return root;
    }

    @Override
    public void onClick(View v) {
        if(!getXposedStatus())
            Toast.makeText(getContext(), getString(R.string.xposed_actvate_first), Toast.LENGTH_SHORT).show();
        else switch (v.getId()) {
            case R.id.hook_self:
                isHookSelf = !isHookSelf;
                getActivity().getSharedPreferences("Templates", Context.MODE_WORLD_READABLE).edit().putBoolean("HookSelf", isHookSelf).apply();
                v.setBackgroundColor(isHookSelf ? ContextCompat.getColor(getContext(), R.color.cyan_A700) : Color.GRAY);
                Toast.makeText(getContext(), getString(R.string.xposed_restart_self_to_apply), Toast.LENGTH_SHORT).show();
                break;
            case R.id.xposed_tv_template_manage:
                if (isHookSelfOnCreate)
                    Toast.makeText(getContext(), getString(R.string.xposed_disable_hook_self_first), Toast.LENGTH_SHORT).show();
                else
                    startActivity(new Intent(main, TemplateManageActivity.class));
                break;
            case R.id.xposed_tv_scope_manage:
                if (isHookSelfOnCreate)
                    Toast.makeText(getContext(), getString(R.string.xposed_disable_hook_self_first), Toast.LENGTH_SHORT).show();
                else
                    startActivity(new Intent(main, ScopeManageActivity.class));
                break;
        }
    }
}