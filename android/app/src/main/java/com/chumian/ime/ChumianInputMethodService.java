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
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.HorizontalScrollView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
    private TextView morseDisplay;
    private static final Map<String, String> MORSE_MAP = new HashMap<>();
    private static final String[] PINYIN_INITIALS = {"b","p","m","f","d","t","n","l","g","k","h","j","q","x","zh","ch","sh","r","z","c","s","y","w"};
    private static final String[] PINYIN_FINALS = {"a","o","e","i","u","v","ai","ei","ui","ao","ou","iu","ie","ve","er","an","en","in","un","vn","ang","eng","ing","ong"};
    private static final String[] RARE_CHARS = {"犇","骉","羴","鱻","麤","龘","靐","齉","籲","灪","爩","鱻","麤","龗","齾","齉","籲","灪","爩","灩","灪","爩","鱻","麤","龘","靐","齉","籲","灪","爩","鱻","麤","龗","齾","齉","籲","灪","爩","灩","灪","爩","鱻","麤","龘","靐","齉","籲","灪","爩"};
    private static final String[] TRADITIONAL_CHARS = {"的","是","不","了","在","人","有","我","他","这","个","中","来","上","大","为","和","国","地","到","以","说","时","要","就","出","会","可","也","你","对","生","能","而","子","那","得","于","着","下","自","之","年","过","发","后","作","里","用","道","行","所","然","家","种","事","成","方","多","经","么","去","法","学","如","都","同","现","当","没","动","面","起","看","定","天","分","还","进","好","小","部","其","些","主","样","理","心","她","本","前","开","但","因","只","从","想","实","日","军","者","意","无","力","它","与","长","把","机","十","民","第","公","此","已","工","使","情","明","性","知","全","三","又","关","点","正","业","外","将","两","高","间","由","问","很","最","重","并","物","手","应","战","向","头","文","体","政","美","相","见","被","利","什","二","等","产","或","新","己","制","身","果","加","西","斯","月","话","合","回","特","代","东","信","表","法","化","比","展","那","几","口","北","平","安","场","少","报","才","活","感","做","接","队","立","题","统","解","回","市","计","便","友","管","节","米","待","术","流","场","亲","园","史","历","县","区","县","区"};

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
        MORSE_MAP.put("-----","0"); MORSE_MAP.put(".----","1"); MORSE_MAP.put("..---","2");
        MORSE_MAP.put("...--","3"); MORSE_MAP.put("....-","4"); MORSE_MAP.put(".....","5");
        MORSE_MAP.put("-....","6"); MORSE_MAP.put("--...","7"); MORSE_MAP.put("---..","8");
        MORSE_MAP.put("----.","9");
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

        // 手写视图（默认隐藏）
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
        title.setText("手写输入 - 点击笔画输入");
        title.setTextSize(14);
        title.setTextColor(Color.parseColor("#666666"));
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        // 笔画按钮
        LinearLayout strokes = new LinearLayout(this);
        strokes.setOrientation(LinearLayout.HORIZONTAL);
        strokes.setGravity(Gravity.CENTER);
        String[] strokeNames = {"横","竖","撇","捺","点","折","钩","提"};
        for (String s : strokeNames) {
            Button btn = new Button(this);
            btn.setText(s);
            btn.setTextSize(16);
            btn.setBackgroundColor(Color.parseColor("#E3F2FD"));
            btn.setTextColor(Color.parseColor("#1565C0"));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            bp.setMargins(2, 4, 2, 4);
            btn.setLayoutParams(bp);
            btn.setOnClickListener(v -> {
                // 简化手写：根据笔画数匹配常用字
                commitText(getCharByStrokes(s));
            });
            strokes.addView(btn);
        }
        layout.addView(strokes);

        // 常用字快捷
        HorizontalScrollView commonScroll = new HorizontalScrollView(this);
        LinearLayout commonLayout = new LinearLayout(this);
        commonLayout.setOrientation(LinearLayout.HORIZONTAL);
        String[] commonChars = {"的","一","是","在","不","了","有","和","人","这","中","大","为","上","个","我","以","要","他","时","来","用","们","生","到","作","地","于","出","就","分","对","成","会","可","你","能","而","子","那","得","着","下","自","之","年","过","发","后","作","里","用","道","行","所","然","家","种","事","成","方","多","经","么","去","法","学","如","都","同","现","当","没","动","面","起","看","定","天","分","还","进","好","小","部","其","些","主","样","理","心","她","本","前","开","但","因","只","从","想","实"};
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

    private String getCharByStrokes(String stroke) {
        Map<String, String> map = new HashMap<>();
        map.put("横","一"); map.put("竖","丨"); map.put("撇","丿");
        map.put("捺","㇏"); map.put("点","丶"); map.put("折","𠃍");
        map.put("钩","亅"); map.put("提","㇀");
        return map.getOrDefault(stroke, stroke);
    }

    private void switchKeyboard(int mode) {
        currentMode = mode;
        if (handwritingView != null) handwritingView.setVisibility(View.GONE);
        if (keyboardView != null) keyboardView.setVisibility(View.VISIBLE);
        morseBuilder.setLength(0);

        switch (mode) {
            case 0: currentKeyboard = createPinyinKeyboard(); break;
            case 1: currentKeyboard = createEnglishKeyboard(); break;
            case 2:
                if (handwritingView != null) handwritingView.setVisibility(View.VISIBLE);
                if (keyboardView != null) keyboardView.setVisibility(View.GONE);
                return;
            case 3: currentKeyboard = createMorseKeyboard(); break;
            case 4: currentKeyboard = createEmojiKeyboard(); break;
            case 5: currentKeyboard = createRandomKeyboard(); break;
            case 6: currentKeyboard = createTraditionalKeyboard(); break;
            case 7: currentKeyboard = createRareCharKeyboard(); break;
        }
        if (keyboardView != null && currentKeyboard != null) {
            keyboardView.setKeyboard(currentKeyboard);
        }
    }

    private Keyboard createPinyinKeyboard() {
        String[] rows = {
            "qwertyuiop",
            "asdfghjkl",
            "zxcvbnm"
        };
        return createKeyboardFromRows(rows, true);
    }

    private Keyboard createEnglishKeyboard() {
        String[] rows = {
            "qwertyuiop",
            "asdfghjkl",
            "zxcvbnm"
        };
        return createKeyboardFromRows(rows, false);
    }

    private Keyboard createMorseKeyboard() {
        List<Keyboard.Key> keys = new ArrayList<>();
        int y = 0;
        // 点和划
        addKey(keys, ".", 0, y, 150, 60);
        addKey(keys, "-", 160, y, 150, 60);
        addKey(keys, " ", 320, y, 100, 60);
        addKey(keys, "DEL", 430, y, 80, 60);
        y += 70;
        // 字母显示
        addKey(keys, "ABC", 0, y, 200, 50);
        addKey(keys, "123", 210, y, 100, 50);
        addKey(keys, "切换", 320, y, 100, 50);
        y += 60;
        addKey(keys, "拼音", 0, y, 100, 50);
        addKey(keys, "英语", 110, y, 100, 50);
        addKey(keys, "手写", 220, y, 100, 50);
        addKey(keys, "Emoji", 330, y, 100, 50);
        addKey(keys, "繁体", 0, y+60, 100, 50);
        addKey(keys, "生僻", 110, y+60, 100, 50);
        addKey(keys, "随机", 220, y+60, 100, 50);

        Keyboard keyboard = new Keyboard(this, 0);
        keyboard.setKeys(keys);
        return keyboard;
    }

    private Keyboard createEmojiKeyboard() {
        List<Keyboard.Key> keys = new ArrayList<>();
        String[] emojis = {"😀","😃","😄","😁","😆","😅","🤣","😂","🙂","🙃","😉","😊","😇","🥰","😍","🤩","😘","😗","😚","😙","🥲","😋","😛","😜","🤪","😝","🤑","🤗","🤭","🤫","🤔","🤐","🤨","😐","😑","😶","😏","😒","🙄","😬","🤥","😌","😔","😪","🤤","😴","😷","🤒","🤕","🤢","🤮","🥵","🥶","🥴","😵","🤯","🤠","🥳","😎","🤓","🧐","😕","😟","🙁","☹️","😮","😯","😲","😳","🥺","😦","😧","😨","😰","😥","😢","😭","😱","😖","😣","😞","😓","😩","😫","🥱","😤","😡","😠","🤬","😈","👿","💀","☠️","💩","🤡","👹","👺","👻","👽","👾","🤖","🎃","😺","😸","😹","😻","😼","😽","🙀","😿","😾","❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❣️","💕","💞","💓","💗","💖","💘","💝","💟","👍","👎","👌","✌️","🤞","🤟","🤘","🤙","👈","👉","👆","👇","☝️","✋","🤚","🖐️","🖖","👋","🤝","👏","🙌","👐","🤲","🙏","✍️","💅","🤳","💪","🦾","🦿","🦵","🦶","👂","🦻","👃","🧠","🦷","🦴","👀","👁️","👅","👄","💋","🩸","💼","👜","👝","🛍️","🎒","👓","🕶️","🥽","🥼","🦺","👔","👕","👖","🧣","🧤","🧥","🧦","👗","👘","🥻","🩱","🩲","🩳","👙","👚","👛","👒","🎩","🎓","🧢","⛑️","📿","💄","💍","💎","🔇","🔈","🔉","🔊","🔔","🔕","📯","🎙️","🎚️","🎛️","🎤","🎧","📻","🎷","🎸","🎹","🎺","🎻","🥁","🪘","🎬","🎞️","📽️","🎥","📺","📷","📸","📹","📼","🔍","🔎","🕯️","💡","🔦","🏮","📔","📕","📖","📗","📘","📙","📚","📓","📒","📃","📄","📰","🗞️","📑","🔖","🧾","💰","💴","💵","💷","💶","💳","💎","⚖️","🧰","🔧","🔨","⚒️","🛠️","⛏️","🔩","⚙️","🪜","🧱","⛓️","🧲","🔫","💣","🪓","🔪","🗡️","⚔️","🛡️","🚬","⚰️","🪦","⚱️","🏺","🔮","📿","🧿","🪄","🔮","🎈","🎉","🎊","🎁","🎀","🪅","🎏","🎐","🧧","✉️","📩","📨","📧","💌","📥","📤","📦","🏷️","📪","📫","📬","📭","📮","📯","📜","📃","📄","📑","🧾","📊","📈","📉","🗒️","🗓️","📆","📅","📇","🗃️","🗳️","🗄️","📋","📁","📂","🗂️","📌","📍","📎","🖇️","📏","📐","✂️","🗃️","🗳️","🗄️","📋","📁","📂","🗂️","📌","📍","📎","🖇️","📏","📐","✂️"};
        int x = 0, y = 0;
        int cols = 10;
        for (int i = 0; i < Math.min(emojis.length, 200); i++) {
            addKey(keys, emojis[i], x * 36, y * 40, 34, 38);
            x++;
            if (x >= cols) { x = 0; y++; }
        }
        // 底部切换
        y++;
        addKey(keys, "拼音", 0, y*40, 80, 40);
        addKey(keys, "英语", 90, y*40, 80, 40);
        addKey(keys, "手写", 180, y*40, 80, 40);
        addKey(keys, "摩斯", 270, y*40, 80, 40);
        addKey(keys, "繁体", 360, y*40, 80, 40);
        addKey(keys, "生僻", 450, y*40, 80, 40);
        addKey(keys, "随机", 540, y*40, 80, 40);

        Keyboard keyboard = new Keyboard(this, 0);
        keyboard.setKeys(keys);
        return keyboard;
    }

    private Keyboard createRandomKeyboard() {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        List<Character> charList = new ArrayList<>();
        for (char c : chars.toCharArray()) charList.add(c);
        Collections.shuffle(charList, new Random(System.currentTimeMillis()));
        StringBuilder row1 = new StringBuilder();
        StringBuilder row2 = new StringBuilder();
        StringBuilder row3 = new StringBuilder();
        for (int i = 0; i < 10; i++) row1.append(charList.get(i));
        for (int i = 10; i < 19; i++) row2.append(charList.get(i));
        for (int i = 19; i < 26; i++) row3.append(charList.get(i));
        return createKeyboardFromRows(new String[]{row1.toString(), row2.toString(), row3.toString()}, false);
    }

    private Keyboard createTraditionalKeyboard() {
        List<Keyboard.Key> keys = new ArrayList<>();
        int x = 0, y = 0;
        int cols = 10;
        for (int i = 0; i < Math.min(TRADITIONAL_CHARS.length, 100); i++) {
            addKey(keys, TRADITIONAL_CHARS[i], x * 36, y * 44, 34, 42);
            x++;
            if (x >= cols) { x = 0; y++; }
        }
        y++;
        addKey(keys, "拼音", 0, y*44, 80, 40);
        addKey(keys, "英语", 90, y*44, 80, 40);
        addKey(keys, "手写", 180, y*44, 80, 40);
        addKey(keys, "摩斯", 270, y*44, 80, 40);
        addKey(keys, "Emoji", 360, y*44, 80, 40);
        addKey(keys, "生僻", 450, y*44, 80, 40);
        addKey(keys, "随机", 540, y*44, 80, 40);
        Keyboard keyboard = new Keyboard(this, 0);
        keyboard.setKeys(keys);
        return keyboard;
    }

    private Keyboard createRareCharKeyboard() {
        List<Keyboard.Key> keys = new ArrayList<>();
        int x = 0, y = 0;
        int cols = 8;
        for (int i = 0; i < Math.min(RARE_CHARS.length, 48); i++) {
            addKey(keys, RARE_CHARS[i], x * 45, y * 50, 43, 48);
            x++;
            if (x >= cols) { x = 0; y++; }
        }
        y++;
        addKey(keys, "拼音", 0, y*50, 80, 40);
        addKey(keys, "英语", 90, y*50, 80, 40);
        addKey(keys, "手写", 180, y*50, 80, 40);
        addKey(keys, "摩斯", 270, y*50, 80, 40);
        addKey(keys, "Emoji", 360, y*50, 80, 40);
        addKey(keys, "繁体", 450, y*50, 80, 40);
        addKey(keys, "随机", 540, y*50, 80, 40);
        Keyboard keyboard = new Keyboard(this, 0);
        keyboard.setKeys(keys);
        return keyboard;
    }

    private Keyboard createKeyboardFromRows(String[] rows, boolean isPinyin) {
        List<Keyboard.Key> keys = new ArrayList<>();
        int y = 0;
        for (String row : rows) {
            int x = 0;
            int keyWidth = 32;
            int gap = 2;
            for (char c : row.toCharArray()) {
                String label = isShift ? String.valueOf(c).toUpperCase() : String.valueOf(c);
                addKey(keys, label, x, y, keyWidth, 40);
                x += keyWidth + gap;
            }
            y += 44;
        }
        // 功能行
        addKey(keys, isShift ? "↑" : "shift", 0, y, 50, 40);
        addKey(keys, "符号", 60, y, 50, 40);
        addKey(keys, "空格", 120, y, 150, 40);
        addKey(keys, ".", 280, y, 35, 40);
        addKey(keys, "DEL", 325, y, 50, 40);
        addKey(keys, "回车", 385, y, 60, 40);
        y += 44;
        // 切换行
        addKey(keys, "拼音", 0, y, 60, 36);
        addKey(keys, "英语", 70, y, 60, 36);
        addKey(keys, "手写", 140, y, 60, 36);
        addKey(keys, "摩斯", 210, y, 60, 36);
        addKey(keys, "Emoji", 280, y, 60, 36);
        addKey(keys, "繁体", 350, y, 60, 36);
        addKey(keys, "生僻", 420, y, 60, 36);
        addKey(keys, "随机", 490, y, 60, 36);

        Keyboard keyboard = new Keyboard(this, 0);
        keyboard.setKeys(keys);
        return keyboard;
    }

    private void addKey(List<Keyboard.Key> keys, String label, int x, int y, int width, int height) {
        Keyboard.Key key = new Keyboard.Key();
        key.label = label;
        key.codes = new int[]{label.charAt(0)};
        key.x = x;
        key.y = y;
        key.width = width;
        key.height = height;
        key.gap = 2;
        keys.add(key);
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        String label = currentKeyboard != null ? currentKeyboard.getKeys().get(findKeyIndex(primaryCode)).label : "";

        // 切换键盘
        if (label.equals("拼音")) { switchKeyboard(0); return; }
        if (label.equals("英语")) { switchKeyboard(1); return; }
        if (label.equals("手写")) { switchKeyboard(2); return; }
        if (label.equals("摩斯")) { switchKeyboard(3); return; }
        if (label.equals("Emoji")) { switchKeyboard(4); return; }
        if (label.equals("随机")) { switchKeyboard(5); return; }
        if (label.equals("繁体")) { switchKeyboard(6); return; }
        if (label.equals("生僻")) { switchKeyboard(7); return; }

        // 摩斯电码模式
        if (currentMode == 3) {
            if (label.equals(".")) { morseBuilder.append("."); updateMorseDisplay(); return; }
            if (label.equals("-")) { morseBuilder.append("-"); updateMorseDisplay(); return; }
            if (label.equals(" ")) {
                String letter = MORSE_MAP.get(morseBuilder.toString());
                if (letter != null) commitText(letter.toLowerCase());
                morseBuilder.setLength(0);
                updateMorseDisplay();
                return;
            }
            if (label.equals("DEL")) {
                if (morseBuilder.length() > 0) morseBuilder.deleteCharAt(morseBuilder.length()-1);
                updateMorseDisplay();
                return;
            }
        }

        // Emoji/繁体/生僻字直接输入
        if (currentMode == 4 || currentMode == 6 || currentMode == 7) {
            if (label.length() > 0 && label.charAt(0) > 127) {
                commitText(label);
                return;
            }
        }

        // 普通按键
        if (label.equals("shift") || label.equals("↑")) {
            isShift = !isShift;
            if (currentMode == 0) switchKeyboard(0);
            else if (currentMode == 1) switchKeyboard(1);
            else if (currentMode == 5) switchKeyboard(5);
            return;
        }
        if (label.equals("DEL")) {
            getCurrentInputConnection().deleteSurroundingText(1, 0);
            return;
        }
        if (label.equals("回车")) {
            getCurrentInputConnection().performEditorAction(EditorInfo.IME_ACTION_DONE);
            return;
        }
        if (label.equals("空格")) {
            commitText(" ");
            if (currentMode == 0) { pinyinBuilder.setLength(0); updateCandidates(); }
            return;
        }
        if (label.equals("符号")) {
            // 切换符号键盘
            commitText("");
            return;
        }

        // 字母输入
        if (label.length() == 1 && Character.isLetter(label.charAt(0))) {
            if (currentMode == 0) {
                // 拼音模式
                pinyinBuilder.append(label.toLowerCase());
                updateCandidates();
            } else {
                commitText(label);
            }
        } else {
            commitText(label);
        }
    }

    private int findKeyIndex(int code) {
        if (currentKeyboard == null) return 0;
        List<Keyboard.Key> keys = currentKeyboard.getKeys();
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).codes != null && keys.get(i).codes.length > 0 && keys.get(i).codes[0] == code) {
                return i;
            }
        }
        return 0;
    }

    private void updateMorseDisplay() {
        // 更新摩斯电码显示（通过候选词栏）
        candidateBar.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText("摩斯: " + morseBuilder.toString() + " = " + MORSE_MAP.getOrDefault(morseBuilder.toString(), "?"));
        tv.setTextSize(16);
        tv.setTextColor(Color.parseColor("#333333"));
        tv.setPadding(16, 8, 16, 8);
        candidateBar.addView(tv);
    }

    private void updateCandidates() {
        candidateBar.removeAllViews();
        String pinyin = pinyinBuilder.toString();
        if (pinyin.isEmpty()) return;

        // 简化拼音匹配：显示拼音和常用字
        candidates.clear();
        candidates.add(pinyin);
        // 添加一些常用字候选
        String[] commonCandidates = getCandidatesForPinyin(pinyin);
        for (String c : commonCandidates) candidates.add(c);

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
                if (c.equals(pinyin)) {
                    commitText(pinyin);
                } else {
                    commitText(c);
                }
                pinyinBuilder.setLength(0);
                updateCandidates();
            });
            candidateBar.addView(btn);
        }
    }

    private String[] getCandidatesForPinyin(String pinyin) {
        Map<String, String[]> map = new HashMap<>();
        map.put("a", new String[]{"啊","阿","呵","嗄","腌"});
        map.put("ai", new String[]{"爱","哎","唉","矮","碍","艾","癌","蔼","霭","隘"});
        map.put("an", new String[]{"安","按","暗","岸","案","俺","氨","胺","鞍","黯"});
        map.put("ang", new String[]{"昂","盎","肮","仰"});
        map.put("ao", new String[]{"奥","熬","傲","凹","澳","懊","翱","獒","螯","鳌"});
        map.put("ba", new String[]{"吧","把","八","爸","巴","拔","霸","坝","罢","跋"});
        map.put("bai", new String[]{"白","百","拜","败","摆","柏","佰","稗","捭","掰"});
        map.put("ban", new String[]{"班","半","办","版","般","搬","板","伴","扮","拌"});
        map.put("bang", new String[]{"帮","棒","榜","绑","磅","蚌","傍","谤","梆","膀"});
        map.put("bao", new String[]{"包","报","保","宝","抱","爆","薄","堡","饱","暴"});
        map.put("bei", new String[]{"北","被","背","杯","悲","贝","备","卑","碑","惫"});
        map.put("ben", new String[]{"本","笨","奔","苯","畚","夯"});
        map.put("beng", new String[]{"蹦","崩","绷","甭","迸","蚌","泵"});
        map.put("bi", new String[]{"比","必","笔","币","毕","闭","壁","臂","避","逼"});
        map.put("bian", new String[]{"变","边","便","遍","编","辨","辩","辫","扁","贬"});
        map.put("biao", new String[]{"表","标","彪","膘","镖","飙","裱","婊","鳔","骠"});
        map.put("bie", new String[]{"别","憋","瘪","鳖","蹩","彆"});
        map.put("bin", new String[]{"宾","滨","彬","斌","濒","殡","鬓","槟","豳","傧"});
        map.put("bing", new String[]{"并","病","冰","兵","饼","丙","柄","炳","禀","摒"});
        map.put("bo", new String[]{"波","博","播","驳","泊","勃","铂","魄","柏","薄"});
        map.put("bu", new String[]{"不","部","步","布","补","捕","卜","哺","埠","簿"});
        map.put("ca", new String[]{"擦","嚓","礤","拆"});
        map.put("cai", new String[]{"才","菜","财","猜","材","采","彩","裁","蔡","睬"});
        map.put("can", new String[]{"参","餐","残","惨","蚕","灿","璨","粲","孱","璨"});
        map.put("cang", new String[]{"藏","仓","苍","舱","沧","伧","臧","脏"});
        map.put("cao", new String[]{"草","操","曹","槽","糙","嘈","漕","螬","艚","艹"});
        map.put("ce", new String[]{"测","侧","册","策","厕","恻","栅","岑","涔"});
        map.put("ceng", new String[]{"层","曾","蹭","噌","竲"});
        map.put("cha", new String[]{"查","茶","差","插","察","叉","茬","诧","刹","碴"});
        map.put("chai", new String[]{"拆","柴","差","钗","豺","侪","虿"});
        map.put("chan", new String[]{"产","缠","馋","掺","蝉","颤","谗","潺","蟾","孱"});
        map.put("chang", new String[]{"长","常","场","唱","厂","尝","昌","畅","倡","肠"});
        map.put("chao", new String[]{"超","朝","潮","炒","吵","抄","钞","巢","嘲","绰"});
        map.put("che", new String[]{"车","扯","彻","撤","掣","坼","砗"});
        map.put("chen", new String[]{"陈","沉","晨","称","趁","衬","辰","尘","臣","忱"});
        map.put("cheng", new String[]{"成","城","程","称","诚","承","乘","盛","惩","澄"});
        map.put("chi", new String[]{"吃","持","尺","迟","池","翅","赤","斥","齿","耻"});
        map.put("chong", new String[]{"冲","虫","充","重","崇","宠","忡","憧","铳","艟"});
        map.put("chou", new String[]{"抽","丑","臭","仇","愁","筹","畴","稠","酬","踌"});
        map.put("chu", new String[]{"出","处","初","除","楚","触","础","储","厨","锄"});
        map.put("chuan", new String[]{"穿","川","传","船","串","喘","椽","舛","遄","钏"});
        map.put("chuang", new String[]{"窗","床","创","闯","疮","幢","怆","床"});
        map.put("chui", new String[]{"吹","垂","锤","炊","捶","槌","棰","陲","锤"});
        map.put("chun", new String[]{"春","纯","唇","蠢","醇","淳","鹑","蝽","椿","蠢"});
        map.put("chuo", new String[]{"戳","绰","辍","龊","啜","惙","踔","戳"});
        map.put("ci", new String[]{"此","次","词","刺","磁","瓷","慈","雌","辞","祠"});
        map.put("cong", new String[]{"从","聪","葱","丛","淙","琮","骢","苁","枞","淙"});
        map.put("cou", new String[]{"凑","辏","腠","凑"});
        map.put("cu", new String[]{"粗","促","醋","簇","卒","猝","蔟","蹙","蹴","殂"});
        map.put("cuan", new String[]{"窜","篡","蹿","攒","爨","汆","撺","镩","窜"});
        map.put("cui", new String[]{"催","脆","翠","崔","摧","粹","悴","萃","啐","瘁"});
        map.put("cun", new String[]{"村","存","寸","忖","邨","皴","寸"});
        map.put("cuo", new String[]{"错","措","挫","撮","搓","措","锉","痤","嵯","脞"});
        map.put("da", new String[]{"大","打","达","答","搭","瘩","耷","嗒","鞑","怛"});
        map.put("dai", new String[]{"带","代","待","大","袋","戴","逮","贷","歹","傣"});
        map.put("dan", new String[]{"但","单","蛋","担","淡","弹","旦","胆","丹","诞"});
        map.put("dang", new String[]{"当","党","挡","档","荡","铛","裆","凼","砀","挡"});
        map.put("dao", new String[]{"到","道","刀","倒","导","岛","盗","捣","蹈","悼"});
        map.put("de", new String[]{"的","得","德","地","底","低","滴","笛","迪","涤"});
        map.put("deng", new String[]{"等","灯","登","邓","凳","瞪","蹬","噔","嶝","磴"});
        map.put("di", new String[]{"的","地","低","底","敌","第","弟","递","帝","滴"});
        map.put("dian", new String[]{"点","电","店","典","颠","垫","淀","殿","奠","惦"});
        map.put("diao", new String[]{"掉","调","吊","钓","叼","雕","吊","貂","碉","凋"});
        map.put("die", new String[]{"跌","爹","碟","蝶","叠","谍","迭","垤","喋","瓞"});
        map.put("ding", new String[]{"定","顶","丁","订","钉","盯","叮","锭","鼎","酊"});
        map.put("diu", new String[]{"丢","铥","丢"});
        map.put("dong", new String[]{"东","动","懂","冬","洞","冻","栋","咚","峒","氡"});
        map.put("dou", new String[]{"都","斗","豆","抖","陡","逗","读","毒","独","度"});
        map.put("du", new String[]{"度","读","毒","独","堵","杜","肚","赌","镀","妒"});
        map.put("duan", new String[]{"段","短","断","端","锻","缎","椴","煅","簖","断"});
        map.put("dui", new String[]{"对","队","堆","兑","怼","憝","镦","队"});
        map.put("dun", new String[]{"顿","吨","蹲","盾","敦","墩","炖","盹","礅","蹾"});
        map.put("duo", new String[]{"多","朵","躲","夺","堕","舵","剁","跺","驮","铎"});
        map.put("e", new String[]{"饿","恶","额","俄","鹅","蛾","峨","讹","鄂","遏"});
        map.put("en", new String[]{"恩","嗯","摁","蒽","峎"});
        map.put("er", new String[]{"二","而","儿","尔","耳","饵","洱","贰","珥","铒"});
        map.put("fa", new String[]{"发","法","罚","伐","乏","阀","筏","垡","砝","珐"});
        map.put("fan", new String[]{"反","饭","犯","烦","翻","番","泛","范","繁","凡"});
        map.put("fang", new String[]{"方","放","房","防","访","仿","坊","芳","纺","舫"});
        map.put("fei", new String[]{"非","飞","费","肥","废","肺","沸","菲","匪","诽"});
        map.put("fen", new String[]{"分","份","芬","纷","坟","焚","粪","愤","奋","忿"});
        map.put("feng", new String[]{"风","封","丰","疯","峰","锋","蜂","逢","讽","凤"});
        map.put("fo", new String[]{"佛","仏","坲","梻"});
        map.put("fou", new String[]{"否","缶","不","雬","鴀"});
        map.put("fu", new String[]{"父","服","福","府","副","富","付","妇","负","附"});
        map.put("gai", new String[]{"该","改","盖","概","钙","溉","戤","陔","垓","赅"});
        map.put("gan", new String[]{"干","敢","感","赶","甘","杆","肝","赶","秆","赣"});
        map.put("gang", new String[]{"刚","钢","港","岗","纲","杠","肛","罡","戆","筻"});
        map.put("gao", new String[]{"高","搞","告","稿","糕","搞","皋","镐","诰","郜"});
        map.put("ge", new String[]{"个","歌","各","哥","格","阁","革","隔","葛","蛤"});
        map.put("gei", new String[]{"给","给"});
        map.put("gen", new String[]{"跟","根","亘","艮","茛","哏","根"});
        map.put("geng", new String[]{"更","耕","庚","羹","埂","耿","梗","哽","鲠","赓"});
        map.put("gong", new String[]{"工","公","共","功","供","攻","宫","弓","恭","巩"});
        map.put("gou", new String[]{"够","狗","沟","购","构","钩","勾","苟","垢","篝"});
        map.put("gu", new String[]{"古","故","顾","骨","谷","股","鼓","固","孤","姑"});
        map.put("gua", new String[]{"挂","瓜","刮","寡","卦","胍","剐","诖","褂","呱"});
        map.put("guai", new String[]{"怪","乖","拐","夬","怪"});
        map.put("guan", new String[]{"关","管","观","官","馆","惯","灌","冠","贯","棺"});
        map.put("guang", new String[]{"光","广","逛","胱","犷","桄","恍"});
        map.put("gui", new String[]{"归","贵","鬼","规","柜","跪","轨","桂","龟","闺"});
        map.put("gun", new String[]{"滚","棍","辊","衮","磙","鲧","棍"});
        map.put("guo", new String[]{"国","过","果","锅","裹","郭","聒","虢","椁","馘"});
        map.put("ha", new String[]{"哈","蛤","铪","嗨"});
        map.put("hai", new String[]{"还","海","害","孩","嗨","骸","骇","氦","胲","醢"});
        map.put("han", new String[]{"含","汉","寒","喊","汗","旱","韩","函","罕","翰"});
        map.put("hang", new String[]{"行","航","杭","巷","沆","绗","桁","航"});
        map.put("hao", new String[]{"好","号","浩","耗","豪","毫","郝","嚎","壕","濠"});
        map.put("he", new String[]{"和","河","他","喝","合","贺","赫","鹤","荷","核"});
        map.put("hei", new String[]{"黑","嘿","嗨","黑"});
        map.put("hen", new String[]{"很","恨","狠","痕","佷","哏","很"});
        map.put("heng", new String[]{"横","恒","衡","亨","哼","恒","珩","桁","横"});
        map.put("hong", new String[]{"红","洪","宏","轰","虹","鸿","弘","烘","泓","蕻"});
        map.put("hou", new String[]{"后","厚","候","侯","猴","吼","喉","逅","堠","糇"});
        map.put("hu", new String[]{"和","户","湖","胡","虎","护","互","乎","忽","壶"});
        map.put("hua", new String[]{"话","花","华","画","化","划","哗","骅","桦","铧"});
        map.put("huai", new String[]{"坏","怀","淮","槐","徊","踝","坏"});
        map.put("huan", new String[]{"还","换","欢","环","缓","幻","唤","焕","涣","痪"});
        map.put("huang", new String[]{"黄","皇","荒","慌","晃","谎","惶","煌","蝗","徨"});
        map.put("hui", new String[]{"会","回","灰","辉","毁","挥","徽","恢","汇","慧"});
        map.put("hun", new String[]{"婚","混","昏","魂","浑","荤","馄","珲","婚"});
        map.put("huo", new String[]{"或","活","火","货","获","祸","惑","霍","豁","伙"});
        map.put("ji", new String[]{"几","机","集","记","己","及","急","既","即","级"});
        map.put("jia", new String[]{"家","加","假","价","架","驾","夹","佳","嘉","颊"});
        map.put("jian", new String[]{"见","间","建","件","简","坚","尖","监","减","健"});
        map.put("jiang", new String[]{"将","讲","江","降","姜","僵","疆","酱","匠","缰"});
        map.put("jiao", new String[]{"叫","教","交","脚","角","觉","较","胶","焦","骄"});
        map.put("jie", new String[]{"结","接","节","街","解","界","借","介","戒","届"});
        map.put("jin", new String[]{"进","今","金","近","尽","紧","劲","禁","晋","浸"});
        map.put("jing", new String[]{"经","京","精","静","境","景","警","竞","净","晶"});
        map.put("jiu", new String[]{"就","九","久","酒","旧","救","纠","究","揪","舅"});
        map.put("ju", new String[]{"句","举","局","具","剧","聚","拒","据","距","锯"});
        map.put("juan", new String[]{"卷","娟","倦","眷","捐","鹃","镌","蠲","卷"});
        map.put("jue", new String[]{"觉","决","绝","角","掘","嚼","爵","倔","崛","厥"});
        map.put("jun", new String[]{"军","均","君","俊","菌","骏","峻","浚","竣","郡"});
        map.put("ka", new String[]{"卡","咖","喀","咔","佧","胩"});
        map.put("kai", new String[]{"开","凯","慨","楷","铠","蒈","忾","开"});
        map.put("kan", new String[]{"看","砍","刊","勘","堪","瞰","戡","侃","龛","坎"});
        map.put("kang", new String[]{"抗","扛","康","糠","炕","亢","伉","钪","康"});
        map.put("kao", new String[]{"考","靠","烤","拷","栲","犒","铐","考"});
        map.put("ke", new String[]{"可","课","克","客","刻","科","棵","颗","壳","渴"});
        map.put("ken", new String[]{"肯","啃","垦","恳","裉","肯"});
        map.put("keng", new String[]{"坑","铿","吭","硁","鍞"});
        map.put("kong", new String[]{"空","孔","控","恐","箜","崆","倥","空"});
        map.put("kou", new String[]{"口","扣","寇","叩","抠","芤","眍","口"});
        map.put("ku", new String[]{"苦","哭","库","酷","裤","枯","窟","骷","刳","喾"});
        map.put("kua", new String[]{"夸","跨","垮","挎","胯","侉","夸"});
        map.put("kuai", new String[]{"快","块","筷","会","蒯","脍","狯","快"});
        map.put("kuan", new String[]{"宽","款","髋","宽"});
        map.put("kuang", new String[]{"况","矿","狂","框","筐","旷","诓","眶","哐","邝"});
        map.put("kui", new String[]{"亏","愧","溃","葵","魁","窥","馈","匮","喟","睽"});
        map.put("kun", new String[]{"困","昆","捆","坤","琨","锟","醌","鲲","困"});
        map.put("kuo", new String[]{"扩","阔","括","廓","蛞","栝","阔"});
        map.put("la", new String[]{"拉","啦","辣","腊","蜡","垃","喇","邋","旯","砬"});
        map.put("lai", new String[]{"来","赖","莱","睐","徕","涞","濑","癞","籁","来"});
        map.put("lan", new String[]{"兰","蓝","烂","懒","栏","拦","篮","澜","斓","揽"});
        map.put("lang", new String[]{"浪","狼","郎","朗","廊","琅","榔","螂","阆","朗"});
        map.put("lao", new String[]{"老","劳","牢","捞","姥","酪","烙","涝","唠","佬"});
        map.put("le", new String[]{"了","乐","勒","雷","类","累","泪","垒","擂","肋"});
        map.put("lei", new String[]{"类","累","雷","泪","垒","擂","肋","镭","羸","蕾"});
        map.put("leng", new String[]{"冷","愣","棱","楞","冷"});
        map.put("li", new String[]{"里","力","立","理","李","例","礼","厉","丽","利"});
        map.put("lia", new String[]{"俩","倆"});
        map.put("lian", new String[]{"连","脸","练","联","恋","莲","链","廉","怜","镰"});
        map.put("liang", new String[]{"两","亮","量","良","凉","梁","辆","谅","晾","粱"});
        map.put("liao", new String[]{"了","料","聊","辽","疗","僚","寥","潦","燎","镣"});
        map.put("lie", new String[]{"列","烈","猎","裂","劣","冽","洌","趔","烈"});
        map.put("lin", new String[]{"林","临","邻","淋","琳","凛","赁","吝","鳞","嶙"});
        map.put("ling", new String[]{"领","零","灵","令","另","凌","铃","龄","陵","羚"});
        map.put("liu", new String[]{"流","留","六","刘","柳","楼","溜","琉","榴","馏"});
        map.put("long", new String[]{"龙","隆","笼","聋","拢","垄","珑","胧","窿","陇"});
        map.put("lou", new String[]{"楼","漏","陋","搂","篓","镂","蝼","髅","楼"});
        map.put("lu", new String[]{"路","录","陆","炉","鲁","卢","露","鹿","芦","颅"});
        map.put("lv", new String[]{"绿","律","旅","虑","率","铝","履","屡","缕","滤"});
        map.put("luan", new String[]{"乱","卵","滦","峦","挛","孪","栾","銮","鸾","略"});
        map.put("lun", new String[]{"论","轮","伦","沦","纶","抡","仑","论"});
        map.put("luo", new String[]{"落","罗","洛","络","骆","螺","逻","萝","锣","箩"});
        map.put("ma", new String[]{"吗","妈","马","麻","骂","嘛","玛","码","蚂","摩"});
        map.put("mai", new String[]{"买","卖","麦","埋","迈","脉","霾","荬","劢","买"});
        map.put("man", new String[]{"满","慢","漫","蛮","瞒","馒","螨","曼","谩","幔"});
        map.put("mang", new String[]{"忙","芒","盲","茫","莽","氓","邙","漭","忙"});
        map.put("mao", new String[]{"毛","猫","冒","帽","茂","貌","贸","茅","锚","髦"});
        map.put("me", new String[]{"么","嘛","嚒","麽"});
        map.put("mei", new String[]{"没","美","每","妹","眉","煤","梅","媒","玫","霉"});
        map.put("men", new String[]{"们","门","闷","扪","焖","懑","钔","们"});
        map.put("meng", new String[]{"梦","猛","蒙","盟","孟","檬","朦","礞","蜢","勐"});
        map.put("mi", new String[]{"米","密","迷","蜜","眯","谜","弥","觅","泌","谧"});
        map.put("mian", new String[]{"面","免","绵","棉","眠","缅","腼","冕","娩","渑"});
        map.put("miao", new String[]{"秒","苗","庙","妙","描","瞄","渺","缈","藐","淼"});
        map.put("mie", new String[]{"灭","蔑","咩","篾","蠛","灭"});
        map.put("min", new String[]{"民","敏","闽","悯","皿","抿","泯","珉","缗","鳘"});
        map.put("ming", new String[]{"明","名","命","鸣","冥","铭","螟","瞑","茗","酩"});
        map.put("miu", new String[]{"谬","缪","谬"});
        map.put("mo", new String[]{"摸","末","墨","默","莫","魔","膜","磨","模","抹"});
        map.put("mou", new String[]{"某","谋","牟","眸","鍪","蛑","谋"});
        map.put("mu", new String[]{"母","木","目","牧","幕","墓","慕","穆","模","拇"});
        map.put("na", new String[]{"那","拿","哪","呐","纳","娜","钠","衲","捺","镎"});
        map.put("nai", new String[]{"奶","耐","乃","奈","鼐","艿","萘","奶"});
        map.put("nan", new String[]{"南","男","难","楠","喃","腩","蝻","赧","囡","南"});
        map.put("nang", new String[]{"囊","馕","囔","攮","曩","囊"});
        map.put("nao", new String[]{"脑","闹","恼","挠","饶","瑙","垴","硇","铙","闹"});
        map.put("ne", new String[]{"呢","讷","哪吒","呢"});
        map.put("nei", new String[]{"内","馁","那","哪","内"});
        map.put("nen", new String[]{"嫩","恁","嫩"});
        map.put("neng", new String[]{"能","熥","能"});
        map.put("ni", new String[]{"你","呢","泥","逆","尼","拟","倪","匿","腻","溺"});
        map.put("nian", new String[]{"年","念","粘","撵","捻","辇","黏","鲶","廿","念"});
        map.put("niang", new String[]{"娘","酿","嬢","酿"});
        map.put("niao", new String[]{"鸟","尿","茑","脲","袅","鸟"});
        map.put("nie", new String[]{"捏","涅","聂","孽","镍","涅","嗫","蹑","蘖","臬"});
        map.put("nin", new String[]{"您","恁","您"});
        map.put("ning", new String[]{"宁","凝","拧","柠","咛","狞","聍","甯","佞","宁"});
        map.put("niu", new String[]{"牛","扭","钮","纽","忸","狃","杻","靵","牛"});
        map.put("nong", new String[]{"农","浓","弄","脓","侬","哝","挊","农"});
        map.put("nu", new String[]{"女","努","怒","奴","弩","驽","孥","胬","帑","女"});
        map.put("nuan", new String[]{"暖","煖","餪","暖"});
        map.put("nun", new String[]{"嫩","恁","嫩"});
        map.put("nuo", new String[]{"诺","挪","懦","糯","喏","搦","锘","傩","诺"});
        map.put("o", new String[]{"哦","噢","喔","嚄","哦"});
        map.put("ou", new String[]{"欧","偶","呕","藕","殴","鸥","瓯","怄","沤","讴"});
        map.put("pa", new String[]{"怕","爬","帕","趴","啪","扒","杷","筢","琶","怕"});
        map.put("pai", new String[]{"排","派","拍","牌","徘","湃","俳","蒎","排"});
        map.put("pan", new String[]{"盘","判","盼","攀","潘","畔","磐","蹒","蟠","泮"});
        map.put("pang", new String[]{"旁","胖","庞","彷","磅","螃","耪","滂","膀","旁"});
        map.put("pao", new String[]{"跑","炮","泡","抛","袍","刨","咆","庖","狍","匏"});
        map.put("pei", new String[]{"配","陪","培","赔","佩","沛","霈","辔","旆","帔"});
        map.put("pen", new String[]{"盆","喷","湓","葐","盆"});
        map.put("peng", new String[]{"朋","碰","彭","捧","蓬","棚","膨","澎","篷","硼"});
        map.put("pi", new String[]{"皮","批","屁","脾","疲","匹","劈","坯","砒","霹"});
        map.put("pian", new String[]{"片","篇","偏","骗","便","篇","翩","骈","胼","蹁"});
        map.put("piao", new String[]{"票","飘","漂","瓢","嫖","瞟","缥","飘","殍","莩"});
        map.put("pie", new String[]{"撇","瞥","苤","丿","氕","撇"});
        map.put("pin", new String[]{"品","拼","贫","频","聘","乒","嫔","颦","拚","姘"});
        map.put("ping", new String[]{"平","评","瓶","苹","凭","屏","乒","坪","萍","枰"});
        map.put("po", new String[]{"破","坡","泼","婆","迫","魄","粕","珀","笸","叵"});
        map.put("pou", new String[]{"剖","掊","裒","剖"});
        map.put("pu", new String[]{"普","仆","扑","铺","朴","浦","谱","曝","瀑","匍"});
        map.put("qi", new String[]{"其","起","期","七","气","奇","齐","骑","棋","旗"});
        map.put("qia", new String[]{"恰","洽","掐","卡","髂","袷","洽"});
        map.put("qian", new String[]{"前","钱","千","浅","签","牵","铅","谦","乾","潜"});
        map.put("qiang", new String[]{"强","墙","抢","枪","腔","羌","锵","蔷","樯","襁"});
        map.put("qiao", new String[]{"桥","巧","敲","瞧","翘","壳","锹","侨","荞","樵"});
        map.put("qie", new String[]{"切","且","窃","茄","怯","惬","趄","伽","挈","锲"});
        map.put("qin", new String[]{"亲","琴","勤","侵","秦","寝","沁","禽","擒","噙"});
        map.put("qing", new String[]{"请","清","青","轻","情","晴","庆","倾","卿","擎"});
        map.put("qiong", new String[]{"穷","琼","穹","茕","蛩","筇","跫","銎","穷"});
        map.put("qiu", new String[]{"球","求","秋","丘","囚","酋","泅","俅","逑","遒"});
        map.put("qu", new String[]{"去","取","区","曲","趣","驱","须","虽","屈","躯"});
        map.put("quan", new String[]{"全","权","圈","泉","拳","犬","劝","醛","铨","痊"});
        map.put("que", new String[]{"却","确","缺","雀","鹊","瘸","榷","阕","阙","悫"});
        map.put("qun", new String[]{"群","裙","逡","麇","群"});
        map.put("ran", new String[]{"然","燃","染","冉","苒","髯","蚺","然"});
        map.put("rang", new String[]{"让","嚷","壤","攘","禳","穰","让"});
        map.put("rao", new String[]{"绕","扰","饶","娆","桡","荛","饶"});
        map.put("re", new String[]{"热","惹","喏","热"});
        map.put("ren", new String[]{"人","认","任","忍","仁","刃","韧","妊","纫","荏"});
        map.put("reng", new String[]{"仍","扔","礽","仍"});
        map.put("ri", new String[]{"日","驲","囸","日"});
        map.put("rong", new String[]{"容","荣","融","熔","溶","戎","茸","冗","嵘","榕"});
        map.put("rou", new String[]{"肉","柔","揉","蹂","糅","鞣","肉"});
        map.put("ru", new String[]{"如","入","乳","儒","茹","辱","褥","蠕","孺","濡"});
        map.put("ruan", new String[]{"软","阮","朊","軟","软"});
        map.put("rui", new String[]{"瑞","锐","蕊","睿","蚋","枘","锐"});
        map.put("run", new String[]{"润","闰","潤","閏","润"});
        map.put("ruo", new String[]{"若","弱","偌","箬","爇","鰙","若"});
        map.put("sa", new String[]{"撒","洒","萨","卅","飒","脎","隡","撒"});
        map.put("sai", new String[]{"赛","塞","腮","鳃","噻","僿","赛"});
        map.put("san", new String[]{"三","散","伞","叁","毵","糁","馓","三"});
        map.put("sang", new String[]{"桑","嗓","丧","搡","颡","磉","桑"});
        map.put("sao", new String[]{"扫","骚","嫂","臊","瘙","鳋","扫"});
        map.put("se", new String[]{"色","涩","瑟","塞","啬","穑","铯","瑟"});
        map.put("sen", new String[]{"森","椮","槮","森"});
        map.put("seng", new String[]{"僧","鬙","僧"});
        map.put("sha", new String[]{"杀","沙","傻","啥","纱","刹","砂","煞","霎","鲨"});
        map.put("shai", new String[]{"晒","筛","酾","晒"});
        map.put("shan", new String[]{"山","善","闪","衫","扇","杉","珊","删","煽","赡"});
        map.put("shang", new String[]{"上","商","伤","尚","赏","裳","晌","垧","殇","熵"});
        map.put("shao", new String[]{"少","烧","稍","勺","哨","邵","绍","捎","鞘","芍"});
        map.put("she", new String[]{"社","设","射","蛇","舌","舍","涉","摄","奢","赦"});
        map.put("shei", new String[]{"谁","谁"});
        map.put("shen", new String[]{"什","深","身","神","甚","肾","慎","渗","婶","申"});
        map.put("sheng", new String[]{"生","声","省","圣","胜","盛","剩","牲","升","绳"});
        map.put("shi", new String[]{"是","时","事","市","十","石","实","识","史","使"});
        map.put("shou", new String[]{"手","收","首","受","瘦","售","寿","授","兽","狩"});
        map.put("shu", new String[]{"书","数","树","熟","输","叔","舒","束","术","述"});
        map.put("shua", new String[]{"刷","耍","唰","誜","刷"});
        map.put("shuai", new String[]{"帅","摔","衰","甩","蟀","帅"});
        map.put("shuan", new String[]{"栓","拴","闩","涮","栓"});
        map.put("shuang", new String[]{"双","爽","霜","孀","骦","鹴","双"});
        map.put("shui", new String[]{"水","谁","睡","税","说"," Shui","水"});
        map.put("shun", new String[]{"顺","瞬","舜","吮","蕣","顺"});
        map.put("shuo", new String[]{"说","硕","朔","烁","铄","妁","槊","蒴","说"});
        map.put("si", new String[]{"四","死","思","私","司","似","寺","斯","丝","撕"});
        map.put("song", new String[]{"送","松","宋","颂","诵","耸","竦","淞","菘","崧"});
        map.put("sou", new String[]{"搜","艘","嗖","叟","擞","嗖","馊","飕","螋","瘦"});
        map.put("su", new String[]{"苏","速","素","诉","俗","肃","酸","虽","随","隋"});
        map.put("suan", new String[]{"算","酸","蒜","狻","筭","算"});
        map.put("sui", new String[]{"虽","随","岁","碎","隋","遂","隧","髓","祟","绥"});
        map.put("sun", new String[]{"孙","损","笋","荪","狲","榫","隼"," Sun","孙"});
        map.put("suo", new String[]{"所","锁","索","缩","梭","唆","蓑","嗦","挲","羧"});
        map.put("ta", new String[]{"他","她","它","塔","踏","塌","塔","獭","挞","闼"});
        map.put("tai", new String[]{"太","台","态","泰","抬","胎","苔","汰","钛","肽"});
        map.put("tan", new String[]{"谈","弹","探","叹","碳","探","潭","谭","坦","毯"});
        map.put("tang", new String[]{"堂","糖","唐","汤","躺","趟","倘","塘","搪","膛"});
        map.put("tao", new String[]{"套","逃","桃","淘","涛","掏","滔","韬","饕","洮"});
        map.put("te", new String[]{"特","忑","慝","铽","特"});
        map.put("teng", new String[]{"疼","腾","藤","滕","誊"," Teng","疼"});
        map.put("ti", new String[]{"提","体","题","替","踢","蹄","啼","屉","惕","涕"});
        map.put("tian", new String[]{"天","田","甜","填","添","腆","掭","忝","殄","畋"});
        map.put("tiao", new String[]{"条","跳","调","挑","眺","窕","龆","笤","蜩","髫"});
        map.put("tie", new String[]{"铁","贴","帖","餮","萜"," Tie","铁"});
        map.put("ting", new String[]{"听","停","亭","廷","挺","庭","艇","汀","廷","婷"});
        map.put("tong", new String[]{"同","通","痛","统","童","铜","彤","桐","筒","瞳"});
        map.put("tou", new String[]{"头","投","透","偷","骰","妵","头"});
        map.put("tu", new String[]{"图","土","涂","途","兔","吐","秃","突","徒","屠"});
        map.put("tuan", new String[]{"团","湍","疃","彖"," Tuán","团"});
        map.put("tui", new String[]{"推","退","腿","颓","蜕","褪","煺","蹆","推"});
        map.put("tun", new String[]{"吞","屯","臀","囤","豚","暾","饨"," Tun","吞"});
        map.put("tuo", new String[]{"脱","拖","托","驮","妥","拓","唾","鸵","陀","魄"});
        map.put("wa", new String[]{"挖","哇","蛙","瓦","袜","凹","佤","窊"," Wa","挖"});
        map.put("wai", new String[]{"外","歪","崴"," Wai","外"});
        map.put("wan", new String[]{"完","万","晚","玩","碗","弯","湾","丸","婉","腕"});
        map.put("wang", new String[]{"王","往","网","望","忘","旺","妄","汪","枉","惘"});
        map.put("wei", new String[]{"为","位","围","微","味","胃","卫","畏","喂","魏"});
        map.put("wen", new String[]{"文","问","闻","温","稳","纹","吻","瘟","紊","雯"});
        map.put("weng", new String[]{"翁","嗡","瓮","蓊","蕹"," Weng","翁"});
        map.put("wo", new String[]{"我","握","窝","卧","沃","蜗","斡","肟","仵","我"});
        map.put("wu", new String[]{"五","无","物","武","务","舞","屋","雾","误","污"});
        map.put("xi", new String[]{"西","习","喜","洗","系","戏","细","吸","稀","息"});
        map.put("xia", new String[]{"下","夏","吓","虾","瞎","峡","侠","狭","暇","霞"});
        map.put("xian", new String[]{"先","现","线","县","鲜","仙","贤","咸","险","显"});
        map.put("xiang", new String[]{"想","向","像","香","响","乡","相","箱","祥","翔"});
        map.put("xiao", new String[]{"小","笑","校","效","萧","消","销","宵","晓","肖"});
        map.put("xie", new String[]{"些","写","谢","鞋","斜","血","歇","协","挟","谐"});
        map.put("xin", new String[]{"新","心","信","欣","辛","馨","鑫","芯","薪","衅"});
        map.put("xing", new String[]{"行","星","兴","型","姓","醒","刑","杏","幸","性"});
        map.put("xiong", new String[]{"雄","熊","凶","兄","胸","匈","汹","芎","雄"});
        map.put("xiu", new String[]{"修","休","秀","袖","绣","锈","嗅","羞","宿","朽"});
        map.put("xu", new String[]{"需","许","续","须","虚","序","畜","恤","絮","婿"});
        map.put("xuan", new String[]{"选","宣","悬","旋","玄","选","眩","绚","喧","轩"});
        map.put("xue", new String[]{"学","雪","血","穴","靴","薛","噱","鳕","学"});
        map.put("xun", new String[]{"寻","训","讯","勋","循","巡","询","旬","驯","汛"});
        map.put("ya", new String[]{"呀","压","牙","鸦","雅","哑","亚","讶","蚜","崖"});
        map.put("yan", new String[]{"眼","言","严","烟","沿","盐","演","艳","燕","验"});
        map.put("yang", new String[]{"样","阳","养","央","羊","洋","氧","仰","痒","漾"});
        map.put("yao", new String[]{"要","药","摇","咬","腰","邀","耀","窑","谣","遥"});
        map.put("ye", new String[]{"也","夜","叶","业","野","爷","液","耶","咽","掖"});
        map.put("yi", new String[]{"一","以","已","意","义","议","医","衣","依","易"});
        map.put("yin", new String[]{"因","音","银","引","印","阴","饮","隐","吟","淫"});
        map.put("ying", new String[]{"应","英","影","营","迎","赢","盈","颖","硬","映"});
        map.put("yo", new String[]{"哟","唷"," Yo","哟"});
        map.put("yong", new String[]{"用","永","勇","拥","涌","庸","咏","泳","蛹","恿"});
        map.put("you", new String[]{"有","又","右","友","优","游","油","由","邮","犹"});
        map.put("yu", new String[]{"与","于","语","雨","鱼","遇","欲","玉","育","域"});
        map.put("yuan", new String[]{"元","原","远","园","员","圆","愿","援","缘","源"});
        map.put("yue", new String[]{"月","越","约","乐","跃","阅","岳","粤","钥","悦"});
        map.put("yun", new String[]{"云","运","允","韵","孕","蕴","耘","匀","陨","晕"});
        map.put("za", new String[]{"杂","砸","咋","匝","咂","拶"," Za","杂"});
        map.put("zai", new String[]{"在","再","载","灾","栽","宰","哉","崽","甾","载"});
        map.put("zan", new String[]{"咱","暂","赞","攒","簪","瓒","咱"});
        map.put("zang", new String[]{"脏","葬","藏","臧","奘","驵","脏"});
        map.put("zao", new String[]{"早","造","找","遭","糟","凿","枣","澡","灶","躁"});
        map.put("ze", new String[]{"则","责","择","泽","仄","啧","箦","舴"," Ze","则"});
        map.put("zei", new String[]{"贼","鲗","贼"});
        map.put("zen", new String[]{"怎","谮"," Zen","怎"});
        map.put("zeng", new String[]{"增","曾","赠","憎","缯","甑","罾","增"});
        map.put("zha", new String[]{"扎","炸","渣","眨","栅","榨","咋","札","轧","闸"});
        map.put("zhai", new String[]{"摘","窄","宅","债","寨","斋","翟","瘵"," Zhai","摘"});
        map.put("zhan", new String[]{"站","战","占","展","战","粘","沾","盏","斩","辗"});
        map.put("zhang", new String[]{"张","长","章","掌","涨","丈","帐","杖","彰","瘴"});
        map.put("zhao", new String[]{"找","照","招","赵","召","兆","罩","爪","肇","诏"});
        map.put("zhe", new String[]{"这","着","者","哲","折","涉","浙","蔗","赭","鹧"});
        map.put("zhei", new String[]{"这","这"});
        map.put("zhen", new String[]{"真","针","镇","阵","振","震","枕","珍","斟","甄"});
        map.put("zheng", new String[]{"正","整","政","证","征","争","症","郑","怔","挣"});
        map.put("zhi", new String[]{"只","知","之","直","至","治","制","志","质","支"});
        map.put("zhong", new String[]{"中","种","重","众","钟","终","忠","肿","衷","冢"});
        map.put("zhou", new String[]{"周","州","洲","舟","粥","轴","肘","帚","咒","皱"});
        map.put("zhu", new String[]{"主","住","注","猪","竹","助","柱","祝","珠","筑"});
        map.put("zhua", new String[]{"抓","爪","挝","抓"});
        map.put("zhuai", new String[]{"拽","转","曳","拽"});
        map.put("zhuan", new String[]{"转","专","赚","砖","撰","篆","啭","馔","专"});
        map.put("zhuang", new String[]{"装","状","壮","撞","庄","妆","桩","僮","奘","装"});
        map.put("zhui", new String[]{"追","坠","缀","锥","赘","骓","椎","追"});
        map.put("zhun", new String[]{"准","谆","肫","窀","准"});
        map.put("zhuo", new String[]{"着","桌","捉","拙","灼","卓","浊","酌","琢","擢"});
        map.put("zi", new String[]{"子","自","字","紫","资","姿","滋","籽","孜","梓"});
        map.put("zong", new String[]{"总","宗","综","踪","纵","棕","鬃","粽","腙","总"});
        map.put("zou", new String[]{"走","奏","揍","邹","驺","诹","陬","鄹","走"});
        map.put("zu", new String[]{"足","组","族","阻","祖","租","卒","诅","俎","镞"});
        map.put("zuan", new String[]{"钻","躜","纂","缵","钻"});
        map.put("zui", new String[]{"最","嘴","醉","罪","最","蕞","嘴"});
        map.put("zun", new String[]{"尊","遵","樽","鳟","撙","尊"});
        map.put("zuo", new String[]{"做","作","坐","左","座","昨","佐","做作","唑","阼"});

        String[] result = map.get(pinyin);
        if (result != null) return result;
        // 模糊匹配
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            if (entry.getKey().startsWith(pinyin) || pinyin.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
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
