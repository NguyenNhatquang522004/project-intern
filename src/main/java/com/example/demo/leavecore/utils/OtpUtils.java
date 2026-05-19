package com.example.demo.leavecore.utils;

import java.security.SecureRandom;

import lombok.experimental.UtilityClass;

@UtilityClass
public class OtpUtils {
    // Sử dụng SecureRandom làm cơ chế sinh số ngẫu nhiên bảo mật cao
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String DIGITS = "0123456789";

    /**
     * Tạo mã OTP dạng số với độ dài linh hoạt (Best practice là 6 số)
     *
     * @param length Độ dài của mã OTP (Thường từ 4 đến 8)
     * @return Chuỗi OTP hoàn toàn ngẫu nhiên và bảo mật
     */
    public String generateNumericOtp(int length) {
        if (length < 4 || length > 8) {
            throw new IllegalArgumentException(
                    "Độ dài OTP phải nằm trong khoảng từ 4 đến 8 ký tự để đảm bảo UX và Bảo mật.");
        }

        StringBuilder otp = new StringBuilder(length);

        // Vòng lặp đảm bảo tính phân phối đồng đều (Uniform Distribution) cho từng vị
        // trí số
        for (int i = 0; i < length; i++) {
            int randomIndex = SECURE_RANDOM.nextInt(DIGITS.length());
            otp.append(DIGITS.charAt(randomIndex));
        }

        return otp.toString();
    }
}
