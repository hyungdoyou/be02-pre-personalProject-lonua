package com.example.lonua.user.service;

import com.example.lonua.config.BaseRes;
import com.example.lonua.exception.ErrorCode;
import com.example.lonua.exception.exception.UserException;
import com.example.lonua.grade.model.entity.Grade;
import com.example.lonua.orders.model.entity.Orders;
import com.example.lonua.product.model.response.GetReadOrdersProductRes;
import com.example.lonua.user.config.utils.JwtUtils;
import com.example.lonua.user.model.entity.request.PostUserLoginReq;
import com.example.lonua.user.model.entity.request.PostSignUpReq;
import com.example.lonua.user.model.entity.User;
import com.example.lonua.user.model.entity.response.*;
import com.example.lonua.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService{

    @Value("${jwt.secret-key}")
    private String secretKey;
    @Value("${jwt.token.expired-time-ms}")
    private Long expiredTimeMs;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender emailSender;
    private final EmailVerifyService emailVerifyService;


    // 회원가입
    public BaseRes signup(PostSignUpReq postSignUpReq) {
        // 중복된 ID에 대한 예외처리 추가
        Optional<User> result = userRepository.findByUserEmail(postSignUpReq.getUserEmail());

        if(result.isPresent()) {
            throw new UserException(ErrorCode.DUPLICATED_USER, String.format("Email is %s", postSignUpReq.getUserEmail()));
        }

        User user = User.builder()
                .grade(Grade.builder()
                        .gradeIdx(1) // 처음 회원 가입 시 회원등급은 반드시 1번 등급(실버)으로 가입
                        .build())
                .userPassword(passwordEncoder.encode(postSignUpReq.getUserPassword()))
                .name(postSignUpReq.getUserName())
                .userBirth(postSignUpReq.getUserBirth())
                .userGender(postSignUpReq.getUserGender())
                .userPhoneNumber(postSignUpReq.getUserPhoneNumber())
                .userEmail(postSignUpReq.getUserEmail())
                .userAddr(postSignUpReq.getUserAddr())
                .preferStyle(postSignUpReq.getPreferStyle())
                .upperType(postSignUpReq.getUpperType())
                .lowerType(postSignUpReq.getLowerType())
                .userMileage(0)
                .authority("ROLE_USER")
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")))
                .updatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")))
                .status(false)
                .build();

        userRepository.save(user);

        BaseRes baseRes = BaseRes.builder()
                .code(200)
                .isSuccess(true)
                .message("회원가입에 성공하였습니다.")
                .result(PostSignupRes.builder()
                        .userEmail(user.getUserEmail())
                        .userName(user.getName())
                        .build())
                .build();

        return baseRes;
    }

    public List<GetListUserRes> list() {
        List<User> result = userRepository.findAll();

        List<GetListUserRes> getListUserResList = new ArrayList<>();
        for(User user : result) {
            List<GetUserOrdersRes> getUserOrdersResList = new ArrayList<>();
            List<Orders> ordersList = user.getOrdersList();

            for(Orders orders : ordersList) {
                GetUserOrdersRes getUserOrdersRes = GetUserOrdersRes.builder()
                        .ordersIdx(orders.getOrdersIdx())
                        .getReadOrdersProductRes(GetReadOrdersProductRes.builder()
                                .productIdx(orders.getProduct().getProductIdx())
                                .brandName(orders.getProduct().getBrand().getBrandName())
                                .productName(orders.getProduct().getProductName())
                                .price(orders.getProduct().getPrice())
                                .build())
                        .build();

                getUserOrdersResList.add(getUserOrdersRes);
            }

            GetListUserRes getListUserRes = GetListUserRes.builder()
                    .userIdx(user.getUserIdx())
                    .userEmail(user.getUserEmail())
                    .userName(user.getUsername())
                    .userBirth(user.getUserBirth())
                    .userGender(user.getUserGender())
                    .userPhoneNumber(user.getUserPhoneNumber())
                    .preferStyle(user.getPreferStyle())
                    .upperType(user.getUpperType())
                    .lowerType(user.getLowerType())
                    .gradeType(user.getGrade().getGradeType())
                    .getUserOrdersResList(getUserOrdersResList)
                    .build();

            getListUserResList.add(getListUserRes);
        }

        return getListUserResList;
    }

    public GetListUserRes read(String email) {
        Optional<User> result = userRepository.findByUserEmail(email);
        User user = result.get();

        List<GetUserOrdersRes> getUserOrdersResList = new ArrayList<>();
        List<Orders> ordersList = user.getOrdersList();

        for(Orders orders : ordersList) {
            GetUserOrdersRes getUserOrdersRes = GetUserOrdersRes.builder()
                    .ordersIdx(orders.getOrdersIdx())
                    .getReadOrdersProductRes(GetReadOrdersProductRes.builder()
                            .productIdx(orders.getProduct().getProductIdx())
                            .brandName(orders.getProduct().getBrand().getBrandName())
                            .productName(orders.getProduct().getProductName())
                            .price(orders.getProduct().getPrice())
                            .build())
                    .build();
            getUserOrdersResList.add(getUserOrdersRes);
        }

        GetListUserRes getListUserRes = GetListUserRes.builder()
                .userIdx(user.getUserIdx())
                .userEmail(user.getUserEmail())
                .userName(user.getUsername())
                .userBirth(user.getUserBirth())
                .userGender(user.getUserGender())
                .userPhoneNumber(user.getUserPhoneNumber())
                .userAddr(user.getUserAddr())
                .preferStyle(user.getPreferStyle())
                .upperType(user.getUpperType())
                .lowerType(user.getLowerType())
                .gradeType(user.getGrade().getGradeType())
                .getUserOrdersResList(getUserOrdersResList)
                .build();

        return getListUserRes;
    }

    // 회원 로그인
    public PostUserLoginRes login(PostUserLoginReq postUserLoginReq) {
        Optional<User> result = userRepository.findByUserEmail(postUserLoginReq.getEmail());
        if (result.isPresent()) {
            User user = result.get();
            if (passwordEncoder.matches(postUserLoginReq.getPassword(), user.getPassword())) {
                PostUserLoginRes postUserLoginRes = PostUserLoginRes.builder()
                        .token(JwtUtils.generateAccessToken(user, secretKey, expiredTimeMs))
                        .build();
                return postUserLoginRes;
            } else {
                return null;
            }
        }
        return null;
    }

    // 인증메일 발송
    public void sendEmail(PostSignUpReq postSignUpReq) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(postSignUpReq.getUserEmail());
        message.setSubject("[LONUA] 회원가입을 완료하기 위해서 이메일 인증을 진행해 주세요"); // 메일 제목

        String uuid = UUID.randomUUID().toString();
        message.setText("http://localhost:8080/user/verify?email=" + postSignUpReq.getUserEmail() + "&uuid=" + uuid);    // 메일 내용

        emailSender.send(message);
        emailVerifyService.create(postSignUpReq.getUserEmail(), uuid);
    }

    // 메일 인증 완료 후 회원 상태 수정
    public BaseRes updateStatus(String email) {
        Optional<User> result = userRepository.findByUserEmail(email);
        if(result.isPresent()) {
            User user = result.get();
            user.setStatus(true);
            userRepository.save(user);

            return BaseRes.builder()
                    .code(200)
                    .isSuccess(true)
                    .message("메일 인증에 성공하였습니다.")
                    .result(GetEmailVerifyRes.builder()
                            .email(user.getUserEmail())
                            .status(user.getStatus())
                            .build())
                    .build();
        } else {
            return null;
        }
    }

    // 카카오 회원가입
    public void kakaoSignup(String nickName) {

        User user = User.builder()
                .grade(Grade.builder()
                        .gradeIdx(1) // 처음 회원 가입 시 회원등급은 반드시 1번 등급(실버)으로 가입
                        .build())
                .userEmail(nickName)
                .userPassword(passwordEncoder.encode("kako"))
                .name(nickName)
                .userBirth("0000-00-00")
                .userGender("K")
                .userPhoneNumber("kakao")
                .userAddr("카카오 로그인")
                .preferStyle("캐쥬얼")
                .upperType(1)
                .lowerType(1)
                .userMileage(0)
                .authority("ROLE_USER")
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")))
                .updatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")))
                .status(false)
                .build();

        userRepository.save(user);
    }

    // 회원 이메일 검증
    public User getUserEmail(String email) {
        Optional<User> result = userRepository.findByUserEmail(email);
        if (result.isPresent()) {
            return result.get();
        }
        return null;
    }
}
