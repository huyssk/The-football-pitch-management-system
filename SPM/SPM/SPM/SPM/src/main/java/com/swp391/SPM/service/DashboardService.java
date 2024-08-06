package com.swp391.SPM.service;


import com.swp391.SPM.entity.YeuCauHuy;
import com.swp391.SPM.entity.YeuCauThue;
import com.swp391.SPM.repository.YeuCauHuyRepository;
import com.swp391.SPM.repository.YeuCauThueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private YeuCauThueRepository yeuCauThueRepository;

    @Autowired
    private YeuCauHuyRepository yeuCauHuyRepository;

    @Autowired
    SanBongService sanBongService;

    public Map<String, Map<String, Double>> getDoanhThuSan(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<YeuCauThue> yeuCauThues = yeuCauThueRepository.findByNgayTaoBetween(start, end);
        List<YeuCauHuy> yeuCauHuys = yeuCauHuyRepository.findByNgayTaoBetween(start, end);

        Map<String, Map<String, Double>> result = new HashMap<>();

        for (YeuCauThue yct : yeuCauThues) {
            String sanBongName = sanBongService.getSanBongById(yct.getIdSanBong()).getTenSanBong();
            String ngayTao = String.valueOf(yct.getNgayTao());
            int tienSan = sanBongService.getSanBongById(yct.getIdSanBong()).getTienSan();

            result.computeIfAbsent(sanBongName, k -> new HashMap<>());
            result.get(sanBongName).merge(ngayTao, (double)tienSan, Double::sum);
        }

        for (YeuCauHuy ych : yeuCauHuys) {
            String sanBongName = sanBongService.getSanBongById(ych.getIdSanBong()).getTenSanBong();
            String ngayTao = String.valueOf(ych.getNgayTao());
            int tienSan = sanBongService.getSanBongById(ych.getIdSanBong()).getTienSan()*20/100;

            result.computeIfAbsent(sanBongName, k -> new HashMap<>());
            result.get(sanBongName).merge(ngayTao, (double)tienSan, Double::sum);
        }
        return result;
    }

    public Map<String, Double> getDoanhThuWeb(LocalDate startDate, LocalDate endDate) {
        Map<String, Map<String, Double>> doanhThuSan = getDoanhThuSan(startDate, endDate);

        Map<String, Double> result = new HashMap<>();

        for (Map<String, Double> sanData : doanhThuSan.values()) {
            for (Map.Entry<String, Double> entry : sanData.entrySet()) {
                result.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        return result;
    }

    public Map<String, Map<String, Map<String, Integer>>> getSoLuongDatHuySan(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<YeuCauThue> yeuCauThues = yeuCauThueRepository.findByNgayTaoBetween(start, end);
        List<YeuCauHuy> yeuCauHuys = yeuCauHuyRepository.findByNgayTaoBetween(start, end);

        Map<String, Map<String, Map<String, Integer>>> result = new HashMap<>();

        for (YeuCauThue yct : yeuCauThues) {
            String sanBongName = sanBongService.getSanBongById(yct.getIdSanBong()).getTenSanBong();
            String ngayTao = String.valueOf(yct.getNgayTao());

            result.computeIfAbsent(sanBongName, k -> new HashMap<>());
            result.get(sanBongName).computeIfAbsent("dat", k -> new HashMap<>());
            result.get(sanBongName).get("dat").merge(ngayTao, 1, Integer::sum);
        }

        for (YeuCauHuy ych : yeuCauHuys) {
            String sanBongName = sanBongService.getSanBongById(ych.getIdSanBong()).getTenSanBong();
            String ngayTao = String.valueOf(ych.getNgayTao());
            result.computeIfAbsent(sanBongName, k -> new HashMap<>());
            result.get(sanBongName).computeIfAbsent("huy", k -> new HashMap<>());
            result.get(sanBongName).get("huy").merge(ngayTao, 1, Integer::sum);
        }

        return result;
    }
    public Map<YearMonth, Map<String, Double>> getDoanhThuWebTheoThang(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<YeuCauThue> yeuCauThueList = yeuCauThueRepository.findByNgayTaoBetween(startDateTime, endDateTime);
        List<YeuCauHuy> yeuCauHuyList = yeuCauHuyRepository.findByNgayTaoBetween(startDateTime, endDateTime);

        Map<YearMonth, Map<String, Double>> result = new LinkedHashMap<>();

        // Process YeuCauThue (add to revenue)
        for (YeuCauThue yeuCauThue : yeuCauThueList) {
            LocalDate date = yeuCauThue.getNgayTao().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            YearMonth yearMonth = YearMonth.from(date);
            String dayOfMonth = String.format("%02d", date.getDayOfMonth()); // Thay đổi ở đây
            int amount = sanBongService.getSanBongById(yeuCauThue.getIdSanBong()).getTienSan();

            result.computeIfAbsent(yearMonth, k -> new LinkedHashMap<>())
                    .merge(dayOfMonth, (double)amount, Double::sum);
        }

        // Process YeuCauHuy (subtract from revenue, assuming full refund)
        for (YeuCauHuy yeuCauHuy : yeuCauHuyList) {
            LocalDate date = yeuCauHuy.getNgayTao().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            YearMonth yearMonth = YearMonth.from(date);
            String dayOfMonth = String.format("%02d", date.getDayOfMonth()); // Thay đổi ở đây
            int amount = sanBongService.getSanBongById(yeuCauHuy.getIdSanBong()).getTienSan()*20/100;

            result.computeIfAbsent(yearMonth, k -> new LinkedHashMap<>())
                    .merge(dayOfMonth, (double)amount, Double::sum);
        }

        // Fill in missing days with zero revenue
        for (Map.Entry<YearMonth, Map<String, Double>> entry : result.entrySet()) {
            YearMonth yearMonth = entry.getKey();
            Map<String, Double> monthData = entry.getValue();

            int daysInMonth = yearMonth.lengthOfMonth();
            for (int day = 1; day <= daysInMonth; day++) {
                String dayStr = String.format("%02d", day); // Thay đổi ở đây
                monthData.putIfAbsent(dayStr, 0.0);
            }
        }

        return result;
    }
}

