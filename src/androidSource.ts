export interface AndroidFile {
  name: string;
  path: string;
  language: string;
  content: string;
  description: string;
}

export const ANDROID_PROJECT_FILES: AndroidFile[] = [
  {
    name: 'MainActivity.java',
    path: 'app/src/main/java/com/studia/reminder/MainActivity.java',
    language: 'java',
    description: 'Main activity managing Sessions, Chapters, Lectures, YouTube intent and SharedPreferences/Room database.',
    content: `package com.studia.reminder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements LectureAdapter.OnLectureClickListener {

    private RecyclerView recyclerViewSessions;
    private RecyclerView recyclerViewLectures;
    private ProgressBar progressBarChapter;
    private TextView tvProgressText;
    private TextView tvChapterTitle;
    private LectureAdapter lectureAdapter;
    private List<LectureModel> currentLectures;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI Views
        recyclerViewSessions = findViewById(R.id.recyclerViewSessions);
        recyclerViewLectures = findViewById(R.id.recyclerViewLectures);
        progressBarChapter = findViewById(R.id.progressBarChapter);
        tvProgressText = findViewById(R.id.tvProgressText);
        tvChapterTitle = findViewById(R.id.tvChapterTitle);

        currentLectures = new ArrayList<>();
        initSampleData();

        // Setup Lecture Grid
        recyclerViewLectures.setLayoutManager(new GridLayoutManager(this, 2));
        lectureAdapter = new LectureAdapter(this, currentLectures, this);
        recyclerViewLectures.setAdapter(lectureAdapter);

        updateProgress();

        // Bulk Link Importer Button
        findViewById(R.id.btnBulkImport).setOnClickListener(v -> showBulkImportDialog());

        // Total Lectures Adjuster
        findViewById(R.id.btnSetTotalLectures).setOnClickListener(v -> showSetTotalLecturesDialog());

        // Add Session FAB
        FloatingActionButton fabAddSession = findViewById(R.id.fabAddSession);
        fabAddSession.setOnClickListener(v -> showAddSessionDialog());
    }

    private void initSampleData() {
        for (int i = 1; i <= 20; i++) {
            currentLectures.add(new LectureModel(
                i, 
                "লেকচার " + i, 
                i <= 5 ? "https://www.youtube.com/watch?v=dQw4w9WgXcQ" : "",
                i <= 3, 
                ""
            ));
        }
    }

    public void updateProgress() {
        if (currentLectures.isEmpty()) return;
        int completedCount = 0;
        for (LectureModel model : currentLectures) {
            if (model.isCompleted()) completedCount++;
        }
        int percent = (int) (((float) completedCount / currentLectures.size()) * 100);
        progressBarChapter.setProgress(percent);
        tvProgressText.setText(completedCount + " / " + currentLectures.size() + " সম্পন্ন (" + percent + "%)");
    }

    @Override
    public void onLectureClick(LectureModel lecture) {
        if (lecture.getVideoUrl() != null && !lecture.getVideoUrl().isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(lecture.getVideoUrl()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "ব্রাউজারে লিঙ্ক খোলা সম্ভব হয়নি", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "এই লেকচারের কোনো ভিডিও লিঙ্ক যুক্ত করা নেই!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCheckboxToggle(LectureModel lecture, boolean isChecked) {
        lecture.setCompleted(isChecked);
        updateProgress();
        Toast.makeText(this, isChecked ? "লেকচার " + lecture.getNumber() + " সম্পন্ন হয়েছে!" : "স্ট্যাটাস আপডেট করা হয়েছে", Toast.LENGTH_SHORT).show();
    }

    private void showBulkImportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_bulk_import, null);
        EditText etBulkLinks = view.findViewById(R.id.etBulkLinks);

        builder.setView(view)
               .setTitle("স্মার্ট লিঙ্ক ম্যাপিং (Bulk Importer)")
               .setPositiveButton("ম্যাপ করুন", (dialog, which) -> {
                   String rawText = etBulkLinks.getText().toString();
                   List<String> links = extractUrls(rawText);
                   for (int i = 0; i < links.size() && i < currentLectures.size(); i++) {
                       currentLectures.get(i).setVideoUrl(links.get(i));
                   }
                   lectureAdapter.notifyDataSetChanged();
                   Toast.makeText(MainActivity.this, links.size() + "টি লিঙ্ক সফলভাবে যুক্ত হয়েছে!", Toast.LENGTH_LONG).show();
               })
               .setNegativeButton("বাতিল", null)
               .show();
    }

    private List<String> extractUrls(String text) {
        List<String> links = new ArrayList<>();
        Pattern pattern = Pattern.compile("https?://[\\\\w\\\\d:#@%/;$()~_?\\\\+-=\\\\\\\\&]+");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            links.add(matcher.group());
        }
        return links;
    }

    private void showSetTotalLecturesDialog() {
        EditText input = new EditText(this);
        input.setHint("মোট লেকচার সংখ্যা (যেমন: ২০)");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
            .setTitle("লেকচার সংখ্যা পরিবর্তন করুন")
            .setView(input)
            .setPositiveButton("আপডেট", (dialog, which) -> {
                try {
                    int count = Integer.parseInt(input.getText().toString().trim());
                    if (count > 0 && count <= 200) {
                        while (currentLectures.size() < count) {
                            int nextNum = currentLectures.size() + 1;
                            currentLectures.add(new LectureModel(nextNum, "লেকচার " + nextNum, "", false, ""));
                        }
                        while (currentLectures.size() > count) {
                            currentLectures.remove(currentLectures.size() - 1);
                        }
                        lectureAdapter.notifyDataSetChanged();
                        updateProgress();
                    }
                } catch (Exception ignored) {}
            })
            .setNegativeButton("বাতিল", null)
            .show();
    }

    private void showAddSessionDialog() {
        Toast.makeText(this, "সেশন তৈরির ফর্ম ওপেন হয়েছে", Toast.LENGTH_SHORT).show();
    }
}
`,
  },
  {
    name: 'LectureAdapter.java',
    path: 'app/src/main/java/com/studia/reminder/LectureAdapter.java',
    language: 'java',
    description: 'RecyclerView Adapter for rendering Lecture cards with checkmark, play button and status indicator.',
    content: `package com.studia.reminder;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LectureAdapter extends RecyclerView.Adapter<LectureAdapter.ViewHolder> {

    public interface OnLectureClickListener {
        void onLectureClick(LectureModel lecture);
        void onCheckboxToggle(LectureModel lecture, boolean isChecked);
    }

    private Context context;
    private List<LectureModel> lectureList;
    private OnLectureClickListener listener;

    public LectureAdapter(Context context, List<LectureModel> lectureList, OnLectureClickListener listener) {
        this.context = context;
        this.lectureList = lectureList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_lecture, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LectureModel lecture = lectureList.get(position);
        holder.tvLectureNumber.setText(lecture.getTitle());
        holder.cbCompleted.setChecked(lecture.isCompleted());

        if (lecture.isCompleted()) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#064E3B")); // Dark Emerald Green
            holder.tvStatus.setText("✓ সম্পন্ন");
            holder.tvStatus.setTextColor(Color.parseColor("#34D399"));
        } else {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#1E293B")); // Slate 800
            holder.tvStatus.setText(lecture.getVideoUrl().isEmpty() ? "লিঙ্ক নেই" : "ভিডিও প্রস্তুত");
            holder.tvStatus.setTextColor(Color.parseColor("#94A3B8"));
        }

        holder.cardView.setOnClickListener(v -> listener.onLectureClick(lecture));
        holder.cbCompleted.setOnClickListener(v -> listener.onCheckboxToggle(lecture, holder.cbCompleted.isChecked()));
    }

    @Override
    public int getItemCount() {
        return lectureList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvLectureNumber;
        TextView tvStatus;
        CheckBox cbCompleted;
        ImageView ivPlayIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardLecture);
            tvLectureNumber = itemView.findViewById(R.id.tvLectureNumber);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            cbCompleted = itemView.findViewById(R.id.cbCompleted);
            ivPlayIcon = itemView.findViewById(R.id.ivPlayIcon);
        }
    }
}
`,
  },
  {
    name: 'LectureModel.java',
    path: 'app/src/main/java/com/studia/reminder/LectureModel.java',
    language: 'java',
    description: 'Data model class for Chapter lectures.',
    content: `package com.studia.reminder;

public class LectureModel {
    private int number;
    private String title;
    private String videoUrl;
    private boolean isCompleted;
    private String notes;

    public LectureModel(int number, String title, String videoUrl, boolean isCompleted, String notes) {
        this.number = number;
        this.title = title;
        this.videoUrl = videoUrl;
        this.isCompleted = isCompleted;
        this.notes = notes;
    }

    public int getNumber() { return number; }
    public String getTitle() { return title; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
`,
  },
  {
    name: 'activity_main.xml',
    path: 'app/src/main/res/layout/activity_main.xml',
    language: 'xml',
    description: 'Main activity XML layout featuring Header, Sessions, Progress Bar, and Lecture Grid.',
    content: `<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#0F172A">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:padding="16dp">

        <!-- Header -->
        <TextView
            android:id="@+id/tvHeaderTitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="📱 স্টাডি রিমাইন্ডার ও লেকচার ট্র্যাকার"
            android:textColor="#FFFFFF"
            android:textSize="20sp"
            android:textStyle="bold" />

        <!-- Chapter & Progress Card -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            app:cardCornerRadius="14dp"
            app:cardBackgroundColor="#1E293B">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">

                <TextView
                    android:id="@+id/tvChapterTitle"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="পদার্থবিজ্ঞান: অধ্যায় ০২: ভেক্টর"
                    android:textColor="#38BDF8"
                    android:textSize="17sp"
                    android:textStyle="bold" />

                <ProgressBar
                    android:id="@+id/progressBarChapter"
                    style="?android:attr/progressBarStyleHorizontal"
                    android:layout_width="match_parent"
                    android:layout_height="12dp"
                    android:layout_marginTop="10dp"
                    android:progress="30"
                    android:progressDrawable="@drawable/progress_bar_custom" />

                <TextView
                    android:id="@+id/tvProgressText"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="6dp"
                    android:text="৬ / ২০ সম্পন্ন (৩০%)"
                    android:textColor="#10B981"
                    android:textSize="14sp" />

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginTop="12dp">

                    <Button
                        android:id="@+id/btnBulkImport"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:layout_marginEnd="6dp"
                        android:backgroundTint="#3B82F6"
                        android:text="🔗 বাল্ক লিঙ্ক ম্যাপিং"
                        android:textSize="12sp" />

                    <Button
                        android:id="@+id/btnSetTotalLectures"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:layout_marginStart="6dp"
                        android:backgroundTint="#6366F1"
                        android:text="🔢 মোট লেকচার"
                        android:textSize="12sp" />
                </LinearLayout>
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <!-- Lectures List -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:text="লেকচারসমূহ (১-ক্লিকে প্লে ও ট্র্যাকিং):"
            android:textColor="#CBD5E1"
            android:textSize="15sp"
            android:textStyle="bold" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recyclerViewLectures"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:layout_marginTop="8dp" />
    </LinearLayout>

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabAddSession"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="20dp"
        android:backgroundTint="#10B981"
        android:src="@android:drawable/ic_input_add"
        app:tint="#FFFFFF" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
`,
  },
  {
    name: 'item_lecture.xml',
    path: 'app/src/main/res/layout/item_lecture.xml',
    language: 'xml',
    description: 'XML layout for individual lecture items with completion status and play button.',
    content: `<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/cardLecture"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="6dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="3dp"
    app:cardBackgroundColor="#1E293B">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="12dp">

        <RelativeLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content">

            <TextView
                android:id="@+id/tvLectureNumber"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="লেকচার ১"
                android:textColor="#FFFFFF"
                android:textSize="15sp"
                android:textStyle="bold" />

            <CheckBox
                android:id="@+id/cbCompleted"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_alignParentEnd="true"
                android:buttonTint="#10B981" />
        </RelativeLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="8dp"
            android:gravity="center_vertical">

            <ImageView
                android:id="@+id/ivPlayIcon"
                android:layout_width="22dp"
                android:layout_height="22dp"
                android:src="@android:drawable/ic_media_play"
                app:tint="#EF4444" />

            <TextView
                android:id="@+id/tvStatus"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginStart="6dp"
                android:text="ভিডিও প্রস্তুত"
                android:textColor="#94A3B8"
                android:textSize="12sp" />
        </LinearLayout>
    </LinearLayout>
</androidx.cardview.widget.CardView>
`,
  },
  {
    name: 'dialog_bulk_import.xml',
    path: 'app/src/main/res/layout/dialog_bulk_import.xml',
    language: 'xml',
    description: 'Dialog layout for pasting multiple video links at once.',
    content: `<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="20dp"
    android:background="#1E293B">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="নিচে সবকটি ইউটিউব বা ভিডিও লিঙ্ক একসাথে পেস্ট করুন:"
        android:textColor="#E2E8F0"
        android:textSize="14sp" />

    <EditText
        android:id="@+id/etBulkLinks"
        android:layout_width="match_parent"
        android:layout_height="160dp"
        android:layout_marginTop="10dp"
        android:gravity="top|start"
        android:background="#0F172A"
        android:padding="12dp"
        android:textColor="#FFFFFF"
        android:hint="https://youtu.be/...\nhttps://youtu.be/...\nhttps://youtu.be/..."
        android:textColorHint="#64748B"
        android:inputType="textMultiLine" />
</LinearLayout>
`,
  },
  {
    name: 'AndroidManifest.xml',
    path: 'app/src/main/AndroidManifest.xml',
    language: 'xml',
    description: 'Android App Manifest with Internet permission and Activity declarations.',
    content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.studia.reminder">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="স্টাডি রিমাইন্ডার"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Material3.Dark.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
`,
  },
  {
    name: 'build.gradle (Module: app)',
    path: 'app/build.gradle',
    language: 'groovy',
    description: 'Gradle build file with AndroidX, Material Design 3 and RecyclerView dependencies.',
    content: `plugins {
    id 'com.android.application'
}

android {
    namespace 'com.studia.reminder'
    compileSdk 34

    defaultConfig {
        applicationId "com.studia.reminder"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.2.0'
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'
}
`,
  }
];
