package com.example.demo.common.share.email;

import com.example.demo.common.Dto.EmailRequest;

public interface IEmail {
    void sendEmail(EmailRequest request);
}
