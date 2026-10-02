package com.nextwatch.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.view.Gravity;
import android.view.WindowInsets;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int EXPORT_REQUEST = 501;
    private static final int IMPORT_REQUEST = 502;

    private final int BG = Color.rgb(11,13,18);
    private final int PANEL = Color.rgb(21,25,35);
    private final int PANEL_ALT = Color.rgb(28,34,48);
    private final int TEXT = Color.rgb(244,247,251);
    private final int MUTED = Color.rgb(154,165,181);
    private final int ACCENT = Color.rgb(139,92,246);
    private final int GREEN = Color.rgb(34,197,94);
    private final int DANGER = Color.rgb(239,68,68);

    private LinearLayout page;
    private final List<Button> navButtons = new ArrayList<>();
    private String currentTab = "home";
    private DataStore store;
    private List<CatalogItem> catalog;
    private RecommendationEngine engine;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        store = new DataStore(this);
        catalog = Catalog.items();
        engine = new RecommendationEngine(catalog, store);
        seedStarterProfile();
        buildShell();
        showHome();
    }

    private void seedStarterProfile() {
        if (!store.ratings().isEmpty() || !store.statuses().isEmpty()) return;
        String[] titles = {"Attack on Titan","Solo Leveling","Vinland Saga","Jujutsu Kaisen"};
        int[] scores = {10,10,9,9};
        for (int i=0;i<titles.length;i++) {
            store.setRating(titles[i], scores[i]);
            store.setStatus(titles[i], DataStore.STATUS_WATCHED);
        }
        store.setStatus("Frieren: Beyond Journey's End", DataStore.STATUS_WATCHLIST);
    }

    private void buildShell() {
        LinearLayout root = col();
        root.setBackgroundColor(BG);
        root.setPadding(dp(14), dp(8), dp(14), dp(8));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        page = col();
        page.setPadding(0, dp(8), 0, dp(24));
        scroll.addView(page, new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1f));

        LinearLayout nav = row();
        nav.setPadding(dp(4),dp(4),dp(4),dp(4));
        nav.setBackground(roundRect(PANEL,18));
        addNav(nav,"Home","home");
        addNav(nav,"Discover","discover");
        addNav(nav,"AI","ai");
        addNav(nav,"My Stuff","library");
        addNav(nav,"Taste","taste");
        root.addView(nav,new LinearLayout.LayoutParams(-1,dp(62)));
        setContentView(root);

        // Android 15+ can lay apps edge-to-edge by default. Respect system bars
        // so the bottom navigation never sits underneath Back/Home/Gesture controls.
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int left = 0;
            int top = 0;
            int right = 0;
            int bottom = 0;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = Math.max(bars.bottom, ime.bottom);
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }

            v.setPadding(
                dp(14) + left,
                dp(8) + top,
                dp(14) + right,
                dp(8) + bottom
            );
            return insets;
        });
        root.requestApplyInsets();
    }

    private void addNav(LinearLayout nav,String label,String tag) {
        Button b = new Button(this);
        b.setText(label);
        b.setTag(tag);
        b.setAllCaps(false);
        b.setTextSize(11);
        b.setTextColor(MUTED);
        b.setPadding(dp(2),0,dp(2),0);
        b.setBackground(roundRect(Color.TRANSPARENT,14));
        b.setOnClickListener(v -> {
            String t=(String)v.getTag();
            if ("home".equals(t)) showHome();
            else if ("discover".equals(t)) showDiscover("All","");
            else if ("ai".equals(t)) showAi("");
            else if ("library".equals(t)) showLibrary(DataStore.STATUS_WATCHLIST);
            else showTaste();
        });
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1f);
        lp.setMargins(dp(2),0,dp(2),0);
        nav.addView(b,lp);
        navButtons.add(b);
    }

    private void setActive(String tab) {
        currentTab=tab;
        for (Button b:navButtons) {
            boolean active=tab.equals(b.getTag());
            b.setTextColor(active?TEXT:MUTED);
            b.setTypeface(null,active?Typeface.BOLD:Typeface.NORMAL);
            b.setBackground(roundRect(active?PANEL_ALT:Color.TRANSPARENT,14));
        }
    }

    private void clearPage() { page.removeAllViews(); }

    private void showHome() {
        setActive("home"); clearPage();
        LinearLayout top=row();
        LinearLayout titles=col();
        titles.addView(label("Good evening, Bernard.",25,TEXT,true));
        titles.addView(label("What are you in the mood for?",14,MUTED,false));
        top.addView(titles,new LinearLayout.LayoutParams(0,-2,1f));
        Button shield=smallButton(store.spoilerShield()?"🛡 Spoilers ON":"Spoilers OFF",store.spoilerShield()?PANEL_ALT:DANGER);
        shield.setOnClickListener(v->{store.setSpoilerShield(!store.spoilerShield());showHome();});
        top.addView(shield);
        page.addView(top,block(0));

        EditText q=input("Describe what you want to watch…");
        page.addView(q,block(18));
        Button find=primary("Find my next watch");
        find.setOnClickListener(v->showAi(q.getText().toString().trim()));
        page.addView(find,block(10));

        List<CatalogItem> picks=engine.recommend("",4);
        if(!picks.isEmpty()) {
            page.addView(section("TOP PICK FOR YOU","Personalized from your ratings"),block(22));
            page.addView(hero(picks.get(0)),block(9));
        }
        if(picks.size()>1) {
            page.addView(section("More for you","Tap any title for rating, trailer and status"),block(22));
            for(int i=1;i<picks.size();i++) page.addView(compact(picks.get(i),"",true),block(8));
        }

        LinearLayout audit=card();
        audit.addView(label("Taste Check",20,TEXT,true));
        audit.addView(label("Review older ratings that may no longer fit your current taste. Nothing changes unless you approve it.",14,MUTED,false),block(5));
        Button run=secondary("Run review audit");
        run.setOnClickListener(v->showTasteCheck());
        audit.addView(run,block(12));
        page.addView(audit,block(22));
    }

    private View hero(CatalogItem item) {
        int score=engine.match(item,"");
        LinearLayout c=card();
        LinearLayout r=row();
        r.addView(poster(item,106,158));
        LinearLayout body=col();
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1f);
        lp.setMargins(dp(14),0,0,0);
        body.addView(pill(score+"% FOR YOU"));
        body.addView(label(item.title,22,TEXT,true),block(8));
        body.addView(label(item.type+" • "+item.year+" • "+item.commitment,13,MUTED,false),block(3));
        body.addView(label(item.description,14,TEXT,false),block(9));
        r.addView(body,lp);
        c.addView(r);

        TextView why=label(engine.explain(item),13,MUTED,false);
        why.setVisibility(View.GONE);
        c.addView(why,block(10));

        LinearLayout actions=row();
        Button trailer=smallButton("▶ Trailer",ACCENT);
        trailer.setOnClickListener(v->openTrailer(item.title));
        Button whyBtn=smallButton("Why "+score+"%?",PANEL_ALT);
        whyBtn.setOnClickListener(v->why.setVisibility(why.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));
        Button save=smallButton(DataStore.STATUS_WATCHLIST.equals(store.status(item.title))?"✓ Saved":"+ Watchlist",PANEL_ALT);
        save.setOnClickListener(v->{store.setStatus(item.title,DataStore.STATUS_WATCHLIST);toast("Saved to Watchlist");showHome();});
        actions.addView(trailer,actionLp());
        actions.addView(whyBtn,actionLp());
        actions.addView(save,actionLp());
        c.addView(actions,block(13));
        return c;
    }

    private View compact(CatalogItem item,String query,boolean clickable) {
        int score=engine.match(item,query);
        LinearLayout r=row();
        r.setPadding(dp(12),dp(12),dp(12),dp(12));
        r.setBackground(roundRect(PANEL,18));
        r.addView(poster(item,66,94));
        LinearLayout body=col();
        LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(0,-2,1f);
        blp.setMargins(dp(12),0,dp(8),0);
        body.addView(label(item.title,17,TEXT,true));
        body.addView(label(item.type+" • "+item.commitment,12,MUTED,false),block(3));
        body.addView(label(score+"% match",13,GREEN,true),block(6));
        r.addView(body,blp);
        Button open=smallButton("Open",PANEL_ALT);
        open.setOnClickListener(v->showTitleDialog(item));
        r.addView(open);
        if(clickable) r.setOnClickListener(v->showTitleDialog(item));
        return r;
    }

    private void showDiscover(String type,String query) {
        setActive("discover"); clearPage();
        page.addView(label("Discover",28,TEXT,true));
        page.addView(label("Search the starter catalogue. Live catalogue APIs come next.",14,MUTED,false),block(4));
        EditText search=input("Search titles, genres, moods…");
        search.setText(query);
        page.addView(search,block(16));

        HorizontalScrollView hsv=new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chips=row();
        for(String t:new String[]{"All","Anime","Series","Movie"}) {
            Button b=smallButton(t,t.equals(type)?ACCENT:PANEL_ALT);
            b.setOnClickListener(v->showDiscover(t,search.getText().toString().trim()));
            chips.addView(b,actionLp());
        }
        hsv.addView(chips);
        page.addView(hsv,block(9));

        Button go=secondary("Search");
        go.setOnClickListener(v->showDiscover(type,search.getText().toString().trim()));
        page.addView(go,block(9));

        String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);
        int shown=0;
        for(CatalogItem item:catalog) {
            if(!"All".equals(type)&&!item.type.equals(type)) continue;
            String hay=(item.title+" "+item.description+" "+String.join(" ",item.tags)).toLowerCase(Locale.ROOT);
            if(!q.isEmpty()&&!hay.contains(q)) continue;
            page.addView(compact(item,q,true),block(9));
            shown++;
        }
        if(shown==0) page.addView(label("No matches in the starter catalogue.",15,MUTED,false),block(20));
    }

    private void showAi(String initial) {
        setActive("ai"); clearPage();
        page.addView(label("NextWatch AI",13,ACCENT,true));
        page.addView(label("Tell me exactly what you want.",27,TEXT,true),block(4));
        page.addView(label("This first APK uses an on-device recommendation engine. Your ratings and history are automatically considered.",14,MUTED,false),block(6));

        EditText prompt=input("Example: serious fantasy, powerful main character, lots of action, very little romance");
        prompt.setMinLines(3);
        prompt.setGravity(Gravity.TOP);
        prompt.setText(initial);
        page.addView(prompt,block(18));

        LinearLayout chips=row();
        for(String idea:new String[]{"High action","Hidden gem","Dark mystery"}) {
            Button b=smallButton(idea,PANEL_ALT);
            b.setOnClickListener(v->prompt.setText(((Button)v).getText().toString()));
            chips.addView(b,actionLp());
        }
        page.addView(chips,block(8));

        Button find=primary("Find my next watch");
        find.setOnClickListener(v->showAiResults(prompt.getText().toString().trim()));
        page.addView(find,block(12));

        if(!initial.isEmpty()) showAiResults(initial);
    }

    private void showAiResults(String prompt) {
        page.addView(section("Best matches",prompt.isEmpty()?"Based on your Taste DNA":"Using your request + Taste DNA"),block(22));
        for(CatalogItem item:engine.recommend(prompt,3)) page.addView(compact(item,prompt,true),block(9));
    }

    private void showLibrary(String filter) {
        setActive("library"); clearPage();
        page.addView(label("My Stuff",28,TEXT,true));
        page.addView(label("Your ratings and history stay on-device and can be exported as a backup.",14,MUTED,false),block(4));

        HorizontalScrollView hsv=new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chips=row();
        String[][] filters={{"Watchlist",DataStore.STATUS_WATCHLIST},{"Watched",DataStore.STATUS_WATCHED},{"Watching",DataStore.STATUS_WATCHING},{"Dropped",DataStore.STATUS_DROPPED}};
        for(String[] f:filters) {
            Button b=smallButton(f[0]+" "+store.countStatus(f[1]),f[1].equals(filter)?ACCENT:PANEL_ALT);
            b.setOnClickListener(v->showLibrary(f[1]));
            chips.addView(b,actionLp());
        }
        hsv.addView(chips);
        page.addView(hsv,block(14));

        int count=0;
        for(CatalogItem item:catalog) {
            if(filter.equals(store.status(item.title))) {
                page.addView(compact(item,"",true),block(9));
                count++;
            }
        }
        if(count==0) page.addView(label("Nothing here yet.",15,MUTED,false),block(20));

        LinearLayout backup=card();
        backup.addView(label("Backup & restore",20,TEXT,true));
        backup.addView(label("Export before uninstalling or changing phones. Your public GitHub code repository will not contain your private viewing history.",14,MUTED,false),block(5));
        LinearLayout actions=row();
        Button exp=secondary("Export backup");
        exp.setOnClickListener(v->startExport());
        Button imp=secondary("Import backup");
        imp.setOnClickListener(v->startImport());
        actions.addView(exp,actionLp());
        actions.addView(imp,actionLp());
        backup.addView(actions,block(12));
        page.addView(backup,block(24));
    }

    private void showTaste() {
        setActive("taste"); clearPage();
        page.addView(label("Your Taste DNA",28,TEXT,true));
        page.addView(label(engine.ratingCount()+" ratings analyzed",14,MUTED,false),block(4));

        List<Map.Entry<String,Integer>> tags=engine.topTasteTags(8);
        if(tags.isEmpty()) page.addView(label("Rate titles and your strongest preferences will appear here.",15,MUTED,false),block(18));
        else {
            LinearLayout dna=card();
            for(Map.Entry<String,Integer> e:tags) dna.addView(tasteBar(prettyTag(e.getKey()),e.getValue()),block(11));
            page.addView(dna,block(16));
        }

        LinearLayout audit=card();
        audit.addView(label("Taste Check / Review Audit",20,TEXT,true));
        audit.addView(label("Find ratings that seem inconsistent with the preferences your other ratings reveal. I never change a rating automatically.",14,MUTED,false),block(6));
        Button run=primary("Run Taste Check");
        run.setOnClickListener(v->showTasteCheck());
        audit.addView(run,block(12));
        page.addView(audit,block(18));

        page.addView(section("Your ratings","Tap any title to change your mind"),block(22));
        Map<String,Integer> ratings=store.ratings();
        for(CatalogItem item:catalog) {
            Integer rating=ratings.get(item.title);
            if(rating==null) continue;
            LinearLayout line=row();
            line.setPadding(dp(12),dp(11),dp(12),dp(11));
            line.setBackground(roundRect(PANEL,16));
            line.addView(label(item.title,15,TEXT,true),new LinearLayout.LayoutParams(0,-2,1f));
            line.addView(label(rating+"/10",16,GREEN,true));
            line.setOnClickListener(v->showRatingDialog(item));
            page.addView(line,block(7));
        }
    }

    private void showTasteCheck() {
        setActive("taste"); clearPage();
        page.addView(label("Taste Check",28,TEXT,true));
        List<CatalogItem> flagged=engine.auditCandidates();
        if(flagged.isEmpty()) {
            LinearLayout ok=card();
            ok.addView(label("✓ Your ratings look consistent",20,GREEN,true));
            ok.addView(label("No low ratings strongly conflict with your current taste pattern. You can still change any rating manually.",14,MUTED,false),block(7));
            page.addView(ok,block(16));
        } else {
            page.addView(label("These may be worth another look. Nothing changes unless you choose a new rating.",14,MUTED,false),block(6));
            for(CatalogItem item:flagged) {
                int r=store.ratings().getOrDefault(item.title,0);
                LinearLayout c=card();
                c.addView(label(item.title+" • "+r+"/10",19,TEXT,true));
                c.addView(label("This shares several traits that you now rate highly. Do you still feel the same about it?",14,MUTED,false),block(6));
                Button rerate=primary("Re-rate");
                rerate.setOnClickListener(v->showRatingDialog(item));
                c.addView(rerate,block(10));
                page.addView(c,block(10));
            }
        }
        Button back=secondary("Back to Taste DNA");
        back.setOnClickListener(v->showTaste());
        page.addView(back,block(20));
    }

    private View tasteBar(String name,int value) {
        LinearLayout wrap=col();
        LinearLayout top=row();
        top.addView(label(name,14,TEXT,false),new LinearLayout.LayoutParams(0,-2,1f));
        top.addView(label(value+"%",14,TEXT,true));
        wrap.addView(top);

        LinearLayout track=new LinearLayout(this);
        track.setBackground(roundRect(PANEL_ALT,999));
        LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(-1,dp(9));
        tlp.setMargins(0,dp(6),0,0);
        wrap.addView(track,tlp);
        View fill=new View(this);
        fill.setBackground(roundRect(ACCENT,999));
        track.addView(fill,new LinearLayout.LayoutParams(0,-1,value/100f));
        return wrap;
    }

    private void showTitleDialog(CatalogItem item) {
        String[] choices={"▶ Watch trailer","Rate / change rating","Add to Watchlist","Mark Watched","Watching","Dropped","Not Interested","Why this match?"};
        new AlertDialog.Builder(this)
            .setTitle(item.title)
            .setMessage(item.description+"\n\n"+item.type+" • "+item.year+" • "+item.commitment+"\n"+engine.match(item,"")+"% personal match")
            .setItems(choices,(d,which)->{
                if(which==0) openTrailer(item.title);
                else if(which==1) showRatingDialog(item);
                else if(which==2){store.setStatus(item.title,DataStore.STATUS_WATCHLIST);toast("Saved to Watchlist");}
                else if(which==3){store.setStatus(item.title,DataStore.STATUS_WATCHED);showRatingDialog(item);}
                else if(which==4){store.setStatus(item.title,DataStore.STATUS_WATCHING);toast("Marked Watching");}
                else if(which==5){store.setStatus(item.title,DataStore.STATUS_DROPPED);toast("Marked Dropped");}
                else if(which==6){store.setStatus(item.title,DataStore.STATUS_NOT_INTERESTED);toast("Recommendations adjusted");}
                else new AlertDialog.Builder(this).setTitle("Why this match?").setMessage(engine.explain(item)).setPositiveButton("OK",null).show();
            })
            .setNegativeButton("Close",null)
            .show();
    }

    private void showRatingDialog(CatalogItem item) {
        LinearLayout wrap=col();
        wrap.setPadding(dp(18),0,dp(18),0);
        Integer old=store.ratings().get(item.title);
        wrap.addView(label("Current: "+(old==null?"Not rated":old+"/10"),14,MUTED,false));

        LinearLayout row1=row();
        LinearLayout row2=row();
        for(int i=10;i>=1;i--) {
            final int rating=i;
            Button b=smallButton(String.valueOf(i),i>=9?GREEN:(i<=4?DANGER:PANEL_ALT));
            b.setOnClickListener(v->{
                store.setRating(item.title,rating);
                if(store.status(item.title).isEmpty()) store.setStatus(item.title,DataStore.STATUS_WATCHED);
                toast(item.title+" rated "+rating+"/10");
                if("taste".equals(currentTab)) showTaste();
            });
            if(i>=6) row1.addView(b,actionLp()); else row2.addView(b,actionLp());
        }
        wrap.addView(row1,block(10));
        wrap.addView(row2,block(6));

        new AlertDialog.Builder(this)
            .setTitle("Rate • "+item.title)
            .setView(wrap)
            .setNeutralButton("Remove rating",(d,w)->{
                store.setRating(item.title,0);
                toast("Rating removed");
                if("taste".equals(currentTab)) showTaste();
            })
            .setNegativeButton("Close",null)
            .show();
    }

    private void openTrailer(String title) {
        Intent i=new Intent(this,TrailerActivity.class);
        i.putExtra("title",title);
        startActivity(i);
    }

    private void startExport() {
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE,"NextWatch_Backup.json");
        startActivityForResult(i,EXPORT_REQUEST);
    }

    private void startImport() {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        startActivityForResult(i,IMPORT_REQUEST);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK||data==null||data.getData()==null) return;
        Uri uri=data.getData();
        try {
            if(requestCode==EXPORT_REQUEST) {
                try(OutputStream out=getContentResolver().openOutputStream(uri)) {
                    out.write(store.exportJson().getBytes(StandardCharsets.UTF_8));
                }
                toast("NextWatch backup exported");
            } else if(requestCode==IMPORT_REQUEST) {
                StringBuilder sb=new StringBuilder();
                try(BufferedReader br=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri),StandardCharsets.UTF_8))) {
                    String line;
                    while((line=br.readLine())!=null) sb.append(line);
                }
                if(store.importJson(sb.toString())) {
                    toast("Backup restored");
                    showHome();
                } else toast("Not a valid NextWatch backup");
            }
        } catch(Exception e) {
            toast("Backup operation failed");
        }
    }

    private LinearLayout section(String title,String sub) {
        LinearLayout w=col();
        w.addView(label(title,18,TEXT,true));
        w.addView(label(sub,12,MUTED,false),block(3));
        return w;
    }

    private LinearLayout card() {
        LinearLayout c=col();
        c.setPadding(dp(16),dp(16),dp(16),dp(16));
        c.setBackground(roundRect(PANEL,22));
        return c;
    }

    private LinearLayout row() {
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private LinearLayout col() {
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private TextView label(String value,int size,int color,boolean bold) {
        TextView t=new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setLineSpacing(0,1.08f);
        if(bold) t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }

    private TextView pill(String value) {
        TextView t=label(value,12,TEXT,true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(10),dp(6),dp(10),dp(6));
        t.setBackground(roundRect(Color.argb(70,34,197,94),999));
        return t;
    }

    private TextView poster(CatalogItem item,int w,int h) {
        TextView p=label(initials(item.title),26,Color.WHITE,true);
        p.setGravity(Gravity.BOTTOM|Gravity.START);
        p.setPadding(dp(12),dp(12),dp(12),dp(12));
        p.setBackground(roundRect(posterColor(item.title),18));
        p.setLayoutParams(new LinearLayout.LayoutParams(dp(w),dp(h)));
        return p;
    }

    private EditText input(String hint) {
        EditText e=new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setTextSize(16);
        e.setSingleLine(false);
        e.setPadding(dp(15),dp(13),dp(15),dp(13));
        e.setBackground(roundRect(PANEL,18));
        return e;
    }

    private Button primary(String value) {
        Button b=smallButton(value,ACCENT);
        b.setTextSize(15);
        b.setTypeface(null,Typeface.BOLD);
        return b;
    }

    private Button secondary(String value) { return smallButton(value,PANEL_ALT); }

    private Button smallButton(String value,int color) {
        Button b=new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setMinHeight(dp(44));
        b.setMinWidth(0);
        b.setPadding(dp(10),dp(7),dp(10),dp(7));
        b.setBackground(roundRect(color,14));
        return b;
    }

    private LinearLayout.LayoutParams actionLp() {
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(46),1f);
        lp.setMargins(0,0,dp(6),0);
        return lp;
    }

    private LinearLayout.LayoutParams block(int top) {
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,dp(top),0,0);
        return lp;
    }

    private GradientDrawable roundRect(int color,int radius) {
        GradientDrawable g=new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private int posterColor(String title) {
        float hue=Math.abs(title.hashCode()%360);
        return Color.HSVToColor(new float[]{hue,0.55f,0.62f});
    }

    private String initials(String title) {
        String[] parts=title.replaceAll("[^A-Za-z0-9 ]"," ").trim().split("\\s+");
        StringBuilder s=new StringBuilder();
        for(String part:parts) {
            if(!part.isEmpty()) s.append(part.charAt(0));
            if(s.length()==2) break;
        }
        return s.toString().toUpperCase(Locale.ROOT);
    }

    private String prettyTag(String tag) {
        String s=tag.replace('-',' ');
        return s.substring(0,1).toUpperCase(Locale.ROOT)+s.substring(1);
    }

    private void toast(String value) { Toast.makeText(this,value,Toast.LENGTH_SHORT).show(); }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
}
