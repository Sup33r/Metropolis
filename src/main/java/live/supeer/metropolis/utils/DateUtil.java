package live.supeer.metropolis.utils;

import live.supeer.metropolis.Metropolis;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtil {
    public static Metropolis plugin;

    public static String getMonthName(int month) {
        String[] months = {"january", "february", "march", "april", "may", "june",
                "july", "august", "september", "october", "november", "december"};
        return months[month];
    }

    private static final Pattern timePattern = Pattern.compile("(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*(?:s[a-z]*)?)?", Pattern.CASE_INSENSITIVE);
    private static final int maxYears = 100000;

    public static String removeTimePattern(final String input) {
        return timePattern.matcher(input).replaceFirst("").trim();
    }

    public static long parseDateDiff(String time, boolean future) {
        return parseDateDiff(time, future, false);
    }

    public static long parseDateDiff(String time, boolean future, boolean emptyEpoch) {
        final Matcher m = timePattern.matcher(time);
        int years = 0;
        int months = 0;
        int weeks = 0;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        boolean found = false;
        while (m.find()) {
            if (m.group() == null || m.group().isEmpty()) {
                continue;
            }
            for (int i = 0; i < m.groupCount(); i++) {
                if (m.group(i) != null && !m.group(i).isEmpty()) {
                    found = true;
                    break;
                }
            }
            if (found) {
                if (m.group(1) != null && !m.group(1).isEmpty()) {
                    years = Integer.parseInt(m.group(1));
                }
                if (m.group(2) != null && !m.group(2).isEmpty()) {
                    months = Integer.parseInt(m.group(2));
                }
                if (m.group(3) != null && !m.group(3).isEmpty()) {
                    weeks = Integer.parseInt(m.group(3));
                }
                if (m.group(4) != null && !m.group(4).isEmpty()) {
                    days = Integer.parseInt(m.group(4));
                }
                if (m.group(5) != null && !m.group(5).isEmpty()) {
                    hours = Integer.parseInt(m.group(5));
                }
                if (m.group(6) != null && !m.group(6).isEmpty()) {
                    minutes = Integer.parseInt(m.group(6));
                }
                if (m.group(7) != null && !m.group(7).isEmpty()) {
                    seconds = Integer.parseInt(m.group(7));
                }
                break;
            }
        }
        if (!found) {
            return -1;
        }
        final Calendar c = new GregorianCalendar();

        if (emptyEpoch) {
            c.setTimeInMillis(0);
        }

        if (years > 0) {
            if (years > maxYears) {
                years = maxYears;
            }
            c.add(Calendar.YEAR, years * (future ? 1 : -1));
        }
        if (months > 0) {
            c.add(Calendar.MONTH, months * (future ? 1 : -1));
        }
        if (weeks > 0) {
            c.add(Calendar.WEEK_OF_YEAR, weeks * (future ? 1 : -1));
        }
        if (days > 0) {
            c.add(Calendar.DAY_OF_MONTH, days * (future ? 1 : -1));
        }
        if (hours > 0) {
            c.add(Calendar.HOUR_OF_DAY, hours * (future ? 1 : -1));
        }
        if (minutes > 0) {
            c.add(Calendar.MINUTE, minutes * (future ? 1 : -1));
        }
        if (seconds > 0) {
            c.add(Calendar.SECOND, seconds * (future ? 1 : -1));
        }
        final Calendar max = new GregorianCalendar();
        max.add(Calendar.YEAR, maxYears);
        if (c.after(max)) {
            return max.getTimeInMillis();
        }
        return c.getTimeInMillis();
    }

    static int dateDiff(final int type, final Calendar fromDate, final Calendar toDate, final boolean future) {
        final int year = Calendar.YEAR;

        final int fromYear = fromDate.get(year);
        final int toYear = toDate.get(year);
        if (Math.abs(fromYear - toYear) > maxYears) {
            toDate.set(year, fromYear +
                    (future ? maxYears : -maxYears));
        }

        int diff = 0;
        long savedDate = fromDate.getTimeInMillis();
        while ((future && !fromDate.after(toDate)) || (!future && !fromDate.before(toDate))) {
            savedDate = fromDate.getTimeInMillis();
            fromDate.add(type, future ? 1 : -1);
            diff++;
        }
        diff--;
        fromDate.setTimeInMillis(savedDate);
        return diff;
    }

    public static String formatDateDiff(final long date) {
        final Calendar c = new GregorianCalendar();
        c.setTimeInMillis(date);
        final Calendar now = new GregorianCalendar();
        return formatDateDiff(now, c);
    }

    public static String formatDateDiff(final Calendar fromDate, final Calendar toDate) {
        boolean future = false;
        if (toDate.equals(fromDate)) {
            return plugin.getMessage("messages.time.now");
        }
        if (toDate.after(fromDate)) {
            future = true;
        }
        // Temporary 50ms time buffer added to avoid display truncation due to code execution delays
        toDate.add(Calendar.MILLISECOND, future ? 50 : -50);
        final StringBuilder sb = new StringBuilder();
        final int[] types = new int[] {Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND};
        final String[] names = new String[] {plugin.getMessage("messages.time.year"), plugin.getMessage("messages.time.years"), plugin.getMessage("messages.time.month"), plugin.getMessage("messages.time.months"), plugin.getMessage("messages.time.day"), plugin.getMessage("messages.time.days"), plugin.getMessage("messages.time.hour"), plugin.getMessage("messages.time.hours"), plugin.getMessage("messages.time.minute"), plugin.getMessage("messages.time.minutes"), plugin.getMessage("messages.time.second"), plugin.getMessage("messages.time.seconds")};
        int accuracy = 0;
        for (int i = 0; i < types.length; i++) {
            if (accuracy > 2) {
                break;
            }
            final int diff = dateDiff(types[i], fromDate, toDate, future);
            if (diff > 0) {
                accuracy++;
                sb.append(" ").append(diff).append(" ").append(names[i * 2 + (diff > 1 ? 1 : 0)]);
            }
        }
        // Preserve correctness in the original date object by removing the extra buffer time
        toDate.add(Calendar.MILLISECOND, future ? -50 : 50);
        if (sb.isEmpty()) {
            return plugin.getMessage("messages.time.now");
        }
        return sb.toString().trim();
    }

    public static String formatTimeFromSeconds(int totalSeconds) {
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = (totalSeconds / (60 * 60)) % 24;
        int days = (totalSeconds / (24 * 60 * 60)) % 7;
        int weeks = (totalSeconds / (7 * 24 * 60 * 60)) % 52;
        int years = totalSeconds / (365 * 24 * 60 * 60);

        StringBuilder readableTime = new StringBuilder();
        if (years > 0) {
            readableTime.append(years).append(" ").append(plugin.getMessage("messages.time.years")).append(" ");
        }
        if (weeks > 0) {
            readableTime.append(weeks).append(" ").append(plugin.getMessage("messages.time.weeks")).append(" ");
        }
        if (days > 0) {
            readableTime.append(days).append(" ").append(plugin.getMessage("messages.time.days")).append(" ");
        }
        if (hours > 0) {
            readableTime.append(hours).append(" ").append(plugin.getMessage("messages.time.hours")).append(" ");
        }
        if (minutes > 0) {
            readableTime.append(minutes).append(" ").append(plugin.getMessage("messages.time.minutes")).append(" ");
        }
        if (seconds > 0) {
            readableTime.append(seconds).append(" ").append(plugin.getMessage("messages.time.seconds"));
        }

        return readableTime.toString().trim();
    }

    public static long getTimestamp() {
        return System.currentTimeMillis() / 1000L;
    }

    public static String niceDate(long timestamp) {
        if (timestamp == 0) {
            return "unknown";
        }

        long rightNow = getTimestamp();

        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(timestamp * 1000);

        Calendar dateNow = Calendar.getInstance();
        dateNow.setTimeInMillis(rightNow * 1000);

        String day;
        String[] months = {
                plugin.getMessage("messages.months.january"),
                plugin.getMessage("messages.months.february"),
                plugin.getMessage("messages.months.march"),
                plugin.getMessage("messages.months.april"),
                plugin.getMessage("messages.months.may"),
                plugin.getMessage("messages.months.june"),
                plugin.getMessage("messages.months.july"),
                plugin.getMessage("messages.months.august"),
                plugin.getMessage("messages.months.september"),
                plugin.getMessage("messages.months.october"),
                plugin.getMessage("messages.months.november"),
                plugin.getMessage("messages.months.december")
        };

        if (date.get(Calendar.DAY_OF_MONTH) == dateNow.get(Calendar.DAY_OF_MONTH)
                && date.get(Calendar.MONTH) == dateNow.get(Calendar.MONTH)
                && date.get(Calendar.YEAR) == dateNow.get(Calendar.YEAR)) {
            day = plugin.getMessage("messages.days.today");
        } else {
            Calendar dateYesterday = Calendar.getInstance();
            dateYesterday.setTimeInMillis((rightNow - 86400) * 1000);

            day =
                    date.get(Calendar.DAY_OF_MONTH) == dateYesterday.get(Calendar.DAY_OF_MONTH)
                            && date.get(Calendar.MONTH) == dateYesterday.get(Calendar.MONTH)
                            && date.get(Calendar.YEAR) == dateYesterday.get(Calendar.YEAR)
                            ? plugin.getMessage("messages.days.yesterday")
                            : date.get(Calendar.DAY_OF_MONTH)
                            + " "
                            + months[date.get(Calendar.MONTH)]
                            + (dateNow.get(Calendar.YEAR) != date.get(Calendar.YEAR)
                            ? " " + date.get(Calendar.YEAR)
                            : "");
        }

        return day
                + ", "
                + String.format("%02d", date.get(Calendar.HOUR_OF_DAY))
                + ":"
                + String.format("%02d", date.get(Calendar.MINUTE));
    }

    public static String formatDate(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp * 1000L); // Convert seconds to milliseconds

        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        String monthName = plugin.getMessage("messages.months." + DateUtil.getMonthName(month).toLowerCase());

        return String.format("%d %s %d, %02d:%02d", day, monthName, year, hour, minute);
    }
}
