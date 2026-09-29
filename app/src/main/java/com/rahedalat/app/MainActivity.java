package com.rahedalat.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayDeque;

public class MainActivity extends Activity {
    private GameView game;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(12,16,20));
        getWindow().setNavigationBarColor(Color.rgb(12,16,20));
        game = new GameView(this);
        setContentView(game);
    }

    @Override public void onBackPressed() {
        if (!game.goBack()) game.askExit();
    }

    public static class GameView extends View {
        static final int START=0, OFFICE=1, LIBRARY=2, CASE=3, LESSON=4, QUIZ=5,
                RESULT=6, PROGRESS=7, ARCHIVE=8, LAWBOOK=9;

        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        final ArrayDeque<Integer> history = new ArrayDeque<>();
        final SharedPreferences prefs;

        Bitmap officeBg, libraryBg, lawyer, client, judge, officer;
        Bitmap amirStanding, amirSeated, amirTalking, amirDocuments, amirPhone, amirFile;
        MediaPlayer music;
        int screen = START;
        int selectedCase = 1;
        int storyStep = 0;
        boolean settings = false, exitConfirm = false, answerChosen = false, correct = false;
        int case1Action = 0;
        boolean[] clues = new boolean[]{false,false,false};
        String clueDetail = "";
        float ox, oy, scale;

        final String[] titles = {
                "",
                "پولی که ناپدید شد",
                "پرونده مطالبه وجه و تشخیص طرفین دعوا",
                "پرونده نقص دادخواست و تکمیل اطلاعات",
                "پرونده مطالبه خسارت و تعیین خواسته",
                "پرونده سند و اثبات ادعا"
        };

        final String[] casePeople = {"", "امیر رضایی", "رضا کریمی", "سارا موسوی", "علی نادری", "نگار حسینی"};
        final String[] caseIssues = {
                "",
                "مطالبه وجهی که امیر رضایی می‌گوید به شخص دیگری قرض داده و هنوز پس نگرفته است.",
                "مطالبه وجهی که در آن باید خواهان و خوانده درست شناسایی شوند.",
                "دادخواستی که بخشی از اطلاعات ضروری آن ناقص ثبت شده است.",
                "مطالبه خسارت و تشخیص دقیق چیزی که از دادگاه درخواست می‌شود.",
                "اختلاف درباره سند و ارزش آن به عنوان دلیل در دعوا."
        };

        final String[][] story = {
                {},
                {"سه ماه پیش به یکی از آشناها پول قرض دادم؛ قرار بود آخر ماه برگرداند، اما هنوز خبری نیست. این پوشه همه چیزهایی است که دارم.",
                 "داخل پوشه یک رسید انتقال وجه، یک پرینت پیام و یک یادداشت درباره موعد بازپرداخت پیدا می‌کنی. هر مدرک را بررسی کن و ببین دقیقاً چه چیزی را ثابت یا روشن می‌کند.",
                 "امیر از طلبش مطمئن است، اما برای تبدیل این ماجرا به یک پرونده باید اطلاعات اصلی را دقیق جمع‌بندی کنیم: طرفین، خواسته و بهای آن، جهات ادعا و دلایل.",
                 "اولین تصمیم تو این است: اطلاعات لازم برای تنظیم دادخواست را کامل کنیم یا بدون مشخص‌کردن جزئیات، فقط بر اساس یک مدرک جلو برویم؟"},
                {"من از شخصی طلب دارم و می‌خواهم مبلغ بدهی را از راه قانونی مطالبه کنم.",
                 "قبل از هر چیز باید مشخص کنیم چه کسی دعوا را مطرح می‌کند و دعوا علیه چه کسی است.",
                 "پس در این پرونده، عنوان خواهان و خوانده فقط یک اسم نیست؛ نقش هر طرف را مشخص می‌کند.",
                 "حالا باید تصمیم بگیریم در دادخواست چه کسی خواهان و چه کسی خوانده است."},
                {"دادخواست ثبت شده، اما بخشی از اطلاعات آن ناقص است و دفتر دادگاه ایراد گرفته است.",
                 "برای ادامه رسیدگی، باید بدانیم دادخواست چه اطلاعاتی لازم دارد و هر بخش چه کاربردی دارد.",
                 "مشخصات طرفین، خواسته و دلایل از اطلاعاتی هستند که باید با دقت بررسی شوند.",
                 "حالا نوبت توست که تشخیص بدهی کدام اطلاعات برای رفع ایراد مهم‌تر است."},
                {"من خسارتی دیده‌ام و می‌خواهم جبران آن را از دادگاه بخواهم.",
                 "خواسته باید روشن باشد؛ دادگاه باید بداند دقیقاً چه چیزی از او درخواست شده است.",
                 "همچنین در موارد لازم، ارزش خواسته می‌تواند در هزینه دادرسی اثر داشته باشد.",
                 "پس قبل از ادامه پرونده باید خواسته را دقیق و قابل فهم بیان کنیم."},
                {"طرف مقابل می‌گوید سندی دارد که ادعای او را ثابت می‌کند، اما من درباره آن اختلاف دارم.",
                 "اینجا باید مفهوم دلیل را بشناسیم؛ هر ادعا برای اثبات خود به دلیل مناسب نیاز دارد.",
                 "سند یکی از ادله است، اما باید جایگاه و ارتباط آن با ادعا را بررسی کرد.",
                 "اکنون باید تشخیص بدهی دلیل در این پرونده چه نقشی دارد."}
        };

        final String[][] lesson = {
                {},
                {"در این پرونده، پس از روشن شدن موضوع و اطلاعات اصلی دعوا، باید مسیر قانونی طرح دعوا را بشناسیم. ماده ۴۸ قانون آیین دادرسی مدنی مقرر می‌کند شروع رسیدگی در دادگاه مستلزم تقدیم دادخواست است؛ سپس ماده ۵۱ برخی اطلاعات لازم دادخواست، از جمله مشخصات طرفین، خواسته و بهای آن، جهات استحقاق، درخواست از دادگاه و ادله را مشخص می‌کند.",
                 "اصطلاحات کلیدی: دادخواست و شروع رسیدگی — دادخواست ابزار طرح دعواست و برای تنظیم درست آن باید اطلاعات مقرر قانونی را دقیق جمع‌آوری و بیان کرد."},
                {"خواهان کسی است که دعوا را مطرح می‌کند و خوانده کسی است که دعوا علیه او مطرح شده است. تشخیص این دو نقش برای تنظیم درست پرونده ضروری است.",
                 "اصطلاح کلیدی: خواهان و خوانده — دو عنوان ناظر به نقش طرفین در دعوا، نه صرفاً نام اشخاص."},
                {"دادخواست باید اطلاعات لازم درباره اصحاب دعوا، خواسته و جهات و دلایل مورد استناد را در حدود قانون داشته باشد. نقص اطلاعات می‌تواند روند پرونده را متوقف یا اصلاح‌پذیر کند.",
                 "اصطلاح کلیدی: مشخصات اصحاب دعوا — اطلاعاتی که برای شناسایی و ابلاغ به طرفین لازم است."},
                {"خواسته همان چیزی است که خواهان از دادگاه می‌خواهد. روشن بودن خواسته به دادگاه و طرف مقابل کمک می‌کند حدود دعوا مشخص باشد.",
                 "اصطلاح کلیدی: خواسته — نتیجه یا امری که خواهان صدور حکم یا تصمیم درباره آن را از دادگاه درخواست می‌کند."},
                {"دلیل وسیله اثبات یا تقویت ادعای مطرح‌شده در حدود قانون است. سند، اقرار، شهادت و سایر ادله قانونی می‌توانند در اثبات موضوع نقش داشته باشند.",
                 "اصطلاح کلیدی: دلیل — وسیله‌ای که برای اثبات یا تقویت ادعا یا دفاع در چارچوب قانون ارائه می‌شود."}
        };

        final String[] questions = {
                "",
                "پس از جمع‌بندی اطلاعات پرونده، برای شروع رسیدگی در دادگاه چه اقدامی لازم است؟",
                "در پرونده رضا، چه کسی عنوان خواهان دارد؟",
                "برای رفع ایراد دادخواست، کدام مورد باید با دقت بررسی شود؟",
                "در پرونده خسارت، خواسته به چه چیزی اشاره می‌کند؟",
                "در پرونده نگار، دلیل چه نقشی دارد؟"
        };

        final String[][] options = {
                {},
                {"تماس تلفنی با طرف مقابل", "تقدیم دادخواست از مسیر قانونی", "ارسال یک پیام غیررسمی"},
                {"شخصی که دادخواست علیه او مطرح شده", "شخصی که مطالبه وجه را مطرح کرده", "قاضی رسیدگی‌کننده"},
                {"رنگ جلد پرونده", "اطلاعات ضروری اصحاب دعوا و خواسته و دلایل", "شماره صندلی دادگاه"},
                {"نام ساختمان دادگستری", "زمان مراجعه به دفتر", "چیزی که خواهان از دادگاه درخواست می‌کند"},
                {"تزئین پرونده", "تعیین رنگ فرم دادخواست", "وسیله اثبات یا تقویت ادعا در چارچوب قانون"}
        };

        final int[] correctIndex = {0,1,1,1,2,2};
        final String[] explanations = {
                "",
                "برای شروع رسیدگی در دادگاه، تقدیم دادخواست لازم است؛ جزئیات و تشریفات آن باید مطابق مقررات رعایت شود.",
                "در این پرونده رضا که مطالبه وجه را مطرح کرده، خواهان است؛ طرف مقابل خوانده است.",
                "مشخصات اصحاب دعوا، خواسته و دلایل از اطلاعات مهم دادخواست هستند و باید دقیق بررسی شوند.",
                "خواسته همان چیزی است که خواهان از دادگاه درخواست می‌کند؛ در اینجا جبران خسارت.",
                "دلیل برای اثبات یا تقویت ادعا در حدود قانون به کار می‌رود و سند یکی از انواع آن است."
        };

        public GameView(Context c) {
            super(c);
            setFocusable(true);
            prefs = c.getSharedPreferences("rah_edalat", Context.MODE_PRIVATE);
            officeBg = load(R.drawable.office_bg);
            libraryBg = load(R.drawable.library_bg);
            lawyer = load(R.drawable.char_lawyer);
            client = load(R.drawable.char_client);
            amirStanding = load(R.drawable.amir_standing);
            amirSeated = load(R.drawable.amir_seated);
            amirTalking = load(R.drawable.amir_talking);
            amirDocuments = load(R.drawable.amir_documents);
            amirPhone = load(R.drawable.amir_phone);
            amirFile = load(R.drawable.amir_file);
            judge = load(R.drawable.char_judge);
            officer = load(R.drawable.char_officer);
            tp.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        }

        Bitmap load(int id) { return BitmapFactory.decodeResource(getResources(), id); }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w=getWidth(), h=getHeight();
            scale=Math.min(w/1080f,h/2400f);
            ox=(w-1080f*scale)/2f; oy=(h-2400f*scale)/2f;
            c.drawColor(Color.rgb(9,13,17));
            c.save(); c.translate(ox,oy); c.scale(scale,scale);
            if(screen==START) drawStart(c); else {
                drawBackground(c);
                if(screen==OFFICE) drawOffice(c);
                else if(screen==LIBRARY) drawLibrary(c);
                else if(screen==CASE) drawCase(c);
                else if(screen==LESSON) drawLesson(c);
                else if(screen==QUIZ) drawQuiz(c);
                else if(screen==RESULT) drawResult(c);
                else if(screen==PROGRESS) drawProgress(c);
                else if(screen==ARCHIVE) drawArchive(c);
                else if(screen==LAWBOOK) drawLawBook(c);
                drawTop(c);
            }
            if(settings) drawSettings(c);
            if(exitConfirm) drawExit(c);
            c.restore();
        }

        void drawBackground(Canvas c) {
            Bitmap b = screen==LIBRARY ? libraryBg : officeBg;
            if(b!=null) c.drawBitmap(b,null,new RectF(0,0,1080,2400),p);
            p.setColor(0x18000000); c.drawRect(0,0,1080,2400,p);
        }

        void drawStart(Canvas c) {
            p.setColor(0xff0d151b); c.drawRect(0,0,1080,2400,p);
            if(officeBg!=null){p.setAlpha(70);c.drawBitmap(officeBg,null,new RectF(0,0,1080,2400),p);p.setAlpha(255);}
            p.setColor(0x990a1116); c.drawRect(0,0,1080,2400,p);
            text(c,"راه عدالت",540,700,82,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,900);
            text(c,"یادگیری آیین دادرسی مدنی در قالب پرونده و تصمیم",540,835,34,0xffeadfcd,true,Layout.Alignment.ALIGN_CENTER,900);
            round(c,300,1030,780,1260,34,0xff17435d);
            text(c,"ورود به دفتر وکیل",540,1175,44,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,520);
            text(c,"ساخته شده توسط پارسا",540,2120,30,0xffd1c2ab,true,Layout.Alignment.ALIGN_CENTER,650);
        }

        void drawTop(Canvas c) {
            p.setColor(0x9d101a21); c.drawRect(0,0,1080,145,p);
            if(screen!=OFFICE && screen!=START){
                round(c,34,28,154,124,46,0xdd17313f);
                text(c,"‹",94,99,62,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,90);
            }
            round(c,926,28,1046,124,46,0xdd17313f);
            text(c,"⚙",986,98,40,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,90);
            text(c,screenTitle(),540,91,40,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,650);
        }

        String screenTitle(){
            switch(screen){
                case OFFICE:return "دفتر وکیل";
                case LIBRARY:return "کتابخانه پرونده‌ها";
                case CASE:return "پرونده جاری";
                case LESSON:return "یادگیری پرونده";
                case QUIZ:return "تصمیم حقوقی";
                case RESULT:return "نتیجه پرونده";
                case PROGRESS:return "پیشرفت";
                case ARCHIVE:return "بایگانی";
                case LAWBOOK:return "کتاب قانون";
                default:return "راه عدالت";
            }
        }

        void drawOffice(Canvas c) {
            // No visible buttons or labels are placed over office objects.
            // The real objects in the background are the touch targets.
            text(c,"برای تعامل، خودِ اشیای دفتر را لمس کن",540,2310,26,0xfff0e4cf,true,Layout.Alignment.ALIGN_CENTER,850);
        }

        void drawLibrary(Canvas c) {
            text(c,"برای باز کردن پرونده، خودِ پوشه را لمس کن",540,2250,28,0xfff0e4cf,true,Layout.Alignment.ALIGN_CENTER,900);
            for(int i=1;i<=5;i++) drawFolder(c,i,folderX(i),folderY(i),i<=unlocked());
        }

        float folderX(int i){ return 155f + ((i-1)%3)*300f; }
        float folderY(int i){ return 520f + ((i-1)/3)*620f; }

        void drawFolder(Canvas c,int i,float x,float y,boolean open){
            int body=open?0xff7d5a36:0xff4c4b4a;
            round(c,x-92,y-75,x+92,y+85,18,body);
            round(c,x-72,y-93,x+5,y-55,12,body);
            text(c,"پرونده "+toFa(i),x,y-8,25,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,165);
            text(c,"●",x,y+40,22,open?0xffe3bd72:0xff8a8a8a,true,Layout.Alignment.ALIGN_CENTER,60);
        }

        void drawCase(Canvas c) {
            if(selectedCase==1){ drawCaseOne(c); return; }
            int step=Math.min(storyStep,3);
            Bitmap person=caseCharacter(selectedCase);
            float targetH=step==0?760:690;
            drawCharacter(c,person,790,1450,targetH);
            panel(c,55,1540,1025,2240,0xe90c171e);
            text(c,"پرونده "+toFa(selectedCase)+"  |  "+casePeople[selectedCase],965,1630,28,0xffe0b96e,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            text(c,caseIssues[selectedCase],965,1715,30,0xffd7cbb9,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            text(c,story[selectedCase][step],965,1845,34,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            round(c,100,2090,980,2215,30,0xff17435d);
            text(c,step<3?"ادامه روایت":"ورود به بخش آموزشی",540,2170,34,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,850);
        }

        void drawCaseOne(Canvas c){
            int step=Math.min(storyStep,3);
            Bitmap person = step==0 ? amirStanding : (step==1 ? amirSeated : (step==2 ? amirDocuments : amirOffer));
            float h = step==0 ? 760 : 430;
            drawCharacter(c,person,880,1510,h);

            // Evidence / investigation area
            if(step==1){
                drawEvidence(c,55,1280,290,1400,"رسید انتقال وجه",clues[0]);
                drawEvidence(c,305,1280,540,1400,"پرینت پیام",clues[1]);
                drawEvidence(c,555,1280,790,1400,"یادداشت موعد",clues[2]);
                text(c,"مدارک را لمس و بررسی کن",1010,1250,27,0xffe3c487,true,Layout.Alignment.ALIGN_OPPOSITE,470);
            }

            panel(c,55,1545,1025,2260,0xe90c171e);
            text(c,"پرونده ۱ | امیر رضایی",965,1625,30,0xffe0b96e,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            text(c,"پولی که ناپدید شد",965,1685,34,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            text(c,story[1][step],965,1775,32,0xffeee6d8,true,Layout.Alignment.ALIGN_OPPOSITE,900);

            if(step==0){
                text(c,"اولین کار تو چیست؟",965,1980,29,0xffe1bd7b,true,Layout.Alignment.ALIGN_OPPOSITE,800);
                caseChoice(c,90,2020,990,2115,"زمان و نحوه پرداخت را دقیق بپرس",case1Action==1, true);
                caseChoice(c,90,2130,990,2225,"همین حالا بدون بررسی دادخواست بنویس",case1Action==2, false);
                if(case1Action!=0){
                    round(c,90,2260,990,2375,26,0xff294552);
                    text(c,"ادامه روایت",540,2338,29,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,800);
                }
            } else if(step==1){
                if(!clueDetail.isEmpty()) text(c,clueDetail,965,1950,27,0xffe7dac8,true,Layout.Alignment.ALIGN_OPPOSITE,850);
                text(c,"هر سه مدرک را بررسی کن تا روایت کامل شود.",965,2045,27,0xffe1bd7b,true,Layout.Alignment.ALIGN_OPPOSITE,820);
                round(c,90,2100,990,2215,28,0xff17435d);
                text(c,(clues[0]&&clues[1]&&clues[2])?"ادامه روایت":"سه مدرک هنوز کامل بررسی نشده",540,2175,31,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,850);
            } else if(step==2){
                text(c,"کدام جمع‌بندی برای پرونده لازم‌تر است؟",965,1980,29,0xffe1bd7b,true,Layout.Alignment.ALIGN_OPPOSITE,820);
                caseChoice(c,90,2020,990,2115,"موضوع، طرفین، خواسته و مدارک را مشخص کنم",case1Action==3, true);
                caseChoice(c,90,2130,990,2225,"فقط پیام را به‌عنوان کل پرونده در نظر بگیرم",case1Action==4, false);
                if(case1Action==3 || case1Action==4){
                    round(c,90,2260,990,2375,26,0xff294552);
                    text(c,"ادامه روایت",540,2338,29,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,800);
                }
            } else {
                text(c,"اولین تصمیم را انتخاب کن",965,1980,29,0xffe1bd7b,true,Layout.Alignment.ALIGN_OPPOSITE,800);
                caseChoice(c,90,2020,990,2115,"اطلاعات لازم دادخواست را کامل و دقیق کنم",case1Action==5, true);
                caseChoice(c,90,2130,990,2225,"فقط با یک مدرک جلو بروم و جزئیات را رها کنم",case1Action==6, false);
                if(case1Action==5 || case1Action==6){
                    round(c,90,2260,990,2375,26,0xff294552);
                    text(c,"ورود به بخش آموزشی",540,2338,29,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,800);
                }
            }
        }

        void drawEvidence(Canvas c,float l,float t,float r,float b,String label,boolean found){
            round(c,l,t,r,b,18,found?0xff765a35:0xff25343c);
            text(c,found?"✓":"📄",(l+r)/2,t+55,30,found?0xffd9e9d5:0xffe1c88e,true,Layout.Alignment.ALIGN_CENTER,70);
            text(c,label,(l+r)/2,t+102,23,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,r-l-20);
        }

        void caseChoice(Canvas c,float l,float t,float r,float b,String label,boolean selected,boolean correctChoice){
            int color = selected ? (correctChoice ? 0xff49643f : 0xff6a3535) : 0xff173c51;
            round(c,l,t,r,b,24,color);
            text(c,label,930,t+62,27,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,800);
        }

        Bitmap caseCharacter(int n){
            if(n==2 || n==5) return officer;
            if(n==4) return judge;
            if(n==3) return lawyer;
            return client;
        }

        void drawLesson(Canvas c) {
            panel(c,55,230,1025,2220,0xe9101920);
            text(c,"مفهوم حقوقی پرونده",970,390,40,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,880);
            text(c,lesson[selectedCase][0],970,510,34,0xfff2eadc,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            round(c,90,1330,990,1670,28,0xff1b3340);
            text(c,lesson[selectedCase][1],930,1435,29,0xffead9bc,true,Layout.Alignment.ALIGN_OPPOSITE,820);
            text(c,"حالا سؤال پرونده را حل کن.",540,1815,32,0xffe0c38f,true,Layout.Alignment.ALIGN_CENTER,800);
            round(c,120,1900,960,2070,30,0xff17435d);
            text(c,"ورود به تصمیم حقوقی",540,2005,36,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,800);
        }

        void drawQuiz(Canvas c) {
            panel(c,45,220,1035,2225,0xe9101920);
            text(c,questions[selectedCase],960,390,36,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            for(int i=0;i<3;i++){
                float top=650+i*360;
                int color=answerChosen ? (i==correctIndex[selectedCase]?0xff235c43:0xff422a2c) : 0xff173c51;
                round(c,95,top,985,top+245,28,color);
                text(c,options[selectedCase][i],930,top+105,31,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,800);
                if(answerChosen && i==correctIndex[selectedCase]) text(c,"✓",130,top+155,38,0xffb7e7c8,true,Layout.Alignment.ALIGN_CENTER,70);
            }
            if(answerChosen){
                text(c,correct?"پاسخ درست است":"پاسخ انتخابی درست نیست",540,1785,35,correct?0xff9ee0ba:0xffffb5ad,true,Layout.Alignment.ALIGN_CENTER,800);
                text(c,explanations[selectedCase],930,1880,29,0xffe7dac8,true,Layout.Alignment.ALIGN_OPPOSITE,820);
                round(c,170,2070,910,2185,28,0xff294552);
                text(c,"مشاهده نتیجه",540,2145,31,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,700);
            }
        }

        void drawResult(Canvas c) {
            panel(c,75,420,1005,1980,0xe90f1920);
            text(c,correct?"پرونده با موفقیت پیش رفت":"نیاز به مرور و تلاش دوباره",540,720,48,correct?0xffd8eddb:0xffffc1ba,true,Layout.Alignment.ALIGN_CENTER,850);
            text(c,correct?"مفهوم این پرونده به بخش پیشرفت اضافه شد.":"پاسخ را مرور کن و دوباره تصمیم بگیر.",540,835,31,0xffe5d9c8,true,Layout.Alignment.ALIGN_CENTER,850);
            round(c,150,1080,930,1250,30,0xff17435d);
            text(c,correct?(selectedCase<5?"بازگشت به کتابخانه":"پایان پنج پرونده") : "تلاش دوباره",540,1185,34,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,760);
        }

        void drawProgress(Canvas c) {
            panel(c,75,360,1005,2070,0xe90f1920);
            int completed=prefs.getInt("completed",0);
            text(c,"پیشرفت آموزشی",540,510,46,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,850);
            text(c,"پرونده‌های تکمیل‌شده: "+toFa(completed)+" از ۵",540,700,38,0xffeadfcf,true,Layout.Alignment.ALIGN_CENTER,850);
            for(int i=1;i<=5;i++){
                float y=850+(i-1)*210;
                round(c,150,y,930,y+150,24,i<=completed?0xff23543f:0xff263139);
                text(c,"پرونده "+toFa(i),850,y+62,30,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,650);
                text(c,i<=completed?"تکمیل شده":"در انتظار یادگیری",850,y+105,23,0xffd2c6b6,true,Layout.Alignment.ALIGN_OPPOSITE,650);
            }
        }

        void drawArchive(Canvas c) {
            panel(c,75,360,1005,2070,0xe90f1920);
            text(c,"بایگانی پرونده‌ها",540,510,44,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,850);
            int completed=prefs.getInt("completed",0);
            if(completed==0){text(c,"هنوز پرونده‌ای تکمیل نشده است.",540,800,32,0xffd8cdbd,true,Layout.Alignment.ALIGN_CENTER,850);return;}
            for(int i=1;i<=completed;i++){
                float y=700+(i-1)*245;
                text(c,"پرونده "+toFa(i),900,y,32,0xffe0bd7b,true,Layout.Alignment.ALIGN_OPPOSITE,700);
                text(c,titles[i],900,y+55,27,0xffeee5d8,true,Layout.Alignment.ALIGN_OPPOSITE,700);
            }
        }

        void drawLawBook(Canvas c) {
            panel(c,65,300,1015,2180,0xe90e1820);
            text(c,"کتاب قانون",540,440,48,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,850);
            text(c,"راهنمای سریع مفاهیم این پنج پرونده",540,540,29,0xffd5bd98,true,Layout.Alignment.ALIGN_CENTER,850);
            String[] terms={"دادخواست","خواهان و خوانده","اجزای دادخواست","خواسته","دلیل"};
            String[] defs={"وسیله طرح دعوا از مسیر قانونی.","نقش طرفین دعوا را مشخص می‌کند.","اطلاعات ضروری برای شناسایی و بیان دعوا.","چیزی که خواهان از دادگاه درخواست می‌کند.","وسیله اثبات یا تقویت ادعا در حدود قانون."};
            for(int i=0;i<5;i++){
                float y=700+i*270;
                text(c,terms[i],930,y,32,0xffe1bb74,true,Layout.Alignment.ALIGN_OPPOSITE,760);
                text(c,defs[i],930,y+70,27,0xffeee6d9,true,Layout.Alignment.ALIGN_OPPOSITE,760);
            }
        }

        void drawSettings(Canvas c) {
            p.setColor(0x88000000); c.drawRect(0,0,1080,2400,p);
            panel(c,130,600,950,1800,0xf017252d);
            text(c,"تنظیمات",540,750,48,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,700);
            round(c,190,900,890,1080,30,0xff173c51);
            text(c,prefs.getBoolean("music",true)?"موسیقی: روشن":"موسیقی: خاموش",540,1010,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,650);
            round(c,190,1160,890,1340,30,0xff294552);
            text(c,"افکت صدا: روشن",540,1270,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,650);
            round(c,190,1420,890,1600,30,0xff3b2729);
            text(c,"خروج از بازی",540,1530,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,650);
        }

        void drawExit(Canvas c) {
            p.setColor(0xaa000000); c.drawRect(0,0,1080,2400,p);
            panel(c,140,900,940,1510,0xff17242c);
            text(c,"از بازی خارج می‌شوی؟",540,1040,42,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,750);
            round(c,200,1160,520,1310,28,0xff294552); text(c,"خیر",360,1255,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,260);
            round(c,560,1160,880,1310,28,0xff4a282b); text(c,"بله",720,1255,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,260);
        }

        void drawCharacter(Canvas c, Bitmap b, float cx, float bottom, float targetH) {
            if(b==null)return;
            float ratio=targetH/b.getHeight();
            float w=b.getWidth()*ratio;
            RectF d=new RectF(cx-w/2,bottom-targetH,cx+w/2,bottom);
            p.setAlpha(255); c.drawBitmap(b,null,d,p);
        }

        void round(Canvas c,float l,float t,float r,float b,float rad,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(l,t,r,b,rad,rad,p);}
        void panel(Canvas c,float l,float t,float r,float b,int color){round(c,l,t,r,b,34,color);}

        void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Layout.Alignment align,float width){
            tp.setTextSize(size); tp.setColor(color); tp.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));
            StaticLayout sl=StaticLayout.Builder.obtain(s,0,s.length(),tp,(int)width)
                    .setAlignment(align).setTextDirection(TextDirectionHeuristics.RTL).setIncludePad(true).build();
            float tx = align==Layout.Alignment.ALIGN_CENTER ? x-width/2f : x-width;
            c.save(); c.translate(tx,y); sl.draw(c); c.restore();
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            float x=(e.getX()-ox)/scale, y=(e.getY()-oy)/scale;

            if(exitConfirm){
                if(in(x,y,560,1160,880,1310)){finishActivity();return true;}
                if(in(x,y,200,1160,520,1310)){exitConfirm=false;invalidate();return true;}
                return true;
            }
            if(settings){
                if(in(x,y,190,900,890,1080)){toggleMusic();invalidate();return true;}
                if(in(x,y,190,1160,890,1340)){Toast.makeText(getContext(),"افکت صدا در نسخه پایه فعال است.",Toast.LENGTH_SHORT).show();return true;}
                if(in(x,y,190,1420,890,1600)){askExit();return true;}
                if(in(x,y,60,0,1020,560)){settings=false;invalidate();return true;}
                return true;
            }

            if(screen==START){if(in(x,y,300,1030,780,1260)){goTo(OFFICE);}return true;}

            // Large, separated top controls: back on left, settings on right.
            if(screen!=OFFICE && in(x,y,25,20,170,145)){goBack();return true;}
            if(in(x,y,900,20,1060,150)){settings=true;invalidate();return true;}

            if(screen==OFFICE){
                if(in(x,y,0,280,520,1370)){goTo(LIBRARY);return true;}
                if(in(x,y,650,820,930,1560)){goTo(ARCHIVE);return true;}
                if(in(x,y,850,1450,1080,2200)){goTo(PROGRESS);return true;}
                if(in(x,y,0,1760,310,2220)){goTo(LAWBOOK);return true;}
                if(in(x,y,300,1660,820,2300)){selectedCase=1;resetCase1();storyStep=0;goTo(CASE);return true;}
            }
            else if(screen==LIBRARY){
                for(int i=1;i<=5;i++){
                    float cx=folderX(i), cy=folderY(i);
                    if(in(x,y,cx-125,cy-125,cx+125,cy+125)){
                        if(i<=unlocked()){selectedCase=i;resetCase1();storyStep=0;goTo(CASE);} else Toast.makeText(getContext(),"این پرونده هنوز قفل است.",Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
            }
            else if(screen==CASE){
                if(selectedCase==1){
                    if(storyStep==0){
                        if(in(x,y,70,2000,1010,2135)){case1Action=1; Toast.makeText(getContext(),"درست: قبل از تصمیم حقوقی، واقعیت‌ها و اطلاعات پرونده را دقیق جمع می‌کنیم.",Toast.LENGTH_SHORT).show(); invalidate(); return true;}
                        if(in(x,y,70,2120,1010,2250)){case1Action=2; Toast.makeText(getContext(),"این انتخاب عجولانه است؛ هنوز اطلاعات لازم پرونده کامل نشده است.",Toast.LENGTH_SHORT).show(); invalidate(); return true;}
                        if(case1Action!=0 && in(x,y,70,2240,1010,2390)){storyStep++;case1Action=0;invalidate();return true;}
                    } else if(storyStep==1){
                        if(in(x,y,45,1250,305,1435)){clues[0]=true; clueDetail="رسید انتقال وجه: مبلغ از حساب امیر به حساب طرف مقابل منتقل شده است.";invalidate();return true;}
                        if(in(x,y,295,1250,555,1435)){clues[1]=true; clueDetail="پرینت پیام: طرف مقابل دریافت پول و وعده بازپرداخت را در پیام تأیید کرده است.";invalidate();return true;}
                        if(in(x,y,545,1250,805,1435)){clues[2]=true; clueDetail="یادداشت موعد: امیر زمان مورد توافق برای بازپرداخت را یادداشت کرده؛ این یادداشت به‌تنهایی جای همه دلایل را نمی‌گیرد.";invalidate();return true;}
                        if(in(x,y,70,2070,1010,2235) && clues[0]&&clues[1]&&clues[2]){storyStep++;clueDetail="";invalidate();return true;}
                    } else if(storyStep==2){
                        if(in(x,y,70,2000,1010,2135)){case1Action=3;Toast.makeText(getContext(),"درست: اطلاعات اصلی دعوا را قبل از تنظیم دادخواست جمع‌بندی کردی.",Toast.LENGTH_SHORT).show();invalidate();return true;}
                        if(in(x,y,70,2120,1010,2250)){case1Action=4;Toast.makeText(getContext(),"نادرست: یک پیام یا یک مدرک، جای مشخص‌کردن کامل اطلاعات دعوا را نمی‌گیرد.",Toast.LENGTH_SHORT).show();invalidate();return true;}
                        if((case1Action==3 || case1Action==4) && in(x,y,70,2240,1010,2390)){storyStep++;case1Action=0;invalidate();return true;}
                    } else if(storyStep==3){
                        if(in(x,y,70,2000,1010,2135)){case1Action=5;Toast.makeText(getContext(),"درست: اطلاعات لازم دادخواست را کامل می‌کنی.",Toast.LENGTH_SHORT).show();invalidate();return true;}
                        if(in(x,y,70,2120,1010,2250)){case1Action=6;Toast.makeText(getContext(),"نادرست: بدون مشخص‌کردن اطلاعات اصلی، پرونده را ناقص رها می‌کنی.",Toast.LENGTH_SHORT).show();invalidate();return true;}
                        if((case1Action==5 || case1Action==6) && in(x,y,70,2240,1010,2390)){goTo(LESSON);return true;}
                    }
                    return true;
                }
                if(in(x,y,80,2040,1000,2250)){
                    if(storyStep<3){storyStep++;invalidate();} else goTo(LESSON);
                    return true;
                }
            }
            else if(screen==LESSON){if(in(x,y,100,1850,980,2100)){answerChosen=false;goTo(QUIZ);return true;}}
            else if(screen==QUIZ){
                if(answerChosen && in(x,y,120,2020,960,2210)){goTo(RESULT);return true;}
                if(!answerChosen){
                    for(int i=0;i<3;i++) if(in(x,y,80,630+i*360,1000,920+i*360)){
                        answerChosen=true; correct=i==correctIndex[selectedCase]; invalidate(); return true;
                    }
                }
            }
            else if(screen==RESULT){
                if(in(x,y,120,1050,960,1280)){
                    if(correct){
                        int done=prefs.getInt("completed",0);
                        if(selectedCase>done) prefs.edit().putInt("completed",selectedCase).putInt("unlocked",Math.min(5,selectedCase+1)).apply();
                        history.clear();
                        history.push(OFFICE);
                        screen=LIBRARY;
                        stopMusic();
                        invalidate();
                    } else {
                        answerChosen=false;
                        if(!history.isEmpty() && history.peek()==QUIZ) history.pop();
                        screen=QUIZ;
                        invalidate();
                    }
                    return true;
                }
            }
            return true;
        }

        void resetCase1(){
            case1Action=0;
            clues[0]=false; clues[1]=false; clues[2]=false;
            clueDetail="";
        }

        boolean in(float x,float y,float l,float t,float r,float b){return x>=l&&x<=r&&y>=t&&y<=b;}
        int unlocked(){return Math.max(1,Math.min(5,prefs.getInt("unlocked",1)));}
        String toFa(int n){return String.valueOf(n).replace("0","۰").replace("1","۱").replace("2","۲").replace("3","۳").replace("4","۴").replace("5","۵").replace("6","۶").replace("7","۷").replace("8","۸").replace("9","۹");}

        void goTo(int next){
            if(screen!=next) history.push(screen);
            screen=next;
            if(next==LESSON||next==QUIZ||next==CASE) startMusic(); else stopMusic();
            invalidate();
        }

        boolean goBack(){
            if(exitConfirm||settings){exitConfirm=false;settings=false;invalidate();return true;}
            if(history.isEmpty())return false;
            screen=history.pop(); stopMusic(); invalidate(); return true;
        }

        void askExit(){exitConfirm=true;settings=false;invalidate();}
        void finishActivity(){((Activity)getContext()).finish();}

        void toggleMusic(){
            boolean on=prefs.getBoolean("music",true);
            prefs.edit().putBoolean("music",!on).apply();
            if(on) stopMusic(); else startMusic();
        }

        void startMusic(){
            if(!prefs.getBoolean("music",true))return;
            stopMusic();
            try{
                music=MediaPlayer.create(getContext(),R.raw.stage_music);
                if(music!=null){music.setLooping(true);music.setVolume(.55f,.55f);music.start();}
            }catch(Exception ignored){music=null;}
        }

        void stopMusic(){
            if(music!=null){try{music.stop();}catch(Exception ignored){} try{music.release();}catch(Exception ignored){} music=null;}
        }
    }
}
