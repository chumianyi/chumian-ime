package com.chumian.ime;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.HorizontalScrollView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChumianInputMethodService extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView keyboardView;
    private Keyboard currentKeyboard;
    private int currentMode = 0; // 0=拼音,1=英语,2=手写,3=摩斯,4=Emoji,5=随机,6=繁体,7=生僻字
    private boolean isShift = false;
    private StringBuilder pinyinBuilder = new StringBuilder();
    private List<String> candidates = new ArrayList<>();
    private LinearLayout candidateBar;
    private HorizontalScrollView candidateScroll;
    private LinearLayout keyboardContainer;
    private View handwritingView;
    private StringBuilder morseBuilder = new StringBuilder();
    private static final Map<String, String> MORSE_MAP = new HashMap<>();

    static {
        MORSE_MAP.put(".-","A"); MORSE_MAP.put("-...","B"); MORSE_MAP.put("-.-.","C");
        MORSE_MAP.put("-..","D"); MORSE_MAP.put(".","E"); MORSE_MAP.put("..-.","F");
        MORSE_MAP.put("--.","G"); MORSE_MAP.put("....","H"); MORSE_MAP.put("..","I");
        MORSE_MAP.put(".---","J"); MORSE_MAP.put("-.-","K"); MORSE_MAP.put(".-..","L");
        MORSE_MAP.put("--","M"); MORSE_MAP.put("-.","N"); MORSE_MAP.put("---","O");
        MORSE_MAP.put(".--.","P"); MORSE_MAP.put("--.-","Q"); MORSE_MAP.put(".-.","R");
        MORSE_MAP.put("...","S"); MORSE_MAP.put("-","T"); MORSE_MAP.put("..-","U");
        MORSE_MAP.put("...-","V"); MORSE_MAP.put(".--","W"); MORSE_MAP.put("-..-","X");
        MORSE_MAP.put("-.--","Y"); MORSE_MAP.put("--..","Z");
    }

    @Override
    public View onCreateInputView() {
        keyboardContainer = new LinearLayout(this);
        keyboardContainer.setOrientation(LinearLayout.VERTICAL);
        keyboardContainer.setBackgroundColor(Color.parseColor("#F5F5FA"));
        keyboardContainer.setPadding(8, 8, 8, 8);

        // 候选词栏
        candidateScroll = new HorizontalScrollView(this);
        candidateBar = new LinearLayout(this);
        candidateBar.setOrientation(LinearLayout.HORIZONTAL);
        candidateScroll.addView(candidateBar);
        candidateScroll.setBackgroundColor(Color.parseColor("#FFFFFF"));
        candidateScroll.setPadding(4, 8, 4, 8);
        keyboardContainer.addView(candidateScroll);

        // 键盘区域
        keyboardView = new KeyboardView(this, null);
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setPreviewEnabled(true);
        keyboardView.setBackgroundColor(Color.parseColor("#E8E8F0"));
        LinearLayout.LayoutParams kvParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        keyboardContainer.addView(keyboardView, kvParams);

        // 手写视图
        handwritingView = createHandwritingView();
        handwritingView.setVisibility(View.GONE);
        keyboardContainer.addView(handwritingView);

        switchKeyboard(0);
        return keyboardContainer;
    }

    private View createHandwritingView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#FFFFFF"));
        layout.setPadding(16, 16, 16, 16);

        TextView title = new TextView(this);
        title.setText("手写输入 - 点击常用字");
        title.setTextSize(14);
        title.setTextColor(Color.parseColor("#666666"));
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        // 常用字
        HorizontalScrollView commonScroll = new HorizontalScrollView(this);
        LinearLayout commonLayout = new LinearLayout(this);
        commonLayout.setOrientation(LinearLayout.HORIZONTAL);
        String[] commonChars = {"的","一","是","在","不","了","有","和","人","这","中","大","为","上","个","我","以","要","他","时","来","用","们","生","到","作","地","于","出","就","分","对","成","会","可","你","能","而","子","那","得","着","下","自","之","年","过","发","后","里","道","行","所","然","家","种","事","方","多","经","么","去","法","学","如","都","同","现","当","没","动","面","起","看","定","天","还","进","好","小","部","其","些","主","样","理","心","她","本","前","开","但","因","只","从","想","实"};
        for (String c : commonChars) {
            Button btn = new Button(this);
            btn.setText(c);
            btn.setTextSize(18);
            btn.setBackgroundColor(Color.parseColor("#F3E5F5"));
            btn.setTextColor(Color.parseColor("#6A1B9A"));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            bp.setMargins(2, 4, 2, 4);
            btn.setLayoutParams(bp);
            btn.setOnClickListener(v -> commitText(c));
            commonLayout.addView(btn);
        }
        commonScroll.addView(commonLayout);
        layout.addView(commonScroll);

        return layout;
    }

    private void switchKeyboard(int mode) {
        currentMode = mode;
        if (handwritingView != null) handwritingView.setVisibility(View.GONE);
        if (keyboardView != null) keyboardView.setVisibility(View.VISIBLE);
        morseBuilder.setLength(0);

        if (mode == 2) {
            if (handwritingView != null) handwritingView.setVisibility(View.VISIBLE);
            if (keyboardView != null) keyboardView.setVisibility(View.GONE);
            return;
        }

        // 使用XML键盘
        int keyboardRes = getResources().getIdentifier("keyboard_qwerty", "xml", getPackageName());
        if (keyboardRes != 0) {
            currentKeyboard = new Keyboard(this, keyboardRes);
        } else {
            // fallback: 创建简单键盘
            currentKeyboard = createSimpleKeyboard();
        }
        if (keyboardView != null && currentKeyboard != null) {
            keyboardView.setKeyboard(currentKeyboard);
        }
    }

    private Keyboard createSimpleKeyboard() {
        // 使用Android默认键盘布局
        return new Keyboard(this, android.R.xml.qwerty);
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            getCurrentInputConnection().deleteSurroundingText(1, 0);
            return;
        }
        if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            isShift = !isShift;
            if (currentKeyboard != null) currentKeyboard.setShifted(isShift);
            if (keyboardView != null) keyboardView.invalidateAllKeys();
            return;
        }
        if (primaryCode == Keyboard.KEYCODE_DONE || primaryCode == KeyEvent.KEYCODE_ENTER) {
            getCurrentInputConnection().performEditorAction(EditorInfo.IME_ACTION_DONE);
            return;
        }
        if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE) {
            // 切换键盘模式
            currentMode = (currentMode + 1) % 8;
            switchKeyboard(currentMode);
            return;
        }
        if (primaryCode == ' ') {
            commitText(" ");
            pinyinBuilder.setLength(0);
            updateCandidates();
            return;
        }

        // 普通字符
        char code = (char) primaryCode;
        if (Character.isLetter(code)) {
            if (isShift) code = Character.toUpperCase(code);
            if (currentMode == 0) {
                pinyinBuilder.append(Character.toLowerCase(code));
                updateCandidates();
            } else {
                commitText(String.valueOf(code));
            }
        } else {
            commitText(String.valueOf(code));
        }
    }

    private void updateCandidates() {
        candidateBar.removeAllViews();
        String pinyin = pinyinBuilder.toString();
        if (pinyin.isEmpty()) return;

        // 显示拼音
        Button pinyinBtn = new Button(this);
        pinyinBtn.setText(pinyin);
        pinyinBtn.setTextSize(14);
        pinyinBtn.setBackgroundColor(Color.parseColor("#E3F2FD"));
        pinyinBtn.setTextColor(Color.parseColor("#1565C0"));
        pinyinBtn.setOnClickListener(v -> {
            commitText(pinyin);
            pinyinBuilder.setLength(0);
            updateCandidates();
        });
        candidateBar.addView(pinyinBtn);

        // 简单候选字
        String[] candidates = getSimpleCandidates(pinyin);
        for (String c : candidates) {
            Button btn = new Button(this);
            btn.setText(c);
            btn.setTextSize(16);
            btn.setBackgroundColor(Color.parseColor("#FFFFFF"));
            btn.setTextColor(Color.parseColor("#333333"));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            bp.setMargins(4, 0, 4, 0);
            btn.setLayoutParams(bp);
            btn.setOnClickListener(v -> {
                commitText(c);
                pinyinBuilder.setLength(0);
                updateCandidates();
            });
            candidateBar.addView(btn);
        }
    }

    private String[] getSimpleCandidates(String pinyin) {
        Map<String, String[]> map = new HashMap<>();
        map.put("a", new String[]{"啊","阿","呵"});
        map.put("ai", new String[]{"爱","哎","唉","矮","碍"});
        map.put("an", new String[]{"安","按","暗","岸","案"});
        map.put("ang", new String[]{"昂","盎"});
        map.put("ao", new String[]{"奥","熬","傲","凹","澳"});
        map.put("ba", new String[]{"吧","把","八","爸","巴"});
        map.put("bai", new String[]{"白","百","拜","败","摆"});
        map.put("ban", new String[]{"班","半","办","版","般"});
        map.put("bang", new String[]{"帮","棒","榜","绑","磅"});
        map.put("bao", new String[]{"包","报","保","宝","抱"});
        map.put("bei", new String[]{"北","被","背","杯","悲"});
        map.put("ben", new String[]{"本","笨","奔"});
        map.put("beng", new String[]{"蹦","崩","绷"});
        map.put("bi", new String[]{"比","必","笔","币","毕"});
        map.put("bian", new String[]{"变","边","便","遍","编"});
        map.put("biao", new String[]{"表","标","彪","膘"});
        map.put("bie", new String[]{"别","憋","瘪"});
        map.put("bin", new String[]{"宾","滨","彬","斌"});
        map.put("bing", new String[]{"并","病","冰","兵","饼"});
        map.put("bo", new String[]{"波","博","播","驳","泊"});
        map.put("bu", new String[]{"不","部","步","布","补"});
        map.put("ca", new String[]{"擦","嚓"});
        map.put("cai", new String[]{"才","菜","财","猜","材"});
        map.put("can", new String[]{"参","餐","残","惨","蚕"});
        map.put("cang", new String[]{"藏","仓","苍","舱"});
        map.put("cao", new String[]{"草","操","曹","槽"});
        map.put("ce", new String[]{"测","侧","册","策"});
        map.put("ceng", new String[]{"层","曾","蹭"});
        map.put("cha", new String[]{"查","茶","差","插","察"});
        map.put("chai", new String[]{"拆","柴","差"});
        map.put("chan", new String[]{"产","缠","馋","掺","蝉"});
        map.put("chang", new String[]{"长","常","场","唱","厂"});
        map.put("chao", new String[]{"超","朝","潮","炒","吵"});
        map.put("che", new String[]{"车","扯","彻","撤"});
        map.put("chen", new String[]{"陈","沉","晨","称","趁"});
        map.put("cheng", new String[]{"成","城","程","称","诚"});
        map.put("chi", new String[]{"吃","持","尺","迟","池"});
        map.put("chong", new String[]{"冲","虫","充","重","崇"});
        map.put("chou", new String[]{"抽","丑","臭","仇","愁"});
        map.put("chu", new String[]{"出","处","初","除","楚"});
        map.put("chuan", new String[]{"穿","川","传","船","串"});
        map.put("chuang", new String[]{"窗","床","创","闯"});
        map.put("chui", new String[]{"吹","垂","锤","炊"});
        map.put("chun", new String[]{"春","纯","唇","蠢"});
        map.put("chuo", new String[]{"戳","绰","辍"});
        map.put("ci", new String[]{"此","次","词","刺","磁"});
        map.put("cong", new String[]{"从","聪","葱","丛"});
        map.put("cou", new String[]{"凑","辏"});
        map.put("cu", new String[]{"粗","促","醋","簇"});
        map.put("cuan", new String[]{"窜","篡","蹿"});
        map.put("cui", new String[]{"催","脆","翠","崔","摧"});
        map.put("cun", new String[]{"村","存","寸"});
        map.put("cuo", new String[]{"错","措","挫","撮","搓"});
        map.put("da", new String[]{"大","打","达","答","搭"});
        map.put("dai", new String[]{"带","代","待","袋","戴"});
        map.put("dan", new String[]{"但","单","蛋","担","淡"});
        map.put("dang", new String[]{"当","党","挡","档","荡"});
        map.put("dao", new String[]{"到","道","刀","倒","导"});
        map.put("de", new String[]{"的","得","德","地"});
        map.put("deng", new String[]{"等","灯","登","邓"});
        map.put("di", new String[]{"的","地","低","底","敌"});
        map.put("dian", new String[]{"点","电","店","典","颠"});
        map.put("diao", new String[]{"掉","调","吊","钓","叼"});
        map.put("die", new String[]{"跌","爹","碟","蝶","叠"});
        map.put("ding", new String[]{"定","顶","丁","订","钉"});
        map.put("diu", new String[]{"丢"});
        map.put("dong", new String[]{"东","动","懂","冬","洞"});
        map.put("dou", new String[]{"都","斗","豆","抖","陡"});
        map.put("du", new String[]{"度","读","毒","独","堵"});
        map.put("duan", new String[]{"段","短","断","端","锻"});
        map.put("dui", new String[]{"对","队","堆","兑"});
        map.put("dun", new String[]{"顿","吨","蹲","盾","敦"});
        map.put("duo", new String[]{"多","朵","躲","夺","堕"});
        map.put("e", new String[]{"饿","恶","额","俄","鹅"});
        map.put("en", new String[]{"恩","嗯"});
        map.put("er", new String[]{"二","而","儿","尔","耳"});
        map.put("fa", new String[]{"发","法","罚","伐","乏"});
        map.put("fan", new String[]{"反","饭","犯","烦","翻"});
        map.put("fang", new String[]{"方","放","房","防","访"});
        map.put("fei", new String[]{"非","飞","费","肥","废"});
        map.put("fen", new String[]{"分","份","芬","纷","坟"});
        map.put("feng", new String[]{"风","封","丰","疯","峰"});
        map.put("fo", new String[]{"佛"});
        map.put("fou", new String[]{"否"});
        map.put("fu", new String[]{"父","服","福","府","副"});
        map.put("ga", new String[]{"嘎","噶"});
        map.put("gai", new String[]{"该","改","盖","概","钙"});
        map.put("gan", new String[]{"干","敢","感","赶","甘"});
        map.put("gang", new String[]{"刚","钢","港","岗","纲"});
        map.put("gao", new String[]{"高","搞","告","稿","糕"});
        map.put("ge", new String[]{"个","歌","各","哥","格"});
        map.put("gei", new String[]{"给"});
        map.put("gen", new String[]{"跟","根","亘"});
        map.put("geng", new String[]{"更","耕","庚","羹"});
        map.put("gong", new String[]{"工","公","共","功","供"});
        map.put("gou", new String[]{"够","狗","沟","购","构"});
        map.put("gu", new String[]{"古","故","顾","骨","谷"});
        map.put("gua", new String[]{"挂","瓜","刮","寡"});
        map.put("guai", new String[]{"怪","乖","拐"});
        map.put("guan", new String[]{"关","管","观","官","馆"});
        map.put("guang", new String[]{"光","广","逛"});
        map.put("gui", new String[]{"归","贵","鬼","规","柜"});
        map.put("gun", new String[]{"滚","棍","辊"});
        map.put("guo", new String[]{"国","过","果","锅","裹"});
        map.put("ha", new String[]{"哈","蛤"});
        map.put("hai", new String[]{"还","海","害","孩","嗨"});
        map.put("han", new String[]{"含","汉","寒","喊","汗"});
        map.put("hang", new String[]{"行","航","杭","巷"});
        map.put("hao", new String[]{"好","号","浩","耗","豪"});
        map.put("he", new String[]{"和","河","他","喝","合"});
        map.put("hei", new String[]{"黑","嘿"});
        map.put("hen", new String[]{"很","恨","狠","痕"});
        map.put("heng", new String[]{"横","恒","衡","亨"});
        map.put("hong", new String[]{"红","洪","宏","轰","虹"});
        map.put("hou", new String[]{"后","厚","候","侯","猴"});
        map.put("hu", new String[]{"和","户","湖","胡","虎"});
        map.put("hua", new String[]{"话","花","华","画","化"});
        map.put("huai", new String[]{"坏","怀","淮","槐"});
        map.put("huan", new String[]{"还","换","欢","环","缓"});
        map.put("huang", new String[]{"黄","皇","荒","慌","晃"});
        map.put("hui", new String[]{"会","回","灰","辉","毁"});
        map.put("hun", new String[]{"婚","混","昏","魂","浑"});
        map.put("huo", new String[]{"或","活","火","货","获"});
        map.put("ji", new String[]{"几","机","集","记","己"});
        map.put("jia", new String[]{"家","加","假","价","架"});
        map.put("jian", new String[]{"见","间","建","件","简"});
        map.put("jiang", new String[]{"将","讲","江","降","姜"});
        map.put("jiao", new String[]{"叫","教","交","脚","角"});
        map.put("jie", new String[]{"结","接","节","街","解"});
        map.put("jin", new String[]{"进","今","金","近","尽"});
        map.put("jing", new String[]{"经","京","精","静","境"});
        map.put("jiu", new String[]{"就","九","久","酒","旧"});
        map.put("ju", new String[]{"句","举","局","具","剧"});
        map.put("juan", new String[]{"卷","娟","倦","眷"});
        map.put("jue", new String[]{"觉","决","绝","角","掘"});
        map.put("jun", new String[]{"军","均","君","俊","菌"});
        map.put("ka", new String[]{"卡","咖","喀"});
        map.put("kai", new String[]{"开","凯","慨","楷"});
        map.put("kan", new String[]{"看","砍","刊","勘","堪"});
        map.put("kang", new String[]{"抗","扛","康","糠","炕"});
        map.put("kao", new String[]{"考","靠","烤","拷"});
        map.put("ke", new String[]{"可","课","克","客","刻"});
        map.put("ken", new String[]{"肯","啃","垦","恳"});
        map.put("keng", new String[]{"坑","铿"});
        map.put("kong", new String[]{"空","孔","控","恐"});
        map.put("kou", new String[]{"口","扣","寇","叩"});
        map.put("ku", new String[]{"苦","哭","库","酷","裤"});
        map.put("kua", new String[]{"夸","跨","垮","挎"});
        map.put("kuai", new String[]{"快","块","筷","会"});
        map.put("kuan", new String[]{"宽","款"});
        map.put("kuang", new String[]{"况","矿","狂","框","筐"});
        map.put("kui", new String[]{"亏","愧","溃","葵","魁"});
        map.put("kun", new String[]{"困","昆","捆","坤"});
        map.put("kuo", new String[]{"扩","阔","括","廓"});
        map.put("la", new String[]{"拉","啦","辣","腊","蜡"});
        map.put("lai", new String[]{"来","赖","莱","睐"});
        map.put("lan", new String[]{"兰","蓝","烂","懒","栏"});
        map.put("lang", new String[]{"浪","狼","郎","朗","廊"});
        map.put("lao", new String[]{"老","劳","牢","捞","姥"});
        map.put("le", new String[]{"了","乐","勒","雷"});
        map.put("lei", new String[]{"类","累","雷","泪","垒"});
        map.put("leng", new String[]{"冷","愣","棱"});
        map.put("li", new String[]{"里","力","立","理","李"});
        map.put("lia", new String[]{"俩"});
        map.put("lian", new String[]{"连","脸","练","联","恋"});
        map.put("liang", new String[]{"两","亮","量","良","凉"});
        map.put("liao", new String[]{"了","料","聊","辽","疗"});
        map.put("lie", new String[]{"列","烈","猎","裂","劣"});
        map.put("lin", new String[]{"林","临","邻","淋","琳"});
        map.put("ling", new String[]{"领","零","灵","令","另"});
        map.put("liu", new String[]{"流","留","六","刘","柳"});
        map.put("long", new String[]{"龙","隆","笼","聋","拢"});
        map.put("lou", new String[]{"楼","漏","陋","搂"});
        map.put("lu", new String[]{"路","录","陆","炉","鲁"});
        map.put("lv", new String[]{"绿","律","旅","虑","率"});
        map.put("luan", new String[]{"乱","卵","滦","峦"});
        map.put("lun", new String[]{"论","轮","伦","沦"});
        map.put("luo", new String[]{"落","罗","洛","络","骆"});
        map.put("ma", new String[]{"吗","妈","马","麻","骂"});
        map.put("mai", new String[]{"买","卖","麦","埋","迈"});
        map.put("man", new String[]{"满","慢","漫","蛮","瞒"});
        map.put("mang", new String[]{"忙","芒","盲","茫","莽"});
        map.put("mao", new String[]{"毛","猫","冒","帽","茂"});
        map.put("me", new String[]{"么","嘛"});
        map.put("mei", new String[]{"没","美","每","妹","眉"});
        map.put("men", new String[]{"们","门","闷","扪"});
        map.put("meng", new String[]{"梦","猛","蒙","盟","孟"});
        map.put("mi", new String[]{"米","密","迷","蜜","眯"});
        map.put("mian", new String[]{"面","免","绵","棉","眠"});
        map.put("miao", new String[]{"秒","苗","庙","妙","描"});
        map.put("mie", new String[]{"灭","蔑","咩"});
        map.put("min", new String[]{"民","敏","闽","悯","皿"});
        map.put("ming", new String[]{"明","名","命","鸣","冥"});
        map.put("miu", new String[]{"谬","缪"});
        map.put("mo", new String[]{"摸","末","墨","默","莫"});
        map.put("mou", new String[]{"某","谋","牟"});
        map.put("mu", new String[]{"母","木","目","牧","幕"});
        map.put("na", new String[]{"那","拿","哪","呐","纳"});
        map.put("nai", new String[]{"奶","耐","乃","奈"});
        map.put("nan", new String[]{"南","男","难","楠","喃"});
        map.put("nang", new String[]{"囊","馕"});
        map.put("nao", new String[]{"脑","闹","恼","挠"});
        map.put("ne", new String[]{"呢","讷"});
        map.put("nei", new String[]{"内","馁"});
        map.put("nen", new String[]{"嫩","恁"});
        map.put("neng", new String[]{"能"});
        map.put("ni", new String[]{"你","呢","泥","逆","尼"});
        map.put("nian", new String[]{"年","念","粘","撵","捻"});
        map.put("niang", new String[]{"娘","酿"});
        map.put("niao", new String[]{"鸟","尿"});
        map.put("nie", new String[]{"捏","涅","聂","孽","镍"});
        map.put("nin", new String[]{"您"});
        map.put("ning", new String[]{"宁","凝","拧","柠","咛"});
        map.put("niu", new String[]{"牛","扭","钮","纽"});
        map.put("nong", new String[]{"农","浓","弄","脓"});
        map.put("nu", new String[]{"女","努","怒","奴","弩"});
        map.put("nuan", new String[]{"暖"});
        map.put("nuo", new String[]{"诺","挪","懦","糯"});
        map.put("o", new String[]{"哦","噢","喔"});
        map.put("ou", new String[]{"欧","偶","呕","藕","殴"});
        map.put("pa", new String[]{"怕","爬","帕","趴","啪"});
        map.put("pai", new String[]{"排","派","拍","牌","徘"});
        map.put("pan", new String[]{"盘","判","盼","攀","潘"});
        map.put("pang", new String[]{"旁","胖","庞","彷","磅"});
        map.put("pao", new String[]{"跑","炮","泡","抛","袍"});
        map.put("pei", new String[]{"配","陪","培","赔","佩"});
        map.put("pen", new String[]{"盆","喷"});
        map.put("peng", new String[]{"朋","碰","彭","捧","蓬"});
        map.put("pi", new String[]{"皮","批","屁","脾","疲"});
        map.put("pian", new String[]{"片","篇","偏","骗","便"});
        map.put("piao", new String[]{"票","飘","漂","瓢","嫖"});
        map.put("pie", new String[]{"撇","瞥"});
        map.put("pin", new String[]{"品","拼","贫","频","聘"});
        map.put("ping", new String[]{"平","评","瓶","苹","凭"});
        map.put("po", new String[]{"破","坡","泼","婆","迫"});
        map.put("pou", new String[]{"剖","掊"});
        map.put("pu", new String[]{"普","仆","扑","铺","朴"});
        map.put("qi", new String[]{"其","起","期","七","气"});
        map.put("qia", new String[]{"恰","洽","掐","卡"});
        map.put("qian", new String[]{"前","钱","千","浅","签"});
        map.put("qiang", new String[]{"强","墙","抢","枪","腔"});
        map.put("qiao", new String[]{"桥","巧","敲","瞧","翘"});
        map.put("qie", new String[]{"切","且","窃","茄","怯"});
        map.put("qin", new String[]{"亲","琴","勤","侵","秦"});
        map.put("qing", new String[]{"请","清","青","轻","情"});
        map.put("qiong", new String[]{"穷","琼","穹"});
        map.put("qiu", new String[]{"球","求","秋","丘","囚"});
        map.put("qu", new String[]{"去","取","区","曲","趣"});
        map.put("quan", new String[]{"全","权","圈","泉","拳"});
        map.put("que", new String[]{"却","确","缺","雀","鹊"});
        map.put("qun", new String[]{"群","裙"});
        map.put("ran", new String[]{"然","燃","染","冉"});
        map.put("rang", new String[]{"让","嚷","壤","攘"});
        map.put("rao", new String[]{"绕","扰","饶","娆"});
        map.put("re", new String[]{"热","惹"});
        map.put("ren", new String[]{"人","认","任","忍","仁"});
        map.put("reng", new String[]{"仍","扔"});
        map.put("ri", new String[]{"日"});
        map.put("rong", new String[]{"容","荣","融","熔","溶"});
        map.put("rou", new String[]{"肉","柔","揉","蹂"});
        map.put("ru", new String[]{"如","入","乳","儒","茹"});
        map.put("ruan", new String[]{"软","阮"});
        map.put("rui", new String[]{"瑞","锐","蕊","睿"});
        map.put("run", new String[]{"润","闰"});
        map.put("ruo", new String[]{"若","弱","偌"});
        map.put("sa", new String[]{"撒","洒","萨","卅"});
        map.put("sai", new String[]{"赛","塞","腮","鳃"});
        map.put("san", new String[]{"三","散","伞","叁"});
        map.put("sang", new String[]{"桑","嗓","丧","搡"});
        map.put("sao", new String[]{"扫","骚","嫂","臊"});
        map.put("se", new String[]{"色","涩","瑟","塞","啬"});
        map.put("sen", new String[]{"森"});
        map.put("seng", new String[]{"僧"});
        map.put("sha", new String[]{"杀","沙","傻","啥","纱"});
        map.put("shai", new String[]{"晒","筛"});
        map.put("shan", new String[]{"山","善","闪","衫","扇"});
        map.put("shang", new String[]{"上","商","伤","尚","赏"});
        map.put("shao", new String[]{"少","烧","稍","勺","哨"});
        map.put("she", new String[]{"社","设","射","蛇","舌"});
        map.put("shei", new String[]{"谁"});
        map.put("shen", new String[]{"什","深","身","神","甚"});
        map.put("sheng", new String[]{"生","声","省","圣","胜"});
        map.put("shi", new String[]{"是","时","事","市","十"});
        map.put("shou", new String[]{"手","收","首","受","瘦"});
        map.put("shu", new String[]{"书","数","树","熟","输"});
        map.put("shua", new String[]{"刷","耍"});
        map.put("shuai", new String[]{"帅","摔","衰","甩"});
        map.put("shuan", new String[]{"栓","拴","闩","涮"});
        map.put("shuang", new String[]{"双","爽","霜"});
        map.put("shui", new String[]{"水","谁","睡","税","说"});
        map.put("shun", new String[]{"顺","瞬","舜"});
        map.put("shuo", new String[]{"说","硕","朔","烁"});
        map.put("si", new String[]{"四","死","思","私","司"});
        map.put("song", new String[]{"送","松","宋","颂","诵"});
        map.put("sou", new String[]{"搜","艘","嗖","叟"});
        map.put("su", new String[]{"苏","速","素","诉","俗"});
        map.put("suan", new String[]{"算","酸","蒜"});
        map.put("sui", new String[]{"虽","随","岁","碎","隋"});
        map.put("sun", new String[]{"孙","损","笋"});
        map.put("suo", new String[]{"所","锁","索","缩","梭"});
        map.put("ta", new String[]{"他","她","它","塔","踏"});
        map.put("tai", new String[]{"太","台","态","泰","抬"});
        map.put("tan", new String[]{"谈","弹","探","叹","碳"});
        map.put("tang", new String[]{"堂","糖","唐","汤","躺"});
        map.put("tao", new String[]{"套","逃","桃","淘","涛"});
        map.put("te", new String[]{"特"});
        map.put("teng", new String[]{"疼","腾","藤"});
        map.put("ti", new String[]{"提","体","题","替","踢"});
        map.put("tian", new String[]{"天","田","甜","填","添"});
        map.put("tiao", new String[]{"条","跳","调","挑","眺"});
        map.put("tie", new String[]{"铁","贴","帖"});
        map.put("ting", new String[]{"听","停","亭","廷","挺"});
        map.put("tong", new String[]{"同","通","痛","统","童"});
        map.put("tou", new String[]{"头","投","透","偷"});
        map.put("tu", new String[]{"图","土","涂","途","兔"});
        map.put("tuan", new String[]{"团","湍"});
        map.put("tui", new String[]{"推","退","腿","颓"});
        map.put("tun", new String[]{"吞","屯","臀","囤"});
        map.put("tuo", new String[]{"脱","拖","托","驮","妥"});
        map.put("wa", new String[]{"挖","哇","蛙","瓦","袜"});
        map.put("wai", new String[]{"外","歪","崴"});
        map.put("wan", new String[]{"完","万","晚","玩","碗"});
        map.put("wang", new String[]{"王","往","网","望","忘"});
        map.put("wei", new String[]{"为","位","围","微","味"});
        map.put("wen", new String[]{"文","问","闻","温","稳"});
        map.put("weng", new String[]{"翁","嗡","瓮"});
        map.put("wo", new String[]{"我","握","窝","卧","沃"});
        map.put("wu", new String[]{"五","无","物","武","务"});
        map.put("xi", new String[]{"西","习","喜","洗","系"});
        map.put("xia", new String[]{"下","夏","吓","虾","瞎"});
        map.put("xian", new String[]{"先","现","线","县","鲜"});
        map.put("xiang", new String[]{"想","向","像","香","响"});
        map.put("xiao", new String[]{"小","笑","校","效","萧"});
        map.put("xie", new String[]{"些","写","谢","鞋","斜"});
        map.put("xin", new String[]{"新","心","信","欣","辛"});
        map.put("xing", new String[]{"行","星","兴","型","姓"});
        map.put("xiong", new String[]{"雄","熊","凶","兄","胸"});
        map.put("xiu", new String[]{"修","休","秀","袖","绣"});
        map.put("xu", new String[]{"需","许","续","须","虚"});
        map.put("xuan", new String[]{"选","宣","悬","旋","玄"});
        map.put("xue", new String[]{"学","雪","血","穴","靴"});
        map.put("xun", new String[]{"寻","训","讯","勋","循"});
        map.put("ya", new String[]{"呀","压","牙","鸦","雅"});
        map.put("yan", new String[]{"眼","言","严","烟","沿"});
        map.put("yang", new String[]{"样","阳","养","央","羊"});
        map.put("yao", new String[]{"要","药","摇","咬","腰"});
        map.put("ye", new String[]{"也","夜","叶","业","野"});
        map.put("yi", new String[]{"一","以","已","意","义"});
        map.put("yin", new String[]{"因","音","银","引","印"});
        map.put("ying", new String[]{"应","英","影","营","迎"});
        map.put("yo", new String[]{"哟","唷"});
        map.put("yong", new String[]{"用","永","勇","拥","涌"});
        map.put("you", new String[]{"有","又","右","友","优"});
        map.put("yu", new String[]{"与","于","语","雨","鱼"});
        map.put("yuan", new String[]{"元","原","远","园","员"});
        map.put("yue", new String[]{"月","越","约","乐","跃"});
        map.put("yun", new String[]{"云","运","允","韵","孕"});
        map.put("za", new String[]{"杂","砸","咋","匝"});
        map.put("zai", new String[]{"在","再","载","灾","栽"});
        map.put("zan", new String[]{"咱","暂","赞","攒"});
        map.put("zang", new String[]{"脏","葬","藏","臧"});
        map.put("zao", new String[]{"早","造","找","遭","糟"});
        map.put("ze", new String[]{"则","责","择","泽","仄"});
        map.put("zei", new String[]{"贼"});
        map.put("zen", new String[]{"怎","谮"});
        map.put("zeng", new String[]{"增","曾","赠","憎"});
        map.put("zha", new String[]{"扎","炸","渣","眨","栅"});
        map.put("zhai", new String[]{"摘","窄","宅","债","寨"});
        map.put("zhan", new String[]{"站","战","占","展","粘"});
        map.put("zhang", new String[]{"张","长","章","掌","涨"});
        map.put("zhao", new String[]{"找","照","招","赵","召"});
        map.put("zhe", new String[]{"这","着","者","哲","折"});
        map.put("zhei", new String[]{"这"});
        map.put("zhen", new String[]{"真","针","镇","阵","振"});
        map.put("zheng", new String[]{"正","整","政","证","征"});
        map.put("zhi", new String[]{"只","知","之","直","至"});
        map.put("zhong", new String[]{"中","种","重","众","钟"});
        map.put("zhou", new String[]{"周","州","洲","舟","粥"});
        map.put("zhu", new String[]{"主","住","注","猪","竹"});
        map.put("zhua", new String[]{"抓","爪"});
        map.put("zhuai", new String[]{"拽","转"});
        map.put("zhuan", new String[]{"转","专","赚","砖","撰"});
        map.put("zhuang", new String[]{"装","状","壮","撞","庄"});
        map.put("zhui", new String[]{"追","坠","缀","锥"});
        map.put("zhun", new String[]{"准","谆"});
        map.put("zhuo", new String[]{"着","桌","捉","拙","灼"});
        map.put("zi", new String[]{"子","自","字","紫","资"});
        map.put("zong", new String[]{"总","宗","综","踪","纵"});
        map.put("zou", new String[]{"走","奏","揍","邹"});
        map.put("zu", new String[]{"足","组","族","阻","祖"});
        map.put("zuan", new String[]{"钻","躜","纂"});
        map.put("zui", new String[]{"最","嘴","醉","罪"});
        map.put("zun", new String[]{"尊","遵","樽"});
        map.put("zuo", new String[]{"做","作","坐","左","座"});

        String[] result = map.get(pinyin);
        if (result != null) return result;
        return new String[]{};
    }

    private void commitText(String text) {
        getCurrentInputConnection().commitText(text, 1);
    }

    @Override
    public void onPress(int primaryCode) {}
    @Override
    public void onRelease(int primaryCode) {}
    @Override
    public void onText(CharSequence text) {}
    @Override
    public void swipeLeft() {}
    @Override
    public void swipeRight() {}
    @Override
    public void swipeDown() {}
    @Override
    public void swipeUp() {}
}
