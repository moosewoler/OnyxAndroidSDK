/**
 * 
 */
package com.onyx.android.sdk.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.onyx.android.sdk.R;
import com.onyx.android.sdk.device.EpdController;
import com.onyx.android.sdk.device.EpdController.EPDMode;
import com.onyx.android.sdk.ui.data.NumberButtonAdapter;

/**
 * @author dxwts
 *
 */
public class DialogGotoPage extends OnyxDialogBase
{
    public static interface AcceptNumberListener {
        void onAcceptNumber(int num);
    }

    private AcceptNumberListener mAcceptNumberListener = new AcceptNumberListener()
    {

        @Override
        public void onAcceptNumber(int num)
        {
            // do nothing
        }
    };
    public void setAcceptNumberListener(AcceptNumberListener l)
    {
        if (l == null) {
            l = new AcceptNumberListener()
            {

                @Override
                public void onAcceptNumber(int num)
                {
                    // do nothing
                }
            };
        }

        mAcceptNumberListener = l;
    }

    private NumberButtonAdapter mAdapter = null;
    private String mPageNum = null;
    private TextView mTextViewPageNum;
    private DialogReaderMenu mReaderMenu = null;
    
    private EPDMode mEpdModeBackup = EPDMode.AUTO;

    public DialogGotoPage(Context context, DialogReaderMenu readerMenu)
    {
        super(context);

        mReaderMenu = readerMenu;
        init(context);
    }

    public DialogGotoPage(Context context)
    {
        super(context);

        init(context);
    }
    
    private void init(Context context)
    {
        setContentView(R.layout.dialog_go_to_page);
        
        GridView numGridView = (GridView)findViewById(R.id.button_gridview);
        mTextViewPageNum = (TextView)findViewById(R.id.textview_page_num);

        mAdapter = new NumberButtonAdapter(context); 
        numGridView.setAdapter(mAdapter);
        numGridView.setOnItemClickListener(new OnItemClickListener()
        {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                String itemText = mAdapter.getItemText(position);
                if (itemText != ("delete") && itemText !=("OK")) {
                    if (mPageNum == null) {
                        mPageNum = itemText;
                    }
                    else {
                        mPageNum = mPageNum + itemText;
                    }
                    mTextViewPageNum.setText(mPageNum);
                }
                if (itemText.equals("delete") && mPageNum !=null && mPageNum.length() > 0) {
                    mPageNum = mPageNum.substring(0, mPageNum.length() - 1);
                    mTextViewPageNum.setText(mPageNum);
                }
                if(itemText.equals("OK") && mPageNum != null && mPageNum.length()> 0) {
                    int goNum = 0;
                    try {
                        goNum = Integer.parseInt(mPageNum);
                    }
                    catch (NumberFormatException e) {
                        Toast.makeText(getContext(), R.string.the_number_is_too_large, Toast.LENGTH_LONG).show();
                    }

                    if (goNum > 0) {
                        DialogGotoPage.this.cancel();
                        mAcceptNumberListener.onAcceptNumber(goNum);
                    }
                }
            }
        });

        ImageButton btnClose = (ImageButton)findViewById(R.id.close_button);
        btnClose.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                DialogGotoPage.this.cancel();
            }
        });
        
        this.setOnShowListener(new OnShowListener()
        {
            
            @Override
            public void onShow(DialogInterface dialog)
            {
                mEpdModeBackup = EpdController.getMode();
                EpdController.setMode(DialogGotoPage.this.getContext(), EPDMode.AUTO_BLACK_WHITE);
            }
        });
        this.setOnDismissListener(new OnDismissListener()
        {
            
            @Override
            public void onDismiss(DialogInterface dialog)
            {
                EpdController.setMode(DialogGotoPage.this.getContext(), mEpdModeBackup);
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (mReaderMenu != null) {
            int[] location = new int[2];
            mReaderMenu.getWindow().getDecorView().getLocationOnScreen(location);
            int readerMenuY = location[1];

            int loc[] = new int[2];
            DialogGotoPage.this.getWindow().getDecorView().getLocationOnScreen(loc);
            int dialogY = loc[1];

            if (event.getY() + dialogY < readerMenuY) {
                mReaderMenu.dismiss();
            }
        }

        return super.onTouchEvent(event);
    }
}
