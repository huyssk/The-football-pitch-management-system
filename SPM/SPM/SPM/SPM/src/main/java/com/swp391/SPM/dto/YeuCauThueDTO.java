package com.swp391.SPM.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.Date;
import java.sql.Time;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class YeuCauThueDTO {
    private int id;
    private String tensanBong;
    private LocalDate ngayDa;
    private Time gioDa;
    private Date ngayTao;
    private int tienSan;
    private String trangThai;

    public YeuCauThueDTO(int id, String tensanBong, Time gioDa, String trangThai) {
        this.id = id;
        this.tensanBong = tensanBong;
        this.gioDa = gioDa;
        this.trangThai = trangThai;
    }

    public YeuCauThueDTO(int id, String trangThai) {
        this.id = id;
        this.trangThai = trangThai;
    }
}
