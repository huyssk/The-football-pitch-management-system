package com.swp391.SPM.controller;


import com.swp391.SPM.dto.BookingDetailDTO;
import com.swp391.SPM.dto.CancellationRequestDTO;
import com.swp391.SPM.dto.YeuCauThueDTO;
import com.swp391.SPM.entity.*;
import com.swp391.SPM.handle.RegisterUser;
import com.swp391.SPM.repository.RoleRepository;
import com.swp391.SPM.repository.YeuCauThueRepository;
import com.swp391.SPM.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private LoaiSanService loaiSanService;
    @Autowired
    private SanBongService sanBongService;
    @Autowired
    public UserService userService;
    @Autowired
    public RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private YeuCauThueService yeucauThueService;
    @Autowired
    GioHoatDongService gioHoatDongService;
    @Autowired
    YeuCauHuyService yeuCauHuyService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    SlotService slotService;

    @GetMapping("/showEmployee")
    public String showEmployee(Model model) {
        Role role = roleRepository.findById(2).orElse(null);
        List<User> employees = userService.findAllByRole(role);
        model.addAttribute("employees", employees);
        RegisterUser ru = new RegisterUser();
        model.addAttribute("registerUser", ru);
        return "addUpdateEmployee";
    }

    @InitBinder
    public void initBinder(WebDataBinder data) {
        StringTrimmerEditor stringTrimmerEditor = new StringTrimmerEditor(true);
        data.registerCustomEditor(String.class, stringTrimmerEditor);
    }

    @PostMapping("/add_process")
    @ResponseBody
    public ResponseEntity<?> process(@Valid @ModelAttribute RegisterUser registerUser, BindingResult result, Model model) {
        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            Map<String, List<String>> errors = new HashMap<>();
            for (FieldError error : result.getFieldErrors()) {
                errors.computeIfAbsent(error.getField(), k -> new ArrayList<>()).add(error.getDefaultMessage());
            }
            response.put("success", false);
            response.put("errors", errors);
            return ResponseEntity.badRequest().body(response);
        }

        String email = registerUser.getEmail();
        User useExisting = userService.getUserByEmail(email);
        if (useExisting != null) {
            Map<String, List<String>> errors = new HashMap<>();
            errors.put("email", Collections.singletonList("Tài khoản đã tồn tại!!!"));
            response.put("success", false);
            response.put("errors", errors);
            return ResponseEntity.badRequest().body(response);
        }

        // Xử lý thêm nhân viên nếu không có lỗi
        User user = new User();
        user.setEmail(registerUser.getEmail());
        user.setPassword(passwordEncoder.encode(registerUser.getPassword()));
        user.setFullName(registerUser.getFullname());
        user.setPhoneNumber(registerUser.getPhone());

        Role role = new Role();
        role.setId(2);
        user.setRoles(role);
        user.setLogType("Normal");

        userService.save(user);
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/editEmployee")
    public String viewProfileEmploy(@RequestParam(value = "email") String email, Model model) {
        User user = userService.getUserByEmail(email);
        model.addAttribute("user", user);
        // Thêm danh sách các role vào model
        List<Role> roles = roleRepository.findAll();
        model.addAttribute("roles", roles);
        return "editEmploy";
    }

    @PostMapping("/update_profile_employee")
    public String updateEmployy(@Valid @ModelAttribute("user") User user, BindingResult result, Model model) {
        if (result.hasErrors()) {
            List<Role> roles = roleRepository.findAll();
            model.addAttribute("roles", roles);
            return "editEmploy";
        }

        userService.update(user);
        return "redirect:/admin/showEmployee";
    }


    @GetMapping("/deleteEmployee")
    public String deleteEmployy(@RequestParam(value = "email") String email) {
        User user = userService.getUserByEmail(email);
        userService.deleteUser(user);
        return "redirect:/admin/showEmployee";
    }


    @GetMapping("/edit")
    public String test2(Model model) {
        List<SanBong> list = this.sanBongService.getAllSanBong();
        List<LoaiSan> loaiSanList = loaiSanService.getAllLoaiSan();
        model.addAttribute("list", list);
        model.addAttribute("loaiSanList", loaiSanList);
        model.addAttribute("sanBong", new SanBong());
        return "addUpdateSanBong";
    }

    @GetMapping("/editSan")
    public String showForm(@RequestParam(value = "id") Integer id, Model model) {
        SanBong sanBong = sanBongService.getSanBongById(id);
        model.addAttribute("sanBong", sanBong);
        model.addAttribute("loaiSan", loaiSanService.findLoaiSansById(sanBong.getIdLoaiSan()).getTenLoaiSan());
        return "/editSanBong";
    }

    @GetMapping("/deleteSan")
    public String deleteSan(@RequestParam(value = "id") Integer id) {
        sanBongService.deleteById(id);
        return "redirect:/admin/edit";
    }


    @PostMapping("/updateSanBong")
    public String saveSanBong(@ModelAttribute("sanBong") SanBong sanBong,
                              Model model, @RequestParam("imageFile") MultipartFile imageFile) {
        int id = sanBong.getId();
        SanBong sanBongtim = sanBongService.getSanBongById(id);
        if (sanBongtim != null) {
            if (imageFile.isEmpty()) {
                sanBong.setHinhAnh(sanBongtim.getHinhAnh());
            } else {
                String validationResult = validateFile(imageFile);
                if (!validationResult.isEmpty()) {
                    model.addAttribute("my_error", validationResult); // Thêm thông báo lỗi vào model
                    return "editSanBong"; // Trả về view với thông báo lỗi
                }

                try {
                    saveFile(imageFile);
                    sanBong.setHinhAnh("/assets/Uploads/" + imageFile.getOriginalFilename());
                } catch (Exception e) {
                    e.printStackTrace();
                    model.addAttribute("my_error", "Lỗi khi lưu hình ảnh: " + e.getMessage());
                    return "editSanBong"; // Trả về view với thông báo lỗi
                }
            }
        }

        sanBongService.update(sanBong);
        return "redirect:/admin/edit";
    }

    @PostMapping("/addSanBong")
    public String addSanBong(@ModelAttribute("sanBong") SanBong sanBong,
                             Model model, @RequestParam("imageFile") MultipartFile imageFile) {
        String validationResult = validateFile(imageFile);
        if (!validationResult.isEmpty()) {
            List<LoaiSan> loaiSanList = loaiSanService.getAllLoaiSan();
            List<SanBong> list = sanBongService.getAllSanBong();
            model.addAttribute("list", list);
            model.addAttribute("loaiSanList", loaiSanList);
            model.addAttribute("sanBong", sanBong); // Để giữ lại dữ liệu đã nhập
            model.addAttribute("my_error1", validationResult); // Thêm thông báo lỗi vào model
            return "addUpdateSanBong"; // Trả về view với thông báo lỗi
        }

        try {
            saveFile(imageFile);
            sanBong.setHinhAnh("/assets/Uploads/" + imageFile.getOriginalFilename());
        } catch (Exception e) {
            e.printStackTrace();
            List<LoaiSan> loaiSanList = loaiSanService.getAllLoaiSan();
            List<SanBong> list = sanBongService.getAllSanBong();
            model.addAttribute("list", list);
            model.addAttribute("loaiSanList", loaiSanList);
            model.addAttribute("sanBong", sanBong); // Để giữ lại dữ liệu đã nhập
            model.addAttribute("my_error1", "Lỗi khi lưu hình ảnh: " + e.getMessage());
            return "addUpdateSanBong"; // Trả về view với thông báo lỗi
        }

        sanBongService.create(sanBong);

        return "redirect:/admin/edit";
    }

    private String validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (!contentType.equals("image/jpeg") && !contentType.equals("image/png")) {
            return "Tệp phải là định dạng JPG hoặc PNG.";
        }

        if (file.getSize() > 5 * 1024 * 1024) { // 500 MB
            return "Kích thước tệp phải nhỏ hơn 5 MB.";
        }

        return "";
    }

    private void saveFile(MultipartFile file) throws Exception {
        java.nio.file.Path uploadDir = Paths.get("src/main/resources/static/assets/Uploads");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        java.nio.file.Path filePath = uploadDir.resolve(file.getOriginalFilename());
        Files.write(filePath, file.getBytes());
    }

    @GetMapping("/showBookings")
    public String viewAllBookings(Model model) {
        List<BookingDetailDTO> bookingDetails = new ArrayList<>();
        List<YeuCauThue> yeuCauThueList = yeucauThueService.findAllYeuCauThue();
        if (!yeuCauThueList.isEmpty()) {
            for (YeuCauThue yc : yeuCauThueList) {
                User user = userService.getUserById(yc.getUserId());
                GioHoatDong gioHoatDong = gioHoatDongService.getGioHoatDongById(yc.getIdGioHoatDong());
                SanBong sb = sanBongService.getSanBongById(yc.getIdSanBong());
                BookingDetailDTO bk = new BookingDetailDTO(user.getFullName(), user.getEmail(), user.getPhoneNumber(),
                        sb.getTenSanBong(), sb.getDiaDiem(), sb.getTienSan() * 20 / 100, gioHoatDong.getNgayDa(),
                        gioHoatDong.getGioDa(), yc.getNgayTao());
                bookingDetails.add(bk);
            }
        }

        model.addAttribute("bookingDetails", bookingDetails);
        return "admin/bookings";
    }

    @GetMapping("/cancellation-requests")
    public String viewAllCancellationRequests(Model model) {
        List<CancellationRequestDTO> cancellationRequests = new ArrayList<>();
        List<YeuCauHuy> yeuCauHuyList = yeuCauHuyService.getAllYeuCauHuy();
        if (!yeuCauHuyList.isEmpty()) {
            for (YeuCauHuy ych : yeuCauHuyList) {
                User user = userService.getUserById(ych.getUserId());
                SanBong sb = sanBongService.getSanBongById(ych.getIdSanBong());
                CancellationRequestDTO cancellationRequestDTO = new CancellationRequestDTO(user.getFullName(), user.getEmail(), user.getPhoneNumber(),
                        sb.getTenSanBong(), sb.getDiaDiem(), ych.getNgayTao(), ych.getNgayDa(), ych.getGioDa(), ych.getGhiChu());
                cancellationRequests.add(cancellationRequestDTO);
            }
        }
        model.addAttribute("cancellationRequests", cancellationRequests);
        return "admin/cancellation-requests";
    }

    @GetMapping("/dashboard26")
    public String getDashboard26(Model model) {
        try {
            LocalDate startDate = LocalDate.now().minusMonths(1);
            LocalDate endDate = LocalDate.now();

            LocalDate now = LocalDate.now();
            LocalDate startDate1 = now.minusMonths(11).withDayOfMonth(1); // Lấy dữ liệu 12 tháng gần nhất
            LocalDate endDate1 = now.withDayOfMonth(now.lengthOfMonth());

            Map<String, Map<String, Map<String, Integer>>> soLuongDatHuySan = dashboardService.getSoLuongDatHuySan(startDate, endDate);
            Map<String, Map<String, Double>> doanhThuSan = dashboardService.getDoanhThuSan(startDate, endDate);
            Map<String, Double> doanhThuWebTongHop = dashboardService.getDoanhThuWeb(startDate, endDate);
            Map<YearMonth, Map<String, Double>> doanhThuWebTheoThang = dashboardService.getDoanhThuWebTheoThang(startDate1, endDate1);
            Map<YearMonth, Double> tongDoanhThuTheoThang = new LinkedHashMap<>();
            for (Map.Entry<YearMonth, Map<String, Double>> entry : doanhThuWebTheoThang.entrySet()) {
                YearMonth yearMonth = entry.getKey();
                Map<String, Double> doanhThuThang = entry.getValue();
                double tongDoanhThuThang = doanhThuThang.values().stream().mapToDouble(Double::doubleValue).sum();
                tongDoanhThuTheoThang.put(yearMonth, tongDoanhThuThang);
            }

            model.addAttribute("soLuongDatHuySan", soLuongDatHuySan);
            model.addAttribute("doanhThuSan", doanhThuSan);
            model.addAttribute("doanhThuWebTongHop", doanhThuWebTongHop);
            model.addAttribute("doanhThuWebTheoThang", doanhThuWebTheoThang);
            model.addAttribute("tongDoanhThuTheoThang", tongDoanhThuTheoThang);

            return "dashboard26";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "An error occurred: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/tinhTrangSan")
    public String getTinhTrangSan(Model model) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE dd/MM");
        List<GioHoatDong> gioHoatDong = gioHoatDongService.findGioHoatDongByNgayDa(today.format(formatter));
        List<YeuCauThue> yeuCauThueList = new ArrayList<>();
        List<YeuCauThueDTO> yeuCauThueDTOList = new ArrayList<>();
        if (gioHoatDong.size() == 1) {
            List<YeuCauThue> yeuCauThueList1 = yeucauThueService.findYeuCauThueByIdGioHoatDong(gioHoatDong.get(0).getId());
            for (YeuCauThue uc1 : yeuCauThueList1) {
                yeuCauThueList.add(uc1);
            }
        }
        if (gioHoatDong.size() == 2) {
            List<YeuCauThue> yeuCauThueList1 = yeucauThueService.findYeuCauThueByIdGioHoatDong(gioHoatDong.get(0).getId());
            for (YeuCauThue uc1 : yeuCauThueList1) {
                yeuCauThueList.add(uc1);
            }
            List<YeuCauThue> yeuCauThueList2 = yeucauThueService.findYeuCauThueByIdGioHoatDong(gioHoatDong.get(1).getId());
            for (YeuCauThue uc2 : yeuCauThueList2) {
                yeuCauThueList.add(uc2);
            }
        }
        for (YeuCauThue yct : yeuCauThueList) {
            YeuCauThueDTO yeuCauThueDTO = new YeuCauThueDTO(yct.getId(),
                    sanBongService.getSanBongById(yct.getIdSanBong()).getTenSanBong(),
                    gioHoatDongService.getGioHoatDongById(yct.getIdGioHoatDong()).getGioDa(), "thanh cong");
            yeuCauThueDTOList.add(yeuCauThueDTO);
        }
        LocalTime currentTime = LocalTime.now();
        LocalTime targetTime = LocalTime.parse("22:00:00", DateTimeFormatter.ofPattern("HH:mm:ss"));
        if(currentTime.isAfter(targetTime)){
            model.addAttribute("yeuCauThueDTOList", yeuCauThueDTOList);
        }
        return "viewTinhTrangSan";
    }

    @PostMapping("/saveStatus")
    public String saveStatus(@RequestParam Map<String, String> paramMap) {
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("status_")) {
                String id = key.substring("status_".length()); // Extract id from key
                String status = entry.getValue();
                YeuCauThue yeuCauThue = yeucauThueService.findYeuCauThueById(Integer.parseInt(id));
                if (status.equals("cancel")) {
                    User user = userService.getUserById(yeuCauThue.getUserId());
                    GioHoatDong gioHoatDong = gioHoatDongService.getGioHoatDongById(yeuCauThue.getIdGioHoatDong());
                    yeuCauHuyService.saveYeuCauHuy(new YeuCauHuy("khong den da", new Date(), user.getId(),
                            gioHoatDong.getNgayDa(), gioHoatDong.getGioDa(), yeuCauThue.getIdSanBong()));

                    Slot slot = slotService.findSlotByIdGioHoatDongAndIdSanBong(gioHoatDong.getId(), yeuCauThue.getIdSanBong());
                    slotService.deleteSlot(slot);
                    yeucauThueService.deleteYeuCauThue(yeuCauThue);

                }else{
                    yeuCauThue.setTinhTrang(status);
                    yeucauThueService.saveYeuCauThue(yeuCauThue);
                }
            }
        }
       return "redirect:/admin/tinhTrangSan";
    }
}