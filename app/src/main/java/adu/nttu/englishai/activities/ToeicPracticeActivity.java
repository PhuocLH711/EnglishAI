package adu.nttu.englishai.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.ToeicQuestion;
import adu.nttu.englishai.repositories.ToeicRepository;
import adu.nttu.englishai.repositories.ToeicProgressRepository;

public class ToeicPracticeActivity extends AppCompatActivity {

    private TextView btnBack, tvTitle, tvPart, tvProgress, tvTimer, tvScene, tvPassage, tvQuestion, tvFeedback;
    private ImageView imgToeicQuestion;
    private RadioGroup groupAnswers;
    private RadioButton radioA, radioB, radioC, radioD;
    private MaterialButton btnPrevious, btnBookmark, btnPalette, btnAudio, btnCheckOrSubmit, btnNext;

    private final List<ToeicQuestion> questions = new ArrayList<>();
    private final Map<String, String> answers = new HashMap<>();
    private final Map<String, Boolean> bookmarks = new HashMap<>();

    private ToeicRepository repository;
    private ToeicProgressRepository progressRepository;
    private String testId, testTitle, mode;
    private int durationMinutes, partFilter, currentIndex = 0;
    private CountDownTimer countDownTimer;
    private long remainingMillis;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toeic_practice);

        readIntent();
        bindViews();
        setupTts();
        repository = new ToeicRepository();
        progressRepository = new ToeicProgressRepository();
        setupButtons();
        setupSystemBack();
        loadQuestions();
    }

    private void readIntent() {
        testId = getIntent().getStringExtra(ToeicHomeActivity.EXTRA_TEST_ID);
        testTitle = getIntent().getStringExtra(ToeicHomeActivity.EXTRA_TEST_TITLE);
        durationMinutes = getIntent().getIntExtra(ToeicHomeActivity.EXTRA_DURATION, 120);
        mode = getIntent().getStringExtra(ToeicHomeActivity.EXTRA_MODE);
        partFilter = getIntent().getIntExtra(ToeicHomeActivity.EXTRA_PART_FILTER, 0);
        if (mode == null) mode = ToeicHomeActivity.MODE_PRACTICE;
        if (durationMinutes <= 0) durationMinutes = 120;
        remainingMillis = durationMinutes * 60L * 1000L;
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnToeicPracticeBack);
        tvTitle = findViewById(R.id.tvToeicPracticeTitle);
        tvPart = findViewById(R.id.tvToeicPart);
        tvProgress = findViewById(R.id.tvToeicProgress);
        tvTimer = findViewById(R.id.tvToeicTimer);
        tvScene = findViewById(R.id.tvToeicScene);
        imgToeicQuestion = findViewById(R.id.imgToeicQuestion);
        tvPassage = findViewById(R.id.tvToeicPassage);
        tvQuestion = findViewById(R.id.tvToeicQuestion);
        tvFeedback = findViewById(R.id.tvToeicFeedback);
        groupAnswers = findViewById(R.id.groupToeicAnswers);
        radioA = findViewById(R.id.radioToeicA);
        radioB = findViewById(R.id.radioToeicB);
        radioC = findViewById(R.id.radioToeicC);
        radioD = findViewById(R.id.radioToeicD);
        btnPrevious = findViewById(R.id.btnToeicPrevious);
        btnBookmark = findViewById(R.id.btnToeicBookmark);
        btnPalette = findViewById(R.id.btnToeicPalette);
        btnAudio = findViewById(R.id.btnToeicAudio);
        btnCheckOrSubmit = findViewById(R.id.btnToeicCheckOrSubmit);
        btnNext = findViewById(R.id.btnToeicNext);

        tvTitle.setText(safe(testTitle, "TOEIC Practice"));
        tvTimer.setVisibility(isMockMode() ? View.VISIBLE : View.GONE);
        btnCheckOrSubmit.setText(isMockMode() ? "NỘP BÀI" : "KIỂM TRA");
    }

    private void setupTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
                tts.setSpeechRate(0.88f);
            }
        });
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> handleExit());
        btnPrevious.setOnClickListener(v -> { saveCurrentAnswer(); if (currentIndex > 0) { currentIndex--; showQuestion(); } });
        btnNext.setOnClickListener(v -> {
            saveCurrentAnswer();
            if (currentIndex < questions.size() - 1) { currentIndex++; showQuestion(); }
            else if (isMockMode()) confirmSubmit();
            else showPracticeSummary();
        });
        btnBookmark.setOnClickListener(v -> toggleBookmark());
        btnPalette.setOnClickListener(v -> showQuestionPalette());
        btnAudio.setOnClickListener(v -> playCurrentAudio());
        btnCheckOrSubmit.setOnClickListener(v -> {
            saveCurrentAnswer();
            if (isMockMode()) confirmSubmit(); else checkCurrentQuestion();
        });
    }

    private void setupSystemBack() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { handleExit(); }
        });
    }

    private void loadQuestions() {
        tvQuestion.setText("Đang tải câu hỏi...");
        ToeicRepository.QuestionsCallback cb = new ToeicRepository.QuestionsCallback() {
            @Override public void onSuccess(List<ToeicQuestion> loaded) {
                questions.clear();
                if (loaded != null) questions.addAll(loaded);
                if (questions.isEmpty()) {
                    tvQuestion.setText("Bộ đề chưa có câu hỏi cho phần này.");
                    btnCheckOrSubmit.setEnabled(false); btnNext.setEnabled(false); return;
                }
                currentIndex = 0; showQuestion(); if (isMockMode()) startTimer();
            }
            @Override public void onFailure(Exception exception) {
                tvQuestion.setText("Không tải được câu hỏi.");
                Toast.makeText(ToeicPracticeActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
            }
        };
        if (partFilter >= 1 && partFilter <= 7) repository.getQuestionsByTestAndPart(testId, partFilter, cb);
        else repository.getFullTestQuestions(testId, cb);
    }

    private void showQuestion() {
        if (questions.isEmpty()) return;
        stopSpeaking();
        ToeicQuestion q = questions.get(currentIndex);
        tvFeedback.setVisibility(View.GONE);
        tvPart.setText("PART " + q.getPart());
        tvProgress.setText((currentIndex + 1) + "/" + questions.size());

        String passage = safe(q.getPassageText(), "");
        tvPassage.setVisibility(passage.isEmpty() ? View.GONE : View.VISIBLE);
        if (!passage.isEmpty()) tvPassage.setText(passage);

        showPart1Image(q);

        String scene = getExtraString(q, "sceneDescription");
        boolean showSceneText =
                q.getPart() == 1
                        && !scene.isEmpty()
                        && imgToeicQuestion.getVisibility() != View.VISIBLE;

        tvScene.setVisibility(
                showSceneText
                        ? View.VISIBLE
                        : View.GONE
        );

        if (showSceneText) {
            tvScene.setText(
                    "🖼 Demo scene\n" + scene
            );
        }

        boolean listening = q.getPart() >= 1 && q.getPart() <= 4;
        btnAudio.setVisibility(listening ? View.VISIBLE : View.GONE);

        tvQuestion.setText(q.getQuestionNumber() + ". " + safe(q.getQuestionText(), "Listen and choose the best answer."));
        radioA.setText("A. " + safe(q.getOptionA(), "—"));
        radioB.setText("B. " + safe(q.getOptionB(), "—"));
        radioC.setText("C. " + safe(q.getOptionC(), "—"));
        radioD.setText("D. " + safe(q.getOptionD(), "—"));

        groupAnswers.clearCheck();
        String saved = answers.get(q.getId());
        if ("A".equals(saved)) radioA.setChecked(true);
        else if ("B".equals(saved)) radioB.setChecked(true);
        else if ("C".equals(saved)) radioC.setChecked(true);
        else if ("D".equals(saved)) radioD.setChecked(true);

        boolean marked = Boolean.TRUE.equals(bookmarks.get(q.getId()));
        btnBookmark.setText(marked ? "★ Đã đánh dấu" : "☆ Đánh dấu");
        btnPrevious.setEnabled(currentIndex > 0);
        btnNext.setText(currentIndex == questions.size() - 1 ? (isMockMode() ? "NỘP BÀI" : "XONG") : "TIẾP");
        setAnswersEnabled(true);
    }

    private void showPart1Image(
            ToeicQuestion question
    ) {

        if (imgToeicQuestion == null) {
            return;
        }

        imgToeicQuestion.setVisibility(
                View.GONE
        );

        imgToeicQuestion.setImageDrawable(
                null
        );

        if (question == null
                || question.getPart() != 1) {
            return;
        }

        /*
         * Bộ demo hiện tại:
         *
         * toeic_p1_demo01_q01.webp
         * ...
         * toeic_p1_demo01_q06.webp
         *
         * Dùng questionNumber để chọn đúng ảnh.
         * Khi sau này dùng ảnh URL thật, có thể mở rộng
         * phần này để load question.getImageUrl().
         */
        int localPart1Number =
                question.getQuestionNumber();

        /*
         * Nếu Full Test dùng số câu toàn đề 1-200 thì Part 1
         * vẫn là câu 1-6, nên giá trị này đang dùng trực tiếp được.
         */
        if (localPart1Number < 1
                || localPart1Number > 6) {
            return;
        }

        String demoCode = "demo01";

        if (testId != null) {

            if (testId.endsWith("_02")) {
                demoCode = "demo02";
            } else if (testId.endsWith("_03")) {
                demoCode = "demo03";
            }
        }

        String resourceName =
                String.format(
                        Locale.ROOT,
                        "toeic_p1_%s_q%02d",
                        demoCode,
                        localPart1Number
                );

        int resourceId =
                getResources().getIdentifier(
                        resourceName,
                        "drawable",
                        getPackageName()
                );

        if (resourceId == 0) {
            return;
        }

        imgToeicQuestion.setImageResource(
                resourceId
        );

        imgToeicQuestion.setContentDescription(
                "TOEIC Part 1 - câu "
                        + localPart1Number
        );

        imgToeicQuestion.setVisibility(
                View.VISIBLE
        );
    }

    private void playCurrentAudio() {
        if (questions.isEmpty()) return;
        ToeicQuestion q = questions.get(currentIndex);
        String transcript = getExtraString(q, "transcript");
        if (transcript.isEmpty()) {
            Toast.makeText(this, "Câu này chưa có audio/transcript demo.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ttsReady) {
            Toast.makeText(this, "Text-to-Speech chưa sẵn sàng.", Toast.LENGTH_SHORT).show();
            return;
        }
        stopSpeaking();
        tts.speak(transcript, TextToSpeech.QUEUE_FLUSH, null, "toeic_" + q.getId());
    }

    private String getExtraString(ToeicQuestion q, String key) {
        if (q == null || q.getExtraData() == null) return "";
        Object v = q.getExtraData().get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }

    private void saveCurrentAnswer() {
        if (questions.isEmpty()) return;
        String a = selectedAnswer();
        if (a.isEmpty()) return;
        ToeicQuestion q = questions.get(currentIndex);

        answers.put(
                q.getId(),
                a
        );

        q.setSelectedAnswer(
                a
        );

        if (progressRepository != null) {

            progressRepository.markAnswered(
                    testId,
                    q.getPart(),
                    q.getId(),
                    null
            );
        }
    }

    private String selectedAnswer() {
        int id = groupAnswers.getCheckedRadioButtonId();
        if (id == R.id.radioToeicA) return "A";
        if (id == R.id.radioToeicB) return "B";
        if (id == R.id.radioToeicC) return "C";
        if (id == R.id.radioToeicD) return "D";
        return "";
    }

    private void checkCurrentQuestion() {
        String selected = selectedAnswer();
        if (selected.isEmpty()) { Toast.makeText(this, "Hãy chọn một đáp án.", Toast.LENGTH_SHORT).show(); return; }
        ToeicQuestion q = questions.get(currentIndex);
        String correct = safe(q.getCorrectAnswer(), "").toUpperCase(Locale.ROOT);
        boolean ok = selected.equals(correct);
        String text = ok ? "✅ Chính xác." : "❌ Chưa đúng. Đáp án đúng: " + correct;
        String expl = safe(q.getExplanation(), "");
        if (!expl.isEmpty()) text += "\n\n" + expl;
        tvFeedback.setText(text); tvFeedback.setVisibility(View.VISIBLE); setAnswersEnabled(false);
    }

    private void setAnswersEnabled(boolean enabled) {
        radioA.setEnabled(enabled); radioB.setEnabled(enabled); radioC.setEnabled(enabled); radioD.setEnabled(enabled);
    }

    private void toggleBookmark() {
        if (questions.isEmpty()) return;
        ToeicQuestion q = questions.get(currentIndex);
        boolean v = !Boolean.TRUE.equals(bookmarks.get(q.getId()));
        bookmarks.put(q.getId(), v); q.setBookmarked(v);
        btnBookmark.setText(v ? "★ Đã đánh dấu" : "☆ Đánh dấu");
    }

    private void showQuestionPalette() {
        if (questions.isEmpty()) return;
        String[] labels = new String[questions.size()];
        for (int i = 0; i < questions.size(); i++) {
            ToeicQuestion q = questions.get(i);
            boolean answered = answers.containsKey(q.getId());
            boolean marked = Boolean.TRUE.equals(bookmarks.get(q.getId()));
            labels[i] = (answered ? "● " : "○ ") + q.getQuestionNumber() + (marked ? " ★" : "") + "  (Part " + q.getPart() + ")";
        }
        new AlertDialog.Builder(this)
                .setTitle("Danh sách câu")
                .setItems(labels, (dialog, which) -> { saveCurrentAnswer(); currentIndex = which; showQuestion(); })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void startTimer() {
        stopTimer();
        countDownTimer = new CountDownTimer(remainingMillis, 1000L) {
            @Override public void onTick(long ms) {
                remainingMillis = ms; long sec = ms/1000L;
                tvTimer.setText(String.format(Locale.ROOT,"⏱ %02d:%02d",sec/60,sec%60));
            }
            @Override public void onFinish() { remainingMillis = 0; tvTimer.setText("⏱ 00:00"); submitTest(); }
        };
        countDownTimer.start();
    }

    private void confirmSubmit() {
        saveCurrentAnswer();
        int unanswered = questions.size() - answers.size();
        String msg = unanswered > 0 ? "Bạn còn " + unanswered + " câu chưa trả lời. Vẫn nộp bài?" : "Bạn muốn nộp bài ngay?";
        new AlertDialog.Builder(this).setTitle("Nộp bài TOEIC").setMessage(msg)
                .setPositiveButton("Nộp bài", (d,w)-> submitTest())
                .setNegativeButton("Tiếp tục làm", null).show();
    }

    private void showPracticeSummary() { submitTest(); }

    private void submitTest() {
        stopTimer(); stopSpeaking(); saveCurrentAnswer();
        int correct = 0; Map<Integer,int[]> byPart = new HashMap<>();
        for (ToeicQuestion q : questions) {
            String selected = answers.get(q.getId()); if (selected == null) selected = "";
            String ca = safe(q.getCorrectAnswer(),"").toUpperCase(Locale.ROOT);
            int[] st = byPart.get(q.getPart()); if (st == null) { st = new int[]{0,0}; byPart.put(q.getPart(),st); }
            st[1]++; if (selected.equals(ca)) { correct++; st[0]++; }
        }
        int total = questions.size(); int percent = total==0?0:Math.round(correct*100f/total);
        StringBuilder m = new StringBuilder("Đúng: ").append(correct).append('/').append(total)
                .append("\nĐộ chính xác: ").append(percent).append('%');
        for (int part=1; part<=7; part++) { int[] st=byPart.get(part); if(st!=null) m.append("\nPart ").append(part).append(": ").append(st[0]).append('/').append(st[1]); }
        if (isMockMode()) m.append("\n\nĐây là số câu đúng của bộ demo. Chưa quy đổi sang thang điểm TOEIC 10–990.");
        new AlertDialog.Builder(this).setTitle(isMockMode()?"Kết quả thi thử":"Kết quả luyện tập")
                .setMessage(m.toString()).setCancelable(false)
                .setPositiveButton("Làm lại",(d,w)->restartTest())
                .setNegativeButton("Thoát",(d,w)->finish()).show();
    }

    private void restartTest() {
        answers.clear(); bookmarks.clear();
        for (ToeicQuestion q:questions){ q.setSelectedAnswer(""); q.setBookmarked(false); }
        currentIndex=0; remainingMillis=durationMinutes*60L*1000L; showQuestion(); if(isMockMode()) startTimer();
    }

    private boolean isMockMode(){ return ToeicHomeActivity.MODE_MOCK.equals(mode); }

    private void handleExit() {
        if (answers.isEmpty()) { stopTimer(); stopSpeaking(); finish(); return; }
        new AlertDialog.Builder(this).setTitle("Thoát bài TOEIC?")
                .setMessage("Tiến trình hiện tại chưa được lưu. Bạn có chắc muốn thoát?")
                .setPositiveButton("Thoát",(d,w)->{stopTimer();stopSpeaking();finish();})
                .setNegativeButton("Ở lại",null).show();
    }

    private String safe(String value,String fallback){ return value==null||value.trim().isEmpty()?fallback:value.trim(); }
    private void stopTimer(){ if(countDownTimer!=null){countDownTimer.cancel();countDownTimer=null;} }
    private void stopSpeaking(){ if(tts!=null) tts.stop(); }

    @Override protected void onDestroy(){ stopTimer(); if(tts!=null){tts.stop();tts.shutdown();tts=null;} super.onDestroy(); }
}