package com.chumian.ime;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.inputmethod.InputConnection;
import java.util.ArrayList;
import java.util.List;

public class ChumianInputMethodService extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    private static final int MODE_QWERTY = 0;
    private static final int MODE_SYMBOLS = 1;
    private static final int MODE_NUMBERS = 2;
    private static final int MODE_HANDWRITING = 3;
    private static final int MODE_EMOJI = 4;

    private KeyboardView keyboardView;
    private Keyboard qwertyKeyboard;
    private Keyboard symbolsKeyboard;
    private Keyboard numbersKeyboard;
    private int currentMode = MODE_QWERTY;
    private boolean isUpperCase = false;
    private String composingText = "";
    private LinearLayout candidateBar;
    private List<String> candidates = new ArrayList<>();
    private List<String> handwritingHistory = new ArrayList<>();

    // 拼音映射（简化版）
    private static final String[][] PINYIN_MAP = {
        {"a", "啊阿呵吖"}, {"ai", "爱艾唉挨"}, {"an", "安按岸暗"}, {"ang", "昂肮"},
        {"ao", "奥傲熬凹"}, {"ba", "吧把八爸"}, {"bai", "白百拜败"}, {"ban", "半办班版"},
        {"bang", "帮棒榜绑"}, {"bao", "包报保宝"}, {"bei", "被北背杯"}, {"ben", "本笨奔"},
        {"beng", "蹦崩绷"}, {"bi", "比笔必逼"}, {"bian", "边变便遍"}, {"biao", "表标彪"},
        {"bie", "别憋"}, {"bin", "宾滨彬"}, {"bing", "并病冰兵"}, {"bo", "波博播伯"},
        {"bu", "不布步部"}, {"ca", "擦嚓"}, {"cai", "才菜财猜"}, {"can", "参残餐惨"},
        {"cang", "藏仓苍"}, {"cao", "草操曹"}, {"ce", "册测侧策"}, {"ceng", "层曾蹭"},
        {"cha", "查差插茶"}, {"chai", "拆柴"}, {"chan", "产缠馋"}, {"chang", "长常场唱"},
        {"chao", "超朝潮炒"}, {"che", "车彻扯"}, {"chen", "沉陈晨衬"}, {"cheng", "成城程称"},
        {"chi", "吃迟尺持"}, {"chong", "冲虫重充"}, {"chou", "抽愁丑臭"}, {"chu", "出初除处"},
        {"chuan", "穿传船串"}, {"chuang", "窗床创"}, {"chui", "吹垂锤"}, {"chun", "春纯蠢"},
        {"chuo", "戳辍"}, {"ci", "此次词刺"}, {"cong", "从聪丛"}, {"cou", "凑"},
        {"cu", "粗促醋"}, {"cuan", "窜篡"}, {"cui", "催脆翠"}, {"cun", "村存寸"},
        {"cuo", "错措挫"}, {"da", "大打达答"}, {"dai", "带代待袋"}, {"dan", "但单蛋担"},
        {"dang", "当党挡荡"}, {"dao", "到道倒刀"}, {"de", "的得德地"}, {"deng", "等灯登瞪"},
        {"di", "的低地第"}, {"dian", "点电店典"}, {"diao", "掉调吊钓"}, {"die", "跌叠碟"},
        {"ding", "定顶丁订"}, {"diu", "丢"}, {"dong", "东动懂冬"}, {"dou", "都斗豆抖"},
        {"du", "度读独毒"}, {"duan", "段短断端"}, {"dui", "对队堆"}, {"dun", "顿吨盾蹲"},
        {"duo", "多夺朵躲"}, {"e", "饿恶额俄"}, {"en", "恩嗯"}, {"er", "二而儿耳"},
        {"fa", "发法罚乏"}, {"fan", "反饭番烦"}, {"fang", "方放房防"}, {"fei", "非飞费肥"},
        {"fen", "分份纷芬"}, {"feng", "风封丰峰"}, {"fo", "佛"}, {"fou", "否"},
        {"fu", "父服付福"}, {"ga", "嘎噶"}, {"gai", "该改盖概"}, {"gan", "干敢感赶"},
        {"gang", "刚钢港岗"}, {"gao", "高搞告糕"}, {"ge", "个哥歌各"}, {"gei", "给"},
        {"gen", "跟根"}, {"geng", "更耕庚"}, {"gong", "工公功共"}, {"gou", "够狗沟购"},
        {"gu", "古故股骨"}, {"gua", "瓜挂刮寡"}, {"guai", "怪乖拐"}, {"guan", "关管观官"},
        {"guang", "光广逛"}, {"gui", "贵归鬼规"}, {"gun", "滚棍"}, {"guo", "国过果锅"},
        {"ha", "哈蛤"}, {"hai", "还海孩害"}, {"han", "汉含寒喊"}, {"hang", "行航杭"},
        {"hao", "好号浩毫"}, {"he", "和何喝合"}, {"hei", "黑嘿"}, {"hen", "很恨狠"},
        {"heng", "横恒亨"}, {"hong", "红洪宏轰"}, {"hou", "后厚候猴"}, {"hu", "户湖胡虎"},
        {"hua", "话花画华"}, {"huai", "坏怀淮"}, {"huan", "还换欢环"}, {"huang", "黄皇荒慌"},
        {"hui", "会回灰辉"}, {"hun", "婚魂混浑"}, {"huo", "活火或货"}, {"ji", "几机集记"},
        {"jia", "家加假价"}, {"jian", "见间建件"}, {"jiang", "将江讲奖"}, {"jiao", "叫教交脚"},
        {"jie", "节结接解"}, {"jin", "进近金今"}, {"jing", "经京精静"}, {"jiong", "窘迥"},
        {"jiu", "就九久酒"}, {"ju", "句局举巨"}, {"juan", "卷娟眷"}, {"jue", "觉决绝角"},
        {"jun", "军君均俊"}, {"ka", "卡咖喀"}, {"kai", "开凯慨"}, {"kan", "看砍刊勘"},
        {"kang", "抗扛康糠"}, {"kao", "考靠烤"}, {"ke", "可课克客"}, {"ken", "肯啃垦"},
        {"keng", "坑吭"}, {"kong", "空孔控"}, {"kou", "口扣寇"}, {"ku", "苦哭库酷"},
        {"kua", "夸跨垮"}, {"kuai", "快块筷"}, {"kuan", "宽款"}, {"kuang", "况狂矿框"},
        {"kui", "亏愧葵"}, {"kun", "困昆捆"}, {"kuo", "扩阔括"}, {"la", "拉啦辣腊"},
        {"lai", "来赖莱"}, {"lan", "兰蓝懒烂"}, {"lang", "浪狼郎朗"}, {"lao", "老劳牢捞"},
        {"le", "了乐勒"}, {"lei", "类累雷泪"}, {"leng", "冷愣棱"}, {"li", "里力理立"},
        {"lia", "俩"}, {"lian", "连脸练联"}, {"liang", "两亮良量"}, {"liao", "了料聊辽"},
        {"lie", "列烈猎裂"}, {"lin", "林临邻淋"}, {"ling", "领零灵令"}, {"liu", "六留流刘"},
        {"long", "龙隆笼拢"}, {"lou", "楼漏搂陋"}, {"lu", "路录陆鹿"}, {"lv", "绿律旅虑"},
        {"luan", "乱卵滦"}, {"lue", "掠略"}, {"lun", "论轮伦"}, {"luo", "落罗络螺"},
        {"ma", "吗妈马骂"}, {"mai", "买卖麦埋"}, {"man", "满慢漫蛮"}, {"mang", "忙盲芒茫"},
        {"mao", "毛猫冒帽"}, {"me", "么"}, {"mei", "没美每妹"}, {"men", "们门闷"},
        {"meng", "梦猛蒙孟"}, {"mi", "米密迷蜜"}, {"mian", "面免棉眠"}, {"miao", "秒妙苗庙"},
        {"mie", "灭蔑"}, {"min", "民敏闽"}, {"ming", "明名命鸣"}, {"miu", "谬"},
        {"mo", "摸末墨莫"}, {"mou", "某谋眸"}, {"mu", "母木目牧"}, {"na", "那拿哪呐"},
        {"nai", "奶耐奈"}, {"nan", "南男难"}, {"nang", "囊馕"}, {"nao", "脑闹恼挠"},
        {"ne", "呢"}, {"nei", "内馁"}, {"nen", "嫩"}, {"neng", "能"},
        {"ni", "你泥拟逆"}, {"nian", "年念粘碾"}, {"niang", "娘酿"}, {"niao", "鸟尿"},
        {"nie", "捏涅镍"}, {"nin", "您"}, {"ning", "宁凝拧"}, {"niu", "牛扭钮"},
        {"nong", "农浓弄"}, {"nu", "女努怒"}, {"nuan", "暖"}, {"nuo", "诺挪糯"},
        {"o", "哦噢"}, {"ou", "欧偶呕"}, {"pa", "怕爬帕趴"}, {"pai", "排派拍牌"},
        {"pan", "盘判盼攀"}, {"pang", "旁胖庞"}, {"pao", "跑炮泡抛"}, {"pei", "配陪培佩"},
        {"pen", "盆喷"}, {"peng", "朋碰彭捧"}, {"pi", "皮批屁匹"}, {"pian", "片篇偏骗"},
        {"piao", "票飘漂瓢"}, {"pie", "撇瞥"}, {"pin", "品拼贫频"}, {"ping", "平评瓶凭"},
        {"po", "破迫坡泼"}, {"pou", "剖"}, {"pu", "普仆铺朴"}, {"qi", "起其气七"},
        {"qia", "恰洽掐"}, {"qian", "前钱千浅"}, {"qiang", "强抢枪墙"}, {"qiao", "桥巧敲瞧"},
        {"qie", "切且茄窃"}, {"qin", "亲琴勤秦"}, {"qing", "请情清青"}, {"qiong", "穷琼"},
        {"qiu", "球求秋丘"}, {"qu", "去取区曲"}, {"quan", "全权利泉"}, {"que", "却确缺雀"},
        {"qun", "群裙"}, {"ran", "然燃染"}, {"rang", "让嚷壤"}, {"rao", "绕扰饶"},
        {"re", "热惹"}, {"ren", "人认任忍"}, {"reng", "仍扔"}, {"ri", "日"},
        {"rong", "容荣融熔"}, {"rou", "肉柔揉"}, {"ru", "如入乳儒"}, {"ruan", "软阮"},
        {"rui", "瑞锐蕊"}, {"run", "润闰"}, {"ruo", "若弱"}, {"sa", "撒洒萨"},
        {"sai", "赛塞腮"}, {"san", "三散伞"}, {"sang", "桑丧嗓"}, {"sao", "扫骚嫂"},
        {"se", "色涩瑟"}, {"sen", "森"}, {"seng", "僧"}, {"sha", "杀沙傻啥"},
        {"shai", "晒筛"}, {"shan", "山善闪衫"}, {"shang", "上商伤尚"}, {"shao", "少烧稍勺"},
        {"she", "社设射蛇"}, {"shei", "谁"}, {"shen", "什深身神"}, {"sheng", "生声胜省"},
        {"shi", "是时十事"}, {"shou", "手收首受"}, {"shu", "书数树熟"}, {"shua", "刷耍"},
        {"shuai", "帅摔衰"}, {"shuan", "栓拴"}, {"shuang", "双爽霜"}, {"shui", "水睡谁"},
        {"shun", "顺瞬舜"}, {"shuo", "说硕朔"}, {"si", "四思死私"}, {"song", "送松宋颂"},
        {"sou", "搜艘嗖"}, {"su", "苏俗速素"}, {"suan", "算酸蒜"}, {"sui", "虽随岁碎"},
        {"sun", "孙损笋"}, {"suo", "所锁缩索"}, {"ta", "他她它塔"}, {"tai", "太台态抬"},
        {"tan", "谈弹探叹"}, {"tang", "堂糖唐躺"}, {"tao", "套逃桃淘"}, {"te", "特"},
        {"teng", "疼腾藤"}, {"ti", "提体题替"}, {"tian", "天田甜填"}, {"tiao", "条跳挑调"},
        {"tie", "铁贴帖"}, {"ting", "听停亭挺"}, {"tong", "同通痛童"}, {"tou", "头投透偷"},
        {"tu", "土图途吐"}, {"tuan", "团湍"}, {"tui", "推退腿颓"}, {"tun", "吞屯臀"},
        {"tuo", "脱拖托妥"}, {"wa", "挖哇娃瓦"}, {"wai", "外歪"}, {"wan", "完晚万玩"},
        {"wang", "王网往望"}, {"wei", "为位未围"}, {"wen", "问文温闻"}, {"weng", "翁嗡"},
        {"wo", "我握卧窝"}, {"wu", "五无物武"}, {"xi", "西习喜洗"}, {"xia", "下夏吓虾"},
        {"xian", "先现线县"}, {"xiang", "想向像香"}, {"xiao", "小笑学校"}, {"xie", "写些谢斜"},
        {"xin", "新心信欣"}, {"xing", "行 Xing 型"}, {"xiong", "雄熊凶胸"}, {"xiu", "修休秀绣"},
        {"xu", "需许续虚"}, {"xuan", "选宣悬旋"}, {"xue", "学雪血穴"}, {"xun", "寻训讯迅"},
        {"ya", "呀压牙鸦"}, {"yan", "眼言严演"}, {"yang", "样阳养羊"}, {"yao", "要药摇咬"},
        {"ye", "也业叶夜"}, {"yi", "一以意已"}, {"yin", "因音引银"}, {"ying", "应英影营"},
        {"yo", "哟唷"}, {"yong", "用永勇拥"}, {"you", "有又右友"}, {"yu", "与于鱼雨"},
        {"yuan", "元原远园"}, {"yue", "月越约乐"}, {"yun", "云运允韵"}, {"za", "杂砸咋"},
        {"zai", "在再载灾"}, {"zan", "咱暂赞"}, {"zang", "脏葬藏"}, {"zao", "早造枣澡"},
        {"ze", "则责泽择"}, {"zei", "贼"}, {"zen", "怎"}, {"zeng", "增曾赠"},
        {"zha", "扎炸渣眨"}, {"zhai", "宅摘窄债"}, {"zhan", "站战占展"}, {"zhang", "张长掌丈"},
        {"zhao", "找照招赵"}, {"zhe", "这着者折"}, {"zhei", "这"}, {"zhen", "真镇针震"},
        {"zheng", "正整争证"}, {"zhi", "只之知直"}, {"zhong", "中重众钟"}, {"zhou", "周州洲粥"},
        {"zhu", "主住注猪"}, {"zhua", "抓爪"}, {"zhuai", "拽"}, {"zhuan", "转专赚砖"},
        {"zhuang", "装状庄撞"}, {"zhui", "追坠锥"}, {"zhun", "准谆"}, {"zhuo", "着桌捉浊"},
        {"zi", "子自字紫"}, {"zong", "总宗综踪"}, {"zou", "走奏揍"}, {"zu", "足组族阻"},
        {"zuan", "钻纂"}, {"zui", "最嘴醉"}, {"zun", "尊遵"}, {"zuo", "做左坐作"}
    };

    @Override
    public View onCreateInputView() {
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#F2F2F7"));

        // 候选词栏
        candidateBar = new LinearLayout(this);
        candidateBar.setOrientation(LinearLayout.HORIZONTAL);
        candidateBar.setBackgroundColor(Color.WHITE);
        candidateBar.setPadding(8, 4, 8, 4);
        candidateBar.setVisibility(View.GONE);
        rootLayout.addView(candidateBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // 键盘视图
        keyboardView = new KeyboardView(this, null);
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setBackgroundColor(Color.parseColor("#D1D1D6"));
        keyboardView.setKeyBackground(getDrawable(android.R.drawable.btn_default));
        rootLayout.addView(keyboardView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // 加载键盘
        qwertyKeyboard = new Keyboard(this, R.xml.keyboard_qwerty);
        symbolsKeyboard = createSymbolsKeyboard();
        numbersKeyboard = createNumbersKeyboard();

        currentMode = MODE_QWERTY;
        keyboardView.setKeyboard(qwertyKeyboard);

        return rootLayout;
    }

    private Keyboard createSymbolsKeyboard() {
        return new Keyboard(this, R.xml.keyboard_symbols);
    }

    private Keyboard createNumbersKeyboard() {
        return new Keyboard(this, R.xml.keyboard_numbers);
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        currentMode = MODE_QWERTY;
        isUpperCase = false;
        composingText = "";
        updateCandidates();
        keyboardView.setKeyboard(qwertyKeyboard);
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                if (composingText.length() > 0) {
                    composingText = composingText.substring(0, composingText.length() - 1);
                    updateCandidates();
                } else {
                    ic.deleteSurroundingText(1, 0);
                }
                break;
            case Keyboard.KEYCODE_SHIFT:
                isUpperCase = !isUpperCase;
                qwertyKeyboard.setShifted(isUpperCase);
                keyboardView.invalidateAllKeys();
                break;
            case Keyboard.KEYCODE_MODE_CHANGE:
                if (currentMode == MODE_QWERTY) {
                    currentMode = MODE_SYMBOLS;
                    keyboardView.setKeyboard(symbolsKeyboard);
                } else {
                    currentMode = MODE_QWERTY;
                    keyboardView.setKeyboard(qwertyKeyboard);
                }
                composingText = "";
                updateCandidates();
                break;
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;
            case 10001: // 123按钮 - 切换到数字
                currentMode = MODE_NUMBERS;
                keyboardView.setKeyboard(numbersKeyboard);
                composingText = "";
                updateCandidates();
                break;
            case 10002: // ABC按钮 - 回到英文
                currentMode = MODE_QWERTY;
                keyboardView.setKeyboard(qwertyKeyboard);
                composingText = "";
                updateCandidates();
                break;
            case 10003: // 手写按钮
                switchToHandwriting();
                break;
            case 10004: // 表情按钮
                switchToEmoji();
                break;
            case 32: // 空格
                if (composingText.length() > 0 && candidates.size() > 0) {
                    ic.commitText(candidates.get(0), 1);
                    composingText = "";
                    updateCandidates();
                } else {
                    ic.commitText(" ", 1);
                }
                break;
            default:
                char code = (char) primaryCode;
                if (Character.isLetter(code)) {
                    if (isUpperCase) {
                        code = Character.toUpperCase(code);
                        isUpperCase = false;
                        qwertyKeyboard.setShifted(false);
                        keyboardView.invalidateAllKeys();
                    }
                    if (currentMode == MODE_QWERTY && !isUpperCase && Character.isLowerCase(code)) {
                        composingText += code;
                        updateCandidates();
                    } else {
                        ic.commitText(String.valueOf(code), 1);
                    }
                } else {
                    ic.commitText(String.valueOf(code), 1);
                }
        }
    }

    private void updateCandidates() {
        candidateBar.removeAllViews();
        candidates.clear();

        if (composingText.length() == 0) {
            candidateBar.setVisibility(View.GONE);
            return;
        }

        // 查找拼音匹配
        for (String[] entry : PINYIN_MAP) {
            if (entry[0].startsWith(composingText.toLowerCase())) {
                for (int i = 0; i < Math.min(5, entry[1].length()); i++) {
                    candidates.add(String.valueOf(entry[1].charAt(i)));
                }
                break;
            }
        }

        if (candidates.isEmpty()) {
            candidateBar.setVisibility(View.GONE);
            return;
        }

        candidateBar.setVisibility(View.VISIBLE);
        for (int i = 0; i < candidates.size(); i++) {
            final String word = candidates.get(i);
            Button btn = new Button(this);
            btn.setText(word);
            btn.setTextSize(16);
            btn.setBackgroundColor(Color.TRANSPARENT);
            btn.setPadding(16, 8, 16, 8);
            btn.setOnClickListener(v -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.commitText(word, 1);
                    composingText = "";
                    updateCandidates();
                }
            });
            candidateBar.addView(btn);
        }
    }

    private void switchToHandwriting() {
        currentMode = MODE_HANDWRITING;
        // 创建手写面板
        LinearLayout hwLayout = new LinearLayout(this);
        hwLayout.setOrientation(LinearLayout.VERTICAL);
        hwLayout.setBackgroundColor(Color.WHITE);

        // 标题栏
        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setPadding(16, 12, 16, 12);
        titleBar.setBackgroundColor(Color.parseColor("#F2F2F7"));

        TextView title = new TextView(this);
        title.setText("手写输入");
        title.setTextSize(15);
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        titleBar.addView(title);

        Button backBtn = new Button(this);
        backBtn.setText("ABC");
        backBtn.setTextSize(13);
        backBtn.setOnClickListener(v -> {
            currentMode = MODE_QWERTY;
            keyboardView.setKeyboard(qwertyKeyboard);
            setContentView(getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null));
            onCreateInputView();
        });
        titleBar.addView(backBtn);
        hwLayout.addView(titleBar);

        // 手写区域（简化为常用字快捷面板）
        ScrollView scrollView = new ScrollView(this);
        LinearLayout hwArea = new LinearLayout(this);
        hwArea.setOrientation(LinearLayout.VERTICAL);
        hwArea.setPadding(16, 16, 16, 16);

        // 常用字
        TextView label = new TextView(this);
        label.setText("常用字（点击输入）");
        label.setTextSize(13);
        label.setTextColor(Color.GRAY);
        hwArea.addView(label);

        String[] commonChars = {"的", "一", "是", "不", "了", "在", "人", "有", "我", "他",
                "这", "个", "们", "中", "来", "上", "大", "为", "和", "国",
                "地", "到", "以", "说", "时", "要", "就", "出", "会", "可",
                "你", "对", "生", "能", "而", "子", "那", "得", "于", "着",
                "下", "自", "之", "年", "过", "发", "后", "作", "里", "用",
                "道", "行", "所", "然", "家", "种", "事", "成", "方", "多",
                "经", "么", "去", "法", "学", "如", "都", "同", "现", "当",
                "没", "动", "面", "起", "看", "定", "天", "分", "还", "进",
                "好", "小", "部", "其", "些", "主", "样", "理", "心", "她",
                "本", "前", "开", "但", "因", "只", "从", "想", "实", "日"};

        LinearLayout charGrid = new LinearLayout(this);
        charGrid.setOrientation(LinearLayout.VERTICAL);
        for (int row = 0; row < 10; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < 10; col++) {
                final String ch = commonChars[row * 10 + col];
                Button charBtn = new Button(this);
                charBtn.setText(ch);
                charBtn.setTextSize(18);
                charBtn.setBackgroundColor(Color.parseColor("#F2F2F7"));
                charBtn.setPadding(8, 12, 8, 12);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                params.setMargins(2, 2, 2, 2);
                charBtn.setLayoutParams(params);
                charBtn.setOnClickListener(v -> {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        ic.commitText(ch, 1);
                        handwritingHistory.add(ch);
                        if (handwritingHistory.size() > 50) {
                            handwritingHistory.remove(0);
                        }
                    }
                });
                rowLayout.addView(charBtn);
            }
            charGrid.addView(rowLayout);
        }
        hwArea.addView(charGrid);

        // 历史记录
        if (!handwritingHistory.isEmpty()) {
            TextView histLabel = new TextView(this);
            histLabel.setText("最近输入");
            histLabel.setTextSize(13);
            histLabel.setTextColor(Color.GRAY);
            histLabel.setPadding(0, 16, 0, 8);
            hwArea.addView(histLabel);

            StringBuilder histText = new StringBuilder();
            for (int i = handwritingHistory.size() - 1; i >= 0; i--) {
                histText.append(handwritingHistory.get(i));
            }
            TextView histView = new TextView(this);
            histView.setText(histText.toString());
            histView.setTextSize(18);
            histView.setPadding(8, 8, 8, 8);
            histView.setBackgroundColor(Color.parseColor("#F2F2F7"));
            hwArea.addView(histView);
        }

        scrollView.addView(hwArea);
        hwLayout.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        // 替换键盘视图
        if (keyboardView.getParent() != null) {
            ((android.view.ViewGroup) keyboardView.getParent()).removeView(keyboardView);
        }
        ((android.view.ViewGroup) candidateBar.getParent()).addView(hwLayout);
        candidateBar.setVisibility(View.GONE);
    }

    private void switchToEmoji() {
        currentMode = MODE_EMOJI;
        // 简化：直接输入一些常用emoji
        String[] emojis = {"😀", "😂", "🥰", "😎", "🤔", "😴", "😭", "😡",
                "👍", "👏", "🙏", "💪", "❤️", "🔥", "✨", "🎉",
                "☀️", "🌙", "⭐", "🌈", "🍎", "🍕", "☕", "🍺"};

        LinearLayout emojiLayout = new LinearLayout(this);
        emojiLayout.setOrientation(LinearLayout.VERTICAL);
        emojiLayout.setBackgroundColor(Color.WHITE);

        // 标题栏
        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setPadding(16, 12, 16, 12);
        titleBar.setBackgroundColor(Color.parseColor("#F2F2F7"));

        TextView title = new TextView(this);
        title.setText("表情");
        title.setTextSize(15);
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        titleBar.addView(title);

        Button backBtn = new Button(this);
        backBtn.setText("ABC");
        backBtn.setTextSize(13);
        backBtn.setOnClickListener(v -> {
            currentMode = MODE_QWERTY;
            keyboardView.setKeyboard(qwertyKeyboard);
            if (keyboardView.getParent() == null) {
                ((android.view.ViewGroup) candidateBar.getParent()).addView(keyboardView);
            }
            if (emojiLayout.getParent() != null) {
                ((android.view.ViewGroup) emojiLayout.getParent()).removeView(emojiLayout);
            }
        });
        titleBar.addView(backBtn);
        emojiLayout.addView(titleBar);

        // Emoji网格
        ScrollView scrollView = new ScrollView(this);
        LinearLayout emojiGrid = new LinearLayout(this);
        emojiGrid.setOrientation(LinearLayout.VERTICAL);
        emojiGrid.setPadding(16, 16, 16, 16);

        for (int row = 0; row < 6; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < 4; col++) {
                final String emoji = emojis[row * 4 + col];
                Button emojiBtn = new Button(this);
                emojiBtn.setText(emoji);
                emojiBtn.setTextSize(24);
                emojiBtn.setBackgroundColor(Color.TRANSPARENT);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                emojiBtn.setLayoutParams(params);
                emojiBtn.setOnClickListener(v -> {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        ic.commitText(emoji, 1);
                    }
                });
                rowLayout.addView(emojiBtn);
            }
            emojiGrid.addView(rowLayout);
        }
        scrollView.addView(emojiGrid);
        emojiLayout.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        // 替换键盘视图
        if (keyboardView.getParent() != null) {
            ((android.view.ViewGroup) keyboardView.getParent()).removeView(keyboardView);
        }
        ((android.view.ViewGroup) candidateBar.getParent()).addView(emojiLayout);
        candidateBar.setVisibility(View.GONE);
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
