/**
 * 
 */
package com.onyx.android.sdk.ui.dialog;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;

import com.onyx.android.sdk.R;
import com.onyx.android.sdk.ui.DirectoryGridView;
import com.onyx.android.sdk.ui.data.DirectoryItem;
import com.onyx.android.sdk.ui.data.GridViewAnnotationAdapter;
import com.onyx.android.sdk.ui.data.GridViewDirectoryAdapter;

/**
 * @author qingyue
 */
public class DialogDirectory extends OnyxDialogBase
{
    public static enum DirectoryTab {toc, bookmark, annotation};

    public static interface IGotoPageHandler
    {
        public void jumpTOC( DirectoryItem item);
        public void jumpBookmark(DirectoryItem item);
        public void jumpAnnotation(DirectoryItem item);
    }

    private IGotoPageHandler mGotoPageHandler = null;
    private TextView mTextViewTitle = null;

    public DialogDirectory(Context context, ArrayList<DirectoryItem> tocItems, ArrayList<DirectoryItem> bookmarkItems, ArrayList<AnnotationItem> annotationItems, final IGotoPageHandler gotoPageHandler, DirectoryTab tab)
    {
        super(context, R.style.full_screen_dialog);
        setContentView(R.layout.dialog_directory);
        mGotoPageHandler = gotoPageHandler;
        TabHost tab_host = (TabHost) findViewById(R.id.tabhost);
        tab_host.setup();

        TextView toc = (TextView) LayoutInflater.from(context).inflate(R.layout.onyx_tabwidget, null);
        toc.setText(R.string.tabwidget_toc);
        TextView bookmark = (TextView) LayoutInflater.from(context).inflate(R.layout.onyx_tabwidget, null);
        bookmark.setText(R.string.tabwidget_bookmark);
        TextView annotation = (TextView) LayoutInflater.from(context).inflate(R.layout.onyx_tabwidget, null);
        annotation.setText(R.string.tabwidget_annotation);

        Resources resources = context.getResources();

        tab_host.addTab(tab_host.newTabSpec(resources.getString(R.string.tabwidget_toc)).setIndicator(toc).setContent(R.id.layout_toc));
        tab_host.addTab(tab_host.newTabSpec(resources.getString(R.string.tabwidget_bookmark)).setIndicator(bookmark).setContent(R.id.layout_bookmark));
        tab_host.addTab(tab_host.newTabSpec(resources.getString(R.string.tabwidget_annotation)).setIndicator(annotation).setContent(R.id.layout_annotation));

        tab_host.setOnTabChangedListener(new OnTabChangeListener()
        {

            @Override
            public void onTabChanged(String tabId)
            {
                mTextViewTitle.setText(tabId);
            }
        });

        mTextViewTitle = (TextView) findViewById(R.id.textview_title);

        DirectoryGridView gridViewTOC = (DirectoryGridView) findViewById(R.id.gridview_toc);
        DirectoryGridView gridViewBookmark = (DirectoryGridView) findViewById(R.id.gridview_bookmark);
        DirectoryGridView gridViewAnnotation = (DirectoryGridView) findViewById(R.id.gridview_annotation);

        if (tocItems != null) {            
            GridViewDirectoryAdapter tocAdapter = new GridViewDirectoryAdapter(context, gridViewTOC.getGridView(), tocItems);
            gridViewTOC.getGridView().setAdapter(tocAdapter);
        }
        if (bookmarkItems != null) {            
            GridViewDirectoryAdapter bookmarkAdapter = new GridViewDirectoryAdapter(context, gridViewBookmark.getGridView(), bookmarkItems);
            gridViewBookmark.getGridView().setAdapter(bookmarkAdapter);
        }
        if (annotationItems != null) {            
            GridViewAnnotationAdapter annotationAdapter = new GridViewAnnotationAdapter(context, gridViewAnnotation.getGridView(), annotationItems);
            gridViewAnnotation.getGridView().setAdapter(annotationAdapter);
        }

        gridViewTOC.getGridView().setOnItemClickListener(new OnItemClickListener()
        {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                DialogDirectory.this.dismiss();
                DirectoryItem item = (DirectoryItem) view.getTag();
                mGotoPageHandler.jumpTOC(item);
            }
        });
        gridViewBookmark.getGridView().setOnItemClickListener(new OnItemClickListener()
        {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                DialogDirectory.this.dismiss();
                DirectoryItem item = (DirectoryItem) view.getTag();
                mGotoPageHandler.jumpBookmark(item);
            }
        });
        gridViewAnnotation.getGridView().setOnItemClickListener(new OnItemClickListener()
        {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                DialogDirectory.this.dismiss();
                DirectoryItem item = (DirectoryItem) view.getTag();
                mGotoPageHandler.jumpAnnotation(item);
            }
        });

        Button button_exit = (Button) findViewById(R.id.button_exit);
        button_exit.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                DialogDirectory.this.dismiss();
            }
        });

        switch (tab) {
        case toc:
            tab_host.setCurrentTab(0);
            mTextViewTitle.setText(R.string.tabwidget_toc);
            break;
        case bookmark:
            tab_host.setCurrentTab(1);
            mTextViewTitle.setText(R.string.tabwidget_bookmark);
            break;
        case annotation:
            tab_host.setCurrentTab(2);
            mTextViewTitle.setText(R.string.tabwidget_annotation);
            break;
        default:
            tab_host.setCurrentTab(0);
            mTextViewTitle.setText(R.string.tabwidget_toc);
            break;
        }
    }
}
