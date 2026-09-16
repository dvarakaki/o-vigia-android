package com.ovigia.app.ui;

import android.content.Context;
import android.text.format.DateUtils;

import com.ovigia.app.R;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * "há 5 min", "ontem", "há 3 dias"… Com frases próprias (traduzidas em
 * strings.xml), e não com DateUtils, para manter o tom do app em cada idioma.
 */
public final class RelativeTime {

    public static String format(Context context, long timestamp, long now) {
        long elapsed = Math.max(0, now - timestamp);
        if (elapsed < DateUtils.MINUTE_IN_MILLIS) return context.getString(R.string.profile_time_now);
        if (elapsed < DateUtils.HOUR_IN_MILLIS) {
            return context.getString(R.string.profile_time_minutes, (int) (elapsed / DateUtils.MINUTE_IN_MILLIS));
        }
        if (elapsed < DateUtils.DAY_IN_MILLIS) {
            return context.getString(R.string.profile_time_hours, (int) (elapsed / DateUtils.HOUR_IN_MILLIS));
        }
        if (elapsed < 2 * DateUtils.DAY_IN_MILLIS) return context.getString(R.string.profile_time_yesterday);
        if (elapsed < DateUtils.WEEK_IN_MILLIS) {
            return context.getString(R.string.profile_time_days, (int) (elapsed / DateUtils.DAY_IN_MILLIS));
        }
        return context.getString(R.string.profile_time_date,
                DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(new Date(timestamp)));
    }

    private RelativeTime() { }
}
