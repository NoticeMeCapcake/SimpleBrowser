package com.example.simplebrowser_7;

import java.util.Date;

public class HistoryEntry {
    public Date visitDate;
    public Long visitDuration;
    public String url;

    public HistoryEntry(Date date, Long duration, String _url) {
        visitDate = date;
        visitDuration = duration;
        url = _url;
    }

}
