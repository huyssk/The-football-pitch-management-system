Create database SPM

-- Tạo bảng Role
CREATE TABLE Role (
    id INT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL
);

-- Tạo bảng User
CREATE TABLE User (
    id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(50) NOT NULL,
    password VARCHAR(50) NOT NULL,
    email VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20),
    role_id INT,
    FOREIGN KEY (role_id) REFERENCES Role(id)
);

-- Tạo bảng LoaiSan
CREATE TABLE LoaiSan (
    id INT PRIMARY KEY AUTO_INCREMENT,
    ten_loai_san VARCHAR(50) NOT NULL
);

CREATE TABLE SanBong (
    id INT PRIMARY KEY AUTO_INCREMENT,
    ten_san_bong VARCHAR(50) NOT NULL,
    dia_diem VARCHAR(100) NOT NULL,
    tien_san INT NOT NULL,
    hinh_anh VARCHAR(200),
    mo_ta_san TEXT,
    id_loai_san INT,
	ngay_da DATE,
    FOREIGN KEY (id_loai_san) REFERENCES LoaiSan(id)
);


-- Tạo bảng YeuCauThue
CREATE TABLE YeuCauThue (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    id_san_bong INT NOT NULL,
    ngay_tao DATE NOT NULL,
    FOREIGN KEY (user_id) REFERENCES User(id),
    FOREIGN KEY (id_san_bong) REFERENCES SanBong(id)
);

-- Tạo bảng YeuCauHuy
CREATE TABLE YeuCauHuy (
    id INT PRIMARY KEY AUTO_INCREMENT,
    id_yeu_cau_thue INT NOT NULL,
    ghi_chu TEXT,
    ngay_tao DATE NOT NULL,
    FOREIGN KEY (id_yeu_cau_thue) REFERENCES YeuCauThue(id)
);

-- Tạo bảng Slot
CREATE TABLE Slot (
    id_san_bong INT NOT NULL,
    id_gio_hoat_dong INT NOT NULL,
    FOREIGN KEY (id_san_bong) REFERENCES SanBong(id),
    FOREIGN KEY (id_gio_hoat_dong) REFERENCES GioHoatDong(id)
);

-- Tạo bảng GioHoatDong
CREATE TABLE GioHoatDong (
    id INT PRIMARY KEY AUTO_INCREMENT,
    trang_thai BOOLEAN NOT NULL,
    gio_da TIME NOT NULL
);

-- Tạo bảng BinhLuan
CREATE TABLE BinhLuan (
    id INT PRIMARY KEY AUTO_INCREMENT,
    id_san INT NOT NULL,
    user_id INT NOT NULL,
    create_date DATE NOT NULL,
    no_dung TEXT NOT NULL,
    FOREIGN KEY (id_san) REFERENCES SanBong(id),
    FOREIGN KEY (user_id) REFERENCES User(id)
);

CREATE TABLE YeuThich (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    id_san_bong INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES User(id),
    FOREIGN KEY (id_san_bong) REFERENCES SanBong(id)
);

CREATE TABLE HoaDon (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    id_yeu_cau_thue INT NOT NULL,
    create_date DATE NOT NULL,
    ngay_da DATE NOT NULL,
    gio_da DATE NOT NULL,
    trang_thai VARCHAR(50) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES User(id),
    FOREIGN KEY (id_yeu_cau_thue) REFERENCES YeuCauThue(id)
);