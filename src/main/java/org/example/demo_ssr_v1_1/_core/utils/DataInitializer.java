//package org.example.demo_ssr_v1_1._core.utils;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.example.demo_ssr_v1_1.board.Board;
//import org.example.demo_ssr_v1_1.board.BoardRepository;
//import org.example.demo_ssr_v1_1.reply.Reply;
//import org.example.demo_ssr_v1_1.reply.ReplyRepository;
//import org.example.demo_ssr_v1_1.user.*;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * 애플리케이션 시작 시 초기 데이터를 생성하는 클래스
// *
// * CommandLineRunner란?
// * - Spring Boot가 제공하는 인터페이스입니다.
// * - 애플리케이션이 완전히 시작된 후 자동으로 실행됩니다.
// * - run() 메서드에 초기화 로직을 작성합니다.
// *
// * 왜 사용하나요?
// * - data.sql 파일에 BCrypt 해시값을 직접 작성하는 것은 개발자가 하기 어렵습니다.
// * - Java 코드로 PasswordEncoder를 사용하여 비밀번호를 암호화할 수 있습니다.
// * - 더 유연하고 관리하기 쉬운 초기 데이터 생성이 가능합니다.
// *
// * 실행 시점:
// * 1. Spring Boot 애플리케이션 시작
// * 2. 모든 빈(Bean)이 생성되고 주입 완료
// * 3. CommandLineRunner의 run() 메서드 자동 실행
// * 4. 초기 데이터 생성
// *
// * @Component: Spring이 이 클래스를 빈으로 등록 (CommandLineRunner로 인식)
// * @RequiredArgsConstructor: final 필드에 대한 생성자 자동 생성 (DI)
// * @Slf4j: 로깅을 위한 어노테이션 (log 변수 자동 생성)
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class DataInitializer implements CommandLineRunner {
//
//    // 비밀번호 암호화를 위한 PasswordEncoder 주입
//    private final PasswordEncoder passwordEncoder;
//
//    // 사용자 데이터 저장을 위한 Repository 주입
//    private final UserRepository userRepository;
//
//    // 게시글 데이터 저장을 위한 Repository 주입
//    private final BoardRepository boardRepository;
//
//    // 댓글 데이터 저장을 위한 Repository 주입
//    private final ReplyRepository replyRepository;
//
//    /**
//     * 애플리케이션 시작 시 실행되는 메서드
//     *
//     * 동작 흐름:
//     * 1. 기존 데이터 확인 (중복 방지)
//     * 2. 초기 사용자 생성 (비밀번호 암호화 포함)
//     * 3. 초기 게시글 생성
//     * 4. 초기 댓글 생성
//     *
//     * @param args 커맨드 라인 인자 (사용하지 않음)
//     * @throws Exception 예외 발생 시
//     */
//    @Override
//    @Transactional
//    public void run(String... args) throws Exception {
//        // 기존 데이터가 있는지 확인 (중복 방지)
//        // admin 사용자가 이미 존재하면 초기 데이터를 생성하지 않음
//        if (userRepository.findByUsername("admin").isPresent()) {
//            log.info("초기 데이터가 이미 존재합니다. 데이터 생성을 건너뜁니다.");
//            return;
//        }
//
//        log.info("=== 초기 데이터 생성 시작 ===");
//
//        // 1. 초기 사용자 생성
//        List<User> users = createInitialUsers();
//        log.info("초기 사용자 {}명 생성 완료", users.size());
//
//        // 2. 초기 게시글 생성
//        List<Board> boards = createInitialBoards(users);
//        log.info("초기 게시글 {}개 생성 완료", boards.size());
//
//        // 3. 초기 댓글 생성
//        createInitialReplies(users, boards);
//        log.info("초기 댓글 생성 완료");
//
//        log.info("=== 초기 데이터 생성 완료 ===");
//    }
//
//    /**
//     * 초기 사용자 데이터 생성
//     *
//     * 생성되는 사용자:
//     * - admin: 관리자 (ADMIN + USER 권한)
//     * - ssar, cos, hong, kim: 일반 사용자 (USER 권한)
//     *
//     * 비밀번호:
//     * - 모든 사용자의 비밀번호는 "1234"입니다.
//     * - BCrypt로 암호화하여 저장합니다.
//     *
//     * @return 생성된 사용자 리스트
//     */
//    private List<User> createInitialUsers() {
//        List<User> users = new ArrayList<>();
//
//        // 공통 비밀번호를 BCrypt로 암호화
//        // 모든 사용자의 비밀번호는 "1234"입니다.
//        String encodedPassword = passwordEncoder.encode("1234");
//        log.debug("비밀번호 암호화 완료: {}", encodedPassword);
//
//        // 1. admin 사용자 생성 (관리자)
//        User admin = User.builder()
//                .username("admin")
//                .password(encodedPassword) // 암호화된 비밀번호 저장
//                .email("admin@blog.com")
//                .provider(OAuthProvider.LOCAL) // 일반 회원가입
//                .build();
//        // 관리자 권한 추가 (ADMIN + USER)
//        admin.addRole(Role.ADMIN);
//        admin.addRole(Role.USER);
//        users.add(userRepository.save(admin));
//        log.debug("관리자 사용자 생성: {}", admin.getUsername());
//
//        // 2. 일반 사용자들 생성 (ssar, cos, hong, kim)
//        String[] usernames = {"ssar", "cos", "hong", "kim"};
//        String[] emails = {"ssar@nate.com", "cos@gmail.com", "hong@naver.com", "kim@daum.net"};
//
//        for (int i = 0; i < usernames.length; i++) {
//            User user = User.builder()
//                    .username(usernames[i])
//                    .password(encodedPassword) // 암호화된 비밀번호 저장 (모두 같은 해시값 사용)
//                    .email(emails[i])
//                    .provider(OAuthProvider.LOCAL) // 일반 회원가입
//                    .build();
//            // 기본 권한 추가 (USER만)
//            user.addRole(Role.USER);
//            users.add(userRepository.save(user));
//            log.debug("일반 사용자 생성: {}", user.getUsername());
//        }
//
//        return users;
//    }
//
//    /**
//     * 초기 게시글 데이터 생성
//     *
//     * 생성되는 게시글:
//     * - admin: 3개 게시글
//     * - ssar: 3개 게시글
//     * - cos: 2개 게시글
//     * - hong: 1개 게시글
//     * - kim: 1개 게시글
//     *
//     * @param users 생성된 사용자 리스트
//     * @return 생성된 게시글 리스트
//     */
//    private List<Board> createInitialBoards(List<User> users) {
//        List<Board> boards = new ArrayList<>();
//
//        // admin 사용자가 작성한 게시글 (3개)
//        boards.add(boardRepository.save(Board.builder()
//                .title("블로그 개설을 환영합니다!")
//                .content("안녕하세요! 새로운 블로그가 오픈했습니다. 많은 관심과 참여 부탁드립니다.")
//                .user(users.get(0)) // admin
//                .build()));
//
//        boards.add(boardRepository.save(Board.builder()
//                .title("공지사항: 이용수칙 안내")
//                .content("블로그 이용 시 지켜야 할 기본적인 수칙들을 안내드립니다. 건전한 소통 문화를 만들어가요.")
//                .user(users.get(0)) // admin
//                .build()));
//
//        boards.add(boardRepository.save(Board.builder()
//                .title("업데이트 소식")
//                .content("새로운 기능들이 추가되었습니다. 댓글 기능과 좋아요 기능을 곧 만나보실 수 있습니다.")
//                .user(users.get(0)) // admin
//                .build()));
//
//        // ssar 사용자가 작성한 게시글 (3개)
//        boards.add(boardRepository.save(Board.builder()
//                .title("Spring Boot 학습 후기")
//                .content("Spring Boot를 처음 배우면서 느낀 점들을 공유합니다. JPA가 정말 편리하네요!")
//                .user(users.get(1)) // ssar
//                .build()));
//
//        boards.add(boardRepository.save(Board.builder()
//                .title("JPA 연관관계 정리노트")
//                .content("오늘 배운 @ManyToOne, @OneToMany 연관관계에 대해 정리해봤습니다. 헷갈리는 부분이 많아요.")
//                .user(users.get(1)) // ssar
//                .build()));
//
//        boards.add(boardRepository.save(Board.builder()
//                .title("코딩테스트 문제 추천")
//                .content("백준과 프로그래머스에서 풀어볼 만한 문제들을 추천드립니다. 알고리즘 공부 화이팅!")
//                .user(users.get(1)) // ssar
//                .build()));
//
//        // cos 사용자가 작성한 게시글 (2개)
//        boards.add(boardRepository.save(Board.builder()
//                .title("React vs Vue 비교")
//                .content("프론트엔드 프레임워크 선택에 고민이 많았는데, 각각의 장단점을 비교해봤습니다.")
//                .user(users.get(2)) // cos
//                .build()));
//
//        boards.add(boardRepository.save(Board.builder()
//                .title("개발자 취업 팁 공유")
//                .content("신입 개발자로 취업하면서 도움이 되었던 팁들을 공유합니다. 포트폴리오가 중요해요!")
//                .user(users.get(2)) // cos
//                .build()));
//
//        // hong 사용자가 작성한 게시글 (1개)
//        boards.add(boardRepository.save(Board.builder()
//                .title("첫 번째 게시글입니다")
//                .content("안녕하세요! 블로그에 처음 글을 올려봅니다. 앞으로 자주 소통해요~")
//                .user(users.get(3)) // hong
//                .build()));
//
//        // kim 사용자가 작성한 게시글 (1개)
//        boards.add(boardRepository.save(Board.builder()
//                .title("맛집 추천 - 강남역 근처")
//                .content("강남역 근처에서 점심 먹기 좋은 맛집들을 추천드립니다. 가성비도 좋아요!")
//                .user(users.get(4)) // kim
//                .build()));
//
//        return boards;
//    }
//
//    /**
//     * 초기 댓글 데이터 생성
//     *
//     * 각 게시글에 댓글들을 추가합니다.
//     *
//     * @param users 생성된 사용자 리스트
//     * @param boards 생성된 게시글 리스트
//     */
//    private void createInitialReplies(List<User> users, List<Board> boards) {
//        // 1번 게시글 (admin의 '블로그 개설을 환영합니다!')에 대한 댓글
//        replyRepository.save(Reply.builder()
//                .comment("축하드립니다! 새로운 블로그 기대되네요.")
//                .board(boards.get(0))
//                .user(users.get(1)) // ssar
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("관리자님 수고 많으셨습니다. 좋은 컨텐츠 부탁드려요!")
//                .board(boards.get(0))
//                .user(users.get(2)) // cos
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("드디어 오픈했군요. 자주 방문하겠습니다.")
//                .board(boards.get(0))
//                .user(users.get(3)) // hong
//                .build());
//
//        // 2번 게시글 (admin의 '공지사항: 이용수칙 안내')에 대한 댓글
//        replyRepository.save(Reply.builder()
//                .comment("이용수칙 잘 읽어보겠습니다.")
//                .board(boards.get(1))
//                .user(users.get(1)) // ssar
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("건전한 소통 문화 만들기에 동참하겠습니다!")
//                .board(boards.get(1))
//                .user(users.get(4)) // kim
//                .build());
//
//        // 3번 게시글 (admin의 '업데이트 소식')에 대한 댓글
//        replyRepository.save(Reply.builder()
//                .comment("댓글 기능 추가 감사합니다!")
//                .board(boards.get(2))
//                .user(users.get(1)) // ssar
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("좋아요 기능도 빨리 나왔으면 좋겠어요.")
//                .board(boards.get(2))
//                .user(users.get(2)) // cos
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("업데이트 소식 감사합니다. 잘 사용하겠습니다.")
//                .board(boards.get(2))
//                .user(users.get(3)) // hong
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("새로운 기능들이 기대됩니다.")
//                .board(boards.get(2))
//                .user(users.get(4)) // kim
//                .build());
//
//        // 4번 게시글 (ssar의 'Spring Boot 학습 후기')에 대한 댓글
//        replyRepository.save(Reply.builder()
//                .comment("저도 Spring Boot 공부 중인데 많은 도움이 되었습니다!")
//                .board(boards.get(3))
//                .user(users.get(0)) // admin
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("JPA 정말 편리하죠. 처음엔 어려웠지만 익숙해지면 좋더라구요.")
//                .board(boards.get(3))
//                .user(users.get(2)) // cos
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("학습 후기 공유해주셔서 감사합니다.")
//                .board(boards.get(3))
//                .user(users.get(4)) // kim
//                .build());
//
//        // 5번 게시글 (ssar의 'JPA 연관관계 정리노트')에 대한 댓글
//        replyRepository.save(Reply.builder()
//                .comment("연관관계 정말 헷갈리죠 ㅠㅠ 정리 잘 해주셨네요!")
//                .board(boards.get(4))
//                .user(users.get(0)) // admin
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("@ManyToOne 부분이 특히 어려웠는데 덕분에 이해했습니다.")
//                .board(boards.get(4))
//                .user(users.get(2)) // cos
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("저도 공부하면서 참고하겠습니다.")
//                .board(boards.get(4))
//                .user(users.get(3)) // hong
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("양방향 매핑 부분도 추가로 설명해주시면 좋을 것 같아요.")
//                .board(boards.get(4))
//                .user(users.get(4)) // kim
//                .build());
//
//        // 6번 게시글 (ssar의 '코딩테스트 문제 추천')에 대한 댓글
//        replyRepository.save(Reply.builder()
//                .comment("문제 추천 감사합니다! 바로 풀어보겠습니다.")
//                .board(boards.get(5))
//                .user(users.get(0)) // admin
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("백준 문제 중에서 어떤 걸 먼저 풀어보면 좋을까요?")
//                .board(boards.get(5))
//                .user(users.get(3)) // hong
//                .build());
//
//        // 7번 게시글 (cos의 'React vs Vue 비교')에 대한 댓글
//        replyRepository.save(Reply.builder()
//                .comment("React 쪽이 더 인기가 많은 것 같긴 하네요.")
//                .board(boards.get(6))
//                .user(users.get(0)) // admin
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("Vue가 더 배우기 쉽다고 들었는데 실제로는 어떤가요?")
//                .board(boards.get(6))
//                .user(users.get(1)) // ssar
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("둘 다 써봤는데 각각 장단점이 있는 것 같아요.")
//                .board(boards.get(6))
//                .user(users.get(3)) // hong
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("프로젝트 성격에 따라 선택하면 될 것 같습니다.")
//                .board(boards.get(6))
//                .user(users.get(4)) // kim
//                .build());
//
//        // 8번 게시글 (cos의 '개발자 취업 팁 공유')에 대한 댓글
//        replyRepository.save(Reply.builder()
//                .comment("취업 준비 중인데 정말 유용한 정보네요!")
//                .board(boards.get(7))
//                .user(users.get(1)) // ssar
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("포트폴리오 작성 방법도 자세히 알려주세요.")
//                .board(boards.get(7))
//                .user(users.get(3)) // hong
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("면접 준비는 어떻게 하셨나요?")
//                .board(boards.get(7))
//                .user(users.get(4)) // kim
//                .build());
//
//        // 9번 게시글 (hong의 '첫 번째 게시글입니다')에 대한 댓글
//        replyRepository.save(Reply.builder()
//                .comment("첫 게시글 축하드려요! 환영합니다.")
//                .board(boards.get(8))
//                .user(users.get(0)) // admin
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("앞으로 자주 소통해요~")
//                .board(boards.get(8))
//                .user(users.get(1)) // ssar
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("좋은 게시글 기대하겠습니다!")
//                .board(boards.get(8))
//                .user(users.get(2)) // cos
//                .build());
//
//        // 10번 게시글 (kim의 '맛집 추천 - 강남역 근처')에 대한 댓글
//        replyRepository.save(Reply.builder()
//                .comment("강남역 자주 가는데 맛집 정보 감사해요!")
//                .board(boards.get(9))
//                .user(users.get(0)) // admin
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("가성비 좋은 곳 추천해주셔서 고마워요.")
//                .board(boards.get(9))
//                .user(users.get(1)) // ssar
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("저도 가봐야겠네요. 위치 정보도 알려주세요.")
//                .board(boards.get(9))
//                .user(users.get(2)) // cos
//                .build());
//
//        replyRepository.save(Reply.builder()
//                .comment("점심 메뉴 추천도 해주시면 좋을 것 같아요.")
//                .board(boards.get(9))
//                .user(users.get(3)) // hong
//                .build());
//    }
//}
//
