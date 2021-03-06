package me.gensh.helloustb;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import me.gensh.adapter.GuidePagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class GuideActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener {
    private ViewPager view_pager;
    private GuidePagerAdapter guidePager;
    private List<View> views;

    private ImageView[] dot_img;
    private int[] img_id ={R.id.guide_dot_1,R.id.guide_dot_2,R.id.guide_dot_3};

    private int[] guide_bg ={0xFFCCCC99,0xFF3CB371,0xFFEC961B};	//三种背景色

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        initViews();
        initDotsAndBg();
        view_pager.setBackgroundColor(guide_bg[0]);
    }

    protected void initViews()
    {
        LayoutInflater inflater  = LayoutInflater.from(this);
        views = new ArrayList<>();
        views.add(inflater.inflate(R.layout.guide_one, null));
        views.add(inflater.inflate(R.layout.guide_two, null));
        views.add(inflater.inflate(R.layout.guide_three, null));

        guidePager = new GuidePagerAdapter(views,this);
        view_pager = findViewById(R.id.guide_pager);
        view_pager.setAdapter(guidePager);
        view_pager.setOnPageChangeListener(this);
    }

    protected void initDotsAndBg()
    {
        dot_img = new ImageView[views.size()];
        for(int i=0;i<views.size();i++)
        {
            dot_img[i] = findViewById(img_id[i]);
        }
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {

    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {

    }

    @Override
    public void onPageSelected(int id) {
        for(int i=0;i<img_id.length;i++)
        {
            if(id == i)
            {
                dot_img[i].setImageResource(R.drawable.guide_dot_selected);
            }else{
                dot_img[i].setImageResource(R.drawable.guide_dot);
            }
        }
        //更改背景色
        view_pager.setBackgroundColor(guide_bg[id]);
    }

    public void	startNewActivity(View view){
        int id=view.getId();
        if(id == R.id.start)
        {
            Intent newactivity=new Intent(this,MainActivity.class);
            startActivity(newactivity);
            finish();
        }
    }
}
