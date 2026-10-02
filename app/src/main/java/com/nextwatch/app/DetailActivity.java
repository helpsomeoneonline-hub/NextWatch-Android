package com.nextwatch.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class DetailActivity extends Activity {
    private final int BG = Color.rgb(11,13,18);
    private final int PANEL = Color.rgb(21,25,35);
    private final int PANEL_ALT = Color.rgb(28,34,48);
    private final int TEXT = Color.rgb(244,247,251);
    private final int MUTED = Color.rgb(154,165,181);
    private final int ACCENT = Color.rgb(139,92,246);
    private final int GREEN = Color.rgb(34,197,94);
    private final int DANGER = Color.rgb(239,68,68);

    private DataStore store;
    private CatalogItem item;
    private RecommendationEngine engine;
    private LinearLayout page;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        store = new DataStore(this);
        List<CatalogItem> catalog = Catalog.items();
        engine = new RecommendationEngine(catalog, store);
        String title = getIntent().getStringExtra("title");
        for (CatalogItem c : catalog) if (c.title.equals(title)) item = c;
        if (item == null) { finish(); return; }
        build();
    }

    private void build() {
        LinearLayout root = col();
        root.setBackgroundColor(BG);
        root.setPadding(dp(14),dp(8),dp(14),dp(8));

        ScrollView scroll = new ScrollView(this);
        page = col();
        page.setPadding(0,dp(6),0,dp(32));
        scroll.addView(page,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(root);

        root.setOnApplyWindowInsetsListener((v,insets)->{
            int left=0,top=0,right=0,bottom=0;
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                left=bars.left; top=bars.top; right=bars.right; bottom=bars.bottom;
            } else {
                left=insets.getSystemWindowInsetLeft(); top=insets.getSystemWindowInsetTop();
                right=insets.getSystemWindowInsetRight(); bottom=insets.getSystemWindowInsetBottom();
            }
            v.setPadding(dp(14)+left,dp(8)+top,dp(14)+right,dp(8)+bottom);
            return insets;
        });
        root.requestApplyInsets();

        LinearLayout top=row();
        Button back=smallButton("‹ Back",PANEL_ALT);
        back.setOnClickListener(v->finish());
        top.addView(back);
        top.addView(label("NEXTWATCH",12,MUTED,true),new LinearLayout.LayoutParams(0,-2,1f));
        page.addView(top);

        FrameLayout art = poster(item,210,315);
        LinearLayout center=row();
        center.setGravity(Gravity.CENTER);
        center.addView(art);
        page.addView(center,block(20));

        int score=engine.match(item,"");
        TextView match=label(score+"% MATCH FOR YOU",13,GREEN,true);
        match.setGravity(Gravity.CENTER);
        page.addView(match,block(16));

        TextView title=label(item.title,30,TEXT,true);
        title.setGravity(Gravity.CENTER);
        page.addView(title,block(8));

        TextView meta=label(item.type+" • "+item.year+" • "+item.commitment,14,MUTED,false);
        meta.setGravity(Gravity.CENTER);
        page.addView(meta,block(5));

        LinearLayout actions=row();
        Button trailer=smallButton("▶ Trailer",ACCENT);
        trailer.setOnClickListener(v->{
            Intent i=new Intent(this,TrailerActivity.class);
            i.putExtra("title",item.title);
            startActivity(i);
        });
        Button save=smallButton(DataStore.STATUS_WATCHLIST.equals(store.status(item.title))?"✓ Watchlist":"+ Watchlist",PANEL_ALT);
        save.setOnClickListener(v->{store.setStatus(item.title,DataStore.STATUS_WATCHLIST);toast("Saved to Watchlist");buildRefresh();});
        Button watched=smallButton("✓ Watched",PANEL_ALT);
        watched.setOnClickListener(v->{store.setStatus(item.title,DataStore.STATUS_WATCHED);showRatingDialog();});
        actions.addView(trailer,actionLp());
        actions.addView(save,actionLp());
        actions.addView(watched,actionLp());
        page.addView(actions,block(18));

        LinearLayout story=card();
        story.addView(label("About",19,TEXT,true));
        story.addView(label(item.description,15,TEXT,false),block(8));
        page.addView(story,block(18));

        LinearLayout why=card();
        why.addView(label("Why NextWatch picked it",19,TEXT,true));
        why.addView(label(engine.explain(item),14,MUTED,false),block(8));
        page.addView(why,block(12));

        HorizontalScrollView hsv=new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout tags=row();
        for(String tag:item.tags){
            TextView chip=label(pretty(tag),12,TEXT,true);
            chip.setPadding(dp(12),dp(8),dp(12),dp(8));
            chip.setBackground(roundRect(PANEL_ALT,999));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,-2);
            cp.setMargins(0,0,dp(8),0);
            tags.addView(chip,cp);
        }
        hsv.addView(tags);
        page.addView(hsv,block(14));

        LinearLayout rating=card();
        Integer current=store.ratings().get(item.title);
        rating.addView(label("Your rating",19,TEXT,true));
        rating.addView(label(current==null?"Not rated yet":current+"/10",14,current==null?MUTED:GREEN,true),block(5));
        Button rate=primary(current==null?"Rate this title":"Change rating");
        rate.setOnClickListener(v->showRatingDialog());
        rating.addView(rate,block(12));
        page.addView(rating,block(18));

        LinearLayout status=card();
        status.addView(label("Status",19,TEXT,true));
        status.addView(label(statusLabel(),14,MUTED,false),block(5));
        LinearLayout r1=row();
        String[][] options={{"Watching",DataStore.STATUS_WATCHING},{"Dropped",DataStore.STATUS_DROPPED},{"Not for me",DataStore.STATUS_NOT_INTERESTED}};
        for(String[] o:options){
            Button b=secondary(o[0]);
            b.setOnClickListener(v->{store.setStatus(item.title,o[1]);toast("Status updated");buildRefresh();});
            r1.addView(b,actionLp());
        }
        status.addView(r1,block(10));
        page.addView(status,block(12));
    }

    private void buildRefresh() {
        page.removeAllViews();
        buildContentOnly();
    }

    private void buildContentOnly() {
        // Recreate the activity to keep the code path simple and the state consistent.
        recreate();
    }

    private void showRatingDialog() {
        String[] values={"10 — Masterpiece","9 — Loved it","8 — Very good","7 — Good","6 — Okay","5","4","3","2","1"};
        new AlertDialog.Builder(this)
            .setTitle("Rate • "+item.title)
            .setItems(values,(d,which)->{
                int rating=10-which;
                store.setRating(item.title,rating);
                if(store.status(item.title).isEmpty()) store.setStatus(item.title,DataStore.STATUS_WATCHED);
                toast("Saved "+rating+"/10");
                recreate();
            })
            .setNeutralButton("Remove rating",(d,w)->{store.setRating(item.title,0);toast("Rating removed");recreate();})
            .setNegativeButton("Cancel",null)
            .show();
    }

    private String statusLabel() {
        String s=store.status(item.title);
        if(DataStore.STATUS_WATCHLIST.equals(s)) return "On your Watchlist";
        if(DataStore.STATUS_WATCHED.equals(s)) return "Watched";
        if(DataStore.STATUS_WATCHING.equals(s)) return "Currently watching";
        if(DataStore.STATUS_DROPPED.equals(s)) return "Dropped";
        if(DataStore.STATUS_NOT_INTERESTED.equals(s)) return "Not interested";
        return "No status yet";
    }

    private FrameLayout poster(CatalogItem item,int w,int h) {
        FrameLayout box=new FrameLayout(this);
        box.setBackground(roundRect(PANEL_ALT,20));
        box.setClipToOutline(true);
        box.setLayoutParams(new LinearLayout.LayoutParams(dp(w),dp(h)));

        TextView fallback=label(initials(item.title),34,Color.WHITE,true);
        fallback.setGravity(Gravity.CENTER);
        fallback.setBackground(roundRect(posterColor(item.title),20));
        box.addView(fallback,new FrameLayout.LayoutParams(-1,-1));

        ImageView image=new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setVisibility(View.INVISIBLE);
        box.addView(image,new FrameLayout.LayoutParams(-1,-1));
        ArtworkService.loadPoster(this,item,image,fallback);
        return box;
    }

    private LinearLayout card(){
        LinearLayout c=col();
        c.setPadding(dp(16),dp(16),dp(16),dp(16));
        c.setBackground(roundRect(PANEL,22));
        return c;
    }
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView label(String v,int s,int c,boolean bold){TextView t=new TextView(this);t.setText(v);t.setTextSize(s);t.setTextColor(c);t.setLineSpacing(0,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button primary(String v){Button b=smallButton(v,ACCENT);b.setTextSize(15);b.setTypeface(null,Typeface.BOLD);return b;}
    private Button secondary(String v){return smallButton(v,PANEL_ALT);}
    private Button smallButton(String v,int color){Button b=new Button(this);b.setText(v);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(12);b.setMinHeight(dp(46));b.setMinWidth(0);b.setPadding(dp(10),dp(7),dp(10),dp(7));b.setBackground(roundRect(color,14));return b;}
    private LinearLayout.LayoutParams actionLp(){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1f);lp.setMargins(0,0,dp(7),0);return lp;}
    private LinearLayout.LayoutParams block(int top){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(top),0,0);return lp;}
    private GradientDrawable roundRect(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private int posterColor(String title){float hue=Math.abs(title.hashCode()%360);return Color.HSVToColor(new float[]{hue,0.55f,0.62f});}
    private String initials(String title){String[] parts=title.replaceAll("[^A-Za-z0-9 ]"," ").trim().split("\\s+");StringBuilder s=new StringBuilder();for(String p:parts){if(!p.isEmpty())s.append(p.charAt(0));if(s.length()==2)break;}return s.toString().toUpperCase(Locale.ROOT);}
    private String pretty(String tag){String s=tag.replace('-',' ');return s.substring(0,1).toUpperCase(Locale.ROOT)+s.substring(1);}
    private void toast(String v){Toast.makeText(this,v,Toast.LENGTH_SHORT).show();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
