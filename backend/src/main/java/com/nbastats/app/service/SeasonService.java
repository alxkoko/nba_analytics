package com.nbastats.app.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class SeasonService {

    /**
     * NBA season: Oct–June = e.g. 2024-25; July–Sep = previous season.
     */
    public String getCurrentSeason() {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();
        int startYear = month >= 10 ? year : year - 1;
        int endYear = startYear + 1;
        String endYY = String.format("%02d", endYear % 100);
        return startYear + "-" + endYY;
    }

    /**
     * Returns list of season strings from current back: ["2024-25", "2023-24", ...].
     */
    public List<String> getSeasons(int count) {
        String current = getCurrentSeason();
        int startYear = Integer.parseInt(current.substring(0, 4));
        return IntStream.range(0, count)
            .mapToObj(i -> {
                int y = startYear - i;
                String endYY = String.format("%02d", (y + 1) % 100);
                return y + "-" + endYY;
            })
            .toList();
    }
}
