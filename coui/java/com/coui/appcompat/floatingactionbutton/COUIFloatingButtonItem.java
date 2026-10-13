package com.coui.appcompat.floatingactionbutton;

import android.content.res.ColorStateList;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/* JADX INFO: loaded from: classes4.dex */
public class COUIFloatingButtonItem implements Parcelable {
    public static final Parcelable.Creator<COUIFloatingButtonItem> CREATOR = new Parcelable.Creator<COUIFloatingButtonItem>() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButtonItem.1
        @Override // android.os.Parcelable.Creator
        public final COUIFloatingButtonItem createFromParcel(Parcel parcel) {
            return new COUIFloatingButtonItem(parcel);
        }

        @Override // android.os.Parcelable.Creator
        public final COUIFloatingButtonItem[] newArray(int i3) {
            return new COUIFloatingButtonItem[i3];
        }
    };

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public final int f8312a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    @Nullable
    public final String f8313b;

    @StringRes
    public final int c;

    @DrawableRes
    public int d;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    public ColorStateList f8314e;

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    public ColorStateList f8315f;

    /* JADX INFO: renamed from: g, reason: collision with root package name */
    public final ColorStateList f8316g;

    /* JADX INFO: renamed from: h, reason: collision with root package name */
    public final boolean f8317h;

    public COUIFloatingButtonItem(Builder builder) {
        this.f8314e = ColorStateList.valueOf(Integer.MIN_VALUE);
        this.f8315f = ColorStateList.valueOf(Integer.MIN_VALUE);
        ColorStateList.valueOf(Integer.MIN_VALUE);
        this.f8313b = builder.c;
        this.c = builder.d;
        this.d = builder.f8319b;
        this.f8314e = builder.f8320e;
        this.f8315f = builder.f8321f;
        this.f8316g = builder.f8322g;
        this.f8317h = builder.f8323h;
        this.f8312a = builder.f8318a;
    }

    @Override // android.os.Parcelable
    public final int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i3) {
        parcel.writeString(this.f8313b);
        parcel.writeInt(this.c);
        parcel.writeInt(this.d);
        parcel.writeInt(this.f8312a);
    }

    public static class Builder {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        public final int f8318a;

        /* JADX INFO: renamed from: b, reason: collision with root package name */
        @DrawableRes
        public final int f8319b;

        @Nullable
        public String c;

        @StringRes
        public int d;

        /* JADX INFO: renamed from: e, reason: collision with root package name */
        public ColorStateList f8320e;

        /* JADX INFO: renamed from: f, reason: collision with root package name */
        public ColorStateList f8321f;

        /* JADX INFO: renamed from: g, reason: collision with root package name */
        public ColorStateList f8322g;

        /* JADX INFO: renamed from: h, reason: collision with root package name */
        public boolean f8323h;

        public Builder(int i3, @DrawableRes int i10) {
            this.d = Integer.MIN_VALUE;
            this.f8320e = ColorStateList.valueOf(Integer.MIN_VALUE);
            this.f8321f = ColorStateList.valueOf(Integer.MIN_VALUE);
            this.f8322g = ColorStateList.valueOf(Integer.MIN_VALUE);
            this.f8323h = true;
            this.f8318a = i3;
            this.f8319b = i10;
        }

        public Builder(COUIFloatingButtonItem cOUIFloatingButtonItem) {
            this.d = Integer.MIN_VALUE;
            this.f8320e = ColorStateList.valueOf(Integer.MIN_VALUE);
            this.f8321f = ColorStateList.valueOf(Integer.MIN_VALUE);
            this.f8322g = ColorStateList.valueOf(Integer.MIN_VALUE);
            this.f8323h = true;
            this.c = cOUIFloatingButtonItem.f8313b;
            this.d = cOUIFloatingButtonItem.c;
            this.f8319b = cOUIFloatingButtonItem.d;
            this.f8320e = cOUIFloatingButtonItem.f8314e;
            this.f8321f = cOUIFloatingButtonItem.f8315f;
            this.f8322g = cOUIFloatingButtonItem.f8316g;
            this.f8323h = cOUIFloatingButtonItem.f8317h;
            this.f8318a = cOUIFloatingButtonItem.f8312a;
        }
    }

    public COUIFloatingButtonItem(Parcel parcel) {
        this.f8314e = ColorStateList.valueOf(Integer.MIN_VALUE);
        this.f8315f = ColorStateList.valueOf(Integer.MIN_VALUE);
        this.f8316g = ColorStateList.valueOf(Integer.MIN_VALUE);
        this.f8317h = true;
        this.f8313b = parcel.readString();
        this.c = parcel.readInt();
        this.d = parcel.readInt();
        this.f8312a = parcel.readInt();
    }
}
