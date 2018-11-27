package com.google.android.gms.samples.vision.ocrreader;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "word_table")
public class Word {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "word")
    private String mWord;

    @NonNull
    @ColumnInfo(name = "meaning")
    private String meaning;

    public Word(String word, String meaning) {
        this.mWord = word;
        this.meaning = meaning;
    }

    public String getWord() {
        return this.mWord;
    }

    public String getMeaning() {
        return this.meaning;
    }
}
