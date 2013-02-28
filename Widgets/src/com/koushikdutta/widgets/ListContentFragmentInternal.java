package com.koushikdutta.widgets;

import junit.framework.Assert;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class ListContentFragmentInternal extends BetterListFragmentInternal {
    ViewGroup mContent;
    ViewGroup mContainer;

    public ViewGroup getContainer() {
        return mContainer;
    }
    
    public ListContentFragmentInternal(FragmentInterfaceWrapper fragment) {
        super(fragment);
    }

    @Override
    protected int getListHeaderResource() {
        return R.layout.list_content_header;
    }
    
    @Override
    protected void setPadding() {
        super.setPadding();
        float hor = getResources().getDimension(R.dimen.list_horizontal_margin);
        float ver = getResources().getDimension(R.dimen.list_vertical_margin);
        getListView().setPadding(0, 0, 0, 0);
        ver = 0;
        mContainer.setPadding((int)hor, (int)ver, (int)hor, (int)ver);
    }
    
    FragmentInterfaceWrapper mCurrentContent;
    
    @Override
    protected void onCreate(Bundle savedInstanceState, View ret) {
        mContent = (ViewGroup)ret.findViewById(R.id.content);
        mContainer = (ViewGroup)ret.findViewById(R.id.list_content_container);

        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
        super.onCreate(savedInstanceState, ret);
    }
    
    public boolean isPaged() {
        return mContent == null;
    }
    
    int getContentId() {
        if (isPaged())
            return R.id.list_content_container;
        return R.id.content;
    }
    
    static Object backstackListener;
    static Object pendingFragment;

    
    @SuppressLint("InlinedApi")
    private void onDetachNative() {
        Activity fa = getActivity();
        fa.getFragmentManager().popBackStack("content", android.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
        
        if (getActivity() == null || getActivity().isFinishing() || getActivity().isChangingConfigurations())
            return;
        
        if (!isPaged())
            return;
        
        if (getActivity() instanceof FragmentActivity) {
            FragmentActivity fa = (FragmentActivity)getActivity();
            fa.getSupportFragmentManager().popBackStack("content", FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        else {
            onDetachNative();
        }
    }
    
    Object listener;
    @SuppressLint("InlinedApi")
    void setContentNative(final String breadcrumb) {
        android.app.Fragment f = (android.app.Fragment)mCurrentContent;
        Activity fa = getActivity();
        final android.app.FragmentManager fm = fa.getFragmentManager();

        android.app.FragmentTransaction ft = fm.beginTransaction();
        if (isPaged()) {
            if (listener == null) {
                fm.addOnBackStackChangedListener(new android.app.FragmentManager.OnBackStackChangedListener() {
                    {
                        listener = this;
                    }
                    @Override
                    public void onBackStackChanged() {
                        android.app.Fragment f = (android.app.Fragment)getFragment();
                        if (f.isDetached() || f.isRemoving()) {
                            fm.removeOnBackStackChangedListener(this);
                            return;
                        }
                        View v = getFragment().getView();
                        if (v == null)
                            return;
                        final View l = v.findViewById(R.id.list_fragment);
                        if (l == null)
                            return;
                        if (fm.getBackStackEntryCount() > 0 && "content".equals(fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName())) {
                            l.setVisibility(View.GONE);
                        }
                        else {
                            l.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
            
            fm.popBackStack("content", android.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            ft.setBreadCrumbTitle(breadcrumb);
            ft.setBreadCrumbShortTitle(breadcrumb);
            ft.addToBackStack("content");
        }
        ft.replace(getContentId(), f, "content");
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }
    
    public void setContent(FragmentInterfaceWrapper content, boolean clearChoices, String breadcrumb) {
        mCurrentContent = content;
        if (getActivity() instanceof FragmentActivity) {
            Fragment f = (Fragment)mCurrentContent;
            FragmentActivity fa = (FragmentActivity)getActivity();
            final FragmentManager fm = fa.getSupportFragmentManager();
            FragmentTransaction ft = fa.getSupportFragmentManager().beginTransaction();
            if (isPaged()) {
                final int curSize = fm.getBackStackEntryCount();
                View v = getFragment().getView();
                Assert.assertNotNull(v);
                final View l = v.findViewById(R.id.list_fragment);
                Assert.assertNotNull(l);
                l.setVisibility(View.GONE);
                fm.addOnBackStackChangedListener(new OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        if (curSize != fm.getBackStackEntryCount())
                            return;
                        l.setVisibility(View.VISIBLE);
                        fm.removeOnBackStackChangedListener(this);
                    }
                });
                ft.addToBackStack("content");
            }
            ft.replace(getContentId(), f, "content");
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
        }
        else {
            setContentNative(breadcrumb);
        }

        if (clearChoices)
            getListView().clearChoices();
    }

    @Override
    protected int getListItemResource() {
        return R.layout.list_item_selectable;
    }

    @Override
    protected int getListFragmentResource() {
        return R.layout.list_content;
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setPadding();
    }
}
